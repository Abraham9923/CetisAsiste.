package com.example.cetisasiste.ui.padres

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.cetisasiste.Alumno
import com.example.cetisasiste.AlumnoVistas
import com.example.cetisasiste.Anuncio
import com.example.cetisasiste.R
import com.example.cetisasiste.ui.comun.DiaDeRegistros
import com.example.cetisasiste.ui.comun.Registro
import com.example.cetisasiste.ui.comun.Renglon
import com.example.cetisasiste.ui.comun.TipoRegistro
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.mostrar
import com.example.cetisasiste.ui.comun.texto

/*
 * Coloca las VARIABLES dentro de las vistas XML del padre (res/layout/padre_*.xml).
 *
 * Solo pone textos e imágenes en su lugar. No consulta la base de datos y no navega.
 * El Código QR, la Credencial y el Perfil usan las mismas funciones del alumno
 * (AlumnoVistas), porque muestran los datos del hijo con el mismo diseño.
 */
object PadresVistas {

    /** padre_inicio.xml. Si [avisoDestacado] es null, no se ve el recuadro guinda. */
    fun llenarInicio(vista: View, alumno: Alumno, avisoDestacado: String?) {
        vista.texto(R.id.txt_nombre, alumno.nombre)
        vista.texto(R.id.txt_rol, alumno.rol)
        vista.texto(R.id.txt_grupo, alumno.grupo)
        vista.mostrar(R.id.banner_importante, avisoDestacado != null)
        if (avisoDestacado != null) vista.texto(R.id.txt_banner, avisoDestacado)
    }

    /** padre_anuncios.xml (las tarjetas son las del boletín del alumno). */
    fun llenarAnuncios(vista: View, anuncios: List<Anuncio>, cargado: Boolean) {
        AlumnoVistas.llenarBoletin(vista, anuncios)
        vista.mostrar(R.id.txt_vacio, cargado && anuncios.isEmpty())
    }

    /**
     * padre_entradas_salidas.xml: un encabezado por día y un renglón por entrada o salida.
     * [nombre] es el primer nombre del hijo ("Juan ingresó a las 7:05 am").
     */
    fun llenarEntradasSalidas(vista: View, nombre: String, dias: List<DiaDeRegistros>, cargado: Boolean, hayMas: Boolean) {
        val contenedor = vista.findViewById<ViewGroup>(R.id.lista_dias)
        contenedor.removeAllViews()
        val inflador = LayoutInflater.from(vista.context)
        dias.forEachIndexed { i, dia ->
            val encabezado = inflador.inflate(R.layout.comun_item_encabezado, contenedor, false) as TextView
            encabezado.text = dia.fecha
            // El primer día queda a 16 dp de la barra de arriba; los demás, a 20 dp del anterior.
            if (i == 0) {
                (encabezado.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                    vista.resources.getDimensionPixelSize(R.dimen.cetis_space_16)
            }
            contenedor.addView(encabezado)
            dia.registros.forEach { registro ->
                val fila = inflador.inflate(R.layout.comun_item_lista, contenedor, false)
                VistasComunes.llenarRenglon(fila, renglon(vista, nombre, registro))
                contenedor.addView(fila)
            }
        }
        vista.mostrar(R.id.txt_vacio, cargado && dias.isEmpty())
        vista.mostrar(R.id.txt_ver_mas, hayMas)
    }

    private fun renglon(vista: View, nombre: String, registro: Registro): Renglon = when (registro.tipo) {
        TipoRegistro.Entrada -> Renglon(
            icono = R.drawable.ic_log_in,
            titulo = vista.context.getString(R.string.entradas_entrada_message, nombre, registro.hora),
            detalle = vista.context.getString(R.string.entradas_entrada),
        )
        TipoRegistro.Salida -> Renglon(
            icono = R.drawable.ic_log_out,
            titulo = vista.context.getString(R.string.entradas_salida_message, nombre, registro.hora),
            detalle = vista.context.getString(R.string.entradas_salida),
            fondoIcono = R.drawable.shape_circulo_accent,
        )
    }
}
