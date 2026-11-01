package com.example.cetisasiste.pantallas.comun

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.cetisasiste.databinding.ComunItemEncabezadoBinding

/*
 * Adaptadores de las listas (RecyclerView). Cada renglón se infla con su clase de ViewBinding
 * (por ejemplo AlumnoItemAnuncioBinding): las vistas no se buscan por su id.
 */

/** Guarda el enlace (ViewBinding) de un renglón. */
class Contenedor<B : ViewBinding>(val enlace: B) : RecyclerView.ViewHolder(enlace.root)

/**
 * Lista de un solo tipo de renglón. [inflar] es el inflate de la clase de ViewBinding del
 * renglón (por ejemplo AlumnoItemClaseBinding::inflate) y [llenar] pone los datos en él.
 */
class AdaptadorSimple<T, B : ViewBinding>(
    private val inflar: (LayoutInflater, ViewGroup, Boolean) -> B,
    private val llenar: (enlace: B, dato: T, posicion: Int) -> Unit,
) : RecyclerView.Adapter<Contenedor<B>>() {

    private var datos: List<T> = emptyList()

    /** Cambia lo que muestra la lista. */
    @Suppress("NotifyDataSetChanged") // las listas son cortas y cambian completas
    fun mostrar(nuevos: List<T>) {
        datos = nuevos
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = datos.size

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int): Contenedor<B> =
        Contenedor(inflar(LayoutInflater.from(padre.context), padre, false))

    override fun onBindViewHolder(contenedor: Contenedor<B>, posicion: Int) =
        llenar(contenedor.enlace, datos[posicion], posicion)
}

/**
 * Lista con encabezados (comun_item_encabezado.xml) entre los renglones: los días en Entradas
 * y salidas y "Anteriores" en Notificaciones. El primer encabezado queda a 16 dp de arriba.
 */
class AdaptadorConEncabezados<T, B : ViewBinding>(
    private val inflar: (LayoutInflater, ViewGroup, Boolean) -> B,
    private val llenar: (enlace: B, dato: T) -> Unit,
) : RecyclerView.Adapter<Contenedor<ViewBinding>>() {

    private var elementos: List<ElementoLista<T>> = emptyList()

    @Suppress("NotifyDataSetChanged")
    fun mostrar(nuevos: List<ElementoLista<T>>) {
        elementos = nuevos
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = elementos.size

    override fun getItemViewType(posicion: Int): Int =
        if (elementos[posicion] is ElementoLista.Encabezado) TIPO_ENCABEZADO else TIPO_FILA

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int): Contenedor<ViewBinding> {
        val inflador = LayoutInflater.from(padre.context)
        return Contenedor(
            if (tipo == TIPO_ENCABEZADO) ComunItemEncabezadoBinding.inflate(inflador, padre, false) else inflar(inflador, padre, false),
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(contenedor: Contenedor<ViewBinding>, posicion: Int) {
        when (val elemento = elementos[posicion]) {
            is ElementoLista.Encabezado -> {
                val encabezado = contenedor.enlace as ComunItemEncabezadoBinding
                encabezado.textoEncabezado.text = elemento.texto
                val medidas = encabezado.root.layoutParams as ViewGroup.MarginLayoutParams
                medidas.topMargin = encabezado.root.resources.getDimensionPixelSize(
                    if (posicion == 0) com.example.cetisasiste.R.dimen.cetis_space_16 else com.example.cetisasiste.R.dimen.cetis_space_20,
                )
                encabezado.root.layoutParams = medidas
            }
            is ElementoLista.Fila -> llenar(contenedor.enlace as B, elemento.dato)
        }
    }

    private companion object {
        const val TIPO_ENCABEZADO = 0
        const val TIPO_FILA = 1
    }
}
