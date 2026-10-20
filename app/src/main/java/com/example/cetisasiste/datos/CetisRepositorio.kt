package com.example.cetisasiste.datos

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.Collator
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * ÚNICO lugar de la app que habla con Firebase (Authentication y Firestore).
 *
 * Las pantallas no consultan la base: le piden los datos a este objeto.
 * Así, si cambia la base de datos, solo se toca este archivo.
 *
 * Todas las funciones son `suspend`: se llaman desde una corrutina
 * (LaunchedEffect / produceState en Compose) y no congelan la pantalla.
 */

/** Lo que la app sabe de la persona que inició sesión. */
data class Sesion(
    val uid: String,
    val usuario: UsuarioDoc,
)

/** Resultado de intentar iniciar sesión. */
sealed interface ResultadoLogin {
    data class Correcto(val sesion: Sesion) : ResultadoLogin
    data class Error(val mensaje: MensajeLogin) : ResultadoLogin
}

/** Errores posibles al iniciar sesión (el texto de cada uno está en strings.xml). */
enum class MensajeLogin {
    CamposVacios,
    NoControlInvalido,
    DatosIncorrectos,
    CuentaNoRegistrada,
    CuentaDesactivada,
    SinConexion,
    DemasiadosIntentos,
    Desconocido,
}

/** Resultado de cambiar la contraseña (el texto de cada uno está en strings_comun.xml). */
enum class ResultadoContrasena {
    Lista,
    ActualIncorrecta,
    Debil,
    SinConexion,
    DemasiadosIntentos,
    Desconocido,
}

object CetisRepositorio {

    /**
     * Dominio de los correos internos. La persona solo escribe su No. de control o usuario;
     * la app arma el correo "noControl@cetis61.mx" para Firebase Authentication.
     * Debe ser el mismo con el que Control Escolar crea las cuentas.
     */
    const val DOMINIO_CORREO = "cetis61.mx"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** "00000000000000" → "00000000000000@cetis61.mx". Si ya trae @, se deja igual. */
    fun correoDeAcceso(usuario: String): String {
        val limpio = usuario.trim().lowercase(Locale.ROOT)
        return if ("@" in limpio) limpio else "$limpio@$DOMINIO_CORREO"
    }

    // ---------- Sesión ----------

    suspend fun iniciarSesion(usuario: String, contrasena: String): ResultadoLogin {
        if (usuario.isBlank() || contrasena.isEmpty()) {
            return ResultadoLogin.Error(MensajeLogin.CamposVacios)
        }
        // Si escribe solo números es un alumno: su No. de control debe tener 14 dígitos.
        val limpio = usuario.trim()
        if (limpio.all { it in '0'..'9' } && !NoControl.esValido(limpio)) {
            return ResultadoLogin.Error(MensajeLogin.NoControlInvalido)
        }
        return try {
            val resultado = auth.signInWithEmailAndPassword(correoDeAcceso(usuario), contrasena).await()
            val uid = resultado.user?.uid ?: return ResultadoLogin.Error(MensajeLogin.Desconocido)
            cargarSesion(uid)
        } catch (e: FirebaseAuthException) {
            ResultadoLogin.Error(MensajeLogin.DatosIncorrectos)
        } catch (e: FirebaseNetworkException) {
            ResultadoLogin.Error(MensajeLogin.SinConexion)
        } catch (e: FirebaseTooManyRequestsException) {
            ResultadoLogin.Error(MensajeLogin.DemasiadosIntentos)
        } catch (e: FirebaseFirestoreException) {
            auth.signOut()
            ResultadoLogin.Error(
                if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) MensajeLogin.SinConexion
                else MensajeLogin.Desconocido
            )
        }
    }

    /** Si la app ya tenía una sesión abierta (se cerró sin "Cerrar sesión"), la recupera. */
    suspend fun sesionGuardada(): Sesion? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            (cargarSesion(uid) as? ResultadoLogin.Correcto)?.sesion
        } catch (e: FirebaseFirestoreException) {
            null
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Lee usuarios/{uid}. Si no existe o está desactivada, cierra la sesión de Authentication:
     * tener contraseña no basta, Control Escolar también debe haber creado su ficha.
     */
    private suspend fun cargarSesion(uid: String): ResultadoLogin {
        val documento = db.collection(Colecciones.USUARIOS).document(uid).get().await()
        val usuario = documento.toObject(UsuarioDoc::class.java)
        return when {
            usuario == null -> {
                auth.signOut()
                ResultadoLogin.Error(MensajeLogin.CuentaNoRegistrada)
            }
            !usuario.activo -> {
                auth.signOut()
                ResultadoLogin.Error(MensajeLogin.CuentaDesactivada)
            }
            else -> ResultadoLogin.Correcto(Sesion(uid, usuario))
        }
    }

    // ---------- Escuchar en tiempo real ----------
    // Estas funciones no leen una sola vez: avisan cada vez que el documento cambia en
    // Firestore (por ejemplo, si Control Escolar corrige un nombre en la consola), y la
    // pantalla se actualiza sola. Dejan de escuchar cuando la pantalla ya no las usa.

    /** usuarios/{uid}: null si se borró; falla si no hay permiso (cuenta desactivada). */
    fun escucharUsuario(uid: String): Flow<Result<UsuarioDoc?>> =
        escuchar(Colecciones.USUARIOS, uid, UsuarioDoc::class.java)

    /** alumnos/{noControl}: null si no existe. */
    fun escucharAlumno(noControl: String): Flow<Result<AlumnoDoc?>> =
        if (noControl.isBlank()) flowOf(Result.success(null))
        else escuchar(Colecciones.ALUMNOS, noControl, AlumnoDoc::class.java)

    /** grupos/{grupoId}: de aquí sale el nombre del grupo que se muestra ("102"). */
    fun escucharGrupo(grupoId: String): Flow<Result<GrupoDoc?>> =
        if (grupoId.isBlank()) flowOf(Result.success(null))
        else escuchar(Colecciones.GRUPOS, grupoId, GrupoDoc::class.java)

    /** No. de control del alumno que ve la sesión: la propia (alumno) o la del primer hijo (padre). */
    fun noControlDeLaSesion(sesion: Sesion): String = when (sesion.usuario.rol) {
        Rol.ALUMNO -> sesion.usuario.noControl
        Rol.PADRE -> sesion.usuario.hijos.firstOrNull().orEmpty()
        else -> ""
    }

    private fun <T : Any> escuchar(coleccion: String, id: String, clase: Class<T>): Flow<Result<T?>> = callbackFlow {
        val registro = db.collection(coleccion).document(id).addSnapshotListener { documento, error ->
            if (error != null) {
                trySend(Result.failure(error))
                close()
            } else {
                trySend(Result.success(documento?.toObject(clase)))
            }
        }
        awaitClose { registro.remove() }
    }

    /** Escucha una consulta: avisa con la lista completa (id y documento) cada vez que cambia. */
    private fun <T : Any> escucharLista(consulta: Query, clase: Class<T>): Flow<Result<List<Pair<String, T>>>> = callbackFlow {
        val registro = consulta.addSnapshotListener { lista, error ->
            if (error != null) {
                trySend(Result.failure(error))
                close()
            } else {
                trySend(Result.success(lista?.documents.orEmpty().mapNotNull { doc -> doc.toObject(clase)?.let { doc.id to it } }))
            }
        }
        awaitClose { registro.remove() }
    }

    /**
     * Como [escucharLista], pero dentro del semestre en curso: primero lee
     * configuracion/plantel.periodoActual y luego escucha la consulta que arma [consulta] con
     * periodos/{periodoActual}. Sin semestre en curso, avisa con una lista vacía.
     */
    private fun <T : Any> escucharEnElSemestre(
        clase: Class<T>,
        consulta: (periodo: DocumentReference) -> Query,
    ): Flow<Result<List<Pair<String, T>>>> = flow {
        val periodo = try {
            periodoActual()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Result.failure(e))
            return@flow
        }
        if (periodo.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        emitAll(escucharLista(consulta(db.collection(Colecciones.PERIODOS).document(periodo)), clase))
    }

    // ---------- Consultas ----------

    /** alumnos/{noControl}, o null si no existe o no hay permiso. */
    suspend fun alumno(noControl: String): AlumnoDoc? {
        if (noControl.isBlank()) return null
        return db.collection(Colecciones.ALUMNOS).document(noControl).get().await()
            .toObject(AlumnoDoc::class.java)
    }

    /** Perfil del rol (roles/{rol}), o null si no existe. */
    suspend fun rol(rol: String): RolDoc? {
        if (rol.isBlank()) return null
        return db.collection(Colecciones.ROLES).document(rol).get().await()
            .toObject(RolDoc::class.java)
    }

    /** Clases de un grupo en un día ("lunes" … "viernes"). Lista vacía si no hay horario. */
    suspend fun horario(grupoId: String, dia: String): List<ClaseDoc> {
        if (grupoId.isBlank()) return emptyList()
        return db.collection(Colecciones.GRUPOS).document(grupoId)
            .collection(Colecciones.HORARIO).document(dia)
            .get().await()
            .toObject(HorarioDoc::class.java)?.clases.orEmpty()
    }

    /** Anuncios dirigidos al rol, del más nuevo al más viejo. */
    suspend fun anuncios(rol: String, limite: Long = 50): List<AnuncioDoc> =
        db.collection(Colecciones.ANUNCIOS)
            .whereArrayContains("dirigidoA", rol)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(limite)
            .get().await()
            .toObjects(AnuncioDoc::class.java)

    /** Eventos que empiezan entre [desde] (incluido) y [hasta] (excluido), en orden. */
    suspend fun eventos(desde: Timestamp, hasta: Timestamp): List<EventoDoc> =
        db.collection(Colecciones.EVENTOS)
            .whereGreaterThanOrEqualTo("inicio", desde)
            .whereLessThan("inicio", hasta)
            .orderBy("inicio")
            .get().await()
            .toObjects(EventoDoc::class.java)

    /**
     * Ajustes del plantel (configuracion/plantel). Se guardan [VIGENCIA_CONFIGURACION_MS] en
     * memoria: el prefecto los necesita en cada escaneo y así no se leen cada vez (cada lectura
     * cuenta en el límite diario del plan Spark).
     */
    suspend fun configuracion(): ConfiguracionDoc {
        val guardada = configuracionGuardada
        if (guardada != null && System.currentTimeMillis() - guardada.first < VIGENCIA_CONFIGURACION_MS) return guardada.second
        val leida = db.collection(Colecciones.CONFIGURACION).document("plantel").get().await()
            .toObject(ConfiguracionDoc::class.java) ?: ConfiguracionDoc()
        configuracionGuardada = System.currentTimeMillis() to leida
        return leida
    }

    @Volatile
    private var configuracionGuardada: Pair<Long, ConfiguracionDoc>? = null
    private const val VIGENCIA_CONFIGURACION_MS = 5L * 60 * 1000

    /** Semestre en curso (configuracion/plantel.periodoActual), por ejemplo "2026-2027-1". */
    suspend fun periodoActual(): String = configuracion().periodoActual

    /**
     * Entradas y salidas de un alumno **en el semestre en curso**
     * (periodos/{periodoActual}/registros), de la más nueva a la más vieja, de [POR_PAGINA]
     * en [POR_PAGINA]. Para la página siguiente se pasa en [despuesDe] la fechaHora del último
     * registro recibido.
     */
    suspend fun registros(noControl: String, despuesDe: Timestamp? = null): List<RegistroDoc> {
        if (noControl.isBlank()) return emptyList()
        val periodo = periodoActual()
        if (periodo.isBlank()) return emptyList()
        var consulta = db.collection(Colecciones.PERIODOS).document(periodo)
            .collection(Colecciones.REGISTROS)
            .whereEqualTo("alumnoId", noControl)
            .orderBy("fechaHora", Query.Direction.DESCENDING)
        if (despuesDe != null) consulta = consulta.startAfter(despuesDe)
        return consulta.limit(POR_PAGINA).get().await().toObjects(RegistroDoc::class.java)
    }

    /** Cuántos registros se piden a la vez (cada uno cuenta como una lectura). */
    const val POR_PAGINA = 20L

    private fun buzon(uid: String) =
        db.collection(Colecciones.USUARIOS).document(uid).collection(Colecciones.NOTIFICACIONES)

    /**
     * Avisos personales de la cuenta (su buzón), del más nuevo al más viejo, en tiempo real:
     * un aviso nuevo aparece sin salir de la pantalla. Solo se escucha mientras la pantalla de
     * Notificaciones está abierta.
     */
    fun escucharNotificaciones(uid: String, limite: Long = 50): Flow<Result<List<Pair<String, NotificacionDoc>>>> =
        escucharLista(buzon(uid).orderBy("fecha", Query.Direction.DESCENDING).limit(limite), NotificacionDoc::class.java)

    /**
     * Cuántos avisos sin leer tiene la cuenta (para la campana), en tiempo real. Cuenta hasta
     * Avisos.MAXIMO_SIN_LEER: así cada cambio cuesta pocas lecturas.
     */
    fun escucharSinLeer(uid: String): Flow<Result<Int>> =
        escucharLista(buzon(uid).whereEqualTo("leida", false).limit(Avisos.MAXIMO_SIN_LEER), NotificacionDoc::class.java)
            .map { resultado -> resultado.map { it.size } }

    /** Marca como leídas las notificaciones indicadas (solo cambia el campo `leida`). */
    suspend fun marcarLeidas(uid: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val lote = db.batch()
        ids.forEach { lote.update(buzon(uid).document(it), "leida", true) }
        lote.commit().await()
    }

    // ---------- Personal: prefecto y profesor ----------

    /** Grupos del semestre en curso de un turno ("matutino" o "vespertino"), por nombre. Lo usa el prefecto. */
    suspend fun gruposDelTurno(turno: String): List<Pair<String, GrupoDoc>> {
        val periodo = periodoActual()
        if (periodo.isBlank()) return emptyList()
        return db.collection(Colecciones.GRUPOS)
            .whereEqualTo("periodoId", periodo)
            .whereEqualTo("turno", turno)
            .orderBy("nombre")
            .get().await()
            .documents.mapNotNull { doc -> doc.toObject(GrupoDoc::class.java)?.let { doc.id to it } }
    }

    /**
     * Los grupos con esos ids (por ejemplo usuarios/{uid}.grupos del profesor), ordenados por
     * nombre. Los que no existan se omiten.
     */
    suspend fun grupos(ids: List<String>): List<Pair<String, GrupoDoc>> =
        ids.filter { it.isNotBlank() }.distinct().mapNotNull { id ->
            db.collection(Colecciones.GRUPOS).document(id).get().await()
                .toObject(GrupoDoc::class.java)?.let { id to it }
        }.sortedBy { it.second.nombre }

    /**
     * Alumnos inscritos de un grupo, en orden alfabético por apellidos.
     * Ojo: Firestore deja fuera a los alumnos que no tengan el campo apellidoPaterno.
     */
    suspend fun alumnosDelGrupo(grupoId: String): List<AlumnoDoc> {
        if (grupoId.isBlank()) return emptyList()
        // Ordena como en español: "Álvarez" junto a "Alvarado", no al final.
        val orden = Collator.getInstance(Locale.forLanguageTag("es-MX")).apply { strength = Collator.PRIMARY }
        return db.collection(Colecciones.ALUMNOS)
            .whereEqualTo("estado", EstadoAlumno.INSCRITO)
            .whereEqualTo("grupoId", grupoId)
            .orderBy("apellidoPaterno")
            .get().await()
            .toObjects(AlumnoDoc::class.java)
            .sortedWith(compareBy(orden) { "${it.apellidoPaterno} ${it.apellidoMaterno} ${it.nombres}" })
    }

    /** Estatus de un alumno (faltas, reportes, castigos…), del más nuevo al más viejo. */
    suspend fun estatus(noControl: String, limite: Long = 30): List<EstatusDoc> {
        if (noControl.isBlank()) return emptyList()
        return db.collection(Colecciones.ALUMNOS).document(noControl)
            .collection(Colecciones.ESTATUS)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(limite)
            .get().await()
            .toObjects(EstatusDoc::class.java)
    }

    /**
     * Guarda un estatus nuevo del alumno y, si se da [aviso], se lo manda a cada uno de
     * [padres], todo de una vez (se guarda todo o nada). Las reglas solo dejan al profesor de su
     * grupo poner falta, reporte, llamada de atención o castigo, y avisar solo a sus padres.
     */
    suspend fun agregarEstatus(noControl: String, estatus: EstatusDoc, aviso: NotificacionDoc? = null, padres: List<String> = emptyList()) {
        val lote = db.batch()
        lote.set(db.collection(Colecciones.ALUMNOS).document(noControl).collection(Colecciones.ESTATUS).document(), estatus)
        if (aviso != null) padres.forEach { lote.set(buzon(it).document(), aviso) }
        lote.commit().await()
    }

    /** Grupos que ya salieron hoy (salidasDeGrupo del semestre en curso), del más reciente al más viejo. */
    suspend fun salidasDeGrupoDeHoy(): List<SalidaDeGrupoDoc> {
        val periodo = periodoActual()
        if (periodo.isBlank()) return emptyList()
        return db.collection(Colecciones.PERIODOS).document(periodo)
            .collection(Colecciones.SALIDAS_DE_GRUPO)
            .whereEqualTo("fecha", Formatos.fechaDeHoy())
            .orderBy("fechaHora", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(SalidaDeGrupoDoc::class.java)
    }

    /**
     * Igual que [salidasDeGrupoDeHoy], pero en tiempo real: avisa cada vez que un profesor da
     * salida a un grupo. Lo usa el Inicio del prefecto.
     */
    fun escucharSalidasDeHoy(): Flow<Result<List<SalidaDeGrupoDoc>>> =
        escucharEnElSemestre(SalidaDeGrupoDoc::class.java) { periodo ->
            periodo.collection(Colecciones.SALIDAS_DE_GRUPO)
                .whereEqualTo("fecha", Formatos.fechaDeHoy())
                .orderBy("fechaHora", Query.Direction.DESCENDING)
        }.map { resultado -> resultado.map { lista -> lista.map { it.second } } }

    /**
     * Los últimos [limite] escaneos de hoy (con o sin alerta), del más nuevo al más viejo, en
     * tiempo real. Lo usa el Inicio del prefecto ("Últimos escaneos").
     */
    fun escucharEscaneosDeHoy(limite: Long = 8): Flow<Result<List<RegistroDoc>>> =
        escucharEnElSemestre(RegistroDoc::class.java) { periodo ->
            periodo.collection(Colecciones.REGISTROS)
                .whereEqualTo("fecha", Formatos.fechaDeHoy())
                .orderBy("fechaHora", Query.Direction.DESCENDING)
                .limit(limite)
        }.map { resultado -> resultado.map { lista -> lista.map { it.second } } }

    /**
     * Los escaneos de hoy que tuvieron alerta, del más nuevo al más viejo, en tiempo real. Lo
     * usa la pantalla "Alertas de hoy" del prefecto. Necesita el índice registros (fecha,
     * alerta, fechaHora) de firebase/firestore.indexes.json.
     */
    fun escucharAlertasDeHoy(limite: Long = 100): Flow<Result<List<RegistroDoc>>> =
        escucharEnElSemestre(RegistroDoc::class.java) { periodo ->
            periodo.collection(Colecciones.REGISTROS)
                .whereEqualTo("fecha", Formatos.fechaDeHoy())
                .whereIn("alerta", TODAS_LAS_ALERTAS)
                .orderBy("fechaHora", Query.Direction.DESCENDING)
                .limit(limite)
        }.map { resultado -> resultado.map { lista -> lista.map { it.second } } }

    private val TODAS_LAS_ALERTAS = listOf(
        Alerta.NO_DADO_DE_ALTA, Alerta.INACTIVO, Alerta.YA_INGRESO,
        Alerta.NO_HA_INGRESADO, Alerta.CASTIGO, Alerta.SALIDA_FUERA_DE_HORARIO,
    )

    /**
     * El profesor libera a su grupo. Las reglas exigen que dé clase en ese grupo y que
     * `autorizadaPor` sea su propio uid.
     */
    suspend fun darSalidaAlGrupo(salida: SalidaDeGrupoDoc) {
        val periodo = periodoActual()
        check(periodo.isNotBlank()) { "No hay un semestre en curso (configuracion/plantel.periodoActual)." }
        db.collection(Colecciones.PERIODOS).document(periodo)
            .collection(Colecciones.SALIDAS_DE_GRUPO)
            .add(salida).await()
    }

    // ---------- Escaneo del prefecto ----------

    /** Lo que hace falta para decidir la alerta de un escaneo (ver Escaneo.alerta). */
    suspend fun datosDelEscaneo(noControl: String, tipo: String): DatosDelEscaneo {
        val alumno = alumno(noControl) ?: return DatosDelEscaneo(null, tipo, emptyList(), grupoYaSalio = false)
        val estatus = estatus(noControl, limite = 10)
        val grupoYaSalio = tipo == TipoRegistro.SALIDA &&
            salidasDeGrupoDeHoy().any { it.grupoId == alumno.grupoId }
        return DatosDelEscaneo(alumno, tipo, estatus, grupoYaSalio)
    }

    /**
     * Guarda un escaneo de una sola vez (se guarda todo o nada):
     *   - el registro en periodos/{semestre}/registros, siempre, aunque haya alerta;
     *   - si no hubo alerta, la `ubicacion` y el `ultimoRegistro` del alumno;
     *   - si se da [aviso], una notificación para cada padre o tutor del alumno (sin alerta,
     *     siempre; con alerta, si el plantel lo pide: Avisos.alPadrePorEscaneo).
     * Las reglas solo dejan hacerlo al prefecto, con su propio uid en `registradoPor`.
     */
    suspend fun registrarEscaneo(registro: RegistroDoc, alumno: AlumnoDoc?, aviso: NotificacionDoc?) {
        val periodo = periodoActual()
        check(periodo.isNotBlank()) { "No hay un semestre en curso (configuracion/plantel.periodoActual)." }
        val lote = db.batch()
        lote.set(
            db.collection(Colecciones.PERIODOS).document(periodo).collection(Colecciones.REGISTROS).document(),
            registro,
        )
        if (alumno != null && registro.alerta == null) {
            lote.update(
                db.collection(Colecciones.ALUMNOS).document(registro.alumnoId),
                mapOf(
                    "ubicacion" to Escaneo.ubicacionDespues(registro.tipo),
                    "ultimoRegistro" to mapOf("tipo" to registro.tipo, "fechaHora" to registro.fechaHora),
                    "actualizadoEn" to registro.fechaHora,
                ),
            )
        }
        if (aviso != null) Avisos.destinatarios(alumno).forEach { padre -> lote.set(buzon(padre).document(), aviso) }
        lote.commit().await()
    }

    // ---------- Contraseña ----------

    /**
     * Cambia la contraseña de la persona que tiene la sesión abierta. Firebase pide confirmar la
     * contraseña actual antes de cambiarla. Después anota en usuarios/{uid} que ya la cambió y
     * cuándo vence la nueva (configuracion/plantel.diasVenceContrasena; 0 = no vence).
     */
    suspend fun cambiarContrasena(actual: String, nueva: String): ResultadoContrasena {
        val cuenta = auth.currentUser ?: return ResultadoContrasena.Desconocido
        val correo = cuenta.email ?: return ResultadoContrasena.Desconocido
        try {
            cuenta.reauthenticate(EmailAuthProvider.getCredential(correo, actual)).await()
            cuenta.updatePassword(nueva).await()
        } catch (e: FirebaseAuthWeakPasswordException) {
            return ResultadoContrasena.Debil
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return ResultadoContrasena.ActualIncorrecta
        } catch (e: FirebaseNetworkException) {
            return ResultadoContrasena.SinConexion
        } catch (e: FirebaseTooManyRequestsException) {
            return ResultadoContrasena.DemasiadosIntentos
        } catch (e: FirebaseException) {
            return ResultadoContrasena.Desconocido
        }
        // La contraseña ya cambió. Si esta parte falla (por ejemplo, sin internet), lo único que
        // pasa es que la app volverá a pedir el cambio la próxima vez que se entre.
        try {
            val dias = configuracion().diasVenceContrasena
            val vence = if (dias > 0) Timestamp(Date(System.currentTimeMillis() + dias * UN_DIA_MS)) else null
            db.collection(Colecciones.USUARIOS).document(cuenta.uid).update(
                mapOf(
                    "debeCambiarContrasena" to false,
                    "contrasenaVence" to vence,
                    "actualizadoEn" to Timestamp.now(),
                )
            ).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
        return ResultadoContrasena.Lista
    }

    private const val UN_DIA_MS = 24L * 60 * 60 * 1000
}

/** Espera a que termine una tarea de Firebase sin bloquear la pantalla. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuacion ->
    addOnSuccessListener { continuacion.resume(it) }
    addOnFailureListener { continuacion.resumeWithException(it) }
    addOnCanceledListener { continuacion.cancel() }
}
