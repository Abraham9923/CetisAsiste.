package com.example.cetisasiste.pantallas.profesor

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.TipoEstatus
import com.example.cetisasiste.pantallas.comun.ColorEstatus
import com.example.cetisasiste.pantallas.comun.Renglon

/**
 * Cómo se ve cada estatus del alumno en las pantallas del profesor (Figma 78:1084 y 78:1085):
 * la etiqueta de color de la lista y del perfil, y los renglones del historial.
 */
object Estatus {

    /** Los tres estatus que pone el profesor en Figma (Cambiar estatus 78:1086). */
    val DEL_PROFESOR = listOf(TipoEstatus.FALTA, TipoEstatus.REPORTE, TipoEstatus.LLAMADA_ATENCION)

    /** "Falta", "Reporte"… (también lo usa el aviso al padre). */
    @StringRes
    fun nombre(tipo: String): Int = when (tipo) {
        TipoEstatus.FALTA -> R.string.estatus_falta
        TipoEstatus.REPORTE -> R.string.estatus_reporte
        TipoEstatus.LLAMADA_ATENCION -> R.string.estatus_llamada_atencion
        TipoEstatus.CASTIGO -> R.string.estatus_castigo
        TipoEstatus.PASE_SALUD -> R.string.estatus_pase_salud
        else -> R.string.estatus_alta_practicas
    }

    /** Etiqueta del último estatus: "Sin incidencias" (verde), "Falta notificada" (amarilla)… */
    fun etiqueta(contexto: Context, ultimo: EstatusDoc?): Pair<String, ColorEstatus> = when (ultimo?.tipo) {
        null -> contexto.getString(R.string.estatus_sin_incidencias) to ColorEstatus.Exito
        TipoEstatus.FALTA -> contexto.getString(R.string.estatus_falta_notificada) to ColorEstatus.Advertencia
        TipoEstatus.REPORTE, TipoEstatus.LLAMADA_ATENCION -> contexto.getString(nombre(ultimo.tipo)) to ColorEstatus.Advertencia
        TipoEstatus.CASTIGO -> contexto.getString(R.string.estatus_castigo) to ColorEstatus.Peligro
        else -> contexto.getString(nombre(ultimo.tipo)) to ColorEstatus.Exito
    }

    /** Un estatus del historial: "Falta notificada" · "Mié 7 oct · Nombre del profesor". */
    fun renglon(contexto: Context, estatus: EstatusDoc): Renglon {
        val (titulo, color) = etiqueta(contexto, estatus)
        return Renglon(
            icono = icono(estatus.tipo),
            titulo = titulo,
            detalle = Formatos.unir(Formatos.fechaSinHora(estatus.fecha), estatus.asignadoPorNombre),
            fondoIcono = when (color) {
                ColorEstatus.Advertencia -> R.drawable.shape_circulo_advertencia
                ColorEstatus.Peligro -> R.drawable.shape_circulo_peligro
                ColorEstatus.Exito -> R.drawable.shape_circulo_exito
            },
        )
    }

    @DrawableRes
    fun icono(tipo: String): Int = when (tipo) {
        TipoEstatus.FALTA -> R.drawable.ic_flag
        TipoEstatus.REPORTE -> R.drawable.ic_edit
        TipoEstatus.LLAMADA_ATENCION -> R.drawable.ic_warning
        TipoEstatus.CASTIGO -> R.drawable.ic_x_circle
        else -> R.drawable.ic_check_circle
    }
}
