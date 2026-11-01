package com.example.cetisasiste.pantallas.comun

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunCalendarioBinding
import com.example.cetisasiste.databinding.ComunItemDiaBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.EventoDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Calendario del plantel (comun_calendario.xml), del padre y del prefecto: la rejilla del mes
 * (RecyclerView de 7 columnas) y los próximos eventos. Las flechas cambian de mes.
 */
class CalendarioActividad : PantallaBase() {

    private lateinit var enlace: ComunCalendarioBinding
    private val adaptadorEventos = Llenado.adaptadorRenglones()
    private val adaptadorDias = AdaptadorSimple<CasillaDia, ComunItemDiaBinding>(ComunItemDiaBinding::inflate) { casilla, dia, _ ->
        llenarDia(casilla, dia)
    }
    /** 0 = este mes, -1 = el anterior, 1 = el siguiente… */
    private var desplazamiento = 0
    private var consulta: Job? = null

    override fun crearVista(): View {
        enlace = ComunCalendarioBinding.inflate(layoutInflater)
        enlace.listaDias.adapter = adaptadorDias
        enlace.listaEventos.adapter = adaptadorEventos
        enlace.botonMesAnterior.setOnClickListener { desplazamiento--; mostrarMes() }
        enlace.botonMesSiguiente.setOnClickListener { desplazamiento++; mostrarMes() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_calendario)
        mostrarMes()
    }

    private fun mostrarMes() {
        val mes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, desplazamiento)
        }
        enlace.textoMes.text = Formatos.tituloDeMes(mes)
        adaptadorDias.mostrar(casillasDelMes(mes, emptyList()))
        consulta?.cancel()
        consulta = lifecycleScope.launch {
            enlace.textoVacio.isVisible = false
            val (desde, hasta) = limitesDelMes(mes)
            val eventos = cargar { CetisRepositorio.eventos(desde, hasta) }.o(emptyList())
            adaptadorDias.mostrar(casillasDelMes(mes, eventos))
            adaptadorEventos.mostrar(eventos.map { Renglon(R.drawable.ic_calendar_event, it.titulo, Formatos.detalleDeEvento(it)) })
            enlace.textoVacio.isVisible = eventos.isEmpty()
        }
    }

    /** Hoy: círculo guinda con letra blanca y más gruesa. Sábado y domingo: letra gris. */
    private fun llenarDia(casilla: ComunItemDiaBinding, dia: CasillaDia) {
        val numero = casilla.textoDia
        numero.text = dia.numero?.let { getString(R.string.formato_numero, it) }.orEmpty()
        numero.background = null
        numero.setTextAppearance(R.style.Cetis_Texto_CuerpoChico)
        val color = when {
            dia.esHoy -> {
                numero.setTextAppearance(R.style.Cetis_Texto_TituloTarjeta)
                numero.setBackgroundResource(R.drawable.shape_circulo_brand)
                R.color.cetis_text_on_brand
            }
            dia.esFinDeSemana -> R.color.cetis_text_secondary
            else -> R.color.cetis_brand_900
        }
        numero.setTextColor(ContextCompat.getColor(this, color))
        casilla.imagenEvento.visibility = if (dia.conEvento) View.VISIBLE else View.INVISIBLE
    }

    /** Primer instante del mes y primer instante del mes siguiente. */
    private fun limitesDelMes(mes: Calendar): Pair<Timestamp, Timestamp> {
        val inicio = (mes.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fin = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return Timestamp(inicio.time) to Timestamp(fin.time)
    }

    /** Las casillas del mes: vacías antes del día 1 (la semana empieza en lunes), días, hoy y eventos. */
    private fun casillasDelMes(mes: Calendar, eventos: List<EventoDoc>): List<CasillaDia> {
        val primero = (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val dias = primero.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Calendar: domingo = 1 … sábado = 7. La cuadrícula empieza en lunes.
        val vacias = (primero.get(Calendar.DAY_OF_WEEK) + DIAS_POR_SEMANA - 2) % DIAS_POR_SEMANA
        val hoy = Calendar.getInstance()
        val esEsteMes = hoy.get(Calendar.YEAR) == mes.get(Calendar.YEAR) && hoy.get(Calendar.MONTH) == mes.get(Calendar.MONTH)
        val conEvento = mutableSetOf<Int>()
        eventos.forEach { evento ->
            val inicio = evento.inicio?.toDate() ?: return@forEach
            val fin = evento.fin?.toDate() ?: inicio
            val dia = Calendar.getInstance().apply { time = inicio }
            while (!dia.time.after(fin) && dia.get(Calendar.MONTH) == mes.get(Calendar.MONTH)) {
                conEvento += dia.get(Calendar.DAY_OF_MONTH)
                dia.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return List(vacias) { CasillaDia(null) } + (1..dias).map { numero ->
            CasillaDia(
                numero = numero,
                esHoy = esEsteMes && numero == hoy.get(Calendar.DAY_OF_MONTH),
                esFinDeSemana = (vacias + numero - 1) % DIAS_POR_SEMANA >= 5,
                conEvento = numero in conEvento,
            )
        }
    }

    private companion object {
        const val DIAS_POR_SEMANA = 7
    }
}
