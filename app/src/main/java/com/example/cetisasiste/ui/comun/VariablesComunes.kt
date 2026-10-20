package com.example.cetisasiste.ui.comun

import androidx.annotation.DrawableRes
import com.example.cetisasiste.R

/*
 * VARIABLES de las vistas de padre, prefecto, profesor y las comunes (carpeta res/layout).
 *
 * Aquí NO hay datos: solo la forma de la información que muestra cada vista. Los datos
 * reales salen de Firestore (datos/CetisRepositorio.kt) y cada apartado los convierte a
 * estas clases. Las del alumno (Alumno, Clase, Anuncio…) están en AlumnoVariables.kt.
 * (Las primeras cinco estaban antes en ui/padres/PadresModelos.kt.)
 */

/** Un evento del calendario, ya con su texto de fecha y lugar. */
data class Evento(
    val titulo: String,
    val detalle: String,
)

enum class TipoRegistro { Entrada, Salida }

data class Registro(
    val tipo: TipoRegistro,
    val hora: String,
)

/** Entradas y salidas de un mismo día, con su encabezado ("Hoy · lunes 5 de octubre"). */
data class DiaDeRegistros(
    val fecha: String,
    val registros: List<Registro>,
)

/** [Aviso] es cualquier otro tipo (alerta, salida de grupo, estatus…). */
enum class TipoNotificacion { Entrada, Salida, Anuncio, Recordatorio, Credencial, Aviso }

data class Notificacion(
    val tipo: TipoNotificacion,
    val titulo: String,
    val hora: String,
    val texto: String,
    val leida: Boolean,
)

/** El mes que dibuja el calendario. */
data class MesCalendario(
    /** "Octubre 2026" */
    val titulo: String,
    val dias: Int,
    /** Casillas vacías antes del día 1 (la semana empieza en lunes). */
    val casillasVaciasAlInicio: Int,
    /** Día de hoy si el mes es el actual; si no, null. */
    val diaDeHoy: Int?,
    val diasConEvento: Set<Int>,
)

/** Un renglón de lista (comun_item_lista.xml): ícono en un círculo, título y detalle. */
data class Renglon(
    @param:DrawableRes val icono: Int,
    val titulo: String,
    val detalle: String,
    /** Fondo del círculo: guinda (normal) o café (salidas). */
    @param:DrawableRes val fondoIcono: Int = R.drawable.shape_circulo_brand,
)

/** Datos de la cuenta del personal (prefecto, profesor) para su Perfil. */
data class Personal(
    val nombre: String,
    val rol: String,
    val usuario: String,
    val correo: String,
)

/** Estado del alumno que se ve en la etiqueta de color (StatusChip de Figma). */
enum class EstadoVisible { Activo, InactivoPorPago, InactivoPorSalud, EnPracticas }

/** Un alumno en la lista del profesor (StudentRow de Figma). */
data class AlumnoDeLista(
    val noControl: String,
    val iniciales: String,
    val nombre: String,
    val estado: EstadoVisible,
)
