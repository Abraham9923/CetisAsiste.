package com.example.cetisasiste.datos

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import com.example.cetisasiste.R
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.Anuncio
import com.example.cetisasiste.pantallas.comun.Clase
import com.example.cetisasiste.pantallas.comun.DiaSemana
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
 * No consulta la base: solo transforma datos que ya se tienen. Las palabras que se ven en
 * pantalla ("Hoy", "Estudiante", "semestre", "am"…) salen de res/values/strings.xml; por eso
 * la app llama a [iniciar] al arrancar (AplicacionCetisAsiste).
 */
object Formatos {

    private val ESPANOL = Locale.forLanguageTag("es-MX")

    /** Contexto de la aplicación, para leer los textos de strings.xml. */
    private lateinit var contexto: Context

    /** Se llama una sola vez, al abrir la app. */
    fun iniciar(contextoDeLaApp: Context) {
        contexto = contextoDeLaApp.applicationContext
    }

    private fun texto(id: Int, vararg datos: Any): String = contexto.getString(id, *datos)

    /** Texto que se muestra como rol del alumno en tarjetas y credencial ("Estudiante"). */
    fun rolAlumno(): String = texto(R.string.rol_estudiante)

    // ---------- Documentos → variables de las vistas ----------

    /**
     * AlumnoDoc (Firestore) → Alumno (lo que muestran las pantallas).
     * [nombreGrupo] es grupos/{grupoId}.nombre; si no se pudo leer, se usa la copia
     * guardada en la ficha (alumnos/{m}.grupo).
     */
    fun alumno(documento: AlumnoDoc, nombreGrupo: String? = null): Alumno = Alumno(
        nombre = documento.nombre.trim(),
        rol = rolAlumno(),
        grupo = nombreGrupo?.trim()?.ifBlank { null } ?: documento.grupo.trim(),
        noControl = documento.noControl,
        correo = documento.correo,
        vigencia = documento.vigencia,
    )

    fun anuncio(documento: AnuncioDoc): Anuncio = Anuncio(
        etiqueta = documento.area,
        fecha = fechaCorta(documento.fecha),
        titulo = documento.titulo,
        texto = documento.texto,
    )

    fun clase(documento: ClaseDoc): Clase = Clase(
        materia = documento.materia,
        horario = rangoDeHoras(documento.horaInicio, documento.horaFin),
        detalle = when {
            documento.ultimaDelTurno -> texto(R.string.clase_ultima_del_turno)
            documento.aula.isNotBlank() -> texto(R.string.formato_dos_partes, documento.profesor, documento.aula)
            else -> documento.profesor
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

    /** Nombre del día en minúsculas ("miércoles"), para el encabezado del horario. */
    fun nombreDeDia(dia: DiaSemana): String = texto(
        when (dia) {
            DiaSemana.LUNES -> R.string.dia_lunes
            DiaSemana.MARTES -> R.string.dia_martes
            DiaSemana.MIERCOLES -> R.string.dia_miercoles
            DiaSemana.JUEVES -> R.string.dia_jueves
            DiaSemana.VIERNES -> R.string.dia_viernes
        },
    ).lowercase(ESPANOL)

    // ---------- Fechas y horas ----------

    fun esHoy(fecha: Timestamp?): Boolean = fecha != null && mismoDia(fecha.toDate(), Date())

    /** "7:05 am" */
    fun hora(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val calendario = Calendar.getInstance().apply { time = fecha.toDate() }
        return horaDoce(calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), conSufijo = true)
    }

    /** "Hoy · 5 oct" o "Vie · 2 oct" (tarjetas del boletín). */
    fun fechaCorta(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val dia = fecha.toDate()
        val nombre = if (mismoDia(dia, Date())) texto(R.string.texto_hoy) else diaAbreviado(dia)
        return texto(R.string.formato_dos_partes, nombre, "${diaDelMes(dia)} ${mesAbreviado(dia)}")
    }

    /** "Mié 7 oct" (historial de estatus del alumno). */
    fun fechaSinHora(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val dia = fecha.toDate()
        return "${diaAbreviado(dia)} ${diaDelMes(dia)} ${mesAbreviado(dia)}"
    }

    /** "2:01 pm" si es de hoy; "Vie 2 oct" si es de otro día (notificaciones). */
    fun horaOFecha(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val dia = fecha.toDate()
        return if (mismoDia(dia, Date())) hora(fecha) else "${diaAbreviado(dia)} ${diaDelMes(dia)} ${mesAbreviado(dia)}"
    }

    /** "Hoy · lunes 5 de octubre" o "Viernes 2 de octubre" (encabezados de Entradas y salidas). */
    fun tituloDeDia(fecha: Timestamp?): String {
        if (fecha == null) return ""
        val dia = fecha.toDate()
        val titulo = texto(R.string.formato_titulo_dia, formato("EEEE", dia), diaDelMes(dia), formato("MMMM", dia))
        return if (mismoDia(dia, Date())) {
            texto(R.string.formato_dos_partes, texto(R.string.texto_hoy), titulo)
        } else {
            titulo.replaceFirstChar { it.titlecase(ESPANOL) }
        }
    }

    /** "07:00" + "07:50" → "7:00 – 7:50 am"; "11:40" + "12:30" → "11:40 am – 12:30 pm". */
    fun rangoDeHoras(inicio: String, fin: String): String {
        val primera = partirHora(inicio) ?: return listOf(inicio, fin).filter { it.isNotBlank() }.joinToString(" – ")
        val segunda = partirHora(fin) ?: return horaDoce(primera.first, primera.second, conSufijo = true)
        val mismoSufijo = (primera.first < 12) == (segunda.first < 12)
        return "${horaDoce(primera.first, primera.second, conSufijo = !mismoSufijo)} – " +
            horaDoce(segunda.first, segunda.second, conSufijo = true)
    }

    /** "Vie 9 oct · 6:00 pm · Auditorio" o "Lun 19 – Vie 23 oct · Plantel" (Calendario). */
    fun detalleDeEvento(evento: EventoDoc): String {
        val inicio = evento.inicio?.toDate() ?: return evento.lugar
        val fin = evento.fin?.toDate()
        val fechas = if (fin == null || mismoDia(inicio, fin)) {
            unir("${diaAbreviado(inicio)} ${diaDelMes(inicio)} ${mesAbreviado(inicio)}", hora(evento.inicio))
        } else {
            val mesInicio = if (formato("M", inicio) != formato("M", fin)) " ${mesAbreviado(inicio)}" else ""
            "${diaAbreviado(inicio)} ${diaDelMes(inicio)}$mesInicio – ${diaAbreviado(fin)} ${diaDelMes(fin)} ${mesAbreviado(fin)}"
        }
        val donde = evento.lugar.ifBlank { evento.area }
        return unir(fechas, donde, evento.detalle)
    }

    /** "Octubre 2026" */
    fun tituloDeMes(calendario: Calendar): String =
        formato("MMMM yyyy", calendario.time).replaceFirstChar { it.titlecase(ESPANOL) }

    /** Hoy como texto "2026-10-19": el campo `fecha` de registros y salidas de grupo. */
    fun fechaDeHoy(): String = formato("yyyy-MM-dd", Date())

    // ---------- Grupos y alumnos (prefecto y profesor) ----------

    /** 1 → "1.er semestre", 2 → "2.º semestre", 3 → "3.er semestre"… */
    fun semestre(numero: Long): String = when {
        numero <= 0 -> ""
        numero == 1L || numero == 3L -> texto(R.string.formato_semestre_er, numero)
        else -> texto(R.string.formato_semestre_o, numero)
    }

    /** "matutino" → "Matutino" */
    fun turno(turno: String): String = turno.trim().replaceFirstChar { it.titlecase(ESPANOL) }

    /** Detalle de un grupo: "1.er semestre · Matutino". */
    fun detalleDeGrupo(grupo: GrupoDoc): String = unir(semestre(grupo.semestre), turno(grupo.turno))

    /** Nombre del grupo a partir de su id: "2026-2027-1_102" → "102". */
    fun nombreDeGrupo(grupoId: String): String = grupoId.substringAfterLast('_')

    /** Iniciales del círculo de la lista de alumnos: nombres "Juan" + apellido "Pérez" → "JP". */
    fun iniciales(alumno: AlumnoDoc): String {
        val palabras = alumno.nombre.trim().split(" ").filter { it.isNotBlank() }
        val primera = alumno.nombres.trim().firstOrNull() ?: palabras.getOrNull(0)?.first()
        val segunda = alumno.apellidoPaterno.trim().firstOrNull() ?: palabras.getOrNull(1)?.first()
        return listOfNotNull(primera, segunda).joinToString("").uppercase(ESPANOL)
    }

    /** Une las partes que no estén vacías con " · " ("11:40 am · Química · Pedro Pozo"). */
    fun unir(vararg partes: String?): String =
        partes.filterNotNull().map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")

    // ---------- Código QR ----------

    /**
     * Dibuja el código QR (cuadrado de [tamano] píxeles) con [contenido] (el del alumno:
     * NoControl.contenidoQr). Los cuadros van en guinda oscuro, como en Figma.
     */
    fun imagenQr(contenido: String, tamano: Int = 600): Bitmap? {
        if (contenido.isBlank()) return null
        val matriz = QRCodeWriter().encode(
            contenido, BarcodeFormat.QR_CODE, tamano, tamano,
            mapOf(EncodeHintType.MARGIN to 1),
        )
        val colorCuadro = ContextCompat.getColor(contexto, R.color.cetis_brand_900)
        val colorFondo = ContextCompat.getColor(contexto, R.color.cetis_surface_white)
        val pixeles = IntArray(tamano * tamano) { indice ->
            if (matriz[indice % tamano, indice / tamano]) colorCuadro else colorFondo
        }
        return Bitmap.createBitmap(pixeles, tamano, tamano, Bitmap.Config.ARGB_8888)
    }

    // ---------- Ayudantes ----------

    private fun formato(patron: String, fecha: Date): String = SimpleDateFormat(patron, ESPANOL).format(fecha)

    /** "vie." → "Vie" */
    private fun diaAbreviado(fecha: Date): String =
        formato("EEE", fecha).trimEnd('.').take(3).replaceFirstChar { it.titlecase(ESPANOL) }

    /** "oct." → "oct" */
    private fun mesAbreviado(fecha: Date): String = formato("MMM", fecha).trimEnd('.').take(3)

    private fun diaDelMes(fecha: Date): String = formato("d", fecha)

    private fun mismoDia(primera: Date, segunda: Date): Boolean =
        formato("yyyyMMdd", primera) == formato("yyyyMMdd", segunda)

    private fun partirHora(textoHora: String): Pair<Int, Int>? {
        val partes = textoHora.trim().split(":")
        val horas = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val minutos = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return horas to minutos
    }

    private fun horaDoce(hora24: Int, minuto: Int, conSufijo: Boolean): String {
        val hora = if (hora24 % 12 == 0) 12 else hora24 % 12
        val textoHora = "$hora:${minuto.toString().padStart(2, '0')}"
        if (!conSufijo) return textoHora
        val sufijo = texto(if (hora24 < 12) R.string.sufijo_am else R.string.sufijo_pm)
        return texto(R.string.formato_hora_sufijo, textoHora, sufijo)
    }
}
