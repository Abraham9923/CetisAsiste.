package com.example.cetisasiste

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat

/*
 * Coloca las VARIABLES (AlumnoVariables.kt) dentro de las vistas XML de alumno.
 *
 * Solo pone textos e imágenes en su lugar. No consulta bases de datos,
 * no valida nada y no navega entre pantallas.
 *
 * Uso (cuando ya tengas los datos):
 *     val vista = layoutInflater.inflate(R.layout.alumno_perfil, null)
 *     AlumnoVistas.llenarPerfil(vista, alumno)
 */
object AlumnoVistas {

    /** alumno_inicio.xml */
    fun llenarInicio(vista: View, alumno: Alumno, foto: Bitmap? = null) {
        vista.texto(R.id.txt_nombre, alumno.nombre)
        vista.texto(R.id.txt_rol, alumno.rol)
        vista.texto(R.id.txt_grupo, alumno.grupo)
        vista.foto(R.id.img_foto, foto)
    }

    /** alumno_perfil.xml (y alumno_confirmar_cerrar_sesion.xml, que la incluye) */
    fun llenarPerfil(vista: View, alumno: Alumno, foto: Bitmap? = null) {
        vista.texto(R.id.txt_nombre, alumno.nombre)
        vista.texto(R.id.txt_rol_grupo, rolYGrupo(vista, alumno))
        vista.texto(R.id.txt_no_control, alumno.noControl)
        vista.texto(R.id.txt_grupo, alumno.grupo)
        vista.texto(R.id.txt_correo, alumno.correo)
        vista.foto(R.id.img_foto, foto)
    }

    /** alumno_codigo_qr.xml — el QR lo genera quien llame (imagen de 200 × 200 dp). */
    fun llenarCodigoQr(vista: View, alumno: Alumno, qr: Bitmap?) {
        vista.texto(R.id.txt_nombre, alumno.nombre)
        vista.texto(R.id.txt_rol_grupo, rolYGrupo(vista, alumno))
        vista.findViewById<ImageView>(R.id.img_qr).setImageBitmap(qr)
    }

    /** alumno_credencial_digital.xml */
    fun llenarCredencial(vista: View, alumno: Alumno, foto: Bitmap? = null) {
        vista.texto(R.id.txt_nombre, alumno.nombre)
        vista.texto(R.id.txt_rol, alumno.rol)
        vista.texto(R.id.txt_grupo, alumno.grupo)
        vista.texto(R.id.txt_no_control, alumno.noControl)
        vista.texto(R.id.txt_vigencia, alumno.vigencia)
        vista.foto(R.id.img_foto, foto)
    }

    /** alumno_horario.xml */
    fun llenarHorario(vista: View, horario: HorarioDelDia) {
        vista.texto(
            R.id.txt_encabezado,
            vista.context.getString(R.string.alumno_horario_encabezado, horario.grupo, horario.dia.nombre),
        )
        marcarDia(vista, horario.dia)
        vista.findViewById<ListView>(R.id.lista_clases).adapter =
            ListaAdapter(horario.clases, R.layout.alumno_item_clase) { fila, clase ->
                fila.texto(R.id.txt_materia, clase.materia)
                fila.texto(
                    R.id.txt_detalle,
                    fila.context.getString(R.string.alumno_clase_detalle, clase.horario, clase.detalle),
                )
            }
    }

    /** alumno_boletin.xml */
    fun llenarBoletin(vista: View, anuncios: List<Anuncio>) {
        vista.findViewById<ListView>(R.id.lista_anuncios).adapter =
            ListaAdapter(anuncios, R.layout.alumno_item_anuncio) { fila, anuncio ->
                fila.texto(R.id.txt_etiqueta, anuncio.etiqueta)
                fila.texto(R.id.txt_fecha, anuncio.fecha)
                fila.texto(R.id.txt_titulo, anuncio.titulo)
                fila.texto(R.id.txt_texto, anuncio.texto)
            }
    }


    private val CHIPS_DIAS = mapOf(
        DiaSemana.LUNES to R.id.chip_lunes,
        DiaSemana.MARTES to R.id.chip_martes,
        DiaSemana.MIERCOLES to R.id.chip_miercoles,
        DiaSemana.JUEVES to R.id.chip_jueves,
        DiaSemana.VIERNES to R.id.chip_viernes,
    )

    /** Pinta el chip del día activo como en Figma (guinda con letra blanca). */
    private fun marcarDia(vista: View, activo: DiaSemana) {
        val contexto = vista.context
        for ((dia, id) in CHIPS_DIAS) {
            val chip = vista.findViewById<TextView>(id)
            val esActivo = dia == activo
            chip.isSelected = esActivo
            chip.setBackgroundResource(if (esActivo) R.drawable.shape_chip_activo else R.drawable.shape_chip)
            chip.setTextColor(
                ContextCompat.getColor(contexto, if (esActivo) R.color.cetis_text_on_brand else R.color.cetis_brand_900),
            )
        }
    }

    private fun rolYGrupo(vista: View, alumno: Alumno): String =
        vista.context.getString(R.string.role_and_group, alumno.rol, alumno.grupo)

    private fun View.texto(@IdRes id: Int, valor: CharSequence) {
        findViewById<TextView>(id).text = valor
    }

    /** Si no hay foto se queda el avatar del diseño. */
    private fun View.foto(@IdRes id: Int, foto: Bitmap?) {
        if (foto != null) findViewById<ImageView>(id).setImageBitmap(foto)
    }
}

/** Adaptador mínimo para las listas (Horario y Boletín). */
private class ListaAdapter<T>(
    private val elementos: List<T>,
    @param:LayoutRes private val layout: Int,
    private val llenar: (View, T) -> Unit,
) : BaseAdapter() {

    override fun getCount(): Int = elementos.size

    override fun getItem(position: Int): T = elementos[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val fila = convertView ?: LayoutInflater.from(parent.context).inflate(layout, parent, false).also {

            it.clipToOutline = true
        }
        llenar(fila, elementos[position])
        return fila
    }

    override fun isEnabled(position: Int): Boolean = false
}
