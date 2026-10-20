package com.example.cetisasiste.datos

import java.util.Calendar
import java.util.Date

/*
 * REGLAS DEL ESCANEO DEL PREFECTO (entradas y salidas).
 *
 * Aquí no se consulta Firebase: solo se decide qué alerta corresponde con los datos que ya
 * se leyeron. Por eso se puede probar sin teléfono (app/src/test/.../EscaneoTest.kt).
 * Las alertas y cuándo aparecen están en docs/BASE_DE_DATOS.md §5.3 y en ADR-009.
 */

/** Lo que se sabe del alumno en el momento de escanearlo. */
data class DatosDelEscaneo(
    /** Su ficha, o null si no existe ningún alumno con ese No. de control. */
    val alumno: AlumnoDoc?,
    /** TipoRegistro.ENTRADA o TipoRegistro.SALIDA (lo elige el prefecto). */
    val tipo: String,
    /** Sus estatus más recientes (castigos, pases de salud…). */
    val estatus: List<EstatusDoc>,
    /** true si el profesor ya dio salida hoy a su grupo. */
    val grupoYaSalio: Boolean,
    val ahora: Date = Date(),
)

object Escaneo {

    /**
     * La alerta que corresponde, o null si puede pasar sin alerta. Si aplican varias, se da
     * la primera de esta lista:
     *
     *   Entrada: no dado de alta → inactivo (pago o salud) → ya ingresó → castigo.
     *   Salida:  no dado de alta → no ha ingresado → castigo → salida fuera de horario.
     */
    fun alerta(datos: DatosDelEscaneo): String? {
        val alumno = datos.alumno ?: return Alerta.NO_DADO_DE_ALTA
        // Un alumno dado de baja o egresado ya no está inscrito: es como si no estuviera dado de alta.
        if (alumno.estado != EstadoAlumno.INSCRITO) return Alerta.NO_DADO_DE_ALTA
        val castigo = tieneCastigoVigente(datos.estatus, datos.ahora)

        return if (datos.tipo == TipoRegistro.ENTRADA) {
            when {
                !alumno.activoPorPago || !alumno.activoPorSalud -> Alerta.INACTIVO
                estaDentro(alumno) -> Alerta.YA_INGRESO
                castigo -> Alerta.CASTIGO
                else -> null
            }
        } else {
            when {
                !estaDentro(alumno) -> Alerta.NO_HA_INGRESADO
                castigo -> Alerta.CASTIGO
                !salidaAutorizada(datos) -> Alerta.SALIDA_FUERA_DE_HORARIO
                else -> null
            }
        }
    }

    /** Dónde queda el alumno después de un registro sin alerta. */
    fun ubicacionDespues(tipo: String): String =
        if (tipo == TipoRegistro.ENTRADA) Ubicacion.DENTRO else Ubicacion.FUERA

    /** "dentro" y "salida_autorizada" son dentro del plantel. */
    private fun estaDentro(alumno: AlumnoDoc): Boolean =
        alumno.ubicacion == Ubicacion.DENTRO || alumno.ubicacion == Ubicacion.SALIDA_AUTORIZADA

    /**
     * Puede salir sin alerta si su profesor ya liberó al grupo, si tiene pase de salud de hoy,
     * si está en prácticas o si ya tenía la salida autorizada.
     */
    private fun salidaAutorizada(datos: DatosDelEscaneo): Boolean {
        val alumno = datos.alumno ?: return false
        return datos.grupoYaSalio ||
            alumno.enPracticas ||
            alumno.ubicacion == Ubicacion.SALIDA_AUTORIZADA ||
            datos.estatus.any { it.tipo == TipoEstatus.PASE_SALUD && aplicaHoy(it, datos.ahora) }
    }

    /**
     * Un castigo cuenta si su `vigenteHasta` todavía no pasa. Si no tiene fecha de fin (el
     * plantel todavía no dice cuánto dura un castigo), cuenta solo el día en que se puso.
     */
    fun tieneCastigoVigente(estatus: List<EstatusDoc>, ahora: Date): Boolean = castigoVigente(estatus, ahora) != null

    /** El castigo que cuenta hoy (el más reciente de la lista), o null si no hay. Lo muestra el resultado. */
    fun castigoVigente(estatus: List<EstatusDoc>, ahora: Date): EstatusDoc? =
        estatus.firstOrNull { it.tipo == TipoEstatus.CASTIGO && aplicaHoy(it, ahora) }

    /** El estatus sigue vigente: hasta su `vigenteHasta`, o si no tiene, solo el día de su `fecha`. */
    private fun aplicaHoy(estatus: EstatusDoc, ahora: Date): Boolean {
        val hasta = estatus.vigenteHasta?.toDate()
        if (hasta != null) return !hasta.before(ahora)
        val fecha = estatus.fecha?.toDate() ?: return false
        return mismoDia(fecha, ahora)
    }

    private fun mismoDia(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
