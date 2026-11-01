package com.example.cetisasiste.pantallas.comun

import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.MensajeInicioSesion
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.UsuarioDoc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

/** Estado de una consulta a Firestore. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data object Fallo : Carga<Nothing>
    data class Listo<T>(val datos: T) : Carga<T>
}

/** Ejecuta una consulta y convierte cualquier error (sin internet, sin permiso) en [Carga.Fallo]. */
suspend fun <T> cargar(consulta: suspend () -> T): Carga<T> = try {
    Carga.Listo(consulta())
} catch (excepcion: Exception) {
    if (excepcion is CancellationException) throw excepcion
    Carga.Fallo
}

/** Lo que trajo la consulta, o [vacio] si falló o sigue cargando. */
fun <T> Carga<T>.o(vacio: T): T = (this as? Carga.Listo)?.datos ?: vacio

/**
 * La sesión abierta, compartida por todas las pantallas (cada pantalla es una Activity).
 *
 * Mientras hay sesión se escuchan en tiempo real, UNA sola vez para toda la app:
 *   - la cuenta (usuarios/{uid}): si Control Escolar la desactiva o la borra, se cierra la sesión;
 *   - cuántos avisos sin leer tiene (campana y menú ☰);
 *   - la ficha del alumno que ve la sesión (el propio alumno o el primer hijo del padre).
 */
object SesionActiva {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sesionActual = MutableStateFlow<Sesion?>(null)
    private val cuantosSinLeer = MutableStateFlow(0)
    private var escuchas: Job? = null

    /** La sesión abierta, o null si no hay (o se cerró). */
    val sesion: StateFlow<Sesion?> = sesionActual.asStateFlow()

    /** Cuántos avisos sin leer tiene la cuenta (0 si no se sabe). */
    val sinLeer: StateFlow<Int> = cuantosSinLeer.asStateFlow()

    /** Por qué se cerró la sesión sin que la persona lo pidiera (lo muestra Iniciar sesión). */
    var motivoDeCierre: MensajeInicioSesion? = null

    /** true en cuanto la persona cambia su contraseña obligatoria (aunque Firestore tarde en avisar). */
    var contrasenaCambiada = false

    /** Empieza la sesión y sus escuchas en tiempo real. */
    fun iniciar(nueva: Sesion) {
        sesionActual.value = nueva
        motivoDeCierre = null
        escuchas?.cancel()
        escuchas = alcance.launch {
            launch {
                CetisRepositorio.escucharUsuario(nueva.idUsuario).collect { resultado ->
                    val usuario = resultado.getOrNull()
                    when {
                        resultado.isFailure -> Unit // Sin conexión o sin permiso: se queda lo último que se leyó.
                        usuario == null -> cerrar(MensajeInicioSesion.CuentaNoRegistrada)
                        !usuario.activo -> cerrar(MensajeInicioSesion.CuentaDesactivada)
                        else -> sesionActual.value = sesionActual.value?.copy(usuario = usuario)
                    }
                }
            }
            launch {
                CetisRepositorio.escucharSinLeer(nueva.idUsuario).collect { cuantosSinLeer.value = it.getOrNull() ?: 0 }
            }
        }
    }

    /**
     * Si la app se cerró sin "Cerrar sesión" (o Android la cerró en segundo plano), recupera la
     * sesión de Firebase Authentication. null si no había.
     */
    suspend fun recuperar(): Sesion? {
        sesionActual.value?.let { return it }
        val guardada = CetisRepositorio.sesionGuardada() ?: return null
        iniciar(guardada)
        return guardada
    }

    /** Cierra la sesión. Las pantallas abiertas lo notan y regresan a Iniciar sesión. */
    fun cerrar(motivo: MensajeInicioSesion? = null) {
        motivoDeCierre = motivo
        contrasenaCambiada = false
        escuchas?.cancel()
        escuchas = null
        cuantosSinLeer.value = 0
        CetisRepositorio.cerrarSesion()
        sesionActual.value = null
    }

    /**
     * Ficha del alumno que ve la sesión, en tiempo real. Se escucha mientras alguna pantalla
     * la use (y 5 segundos más, para no volver a leerla al pasar de una pantalla a otra).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val alumno: StateFlow<Carga<AlumnoDoc?>> = sesionActual
        // null = no hay sesión (se está cerrando): se espera, no es "sin alumno vinculado".
        .map { actual -> actual?.let(CetisRepositorio::noControlDeLaSesion) }
        .distinctUntilChanged()
        .flatMapLatest { noControl ->
            when {
                noControl == null -> flowOf(Carga.Cargando)
                noControl.isBlank() -> flowOf(Carga.Listo(null))
                else -> CetisRepositorio.escucharAlumno(noControl).map { resultado ->
                    if (resultado.isSuccess) Carga.Listo(resultado.getOrNull()) else Carga.Fallo
                }
            }
        }
        // Al dejar de usarse se olvida la ficha: otra sesión nunca ve la de la anterior.
        .stateIn(alcance, SharingStarted.WhileSubscribed(ESPERA_ANTES_DE_SOLTAR_MS, 0), Carga.Cargando)


    /** true si la cuenta debe cambiar su contraseña: lo pidió Control Escolar o ya venció. */
    fun debeCambiarContrasena(usuario: UsuarioDoc): Boolean =
        !contrasenaCambiada &&
            (usuario.debeCambiarContrasena || usuario.contrasenaVence?.toDate()?.before(Date()) == true)

    private const val ESPERA_ANTES_DE_SOLTAR_MS = 5_000L
}
