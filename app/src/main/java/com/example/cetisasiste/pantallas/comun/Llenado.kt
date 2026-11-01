package com.example.cetisasiste.pantallas.comun

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoItemAnuncioBinding
import com.example.cetisasiste.databinding.AlumnoItemClaseBinding
import com.example.cetisasiste.databinding.ComunItemListaBinding
import com.example.cetisasiste.databinding.ComunItemNotificacionBinding
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NotificacionDoc
import com.example.cetisasiste.datos.TipoAviso

/*
 * Cómo se llenan los renglones que comparten varias pantallas. Solo ponen textos e imágenes en
 * su lugar: no consultan la base de datos y no navegan.
 */
object Llenado {

    /** Adaptador de las tarjetas del boletín (alumno_item_anuncio.xml). */
    fun adaptadorAnuncios() = AdaptadorSimple<Anuncio, AlumnoItemAnuncioBinding>(AlumnoItemAnuncioBinding::inflate) { fila, anuncio, _ ->
        fila.textoEtiqueta.text = anuncio.etiqueta
        fila.textoFecha.text = anuncio.fecha
        fila.textoTitulo.text = anuncio.titulo
        fila.textoMensaje.text = anuncio.texto
    }

    /** Adaptador de las clases del horario (alumno_item_clase.xml). */
    fun adaptadorClases() = AdaptadorSimple<Clase, AlumnoItemClaseBinding>(AlumnoItemClaseBinding::inflate) { fila, clase, _ ->
        fila.textoMateria.text = clase.materia
        fila.textoDetalle.text = fila.root.context.getString(R.string.formato_dos_partes, clase.horario, clase.detalle)
    }

    /**
     * Adaptador de renglones (comun_item_lista.xml). Si se da [alTocar], los renglones llevan
     * flecha y se pueden tocar.
     */
    fun adaptadorRenglones(alTocar: ((Int) -> Unit)? = null) =
        AdaptadorSimple<Renglon, ComunItemListaBinding>(ComunItemListaBinding::inflate) { fila, renglon, posicion ->
            llenarRenglon(fila, renglon, alTocar?.let { { it(posicion) } })
        }

    /** comun_item_lista.xml. Si se da [alTocar], el renglón lleva flecha y se puede tocar. */
    fun llenarRenglon(fila: ComunItemListaBinding, renglon: Renglon, alTocar: (() -> Unit)? = null) {
        fila.root.setBackgroundResource(renglon.fondo)
        fila.circuloIcono.setBackgroundResource(renglon.fondoIcono)
        fila.circuloIcono.setImageResource(renglon.icono)
        fila.textoTitulo.text = renglon.titulo
        fila.textoDetalle.text = renglon.detalle
        fila.textoDetalle.isVisible = renglon.detalle.isNotBlank()
        fila.imagenFlecha.isVisible = alTocar != null
        if (alTocar != null) {
            fila.root.setOnClickListener { alTocar() }
        } else {
            fila.root.setOnClickListener(null)
            fila.root.isClickable = false
        }
    }

    /** Un aviso del buzón (comun_item_notificacion.xml). Sin leer: fondo más oscuro y punto rojo. */
    fun llenarNotificacion(fila: ComunItemNotificacionBinding, notificacion: Notificacion) {
        fila.circuloIcono.setImageResource(
            when (notificacion.tipo) {
                TipoNotificacion.Entrada -> R.drawable.ic_log_in
                TipoNotificacion.Salida -> R.drawable.ic_log_out
                TipoNotificacion.Anuncio, TipoNotificacion.Aviso -> R.drawable.ic_announcements
                TipoNotificacion.Recordatorio -> R.drawable.ic_calendar
                TipoNotificacion.Credencial, TipoNotificacion.Estado -> R.drawable.ic_id_card
                TipoNotificacion.Alerta -> R.drawable.ic_warning
                TipoNotificacion.Estatus, TipoNotificacion.SalidaDeGrupo -> R.drawable.ic_flag
            },
        )
        fila.circuloIcono.setBackgroundResource(
            when (notificacion.tipo) {
                TipoNotificacion.Alerta -> R.drawable.shape_circulo_peligro
                TipoNotificacion.Salida, TipoNotificacion.SalidaDeGrupo -> R.drawable.shape_circulo_accent
                else -> R.drawable.shape_circulo_brand
            },
        )
        fila.root.setBackgroundResource(if (notificacion.leida) R.drawable.shape_tarjeta else R.drawable.shape_tarjeta_sin_leer)
        fila.textoTitulo.text = notificacion.titulo
        fila.textoHora.text = notificacion.hora
        fila.textoMensaje.text = notificacion.texto
        fila.imagenSinLeer.isVisible = !notificacion.leida
    }

    /** NotificacionDoc (Firestore) → Notificacion (lo que dibuja la vista). */
    fun notificacion(documento: NotificacionDoc) = Notificacion(
        tipo = when (documento.tipo) {
            TipoAviso.ENTRADA -> TipoNotificacion.Entrada
            TipoAviso.SALIDA -> TipoNotificacion.Salida
            TipoAviso.ANUNCIO -> TipoNotificacion.Anuncio
            TipoAviso.RECORDATORIO -> TipoNotificacion.Recordatorio
            TipoAviso.CREDENCIAL -> TipoNotificacion.Credencial
            TipoAviso.ALERTA -> TipoNotificacion.Alerta
            TipoAviso.ESTATUS -> TipoNotificacion.Estatus
            TipoAviso.ESTADO -> TipoNotificacion.Estado
            TipoAviso.SALIDA_GRUPO -> TipoNotificacion.SalidaDeGrupo
            else -> TipoNotificacion.Aviso
        },
        titulo = documento.titulo,
        hora = Formatos.horaOFecha(documento.fecha),
        texto = documento.mensaje,
        leida = documento.leida,
    )

    /** Etiqueta de estatus con punto de color (StatusChip de Figma): verde, amarilla o roja. */
    fun llenarEstatus(etiqueta: TextView, texto: String, color: ColorEstatus) {
        val contexto: Context = etiqueta.context
        etiqueta.text = texto
        val (fondo, punto, letra) = when (color) {
            ColorEstatus.Exito -> Triple(R.drawable.shape_estatus_exito, R.drawable.shape_punto_exito, R.color.cetis_status_success)
            ColorEstatus.Advertencia -> Triple(R.drawable.shape_estatus_advertencia, R.drawable.shape_punto_advertencia, R.color.cetis_status_warning)
            ColorEstatus.Peligro -> Triple(R.drawable.shape_estatus_peligro, R.drawable.shape_punto_peligro, R.color.cetis_status_danger)
        }
        etiqueta.setBackgroundResource(fondo)
        etiqueta.setCompoundDrawablesRelativeWithIntrinsicBounds(punto, 0, 0, 0)
        etiqueta.setTextColor(ContextCompat.getColor(contexto, letra))
    }
}
