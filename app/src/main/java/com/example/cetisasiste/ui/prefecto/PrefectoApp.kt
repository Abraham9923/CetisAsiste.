package com.example.cetisasiste.ui.prefecto

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.Turno
import com.example.cetisasiste.ui.comun.CalendarioXml
import com.example.cetisasiste.ui.comun.CambiarContrasenaXml
import com.example.cetisasiste.ui.comun.HorarioXml
import com.example.cetisasiste.ui.comun.NotificacionesXml
import com.example.cetisasiste.ui.comun.PantallaXml
import com.example.cetisasiste.ui.comun.PerfilPersonalXml
import com.example.cetisasiste.ui.comun.Pestana
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.alTocar
import com.example.cetisasiste.ui.comun.cargar
import com.example.cetisasiste.ui.comun.o
import com.google.firebase.Timestamp
import java.util.Calendar

/** Pantallas del prefecto. Las cinco primeras están en el menú ☰. */
enum class PrefectoPantalla {
    Inicio,
    Escanear,
    Horarios,
    Calendario,
    Perfil,
    HorarioDeGrupo,
    Notificaciones,
    CambiarContrasena,
    Captura,
    Resultado,
}

/** Pantallas principales del prefecto: son las opciones del menú ☰ (comun_menu.xml). */
private val PESTANAS = listOf(
    Pestana(PrefectoPantalla.Inicio, R.drawable.ic_home, R.string.nav_inicio),
    Pestana(PrefectoPantalla.Escanear, R.drawable.ic_scan, R.string.prefecto_nav_escanear),
    Pestana(PrefectoPantalla.Horarios, R.drawable.ic_clock, R.string.prefecto_nav_horarios),
    Pestana(PrefectoPantalla.Calendario, R.drawable.ic_calendar, R.string.nav_calendario),
    Pestana(PrefectoPantalla.Perfil, R.drawable.ic_profile, R.string.nav_perfil),
)

/**
 * Apartado del prefecto. Muestra las vistas XML (prefecto_*.xml y comun_*.xml), les pasa sus
 * datos y conecta sus botones.
 *
 * El escaneo (escáner, captura por No. de control y resultado) está en EscaneoPrefecto.kt.
 */
@Composable
fun PrefectoApp(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pila by rememberSaveable { mutableStateOf(listOf(PrefectoPantalla.Inicio)) }
    fun abrir(p: PrefectoPantalla) { pila = pila + p }
    fun regresar() { pila = pila.dropLast(1) }
    fun pestana(p: PrefectoPantalla) {
        pila = if (p == PrefectoPantalla.Inicio) listOf(p) else listOf(PrefectoPantalla.Inicio, p)
    }
    BackHandler(enabled = pila.size > 1) { regresar() }

    // Grupo elegido en Horarios: su id (2026-2027-1_102) y su nombre (102).
    var grupoElegido by rememberSaveable { mutableStateOf(Pair("", "")) }
    var turno by rememberSaveable { mutableStateOf(turnoSegunLaHora()) }
    // Entrada o Salida, lo tecleado y el último resultado: lo comparten las pantallas del escaneo.
    val escaneo = remember { EstadoDelEscaneo() }

    val rol = stringResource(R.string.prefecto_rol)
    /** Conecta el botón ☰ con el menú del prefecto; marca la opción de [p]. */
    fun conectarMenu(vista: View, p: PrefectoPantalla) =
        VistasComunes.conectarMenu(
            vista, sesion.usuario.nombre.trim(), rol, PESTANAS, p, ::pestana,
            onNotificaciones = { abrir(PrefectoPantalla.Notificaciones) },
            onCerrarSesion = onCerrarSesion,
        )

    when (val pantalla = pila.last()) {
        PrefectoPantalla.Inicio -> {
            // En tiempo real: en cuanto un profesor da salida a su grupo, aparece aquí.
            val flujo = remember { CetisRepositorio.escucharSalidasDeHoy() }
            val salidas by flujo.collectAsState(initial = null)
            val hoy = remember { Formatos.tituloDeDia(Timestamp.now()) }
            PantallaXml(R.layout.prefecto_inicio, modifier) { vista ->
                // Si la consulta falla (sin internet), se ve como "ningún grupo ha salido".
                PrefectoVistas.llenarInicio(vista, sesion.usuario.nombre.trim(), hoy, salidas?.getOrDefault(emptyList()))
                vista.alTocar(R.id.btn_notificaciones) { abrir(PrefectoPantalla.Notificaciones) }
                vista.alTocar(R.id.card_escanear) { pestana(PrefectoPantalla.Escanear) }
                vista.alTocar(R.id.card_no_control) {
                    escaneo.mensaje = null
                    abrir(PrefectoPantalla.Captura)
                }
                vista.alTocar(R.id.card_horarios) { pestana(PrefectoPantalla.Horarios) }
                vista.alTocar(R.id.card_calendario) { pestana(PrefectoPantalla.Calendario) }
                conectarMenu(vista, pantalla)
            }
        }

        PrefectoPantalla.Escanear -> EscanerXml(
            estado = escaneo,
            sesion = sesion,
            conectarMenu = { conectarMenu(it, pantalla) },
            onCaptura = { abrir(PrefectoPantalla.Captura) },
            onResultado = { abrir(PrefectoPantalla.Resultado) },
            modifier = modifier,
        )

        PrefectoPantalla.Captura -> CapturaXml(
            estado = escaneo,
            sesion = sesion,
            onRegresar = ::regresar,
            onResultado = { abrir(PrefectoPantalla.Resultado) },
            modifier = modifier,
        )

        PrefectoPantalla.Resultado -> ResultadoXml(escaneo, onSiguiente = ::regresar, modifier = modifier)

        PrefectoPantalla.Horarios -> {
            val grupos by produceState<List<Pair<String, GrupoDoc>>?>(null, turno) {
                value = null
                value = cargar { CetisRepositorio.gruposDelTurno(turno) }.o(emptyList())
            }
            PantallaXml(R.layout.prefecto_horarios, modifier) { vista ->
                PrefectoVistas.llenarHorarios(vista, turno, grupos) { id, grupo ->
                    grupoElegido = id to grupo.nombre.trim()
                    abrir(PrefectoPantalla.HorarioDeGrupo)
                }
                vista.alTocar(R.id.chip_matutino) { turno = Turno.MATUTINO }
                vista.alTocar(R.id.chip_vespertino) { turno = Turno.VESPERTINO }
                conectarMenu(vista, pantalla)
            }
        }

        PrefectoPantalla.Calendario -> CalendarioXml(
            conectarMenu = { conectarMenu(it, pantalla) },
            modifier = modifier,
        )

        PrefectoPantalla.Perfil -> PerfilPersonalXml(
            sesion = sesion,
            rol = stringResource(R.string.prefecto_rol),
            conectarMenu = { conectarMenu(it, pantalla) },
            onCambiarContrasena = { abrir(PrefectoPantalla.CambiarContrasena) },
            onCerrarSesion = onCerrarSesion,
            modifier = modifier,
        )

        PrefectoPantalla.HorarioDeGrupo -> HorarioXml(
            grupoId = grupoElegido.first,
            nombreGrupo = grupoElegido.second,
            titulo = stringResource(R.string.comun_grupo_titulo, grupoElegido.second),
            onRegresar = ::regresar,
            modifier = modifier,
        )

        PrefectoPantalla.Notificaciones -> NotificacionesXml(sesion, onRegresar = ::regresar, modifier = modifier)

        PrefectoPantalla.CambiarContrasena -> CambiarContrasenaXml(
            obligatoria = false,
            onRegresar = ::regresar,
            onCerrarSesion = onCerrarSesion,
            onLista = ::regresar,
            modifier = modifier,
        )
    }
}

/** Antes de la 1 pm se muestran primero los grupos del turno matutino; después, los del vespertino. */
private fun turnoSegunLaHora(): String =
    if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 13) Turno.MATUTINO else Turno.VESPERTINO
