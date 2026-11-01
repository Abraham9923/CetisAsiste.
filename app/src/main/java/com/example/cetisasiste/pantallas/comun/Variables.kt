package com.example.cetisasiste.pantallas.comun

import androidx.annotation.DrawableRes
import com.example.cetisasiste.R

/*
 * VARIABLES de las vistas (las de res/layout).
 *
 * Aquí NO hay datos: solo la forma de la información que muestra cada vista. Los datos reales
 * salen de Firestore (datos/CetisRepositorio.kt) y datos/Formatos.kt los convierte a estas
 * clases. (Antes estaban en AlumnoVariables.kt y ui/comun/VariablesComunes.kt.)
 */

/** Datos del alumno que se muestran en Inicio, Perfil, Código QR y Credencial. */
data class Alumno(
    val nombre: String = "",
    val rol: String = "",
    val grupo: String = "",
    val noControl: String = "",
    val correo: String = "",
    /** Ciclo escolar de la credencial. */
    val vigencia: String = "",
)

/** Una fila del horario (alumno_item_clase.xml). */
data class Clase(
    val materia: String = "",
    /** Rango de horas, tal como se mostrará ("7:00 – 7:50 am"). */
    val horario: String = "",
    /** Texto después del punto medio (profesor, aula, "Última clase del turno"…). */
    val detalle: String = "",
)

/** Días que aparecen como chips (RadioButton) en los horarios. */
enum class DiaSemana { LUNES, MARTES, MIERCOLES, JUEVES, VIERNES }

/** Una tarjeta del Boletín de noticias (alumno_item_anuncio.xml). */
data class Anuncio(
    val etiqueta: String = "",
    val fecha: String = "",
    val titulo: String = "",
    val texto: String = "",
)

/** Un evento del calendario, ya con su texto de fecha y lugar. */
data class Evento(
    val titulo: String,
    val detalle: String,
)

/** Cómo se dibuja cada aviso (ícono y color). [Aviso] es cualquier tipo que la app no conozca. */
enum class TipoNotificacion { Entrada, Salida, Anuncio, Recordatorio, Credencial, Alerta, Estatus, Estado, SalidaDeGrupo, Aviso }

/** Un aviso del buzón (comun_item_notificacion.xml). */
data class Notificacion(
    val tipo: TipoNotificacion,
    val titulo: String,
    val hora: String,
    val texto: String,
    val leida: Boolean,
)

/** Una casilla del calendario (comun_item_dia.xml). [numero] null = casilla vacía. */
data class CasillaDia(
    val numero: Int?,
    val esHoy: Boolean = false,
    val esFinDeSemana: Boolean = false,
    val conEvento: Boolean = false,
)

/** Un renglón de lista (comun_item_lista.xml): ícono en un círculo, título y detalle. */
data class Renglon(
    @param:DrawableRes val icono: Int,
    val titulo: String,
    val detalle: String,
    /** Fondo del círculo: guinda (normal), café (salidas), rojo (alertas), amarillo (historial). */
    @param:DrawableRes val fondoIcono: Int = R.drawable.shape_circulo_brand,
    /** Fondo de la tarjeta: normal o rojo claro (escaneo con alerta). */
    @param:DrawableRes val fondo: Int = R.drawable.shape_tarjeta,
)

/** Datos de la cuenta del personal (prefecto, profesor) para su Perfil. */
data class Personal(
    val nombre: String,
    /** "Prefectura" o "Docente" */
    val area: String,
    val usuario: String,
    val correo: String,
)

/** Colores de la etiqueta de estatus (StatusChip de Figma 76:538). */
enum class ColorEstatus { Exito, Advertencia, Peligro }

/** Un alumno en la lista del profesor (StudentRow de Figma 76:542). */
data class AlumnoDeLista(
    val noControl: String,
    val iniciales: String,
    val nombre: String,
    /** Último estatus ("Sin incidencias", "Falta notificada"…); null mientras se consulta. */
    val estatus: String?,
    val color: ColorEstatus,
)

/** Un elemento de una lista con encabezados (Entradas y salidas, Notificaciones). */
sealed interface ElementoLista<out T> {
    data class Encabezado(val texto: String) : ElementoLista<Nothing>
    data class Fila<T>(val dato: T) : ElementoLista<T>
}
