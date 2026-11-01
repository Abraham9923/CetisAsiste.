package com.example.cetisasiste.pantallas.padre

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunItemListaBinding
import com.example.cetisasiste.databinding.PadreEntradasSalidasBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.RegistroDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.pantallas.comun.AdaptadorConEncabezados
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.ElementoLista
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.Renglon
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/**
 * Entradas y salidas del hijo (padre_entradas_salidas.xml): un encabezado por día y un renglón
 * por entrada o salida. Se piden de 20 en 20 (solo del semestre en curso); "Ver más" trae las
 * siguientes. Extra: [Claves.NO_CONTROL].
 */
class EntradasSalidasActividad : PantallaBase() {

    private lateinit var enlace: PadreEntradasSalidasBinding
    private val adaptador = AdaptadorConEncabezados<Renglon, ComunItemListaBinding>(ComunItemListaBinding::inflate) { fila, renglon ->
        Llenado.llenarRenglon(fila, renglon)
    }
    private var registros: List<RegistroDoc> = emptyList()
    private var cargando = false

    override fun crearVista(): View {
        enlace = PadreEntradasSalidasBinding.inflate(layoutInflater)
        enlace.listaRegistros.adapter = adaptador
        enlace.textoVerMas.setOnClickListener { cargarPagina() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_entradas_salidas)
        cargarPagina()
    }

    private fun cargarPagina() {
        if (cargando) return
        cargando = true
        val noControl = intent.getStringExtra(Claves.NO_CONTROL).orEmpty()
        lifecycleScope.launch {
            val pagina = cargar {
                CetisRepositorio.registros(noControl, despuesDe = registros.lastOrNull()?.fechaHora)
            }.o(emptyList())
            registros = registros + pagina
            cargando = false
            adaptador.mostrar(porDia(registros))
            enlace.textoVacio.isVisible = registros.isEmpty()
            enlace.textoVerMas.isVisible = pagina.size.toLong() == CetisRepositorio.POR_PAGINA
        }
    }

    /**
     * Agrupa los registros por día (más reciente primero) y, dentro del día, en orden de hora.
     * Cada renglón dice "Juan Pérez Rodríguez ingresó a las 7:05 am" (el nombre completo, como
     * en Figma 20:97).
     */
    private fun porDia(lista: List<RegistroDoc>): List<ElementoLista<Renglon>> =
        lista.groupBy { it.fecha }.values.flatMap { delDia ->
            val ordenados = delDia.sortedBy { it.fechaHora }
            listOf(ElementoLista.Encabezado(Formatos.tituloDeDia(ordenados.first().fechaHora))) +
                ordenados.map { ElementoLista.Fila(renglon(it)) }
        }

    private fun renglon(registro: RegistroDoc): Renglon {
        val nombre = registro.alumnoNombre.trim()
        val hora = Formatos.hora(registro.fechaHora)
        return if (registro.tipo == TipoRegistro.SALIDA) {
            Renglon(
                icono = R.drawable.ic_log_out,
                titulo = getString(R.string.formato_salio_a_las, nombre, hora),
                detalle = getString(R.string.tipo_salida),
                fondoIcono = R.drawable.shape_circulo_accent,
            )
        } else {
            Renglon(
                icono = R.drawable.ic_log_in,
                titulo = getString(R.string.formato_ingreso_a_las, nombre, hora),
                detalle = getString(R.string.tipo_entrada),
            )
        }
    }
}
