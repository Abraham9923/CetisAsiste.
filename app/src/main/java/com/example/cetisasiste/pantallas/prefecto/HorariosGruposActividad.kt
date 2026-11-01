package com.example.cetisasiste.pantallas.prefecto

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunItemChipBinding
import com.example.cetisasiste.databinding.PrefectoHorariosBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.AdaptadorSimple
import com.example.cetisasiste.pantallas.comun.DiaSemana
import com.example.cetisasiste.pantallas.comun.HorarioEnPantalla
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/**
 * Horarios de grupos (prefecto_horarios.xml, Figma 77:504): en una sola pantalla se elige el
 * grupo (chips en una lista horizontal), el día (RadioButton) y se ven sus clases.
 */
class HorariosGruposActividad : PantallaBase() {

    private lateinit var enlace: PrefectoHorariosBinding
    private lateinit var horario: HorarioEnPantalla
    private var grupos: List<Pair<String, GrupoDoc>> = emptyList()
    private var elegido = 0

    private val adaptadorGrupos = AdaptadorSimple<Pair<String, GrupoDoc>, ComunItemChipBinding>(ComunItemChipBinding::inflate) { chip, (_, grupo), posicion ->
        val esElegido = posicion == elegido
        chip.textoChip.text = grupo.nombre.trim()
        chip.textoChip.isSelected = esElegido
        chip.textoChip.setBackgroundResource(if (esElegido) R.drawable.shape_chip_activo else R.drawable.shape_chip)
        chip.textoChip.setTextColor(ContextCompat.getColor(this, if (esElegido) R.color.cetis_text_on_brand else R.color.cetis_brand_900))
        chip.textoChip.setOnClickListener { elegir(posicion) }
    }

    override fun crearVista(): View {
        enlace = PrefectoHorariosBinding.inflate(layoutInflater)
        enlace.listaGrupos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        enlace.listaGrupos.adapter = adaptadorGrupos
        horario = HorarioEnPantalla(
            lifecycleScope, enlace.grupoDias,
            mapOf(
                DiaSemana.LUNES to enlace.chipLunes, DiaSemana.MARTES to enlace.chipMartes,
                DiaSemana.MIERCOLES to enlace.chipMiercoles, DiaSemana.JUEVES to enlace.chipJueves,
                DiaSemana.VIERNES to enlace.chipViernes,
            ),
            enlace.textoEncabezado, enlace.listaClases, enlace.textoVacio,
        )
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_horarios_grupos)
        lifecycleScope.launch {
            grupos = cargar { CetisRepositorio.gruposDelSemestre() }.o(emptyList())
            enlace.textoVacioGrupos.isVisible = grupos.isEmpty()
            adaptadorGrupos.mostrar(grupos)
            if (grupos.isNotEmpty()) elegir(0)
        }
    }

    private fun elegir(posicion: Int) {
        elegido = posicion
        adaptadorGrupos.mostrar(grupos)
        val (grupoId, grupo) = grupos[posicion]
        horario.mostrar(grupoId, grupo.nombre.trim())
    }
}
