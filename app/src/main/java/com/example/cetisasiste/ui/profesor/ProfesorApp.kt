package com.example.cetisasiste.ui.profesor

import android.view.View
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cetisasiste.AlumnoVistas
import com.example.cetisasiste.Anuncio
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.ConfiguracionDoc
import com.example.cetisasiste.datos.ClaseDoc
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.SalidaDeGrupoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoAviso
import com.example.cetisasiste.ui.comun.CambiarContrasenaXml
import com.example.cetisasiste.ui.comun.Carga
import com.example.cetisasiste.ui.comun.HorarioXml
import com.example.cetisasiste.ui.comun.NotificacionesXml
import com.example.cetisasiste.ui.comun.PantallaXml
import com.example.cetisasiste.ui.comun.PerfilPersonalXml
import com.example.cetisasiste.ui.comun.Pestana
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.alTocar
import com.example.cetisasiste.ui.comun.avisar
import com.example.cetisasiste.ui.comun.cargar
import com.example.cetisasiste.ui.comun.mostrar
import com.example.cetisasiste.ui.comun.o
import com.example.cetisasiste.ui.comun.sinLeerEnVivo
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

/** Pantallas del profesor. Las tres primeras están en el menú ☰. */
enum class ProfesorPantalla {
    Inicio,
    Boletin,
    Perfil,
    Grupo,
    HorarioDelGrupo,
    Alumno,
    CambiarEstatus,
    DarSalida,
    Notificaciones,
    CambiarContrasena,
}

/** Pantallas principales del profesor: son las opciones del menú ☰ (comun_menu.xml). */
private val PESTANAS = listOf(
    Pestana(ProfesorPantalla.Inicio, R.drawable.ic_home, R.string.nav_inicio),
    Pestana(ProfesorPantalla.Boletin, R.drawable.ic_announcements, R.string.alumno_nav_boletin),
    Pestana(ProfesorPantalla.Perfil, R.drawable.ic_profile, R.string.nav_perfil),
)

/**
 * Apartado del profesor. Muestra las vistas XML (profesor_*.xml y comun_*.xml), les pasa sus
 * datos y conecta sus botones.
 *
 * El profesor ve los grupos de usuarios/{uid}.grupos, la lista de alumnos de cada uno, el
 * perfil de cada alumno, y **escribe** en Firestore en dos casos: cambiar el estatus de un
 * alumno (alumnos/{m}/estatus) y dar salida al grupo (periodos/{semestre}/salidasDeGrupo).
 */
@Composable
fun ProfesorApp(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pila by rememberSaveable { mutableStateOf(listOf(ProfesorPantalla.Inicio)) }
    fun abrir(p: ProfesorPantalla) { pila = pila + p }
    fun regresar() { pila = pila.dropLast(1) }
    fun pestana(p: ProfesorPantalla) {
        pila = if (p == ProfesorPantalla.Inicio) listOf(p) else listOf(ProfesorPantalla.Inicio, p)
    }
    BackHandler(enabled = pila.size > 1) { regresar() }
    // Avisos sin leer (campana y menú ☰), en tiempo real.
    val sinLeer = sinLeerEnVivo(sesion.uid)

    val scope = rememberCoroutineScope()
    val usuario = sesion.usuario
    // Grupo elegido: su id (2026-2027-1_102) y su nombre (102). Alumno elegido: su No. de control.
    var grupoElegido by rememberSaveable { mutableStateOf(Pair("", "")) }
    var alumnoElegido by rememberSaveable { mutableStateOf("") }
    val (grupoId, nombreGrupo) = grupoElegido
    // Se suma 1 después de guardar, para que las pantallas vuelvan a leer de Firestore.
    var recarga by remember { mutableIntStateOf(0) }

    // Ficha del alumno elegido (la usan Perfil del alumno y Cambiar estatus).
    val alumno by produceState<AlumnoDoc?>(null, alumnoElegido) {
        value = null
        value = if (alumnoElegido.isBlank()) null else cargar { CetisRepositorio.alumno(alumnoElegido) }.o(null)
    }

    val rol = stringResource(R.string.profesor_rol)
    /** Conecta el botón ☰ con el menú del profesor; marca la opción de [p]. */
    fun conectarMenu(vista: View, p: ProfesorPantalla) =
        VistasComunes.conectarMenu(
            vista, usuario.nombre.trim(), rol, PESTANAS, p, ::pestana,
            onNotificaciones = { abrir(ProfesorPantalla.Notificaciones) },
            onCerrarSesion = onCerrarSesion,
            sinLeer = sinLeer,
        )

    when (val pantalla = pila.last()) {
        ProfesorPantalla.Inicio -> {
            val grupos by produceState<List<Pair<String, GrupoDoc>>?>(null, usuario.grupos) {
                value = cargar { CetisRepositorio.grupos(usuario.grupos) }.o(emptyList())
            }
            PantallaXml(R.layout.profesor_inicio, modifier) { vista ->
                ProfesorVistas.llenarInicio(vista, usuario.nombre.trim(), usuario.grupos.size, grupos) { id, grupo ->
                    grupoElegido = id to grupo.nombre.trim()
                    abrir(ProfesorPantalla.Grupo)
                }
                vista.alTocar(R.id.btn_notificaciones) { abrir(ProfesorPantalla.Notificaciones) }
                VistasComunes.marcarCampana(vista, sinLeer)
                conectarMenu(vista, pantalla)
            }
        }

        ProfesorPantalla.Boletin -> {
            val anuncios by produceState<List<Anuncio>?>(null, sesion.uid) {
                value = cargar { CetisRepositorio.anuncios(usuario.rol) }.o(emptyList()).map(Formatos::anuncio)
            }
            PantallaXml(R.layout.profesor_boletin, modifier) { vista ->
                AlumnoVistas.llenarBoletin(vista, anuncios.orEmpty())
                vista.mostrar(R.id.txt_vacio, anuncios?.isEmpty() == true)
                conectarMenu(vista, pantalla)
            }
        }

        ProfesorPantalla.Perfil -> PerfilPersonalXml(
            sesion = sesion,
            rol = stringResource(R.string.profesor_rol),
            conectarMenu = { conectarMenu(it, pantalla) },
            onCambiarContrasena = { abrir(ProfesorPantalla.CambiarContrasena) },
            onCerrarSesion = onCerrarSesion,
            modifier = modifier,
        )

        ProfesorPantalla.Grupo -> {
            val alumnos by produceState<List<AlumnoDoc>?>(null, grupoId, recarga) {
                value = cargar { CetisRepositorio.alumnosDelGrupo(grupoId) }.o(emptyList())
            }
            val salidaDeHoy = salidaDeHoyDelGrupo(grupoId, recarga)
            PantallaXml(R.layout.profesor_grupo, modifier) { vista ->
                ProfesorVistas.llenarGrupo(vista, nombreGrupo, salidaDeHoy.salida, alumnos) { noControl ->
                    alumnoElegido = noControl
                    abrir(ProfesorPantalla.Alumno)
                }
                vista.alTocar(R.id.opcion_dar_salida) { abrir(ProfesorPantalla.DarSalida) }
                vista.alTocar(R.id.opcion_horario) { abrir(ProfesorPantalla.HorarioDelGrupo) }
                vista.alTocar(R.id.btn_regresar) { regresar() }
            }
        }

        ProfesorPantalla.HorarioDelGrupo -> HorarioXml(
            grupoId = grupoId,
            nombreGrupo = nombreGrupo,
            titulo = stringResource(R.string.comun_grupo_titulo, nombreGrupo),
            onRegresar = ::regresar,
            modifier = modifier,
        )

        ProfesorPantalla.Alumno -> {
            val estatus by produceState<List<EstatusDoc>?>(null, alumnoElegido, recarga) {
                value = cargar { CetisRepositorio.estatus(alumnoElegido) }.o(emptyList())
            }
            PantallaXml(R.layout.profesor_alumno, modifier) { vista ->
                ProfesorVistas.llenarAlumno(vista, alumno, nombreGrupo, estatus)
                vista.alTocar(R.id.opcion_cambiar_estatus) { abrir(ProfesorPantalla.CambiarEstatus) }
                vista.alTocar(R.id.btn_regresar) { regresar() }
            }
        }

        ProfesorPantalla.CambiarEstatus -> {
            var tipo by rememberSaveable { mutableStateOf<String?>(null) }
            var guardando by remember { mutableStateOf(false) }
            var mensaje by remember { mutableStateOf<Int?>(null) }
            // Qué estatus se le avisan al padre (configuracion/plantel). Sin configuración, no se avisa.
            val configuracion by produceState<ConfiguracionDoc?>(null) {
                value = cargar { CetisRepositorio.configuracion() }.o(null)
            }
            val padres = Avisos.destinatarios(alumno)
            fun seAvisa(t: String?): Boolean {
                val c = configuracion ?: return false
                return t != null && padres.isNotEmpty() && Avisos.alPadrePorEstatus(t, c)
            }
            PantallaXml(R.layout.profesor_cambiar_estatus, modifier) { vista ->
                ProfesorVistas.llenarCambiarEstatus(vista, alumno, nombreGrupo, tipo, guardando, mensaje, seAvisa(tipo))
                ProfesorVistas.CHIPS_DE_ESTATUS.forEach { (id, t) -> vista.alTocar(id) { tipo = t } }
                vista.alTocar(R.id.btn_regresar) { regresar() }
                vista.alTocar(R.id.btn_guardar) {
                    val descripcion = vista.findViewById<EditText>(R.id.edit_descripcion).text.toString().trim()
                    val elegido = tipo
                    mensaje = when {
                        elegido == null -> R.string.profesor_estatus_error_tipo
                        descripcion.isEmpty() -> R.string.profesor_estatus_error_descripcion
                        else -> null
                    }
                    if (elegido == null || mensaje != null) return@alTocar
                    guardando = true
                    scope.launch {
                        val ahora = Timestamp.now()
                        val nuevo = EstatusDoc(
                            tipo = elegido,
                            descripcion = descripcion,
                            fecha = ahora,
                            vigenteHasta = null,
                            asignadoPor = sesion.uid,
                            asignadoPorNombre = usuario.nombre.trim(),
                        )
                        val nombreTipo = vista.context.getString(ProfesorVistas.nombreDeEstatus(elegido))
                        val aviso = if (!seAvisa(elegido)) null else Avisos.nuevo(
                            tipo = TipoAviso.ESTATUS,
                            titulo = vista.context.getString(R.string.profesor_aviso_estatus_titulo, nombreTipo),
                            mensaje = vista.context.getString(
                                R.string.profesor_aviso_estatus_mensaje,
                                nombreTipo.lowercase(), alumno?.nombre?.trim().orEmpty(), usuario.nombre.trim(), descripcion,
                            ),
                            alumnoId = alumnoElegido,
                            creadaPor = sesion.uid,
                            ahora = ahora,
                        )
                        val resultado = cargar { CetisRepositorio.agregarEstatus(alumnoElegido, nuevo, aviso, padres) }
                        guardando = false
                        if (resultado is Carga.Listo) {
                            vista.avisar(R.string.profesor_estatus_guardado)
                            recarga++
                            regresar()
                        } else {
                            mensaje = R.string.comun_error_guardar
                        }
                    }
                }
            }
        }

        ProfesorPantalla.DarSalida -> {
            var guardando by remember { mutableStateOf(false) }
            var mensaje by remember { mutableStateOf<Int?>(null) }
            val hoy = remember { Formatos.tituloDeDia(Timestamp.now()) }
            val ultimaClase by produceState<ClaseDoc?>(null, grupoId) {
                val dia = Formatos.diaEscolarDeHoy() ?: return@produceState
                val clases = cargar { CetisRepositorio.horario(grupoId, Formatos.idDeDia(dia)) }.o(emptyList())
                value = clases.lastOrNull { it.ultimaDelTurno } ?: clases.lastOrNull()
            }
            val salidaDeHoy = salidaDeHoyDelGrupo(grupoId, recarga)
            PantallaXml(R.layout.profesor_dar_salida, modifier) { vista ->
                ProfesorVistas.llenarDarSalida(
                    vista, nombreGrupo, hoy, ultimaClase,
                    salidaDeHoy = salidaDeHoy.salida,
                    // Mientras se revisa si ya salió, el botón espera apagado (así no se registra dos veces).
                    revisando = salidaDeHoy is Carga.Cargando || guardando,
                    mensaje = mensaje,
                )
                vista.alTocar(R.id.btn_regresar) { regresar() }
                vista.alTocar(R.id.btn_dar_salida) {
                    mensaje = null
                    guardando = true
                    scope.launch {
                        val salida = SalidaDeGrupoDoc(
                            grupoId = grupoId,
                            grupo = nombreGrupo,
                            fecha = Formatos.fechaDeHoy(),
                            fechaHora = Timestamp.now(),
                            autorizadaPor = sesion.uid,
                            autorizadaPorNombre = usuario.nombre.trim(),
                            materia = ultimaClase?.materia.orEmpty(),
                        )
                        val resultado = cargar { CetisRepositorio.darSalidaAlGrupo(salida) }
                        guardando = false
                        if (resultado is Carga.Listo) {
                            vista.avisar(R.string.profesor_salida_guardada)
                            recarga++
                            regresar()
                        } else {
                            mensaje = R.string.comun_error_guardar
                        }
                    }
                }
            }
        }

        ProfesorPantalla.Notificaciones -> NotificacionesXml(sesion, onRegresar = ::regresar, modifier = modifier)

        ProfesorPantalla.CambiarContrasena -> CambiarContrasenaXml(
            obligatoria = false,
            onRegresar = ::regresar,
            onCerrarSesion = onCerrarSesion,
            onLista = ::regresar,
            modifier = modifier,
        )
    }
}

/**
 * La salida de hoy del grupo: Listo(salida) si el profesor (u otro profesor del grupo) ya la
 * dio, Listo(null) si todavía no, y Cargando mientras se revisa.
 */
@Composable
private fun salidaDeHoyDelGrupo(grupoId: String, recarga: Int): Carga<SalidaDeGrupoDoc?> {
    val salida by produceState<Carga<SalidaDeGrupoDoc?>>(Carga.Cargando, grupoId, recarga) {
        value = Carga.Cargando
        value = Carga.Listo(
            cargar { CetisRepositorio.salidasDeGrupoDeHoy() }.o(emptyList()).firstOrNull { it.grupoId == grupoId },
        )
    }
    return salida
}

/** La salida ya cargada (o null si no hay o todavía se está revisando). */
private val Carga<SalidaDeGrupoDoc?>.salida: SalidaDeGrupoDoc? get() = (this as? Carga.Listo)?.datos
