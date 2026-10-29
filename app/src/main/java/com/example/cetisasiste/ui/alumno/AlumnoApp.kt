package com.example.cetisasiste.ui.alumno

import androidx.activity.compose.BackHandler
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cetisasiste.Anuncio
import com.example.cetisasiste.AlumnoVistas
import com.example.cetisasiste.Clase
import com.example.cetisasiste.HorarioDelDia
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.ui.MensajeCentrado
import com.example.cetisasiste.ui.comun.CHIPS_DE_DIAS
import com.example.cetisasiste.ui.comun.CambiarContrasenaXml
import com.example.cetisasiste.ui.comun.Campana
import com.example.cetisasiste.ui.comun.Carga
import com.example.cetisasiste.ui.comun.LocalCampana
import com.example.cetisasiste.ui.comun.NotificacionesXml
import com.example.cetisasiste.ui.comun.PantallaXml
import com.example.cetisasiste.ui.comun.Pestana
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.alTocar
import com.example.cetisasiste.ui.comun.alumnoDeLaSesionEnVivo
import com.example.cetisasiste.ui.comun.cargar
import com.example.cetisasiste.ui.comun.nombreDeGrupoEnVivo
import com.example.cetisasiste.ui.comun.sinLeerEnVivo


enum class AlumnoPantalla(@param:LayoutRes val layout: Int) {
    Inicio(R.layout.alumno_inicio),
    Boletin(R.layout.alumno_boletin),
    Horario(R.layout.alumno_horario),
    Perfil(R.layout.alumno_perfil),
    CodigoQr(R.layout.alumno_codigo_qr),
    Credencial(R.layout.alumno_credencial_digital),
    ConfirmarCerrarSesion(R.layout.alumno_confirmar_cerrar_sesion),
    // Vistas comunes a todos los roles (ui/comun/PantallasComunes.kt).
    CambiarContrasena(R.layout.comun_cambiar_contrasena),
    Notificaciones(R.layout.comun_notificaciones),
}

/** Pantallas principales del alumno: son las opciones del menú ☰ (comun_menu.xml). */
private val PESTANAS = listOf(
    Pestana(AlumnoPantalla.Inicio, R.drawable.ic_home, R.string.nav_inicio),
    Pestana(AlumnoPantalla.Boletin, R.drawable.ic_announcements, R.string.alumno_nav_boletin),
    Pestana(AlumnoPantalla.Horario, R.drawable.ic_clock, R.string.alumno_nav_horario),
    Pestana(AlumnoPantalla.Perfil, R.drawable.ic_profile, R.string.nav_perfil),
)

/**
 * Pantalla principal del alumno.
 * Los datos salen de Firestore
 */
@Composable
fun AlumnoApp(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pila by rememberSaveable { mutableStateOf(listOf(AlumnoPantalla.Inicio)) }
    fun abrir(p: AlumnoPantalla) { pila = pila + p }
    fun regresar() { pila = pila.dropLast(1) }
    // Las pantallas del menú no se apilan: debajo de cualquiera queda solo Inicio.
    fun pestana(p: AlumnoPantalla) {
        pila = if (p == AlumnoPantalla.Inicio) listOf(p) else listOf(AlumnoPantalla.Inicio, p)
    }
    BackHandler(enabled = pila.size > 1) { regresar() }
    // Avisos sin leer (campana y menú ☰), en tiempo real.
    val sinLeer = sinLeerEnVivo(sesion.uid)

    // Ficha del alumno, en tiempo real: si cambia en Firestore, la vista se vuelve a llenar.
    val doc = when (val estado = alumnoDeLaSesionEnVivo(sesion)) {
        Carga.Cargando -> return MensajeCentrado(stringResource(R.string.cargando_datos), modifier)
        Carga.Fallo -> return MensajeCentrado(stringResource(R.string.error_cargar_datos), modifier, onCerrarSesion)
        is Carga.Listo -> estado.datos
            ?: return MensajeCentrado(stringResource(R.string.sin_alumno_vinculado), modifier, onCerrarSesion)
    }
    val nombreGrupo = nombreDeGrupoEnVivo(doc.grupoId)
    val alumno = remember(doc, nombreGrupo) { Formatos.alumno(doc, nombreGrupo) }

    var dia by rememberSaveable { mutableStateOf(Formatos.diaDeHoy()) }
    val textoUltimaClase = stringResource(R.string.alumno_clase_ultima)
    val clases by produceState(emptyList<Clase>(), doc.grupoId, dia) {
        value = (cargar { CetisRepositorio.horario(doc.grupoId, Formatos.idDeDia(dia)) } as? Carga.Listo)
            ?.datos.orEmpty().map { Formatos.clase(it, textoUltimaClase) }
    }
    val anuncios by produceState(emptyList<Anuncio>(), sesion.uid) {
        value = (cargar { CetisRepositorio.anuncios(sesion.usuario.rol) } as? Carga.Listo)
            ?.datos.orEmpty().map(Formatos::anuncio)
    }
    val qr = remember(doc.noControl) { Formatos.imagenQr(NoControl.contenidoQr(doc.noControl)) }
    val rol = stringResource(R.string.comun_rol_alumno)

    val pantalla = pila.last()
    // La campana sale en la barra de arriba de todas las pantallas (menos en Notificaciones).
    CompositionLocalProvider(LocalCampana provides Campana(sinLeer) { abrir(AlumnoPantalla.Notificaciones) }) {
        when (pantalla) {
            // Las pantallas comunes piden sus propios datos.
            AlumnoPantalla.CambiarContrasena -> CambiarContrasenaXml(
                obligatoria = false,
                onRegresar = ::regresar,
                onCerrarSesion = onCerrarSesion,
                onLista = ::regresar,
                modifier = modifier,
            )
            AlumnoPantalla.Notificaciones -> NotificacionesXml(sesion, onRegresar = ::regresar, modifier = modifier)
            else -> PantallaXml(pantalla.layout, modifier) { vista ->
                when (pantalla) {
                    AlumnoPantalla.Inicio -> {
                        AlumnoVistas.llenarInicio(vista, alumno)
                        vista.alTocar(R.id.card_qr) { abrir(AlumnoPantalla.CodigoQr) }
                        vista.alTocar(R.id.card_credencial) { abrir(AlumnoPantalla.Credencial) }
                        vista.alTocar(R.id.card_horario) { pestana(AlumnoPantalla.Horario) }
                        vista.alTocar(R.id.card_boletin) { pestana(AlumnoPantalla.Boletin) }
                    }
                    AlumnoPantalla.Boletin -> AlumnoVistas.llenarBoletin(vista, anuncios)
                    AlumnoPantalla.Horario -> {
                        AlumnoVistas.llenarHorario(vista, HorarioDelDia(alumno.grupo, dia, clases))
                        CHIPS_DE_DIAS.forEach { (id, d) -> vista.alTocar(id) { dia = d } }
                    }
                    AlumnoPantalla.Perfil, AlumnoPantalla.ConfirmarCerrarSesion -> {
                        AlumnoVistas.llenarPerfil(vista, alumno)
                        vista.alTocar(R.id.opcion_qr) { abrir(AlumnoPantalla.CodigoQr) }
                        vista.alTocar(R.id.opcion_credencial) { abrir(AlumnoPantalla.Credencial) }
                        vista.alTocar(R.id.opcion_contrasena) { abrir(AlumnoPantalla.CambiarContrasena) }
                        vista.alTocar(R.id.opcion_cerrar_sesion) { abrir(AlumnoPantalla.ConfirmarCerrarSesion) }
                        vista.alTocar(R.id.btn_cancelar) { regresar() }
                        vista.alTocar(R.id.btn_confirmar) { onCerrarSesion() }
                    }
                    AlumnoPantalla.CodigoQr -> {
                        AlumnoVistas.llenarCodigoQr(vista, alumno, qr)
                        vista.alTocar(R.id.btn_regresar) { regresar() }
                    }
                    AlumnoPantalla.Credencial -> {
                        AlumnoVistas.llenarCredencial(vista, alumno)
                        vista.alTocar(R.id.btn_regresar) { regresar() }
                    }
                    // Se muestran arriba (pantallas comunes); aquí no llegan.
                    AlumnoPantalla.CambiarContrasena, AlumnoPantalla.Notificaciones -> Unit
                }
                // Menú ☰ (solo en Inicio, Boletín, Horario y Perfil). En "¿Cerrar sesión?" la capa
                // oscura tapa el botón, así que no se conecta.
                if (PESTANAS.any { it.destino == pantalla }) {
                    VistasComunes.conectarMenu(
                        vista, alumno.nombre, rol, PESTANAS, pantalla, ::pestana,
                        onNotificaciones = { abrir(AlumnoPantalla.Notificaciones) },
                        onCerrarSesion = onCerrarSesion,
                        sinLeer = sinLeer,
                    )
                }
            }
        }
    }
}
