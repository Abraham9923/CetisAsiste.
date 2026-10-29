package com.example.cetisasiste.ui.comun

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.cetisasiste.Clase
import com.example.cetisasiste.DiaSemana
import com.example.cetisasiste.HorarioDelDia
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.EventoDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NotificacionDoc
import com.example.cetisasiste.datos.ResultadoContrasena
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoAviso
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar

/*
 * Pantallas que comparten varios roles. Cada una pide sus propios datos a Firestore y usa un
 * layout comun_*.xml. Los *App.kt de cada rol solo las abren y les dicen a dónde regresar.
 *
 *   CalendarioXml: padre y prefecto (pestaña)
 *   NotificacionesXml: todos (campana de Inicio y opción del menú ☰)
 *   HorarioXml: padre (su hijo), prefecto (cualquier grupo) y profesor (sus grupos)
 *   PerfilPersonalXml: prefecto y profesor (pestaña)
 *   CambiarContrasenaXml todos, incluido el alumno
 */

/**
 * Calendario del plantel con los eventos del mes; las flechas cambian de mes.
 * [conectarMenu] conecta el botón ☰ con el menú del rol que lo abre.
 */
@Composable
fun CalendarioXml(
    conectarMenu: (View) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 0 = este mes, -1 = el anterior, 1 = el siguiente…
    var desplazamiento by rememberSaveable { mutableIntStateOf(0) }
    val mes = remember(desplazamiento) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, desplazamiento)
        }
    }
    val eventos by produceState(emptyList<EventoDoc>(), desplazamiento) {
        val (desde, hasta) = limitesDelMes(mes)
        value = cargar { CetisRepositorio.eventos(desde, hasta) }.o(emptyList())
    }
    PantallaXml(R.layout.comun_calendario, modifier) { vista ->
        VistasComunes.llenarCalendario(
            vista,
            mesCalendario(mes, eventos),
            eventos.map { Evento(it.titulo, Formatos.detalleDeEvento(it)) },
        )
        vista.alTocar(R.id.btn_mes_anterior) { desplazamiento-- }
        vista.alTocar(R.id.btn_mes_siguiente) { desplazamiento++ }
        conectarMenu(vista)
    }
}

/**
 * Avisos personales de la cuenta (usuarios/{uid}/notificaciones)
 * [extrasDeHoy] son avisos que no están en el buzón y se muestran arriba de los de hoy (el
 * prefecto ve ahí los grupos que ya salieron de clases).
 */
@Composable
fun NotificacionesXml(
    sesion: Sesion,
    onRegresar: () -> Unit,
    modifier: Modifier = Modifier,
    extrasDeHoy: List<Notificacion> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    val flujo = remember(sesion.uid) { CetisRepositorio.escucharNotificaciones(sesion.uid) }
    // Si falla (sin internet), se ve como "No hay notificaciones de hoy".
    val avisos = flujo.collectAsState(initial = null).value?.getOrNull().orEmpty()
    val (deHoy, anteriores) = avisos.partition { Formatos.esHoy(it.second.fecha) }
    PantallaXml(R.layout.comun_notificaciones, modifier) { vista ->
        VistasComunes.llenarNotificaciones(
            vista,
            extrasDeHoy + deHoy.map { notificacion(it.second) },
            anteriores.map { notificacion(it.second) },
        )
        vista.alTocar(R.id.btn_regresar, onRegresar)
        vista.alTocar(R.id.txt_marcar_leidas) {
            scope.launch {
                cargar { CetisRepositorio.marcarLeidas(sesion.uid, avisos.filter { !it.second.leida }.map { it.first }) }
            }
        }
    }
}

/**
 * Horario de un grupo (grupos/{grupoId}/horario/{día}) con los chips de lunes a viernes.
 * [titulo] va en la barra de arriba ("Horario" o "Grupo 102").
 */
@Composable
fun HorarioXml(
    grupoId: String,
    nombreGrupo: String,
    titulo: String,
    onRegresar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dia by rememberSaveable { mutableStateOf(Formatos.diaDeHoy()) }
    val textoUltimaClase = stringResource(R.string.alumno_clase_ultima)
    // null mientras carga (así no se ve "No hay clases" antes de tiempo).
    val clases by produceState<List<Clase>?>(null, grupoId, dia) {
        value = cargar { CetisRepositorio.horario(grupoId, Formatos.idDeDia(dia)) }.o(emptyList())
            .map { Formatos.clase(it, textoUltimaClase) }
    }
    PantallaXml(R.layout.comun_horario, modifier) { vista ->
        vista.texto(R.id.txt_titulo, titulo)
        AlumnoVistas.llenarHorario(vista, HorarioDelDia(nombreGrupo, dia, clases.orEmpty()))
        vista.mostrar(R.id.txt_vacio, clases?.isEmpty() == true)
        CHIPS_DE_DIAS.forEach { (id, d) -> vista.alTocar(id) { dia = d } }
        vista.alTocar(R.id.btn_regresar, onRegresar)
    }
}

/** Perfil del personal: sus datos de cuenta, cambiar contraseña y cerrar sesión. */
@Composable
fun PerfilPersonalXml(
    sesion: Sesion,
    rol: String,
    conectarMenu: (View) -> Unit,
    onCambiarContrasena: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val usuario = sesion.usuario
    PantallaXml(R.layout.comun_perfil_personal, modifier) { vista ->
        VistasComunes.llenarPerfilPersonal(vista, Personal(usuario.nombre.trim(), rol, usuario.usuario, usuario.correo))
        vista.alTocar(R.id.opcion_contrasena, onCambiarContrasena)
        vista.alTocar(R.id.opcion_cerrar_sesion) { VistasComunes.mostrarDialogoCerrarSesion(vista, onCerrarSesion) }
        conectarMenu(vista)
    }
}

/**
 * Cambiar contraseña. Si [obligatoria] es true (la cuenta tiene debeCambiarContrasena o su
 * contraseña venció), no hay flecha de regresar: solo se puede cambiar o cerrar sesión.
 * [onLista] se llama cuando la contraseña ya cambió.
 */
@Composable
fun CambiarContrasenaXml(
    obligatoria: Boolean,
    onRegresar: () -> Unit,
    onCerrarSesion: () -> Unit,
    onLista: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<Int?>(null) }
    PantallaXml(R.layout.comun_cambiar_contrasena, modifier) { vista ->
        vista.mostrar(R.id.btn_regresar, !obligatoria)
        vista.mostrar(R.id.txt_obligatoria, obligatoria)
        vista.mostrar(R.id.txt_cerrar_sesion, obligatoria)
        vista.alTocar(R.id.btn_regresar, onRegresar)
        vista.alTocar(R.id.txt_cerrar_sesion, onCerrarSesion)

        vista.mostrar(R.id.txt_mensaje, mensaje != null)
        mensaje?.let { vista.findViewById<TextView>(R.id.txt_mensaje).setText(it) }

        val boton = vista.findViewById<Button>(R.id.btn_guardar)
        boton.isEnabled = !guardando
        boton.alpha = if (guardando) 0.6f else 1f
        boton.setText(if (guardando) R.string.comun_contrasena_guardando else R.string.comun_contrasena_guardar)
        boton.setOnClickListener {
            val actual = vista.campo(R.id.edit_actual)
            val nueva = vista.campo(R.id.edit_nueva)
            val error = errorDeContrasena(actual, nueva, vista.campo(R.id.edit_repetir))
            if (error != null) {
                mensaje = error
                return@setOnClickListener
            }
            mensaje = null
            guardando = true
            scope.launch {
                val resultado = CetisRepositorio.cambiarContrasena(actual, nueva)
                guardando = false
                if (resultado == ResultadoContrasena.Lista) {
                    vista.avisar(R.string.comun_contrasena_lista)
                    onLista()
                } else {
                    mensaje = textoDeResultado(resultado)
                }
            }
        }
    }
}

/** Mínimo de caracteres de una contraseña nueva (Firebase pide 6 caracteres en el plantel, 8). */
private const val MINIMO_CONTRASENA = 8

/** Revisa los tres campos antes de llamar a Firebase. Devuelve el texto del error o null. */
@StringRes
private fun errorDeContrasena(actual: String, nueva: String, repetir: String): Int? = when {
    actual.isEmpty() || nueva.isEmpty() || repetir.isEmpty() -> R.string.comun_contrasena_error_vacios
    nueva.length < MINIMO_CONTRASENA -> R.string.comun_contrasena_error_corta
    nueva != repetir -> R.string.comun_contrasena_error_no_coinciden
    nueva == actual -> R.string.comun_contrasena_error_igual
    else -> null
}

@StringRes
private fun textoDeResultado(resultado: ResultadoContrasena): Int = when (resultado) {
    ResultadoContrasena.ActualIncorrecta -> R.string.comun_contrasena_error_actual
    ResultadoContrasena.Debil -> R.string.comun_contrasena_error_debil
    ResultadoContrasena.SinConexion -> R.string.login_error_conexion
    ResultadoContrasena.DemasiadosIntentos -> R.string.login_error_intentos
    ResultadoContrasena.Lista, ResultadoContrasena.Desconocido -> R.string.comun_contrasena_error_desconocido
}

private fun View.campo(id: Int): String = findViewById<EditText>(id).text.toString()

/** Chips de días de comun_horario.xml (los mismos ids que alumno_horario.xml). */
val CHIPS_DE_DIAS = listOf(
    R.id.chip_lunes to DiaSemana.LUNES,
    R.id.chip_martes to DiaSemana.MARTES,
    R.id.chip_miercoles to DiaSemana.MIERCOLES,
    R.id.chip_jueves to DiaSemana.JUEVES,
    R.id.chip_viernes to DiaSemana.VIERNES,
)

/** NotificacionDoc (Firestore) → Notificacion (lo que dibuja la vista). */
private fun notificacion(doc: NotificacionDoc) = Notificacion(
    tipo = when (doc.tipo) {
        TipoAviso.ENTRADA -> TipoNotificacion.Entrada
        TipoAviso.SALIDA -> TipoNotificacion.Salida
        TipoAviso.ANUNCIO -> TipoNotificacion.Anuncio
        TipoAviso.RECORDATORIO -> TipoNotificacion.Recordatorio
        TipoAviso.CREDENCIAL -> TipoNotificacion.Credencial
        TipoAviso.ALERTA -> TipoNotificacion.Alerta
        TipoAviso.ESTATUS -> TipoNotificacion.Estatus
        TipoAviso.ESTADO -> TipoNotificacion.Estado
        TipoAviso.SALIDA_GRUPO -> TipoNotificacion.SalidaDeGrupo
        else -> TipoNotificacion.Aviso
    },
    titulo = doc.titulo,
    hora = Formatos.horaOFecha(doc.fecha),
    texto = doc.mensaje,
    leida = doc.leida,
)

/** Primer instante del mes y primer instante del mes siguiente. */
private fun limitesDelMes(mes: Calendar): Pair<Timestamp, Timestamp> {
    val inicio = (mes.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val fin = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return Timestamp(inicio.time) to Timestamp(fin.time)
}

/** Arma la cuadrícula del mes: casillas vacías, días, hoy y días con evento. */
private fun mesCalendario(mes: Calendar, eventos: List<EventoDoc>): MesCalendario {
    val primero = (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val dias = primero.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Calendar: domingo = 1 … sábado = 7. La cuadrícula empieza en lunes.
    val vacias = (primero.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val hoy = Calendar.getInstance()
    val esEsteMes = hoy.get(Calendar.YEAR) == mes.get(Calendar.YEAR) && hoy.get(Calendar.MONTH) == mes.get(Calendar.MONTH)

    val conEvento = mutableSetOf<Int>()
    eventos.forEach { evento ->
        val inicio = evento.inicio?.toDate() ?: return@forEach
        val fin = evento.fin?.toDate() ?: inicio
        val dia = Calendar.getInstance().apply { time = inicio }
        while (!dia.time.after(fin) && dia.get(Calendar.MONTH) == mes.get(Calendar.MONTH)) {
            conEvento += dia.get(Calendar.DAY_OF_MONTH)
            dia.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return MesCalendario(
        titulo = Formatos.tituloDeMes(mes),
        dias = dias,
        casillasVaciasAlInicio = vacias,
        diaDeHoy = if (esEsteMes) hoy.get(Calendar.DAY_OF_MONTH) else null,
        diasConEvento = conEvento,
    )
}
