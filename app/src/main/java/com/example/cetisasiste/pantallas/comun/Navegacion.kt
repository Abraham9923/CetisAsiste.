package com.example.cetisasiste.pantallas.comun

import android.app.Activity
import android.content.Intent
import com.example.cetisasiste.datos.Rol
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.alumno.InicioAlumnoActividad
import com.example.cetisasiste.pantallas.padre.InicioPadreActividad
import com.example.cetisasiste.pantallas.prefecto.InicioPrefectoActividad
import com.example.cetisasiste.pantallas.profesor.InicioProfesorActividad

/**
 * Nombres de los datos que viajan de una pantalla a otra como extras del Intent.
 * Se escriben una sola vez para no equivocarse.
 */
object Claves {
    /** Boolean: el cambio de contraseña es obligatorio (no se puede salir sin cambiarla). */
    const val CONTRASENA_OBLIGATORIA = "contrasena_obligatoria"
    /** String: qué mensaje muestra MensajeActividad (nombre de [TipoMensaje]). */
    const val TIPO_MENSAJE = "tipo_mensaje"
    /** String: No. de control del alumno. */
    const val NO_CONTROL = "no_control"
    /** String: id del grupo ("2026-2027-1_102"). */
    const val GRUPO_ID = "grupo_id"
    /** String: nombre del grupo ("102"). */
    const val NOMBRE_GRUPO = "nombre_grupo"
    /** String: "entrada" o "salida" (escaneo del prefecto). */
    const val TIPO_REGISTRO = "tipo_registro"
    /** String: tipo de estatus elegido de antemano ("falta", "reporte", "llamada_atencion"). */
    const val TIPO_ESTATUS = "tipo_estatus"

    // Resultado de un escaneo (ResultadoActividad)
    const val RESULTADO_ALERTA = "resultado_alerta"
    const val RESULTADO_HORA = "resultado_hora"
    const val RESULTADO_NOMBRE = "resultado_nombre"
    const val RESULTADO_INICIALES = "resultado_iniciales"
    const val RESULTADO_DETALLE = "resultado_detalle"
    const val RESULTADO_ETIQUETA = "resultado_etiqueta"
    const val RESULTADO_AVISO = "resultado_aviso"
}

/** Mensajes que muestra MensajeActividad. */
enum class TipoMensaje { RolSinPantallas, SinAlumnoVinculado, ErrorAlCargar }

/**
 * Cambio de pantallas. Cada pantalla completa es una Activity y se abre con un Intent
 * explícito (startActivity); los datos van como extras (ver [Claves]).
 */
object Navegacion {

    /** Abre [destino] encima de la pantalla actual. [datos] pone los extras del Intent. */
    fun abrir(actividad: Activity, destino: Class<out Activity>, datos: Intent.() -> Unit = {}) {
        actividad.startActivity(Intent(actividad, destino).apply(datos))
    }

    /** La primera pantalla del rol de la sesión (la de "Inicio" en su menú). */
    fun inicioDelRol(rol: String): Class<out Activity>? = when (rol) {
        Rol.ALUMNO -> InicioAlumnoActividad::class.java
        Rol.PADRE -> InicioPadreActividad::class.java
        Rol.PREFECTO -> InicioPrefectoActividad::class.java
        Rol.PROFESOR -> InicioProfesorActividad::class.java
        else -> null
    }

    /**
     * Después de iniciar sesión (o al abrir la app con una sesión guardada): si la cuenta debe
     * cambiar su contraseña, primero esa pantalla; si no, el Inicio de su rol. Enfermería y
     * Control Escolar ven un mensaje (usan la web, ADR-011). Borra las pantallas anteriores.
     */
    fun abrirInicioDeLaSesion(actividad: Activity, sesion: Sesion) {
        val destino = inicioDelRol(sesion.usuario.rol)
        val intent = when {
            SesionActiva.debeCambiarContrasena(sesion.usuario) ->
                Intent(actividad, CambiarContrasenaActividad::class.java).putExtra(Claves.CONTRASENA_OBLIGATORIA, true)
            destino != null -> Intent(actividad, destino)
            else -> Intent(actividad, MensajeActividad::class.java).putExtra(Claves.TIPO_MENSAJE, TipoMensaje.RolSinPantallas.name)
        }
        actividad.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        actividad.finish()
    }

    /** Muestra un mensaje a pantalla completa (en lugar de la pantalla actual). */
    fun abrirMensaje(actividad: Activity, tipo: TipoMensaje) {
        actividad.startActivity(
            Intent(actividad, MensajeActividad::class.java)
                .putExtra(Claves.TIPO_MENSAJE, tipo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        actividad.finish()
    }

    /** Regresa a Iniciar sesión y borra todas las pantallas abiertas. */
    fun irAIniciarSesion(actividad: Activity) {
        actividad.startActivity(
            Intent(actividad, IniciarSesionActividad::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        actividad.finish()
    }

    /** Cierra la sesión (después de confirmarlo en el diálogo) y regresa a Iniciar sesión. */
    fun cerrarSesion(actividad: Activity) {
        SesionActiva.cerrar()
        irAIniciarSesion(actividad)
    }

    /**
     * Abre una pantalla principal desde el menú ☰. Las pantallas principales no se apilan:
     * debajo de cualquiera de ellas queda solo el Inicio del rol.
     */
    fun abrirDesdeElMenu(actividad: Activity, destino: Class<out Activity>, inicio: Class<out Activity>) {
        val alInicio = Intent(actividad, inicio).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (destino == inicio) {
            actividad.startActivity(alInicio)
        } else {
            actividad.startActivities(arrayOf(alInicio, Intent(actividad, destino)))
        }
        if (actividad.javaClass != inicio) actividad.finish()
    }
}
