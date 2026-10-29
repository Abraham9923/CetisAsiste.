package com.example.cetisasiste.ui.padres

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.RegistroDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.ui.MensajeCentrado
import com.example.cetisasiste.ui.comun.CalendarioXml
import com.example.cetisasiste.ui.comun.CambiarContrasenaXml
import com.example.cetisasiste.ui.comun.Campana
import com.example.cetisasiste.ui.comun.Carga
import com.example.cetisasiste.ui.comun.DiaDeRegistros
import com.example.cetisasiste.ui.comun.HorarioXml
import com.example.cetisasiste.ui.comun.LocalCampana
import com.example.cetisasiste.ui.comun.NotificacionesXml
import com.example.cetisasiste.ui.comun.PantallaXml
import com.example.cetisasiste.ui.comun.Pestana
import com.example.cetisasiste.ui.comun.Registro
import com.example.cetisasiste.ui.comun.TipoRegistro
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.alTocar
import com.example.cetisasiste.ui.comun.alumnoDeLaSesionEnVivo
import com.example.cetisasiste.ui.comun.cargar
import com.example.cetisasiste.ui.comun.nombreDeGrupoEnVivo
import com.example.cetisasiste.ui.comun.o
import com.example.cetisasiste.ui.comun.sinLeerEnVivo
import kotlinx.coroutines.launch

/** Pantallas del padre (después de iniciar sesión). Las cuatro primeras están en el menú ☰. */
enum class PadrePantalla {
    Inicio,
    Anuncios,
    Calendario,
    Perfil,
    CodigoQr,
    Credencial,
    EntradasSalidas,
    Horario,
    Notificaciones,
    CambiarContrasena,
}

/** Pantallas principales del padre: son las opciones del menú ☰ (comun_menu.xml). */
private val PESTANAS = listOf(
    Pestana(PadrePantalla.Inicio, R.drawable.ic_home, R.string.nav_inicio),
    Pestana(PadrePantalla.Anuncios, R.drawable.ic_announcements, R.string.nav_anuncios),
    Pestana(PadrePantalla.Calendario, R.drawable.ic_calendar, R.string.nav_calendario),
    Pestana(PadrePantalla.Perfil, R.drawable.ic_profile, R.string.nav_perfil),
)

/**
 * Apartado del padre de familia. Muestra las vistas XML (padre_*.xml, comun_*.xml y, para el
 * QR y la credencial del hijo, las del alumno), les pasa sus datos y conecta sus botones.
 *
 * Los datos salen de Firestore. El alumno que se muestra es el primer hijo de
 * usuarios/{uid}.hijos.
 */
@Composable
fun PadresApp(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pila de pantallas abiertas: la última es la que se ve y "regresar" la quita.
    var pila by rememberSaveable { mutableStateOf(listOf(PadrePantalla.Inicio)) }
    fun abrir(p: PadrePantalla) { pila = pila + p }
    fun regresar() { pila = pila.dropLast(1) }
    // Las pantallas del menú no se apilan: debajo de cualquiera de ellas siempre queda solo Inicio.
    fun pestana(p: PadrePantalla) {
        pila = if (p == PadrePantalla.Inicio) listOf(p) else listOf(PadrePantalla.Inicio, p)
    }
    BackHandler(enabled = pila.size > 1) { regresar() }
    // Avisos sin leer (campana y menú ☰), en tiempo real.
    val sinLeer = sinLeerEnVivo(sesion.uid)

    // Ficha del hijo, en tiempo real: si cambia en Firestore, la pantalla se actualiza sola.
    val doc = when (val estado = alumnoDeLaSesionEnVivo(sesion)) {
        Carga.Cargando -> return MensajeCentrado(stringResource(R.string.cargando_datos), modifier)
        Carga.Fallo -> return MensajeCentrado(stringResource(R.string.error_cargar_datos), modifier, onCerrarSesion)
        is Carga.Listo -> estado.datos
            ?: return MensajeCentrado(stringResource(R.string.sin_alumno_vinculado), modifier, onCerrarSesion)
    }
    val nombreGrupo = nombreDeGrupoEnVivo(doc.grupoId)
    val alumno = remember(doc, nombreGrupo) { Formatos.alumno(doc, nombreGrupo) }

    val rol = stringResource(R.string.comun_rol_padre)
    /** Conecta el botón ☰ con el menú del padre; marca la opción de [p]. */
    fun conectarMenu(vista: View, p: PadrePantalla) =
        VistasComunes.conectarMenu(
            vista, sesion.usuario.nombre.trim(), rol, PESTANAS, p, ::pestana,
            onNotificaciones = { abrir(PadrePantalla.Notificaciones) },
            onCerrarSesion = onCerrarSesion,
            sinLeer = sinLeer,
        )

    // La campana sale en la barra de arriba de todas las pantallas (menos en Notificaciones).
    CompositionLocalProvider(LocalCampana provides Campana(sinLeer) { abrir(PadrePantalla.Notificaciones) }) {
        when (val pantalla = pila.last()) {
            PadrePantalla.Inicio -> {
                val destacado by produceState<String?>(null, sesion.uid) {
                    value = cargar { CetisRepositorio.anuncios(sesion.usuario.rol) }.o(emptyList())
                        .firstOrNull { it.destacado }?.titulo
                }
                PantallaXml(R.layout.padre_inicio, modifier) { vista ->
                    PadresVistas.llenarInicio(vista, alumno, destacado)
                    vista.alTocar(R.id.banner_importante) { pestana(PadrePantalla.Anuncios) }
                    vista.alTocar(R.id.card_qr) { abrir(PadrePantalla.CodigoQr) }
                    vista.alTocar(R.id.card_credencial) { abrir(PadrePantalla.Credencial) }
                    vista.alTocar(R.id.card_entradas) { abrir(PadrePantalla.EntradasSalidas) }
                    vista.alTocar(R.id.card_horario) { abrir(PadrePantalla.Horario) }
                    conectarMenu(vista, pantalla)
                }
            }

            PadrePantalla.Anuncios -> {
                // null mientras carga (así no se ve "Todavía no hay anuncios" antes de tiempo).
                val anuncios by produceState<List<Anuncio>?>(null, sesion.uid) {
                    value = cargar { CetisRepositorio.anuncios(sesion.usuario.rol) }.o(emptyList()).map(Formatos::anuncio)
                }
                PantallaXml(R.layout.padre_anuncios, modifier) { vista ->
                    PadresVistas.llenarAnuncios(vista, anuncios.orEmpty(), cargado = anuncios != null)
                    conectarMenu(vista, pantalla)
                }
            }

            PadrePantalla.Calendario -> CalendarioXml(
                conectarMenu = { conectarMenu(it, pantalla) },
                modifier = modifier,
            )

            PadrePantalla.Perfil -> PantallaXml(R.layout.padre_perfil, modifier) { vista ->
                AlumnoVistas.llenarPerfil(vista, alumno)
                vista.alTocar(R.id.opcion_credencial) { abrir(PadrePantalla.Credencial) }
                vista.alTocar(R.id.opcion_entradas) { abrir(PadrePantalla.EntradasSalidas) }
                vista.alTocar(R.id.opcion_contrasena) { abrir(PadrePantalla.CambiarContrasena) }
                vista.alTocar(R.id.opcion_cerrar_sesion) { VistasComunes.mostrarDialogoCerrarSesion(vista, onCerrarSesion) }
                conectarMenu(vista, pantalla)
            }

            PadrePantalla.CodigoQr -> {
                val qr = remember(doc.noControl) { Formatos.imagenQr(NoControl.contenidoQr(doc.noControl)) }
                PantallaXml(R.layout.alumno_codigo_qr, modifier) { vista ->
                    AlumnoVistas.llenarCodigoQr(vista, alumno, qr)
                    vista.alTocar(R.id.btn_regresar) { regresar() }
                }
            }

            PadrePantalla.Credencial -> PantallaXml(R.layout.alumno_credencial_digital, modifier) { vista ->
                AlumnoVistas.llenarCredencial(vista, alumno)
                vista.alTocar(R.id.btn_regresar) { regresar() }
            }

            PadrePantalla.EntradasSalidas -> {
                // Se piden de 20 en 20 (solo del semestre en curso); "Ver más" trae los siguientes.
                val scope = rememberCoroutineScope()
                var registros by remember(doc.noControl) { mutableStateOf(emptyList<RegistroDoc>()) }
                var cargado by remember(doc.noControl) { mutableStateOf(false) }
                var hayMas by remember(doc.noControl) { mutableStateOf(false) }
                suspend fun cargarPagina() {
                    val pagina = cargar {
                        CetisRepositorio.registros(doc.noControl, despuesDe = registros.lastOrNull()?.fechaHora)
                    }.o(emptyList())
                    registros = registros + pagina
                    hayMas = pagina.size.toLong() == CetisRepositorio.POR_PAGINA
                    cargado = true
                }
                LaunchedEffect(doc.noControl) { cargarPagina() }
                PantallaXml(R.layout.padre_entradas_salidas, modifier) { vista ->
                    PadresVistas.llenarEntradasSalidas(vista, primerNombre(alumno.nombre), agruparPorDia(registros), cargado, hayMas)
                    vista.alTocar(R.id.txt_ver_mas) { scope.launch { cargarPagina() } }
                    vista.alTocar(R.id.btn_regresar) { regresar() }
                }
            }

            PadrePantalla.Horario -> HorarioXml(
                grupoId = doc.grupoId,
                nombreGrupo = alumno.grupo,
                titulo = stringResource(R.string.horario_title),
                onRegresar = ::regresar,
                modifier = modifier,
            )

            PadrePantalla.Notificaciones -> NotificacionesXml(sesion, onRegresar = ::regresar, modifier = modifier)

            PadrePantalla.CambiarContrasena -> CambiarContrasenaXml(
                obligatoria = false,
                onRegresar = ::regresar,
                onCerrarSesion = onCerrarSesion,
                onLista = ::regresar,
                modifier = modifier,
            )
        }
    }
}

private fun primerNombre(nombre: String): String = nombre.substringBefore(' ')

/** Agrupa los registros por día (más reciente primero) y, dentro del día, en orden de hora. */
private fun agruparPorDia(registros: List<RegistroDoc>): List<DiaDeRegistros> =
    registros.groupBy { it.fecha }.values.map { delDia ->
        val ordenados = delDia.sortedBy { it.fechaHora }
        DiaDeRegistros(
            fecha = Formatos.tituloDeDia(ordenados.first().fechaHora),
            registros = ordenados.map {
                Registro(
                    tipo = if (it.tipo == "salida") TipoRegistro.Salida else TipoRegistro.Entrada,
                    hora = Formatos.hora(it.fechaHora),
                )
            },
        )
    }
