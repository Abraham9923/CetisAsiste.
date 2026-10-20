package com.example.cetisasiste.ui.comun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Sesion
import kotlinx.coroutines.flow.map

/*
 * Ayudantes para pedir datos a Firestore desde las pantallas de todos los roles.
 * (Antes vivían en ui/padres/PadresApp.kt; se movieron aquí porque ya los usan todos.)
 */

/** Estado de una consulta a Firestore. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data object Fallo : Carga<Nothing>
    data class Listo<T>(val datos: T) : Carga<T>
}

/** Ejecuta una consulta y convierte cualquier error (sin internet, sin permiso) en [Carga.Fallo]. */
suspend fun <T> cargar(consulta: suspend () -> T): Carga<T> = try {
    Carga.Listo(consulta())
} catch (e: Exception) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    Carga.Fallo
}

/** Lo que trajo la consulta, o [vacio] si falló. */
fun <T> Carga<T>.o(vacio: T): T = (this as? Carga.Listo)?.datos ?: vacio

/**
 * Ficha del alumno que ve la sesión (el propio alumno o el primer hijo del padre),
 * escuchada en tiempo real. La usan PadresApp y AlumnoApp.
 */
@Composable
fun alumnoDeLaSesionEnVivo(sesion: Sesion): Carga<AlumnoDoc?> {
    val noControl = CetisRepositorio.noControlDeLaSesion(sesion)
    val flujo = remember(noControl) {
        CetisRepositorio.escucharAlumno(noControl).map<Result<AlumnoDoc?>, Carga<AlumnoDoc?>> { r ->
            if (r.isSuccess) Carga.Listo(r.getOrNull()) else Carga.Fallo
        }
    }
    val estado by flujo.collectAsState(initial = Carga.Cargando)
    return estado
}

/**
 * Cuántos avisos sin leer tiene la cuenta (para la campana y el menú ☰), en tiempo real. Si
 * no se puede saber (sin internet), cuenta 0. Lo usa el *App.kt de cada rol una sola vez, para
 * que todas sus pantallas compartan la misma escucha.
 */
@Composable
fun sinLeerEnVivo(uid: String): Int {
    val flujo = remember(uid) { CetisRepositorio.escucharSinLeer(uid) }
    val resultado by flujo.collectAsState(initial = null)
    return resultado?.getOrNull() ?: 0
}

/** Nombre del grupo (grupos/{grupoId}.nombre), escuchado en tiempo real; null mientras carga. */
@Composable
fun nombreDeGrupoEnVivo(grupoId: String): String? {
    val flujo = remember(grupoId) { CetisRepositorio.escucharGrupo(grupoId) }
    val resultado by flujo.collectAsState(initial = null)
    return resultado?.getOrNull()?.nombre
}
