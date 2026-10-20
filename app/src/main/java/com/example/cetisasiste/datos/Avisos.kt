package com.example.cetisasiste.datos

import com.google.firebase.Timestamp
import java.util.Date

/*
 * REGLAS DE LOS AVISOS (notificaciones dentro de la app). Por qué así: docs/adr/ADR-010.
 *
 * Todos los avisos van al mismo lugar, el buzón de cada cuenta: usuarios/{uid}/notificaciones.
 * Aquí no se consulta Firebase: solo se decide a quién se le avisa y se arma el documento.
 * Por eso se puede probar sin teléfono (app/src/test/.../AvisosTest.kt). Los textos que lee la
 * persona están en strings_*.xml; quien llama los pasa ya escritos.
 *
 * Cuando haya Cloud Functions (plan Blaze), una función que escuche ese buzón mandará cada
 * aviso también como notificación push, sin cambiar nada de esto.
 */
object Avisos {

    /**
     * Estatus que se avisan al padre si configuracion/plantel no dice otra cosa: todos los que
     * pone el profesor. El plantel lo cambia con el campo `avisarAlPadreEstatus`.
     */
    val ESTATUS_POR_OMISION = listOf(
        TipoEstatus.FALTA,
        TipoEstatus.REPORTE,
        TipoEstatus.LLAMADA_ATENCION,
        TipoEstatus.CASTIGO,
    )

    /**
     * Alertas del escaneo que se avisan al padre si configuracion/plantel no dice otra cosa:
     * las que el padre necesita saber (no pudo entrar por estar inactivo; quiso salir sin
     * permiso). "Ya ingresó" y "no ha ingresado" suelen ser errores al escanear y no se avisan.
     * El plantel lo cambia con el campo `avisarAlPadreAlertas`.
     */
    val ALERTAS_POR_OMISION = listOf(
        Alerta.INACTIVO,
        Alerta.SALIDA_FUERA_DE_HORARIO,
    )

    /** Días que se guarda un aviso antes de poder borrarlo (campo `venceEn`). */
    const val DIAS_DE_VIGENCIA = 60

    /** Cuántos avisos sin leer se cuentan para la campana; si hay más, se muestra "9+". */
    const val MAXIMO_SIN_LEER = 10L

    /** ¿Se le avisa al padre de este escaneo? Sin alerta, siempre; con alerta, si está en la lista. */
    fun alPadrePorEscaneo(alerta: String?, configuracion: ConfiguracionDoc): Boolean =
        alerta == null || alerta in configuracion.avisarAlPadreAlertas

    /** ¿Se le avisa al padre de este estatus que pone el profesor? */
    fun alPadrePorEstatus(tipo: String, configuracion: ConfiguracionDoc): Boolean =
        tipo in configuracion.avisarAlPadreEstatus

    /** Padres y tutores del alumno a quienes se les puede avisar (sin repetidos ni vacíos). */
    fun destinatarios(alumno: AlumnoDoc?): List<String> =
        alumno?.padres.orEmpty().filter { it.isNotBlank() }.distinct()

    /**
     * Un aviso nuevo, sin leer, firmado por [creadaPor] (el uid de la sesión, como piden las
     * reglas) y con su fecha de vencimiento.
     */
    fun nuevo(
        tipo: String,
        titulo: String,
        mensaje: String,
        alumnoId: String,
        creadaPor: String,
        ahora: Timestamp = Timestamp.now(),
    ): NotificacionDoc = NotificacionDoc(
        tipo = tipo,
        titulo = titulo.trim().take(MAXIMO_TITULO),
        mensaje = mensaje.trim().take(MAXIMO_MENSAJE),
        fecha = ahora,
        leida = false,
        alumnoId = alumnoId,
        creadaPor = creadaPor,
        venceEn = Timestamp(Date(ahora.toDate().time + DIAS_DE_VIGENCIA * UN_DIA_MS)),
    )

    /** Texto del contador de la campana: "" si no hay, "3", o "9+" si hay más de nueve. */
    fun textoSinLeer(sinLeer: Int): String = when {
        sinLeer <= 0 -> ""
        sinLeer >= MAXIMO_SIN_LEER -> "${MAXIMO_SIN_LEER - 1}+"
        else -> sinLeer.toString()
    }

    // Los mismos límites que firebase/firestore.rules (avisoValido).
    private const val MAXIMO_TITULO = 120
    private const val MAXIMO_MENSAJE = 500
    private const val UN_DIA_MS = 24L * 60 * 60 * 1000
}
