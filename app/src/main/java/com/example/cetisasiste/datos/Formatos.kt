package com.example.cetisasiste.datos

import android.graphics.Bitmap
import android.graphics.Color
import com.example.cetisasiste.Alumno
import com.example.cetisasiste.Anuncio
import com.example.cetisasiste.Clase
import com.example.cetisasiste.DiaSemana
import com.google.firebase.Timestamp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Convierte lo que viene de Firestore (fechas, horas, documentos) en los textos
 * que muestran las pantallas, con el formato de Figma:
 *   "Hoy · 5 oct", "Viernes 2 de octubre", "7:05 am", "7:00 – 7:50 am"…
 *
 * No consulta la base: solo transforma datos que ya se tienen.
 */
object Formatos {

    private val ES = Locale.forLanguageTag("es-MX")

    /** Texto que se muestra como rol del alumno en tarjetas y credencial. */
    const val ROL_ALUMNO = "Estudiante"

    // ---------- Documentos → variables de las vistas ----------

    /**
     * AlumnoDoc (Firestore) → Alumno (lo que muestran las pantallas).
     * [nombreGrupo] es grupos/{grupoId}.nombre; si no se pudo leer, se usa la copia
     * guardada en la ficha (alumnos/{m}.grupo).
     */
    fun alumno(doc: AlumnoDoc, nombreGrupo: String? = null): Alumno = Alumno(
        nombre = doc.nombre.trim(),
        rol = ROL_ALUMNO,
        grupo = nombreGrupo?.trim()?.ifBlank { null } ?: doc.grupo.trim(),
        noControl = doc.noControl,
        correo = doc.correo,
        vigencia = doc.vigencia,
    )

    fun anuncio(doc: AnuncioDoc): Anuncio = Anuncio(
        etiqueta = doc.area,
        fecha = fechaCorta(doc.fecha),
        titulo = doc.titulo,
        texto = doc.texto,
    )

    fun clase(doc: ClaseDoc, textoUltimaClase: String): Clase = Clase(
        materia = doc.materia,
        horario = rangoDeHoras(doc.horaInicio, doc.horaFin),
        detalle = when {
            doc.ultimaDelTurno -> textoUltimaClase
            doc.aula.isNotBlank() -> "${doc.profesor} · ${doc.aula}"
            else -> doc.profesor
        },
    )

    // ---------- Días de la semana ----------

    /** Id del documento de horario: "lunes", "martes", "miercoles"… (sin acento). */
    fun idDeDia(dia: DiaSemana): String = dia.name.lowercase(Locale.ROOT)

    /** Día de clases de hoy, o null si es sábado o domingo. */
    fun diaEscolarDeHoy(): DiaSemana? =
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> null
            else -> diaDeHoy()
        }

    /** Día de hoy; en sábado y domingo se muestra el lunes. */
    fun diaDeHoy(): DiaSemana = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.TUESDAY -> DiaSemana.MARTES
        Calendar.WEDNESDAY -> DiaSemana.MIERCOLES
        Calendar.THURSDAY -> DiaSemana.JUEVES
        Calendar.FRIDAY -> DiaSemana.VIERNES
        else -> DiaSemana.LUNES
    }

    // ---------- Fechas y horas ----------

    fun esHoy(fecha: Timestamp?): Boolean = fecha != null && mismoDia(fecha.toDate(), Date())

    /** "7:05 am" */
    fun hora(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val c = Calendar.getInstance().apply { time = fecha.toDate() }
        return horaDoce(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), conSufijo = true)
    }

    /** "Hoy · 5 oct" o "Vie · 2 oct" (tarjetas del boletín). */
    fun fechaCorta(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val d = fecha.toDate()
        val dia = if (mismoDia(d, Date())) "Hoy" else diaAbreviado(d)
        return "$dia · ${diaDelMes(d)} ${mesAbreviado(d)}"
    }

    /** "2:01 pm" si es de hoy; "Vie 2 oct" si es de otro día (notificaciones). */
    fun horaOFecha(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val d = fecha.toDate()
        return if (mismoDia(d, Date())) hora(fecha) else "${diaAbreviado(d)} ${diaDelMes(d)} ${mesAbreviado(d)}"
    }

    /** "Hoy · lunes 5 de octubre" o "Viernes 2 de octubre" (encabezados de Entradas y salidas). */
    fun tituloDeDia(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val d = fecha.toDate()
        val texto = "${formato("EEEE", d)} ${diaDelMes(d)} de ${formato("MMMM", d)}"
        return if (mismoDia(d, Date())) "Hoy · $texto" else texto.replaceFirstChar { it.titlecase(ES) }
    }

    /** "07:00" + "07:50" → "7:00 – 7:50 am"; "11:40" + "12:30" → "11:40 am – 12:30 pm". */
    fun rangoDeHoras(inicio: String, fin: String): String {
        val a = partirHora(inicio) ?: return listOf(inicio, fin).filter { it.isNotBlank() }.joinToString(" – ")
        val b = partirHora(fin) ?: return horaDoce(a.first, a.second, conSufijo = true)
        val mismoSufijo = (a.first < 12) == (b.first < 12)
        return "${horaDoce(a.first, a.second, conSufijo = !mismoSufijo)} – ${horaDoce(b.first, b.second, conSufijo = true)}"
    }

    /** "Vie 9 oct · 6:00 pm · Auditorio" o "Lun 19 – Vie 23 oct · Plantel" (Calendario). */
    fun detalleDeEvento(evento: EventoDoc): String {
        val inicio = evento.inicio?.toDate() ?: return evento.lugar
        val fin = evento.fin?.toDate()
        val fechas = if (fin == null || mismoDia(inicio, fin)) {
            "${diaAbreviado(inicio)} ${diaDelMes(inicio)} ${mesAbreviado(inicio)} · ${hora(evento.inicio)}"
        } else {
            val mesInicio = if (formato("M", inicio) != formato("M", fin)) " ${mesAbreviado(inicio)}" else ""
            "${diaAbreviado(inicio)} ${diaDelMes(inicio)}$mesInicio – ${diaAbreviado(fin)} ${diaDelMes(fin)} ${mesAbreviado(fin)}"
        }
        val donde = evento.lugar.ifBlank { evento.area }
        return listOf(fechas, donde, evento.detalle).filter { it.isNotBlank() }.joinToString(" · ")
    }

    /** "Octubre 2026" */
    fun tituloDeMes(calendario: Calendar): String =
        formato("MMMM yyyy", calendario.time).replaceFirstChar { it.titlecase(ES) }

    /** Hoy como texto "2026-10-19": el campo `fecha` de registros y salidas de grupo. */
    fun fechaDeHoy(): String = formato("yyyy-MM-dd", Date())

    // ---------- Grupos y alumnos (prefecto y profesor) ----------

    /** 1 → "1.er semestre", 2 → "2.º semestre", 3 → "3.er semestre"… */
    fun semestre(numero: Long): String = when {
        numero <= 0 -> ""
        numero == 1L || numero == 3L -> "$numero.er semestre"
        else -> "$numero.º semestre"
    }

    /** "matutino" → "Matutino" */
    fun turno(turno: String): String = turno.trim().replaceFirstChar { it.titlecase(ES) }

    /** Detalle de un grupo: "1.er semestre · Matutino". */
    fun detalleDeGrupo(grupo: GrupoDoc): String =
        listOf(semestre(grupo.semestre), turno(grupo.turno)).filter { it.isNotBlank() }.joinToString(" · ")

    /** Iniciales del círculo de la lista de alumnos: nombres "Juan" + apellido "Pérez" → "JP". */
    fun iniciales(alumno: AlumnoDoc): String {
        val palabras = alumno.nombre.trim().split(" ").filter { it.isNotBlank() }
        val primera = alumno.nombres.trim().firstOrNull() ?: palabras.getOrNull(0)?.first()
        val segunda = alumno.apellidoPaterno.trim().firstOrNull() ?: palabras.getOrNull(1)?.first()
        return listOfNotNull(primera, segunda).joinToString("").uppercase(ES)
    }

    // ---------- Código QR ----------

    /** Dibuja el código QR (cuadrado de [tamano] píxeles) con [texto] (el del alumno: NoControl.contenidoQr). */
    fun imagenQr(texto: String, tamano: Int = 600): Bitmap? {
        if (texto.isBlank()) return null
        val matriz = QRCodeWriter().encode(
            texto, BarcodeFormat.QR_CODE, tamano, tamano,
            mapOf(EncodeHintType.MARGIN to 1),
        )
        val pixeles = IntArray(tamano * tamano) { i ->
            if (matriz[i % tamano, i / tamano]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixeles, tamano, tamano, Bitmap.Config.ARGB_8888)
    }

    // ---------- Ayudantes ----------

    private fun formato(patron: String, fecha: Date): String = SimpleDateFormat(patron, ES).format(fecha)

    /** "vie." → "Vie" */
    private fun diaAbreviado(fecha: Date): String =
        formato("EEE", fecha).trimEnd('.').take(3).replaceFirstChar { it.titlecase(ES) }

    /** "oct." → "oct" */
    private fun mesAbreviado(fecha: Date): String = formato("MMM", fecha).trimEnd('.').take(3)

    private fun diaDelMes(fecha: Date): String = formato("d", fecha)

    private fun mismoDia(a: Date, b: Date): Boolean = formato("yyyyMMdd", a) == formato("yyyyMMdd", b)

    private fun partirHora(texto: String): Pair<Int, Int>? {
        val partes = texto.trim().split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    private fun horaDoce(hora24: Int, minuto: Int, conSufijo: Boolean): String {
        val hora = if (hora24 % 12 == 0) 12 else hora24 % 12
        val texto = "$hora:${minuto.toString().padStart(2, '0')}"
        return if (conSufijo) "$texto ${if (hora24 < 12) "am" else "pm"}" else texto
    }
}
