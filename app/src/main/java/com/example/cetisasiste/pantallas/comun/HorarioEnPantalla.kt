package com.example.cetisasiste.pantallas.comun

import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * El horario de un grupo dentro de una pantalla: los chips de lunes a viernes (RadioButton
 * dentro de un RadioGroup: se elige UN día), el encabezado "Grupo 102 · miércoles" y la lista
 * de clases (RecyclerView con alumno_item_clase.xml).
 *
 * Lo usan Horario del alumno (alumno_horario.xml), Horario del padre (comun_horario.xml) y
 * Horarios de grupos del prefecto (prefecto_horarios.xml), que tienen los mismos ids.
 */
class HorarioEnPantalla(
    private val alcance: LifecycleCoroutineScope,
    private val grupoDias: RadioGroup,
    private val chips: Map<DiaSemana, RadioButton>,
    private val encabezado: TextView,
    lista: RecyclerView,
    private val vacio: TextView,
) {
    private val adaptador = Llenado.adaptadorClases()
    private var grupoId = ""
    private var nombreGrupo = ""
    private var dia = Formatos.diaDeHoy()
    private var consulta: Job? = null

    init {
        lista.adapter = adaptador
        chips.getValue(dia).isChecked = true
        grupoDias.setOnCheckedChangeListener { _, idElegido ->
            val elegido = chips.entries.firstOrNull { it.value.id == idElegido }?.key ?: return@setOnCheckedChangeListener
            if (elegido != dia) {
                dia = elegido
                consultar()
            }
        }
    }

    /** Muestra el horario del grupo [nuevoGrupoId] (su nombre corto es [nuevoNombre]). */
    fun mostrar(nuevoGrupoId: String, nuevoNombre: String) {
        grupoId = nuevoGrupoId
        nombreGrupo = nuevoNombre
        consultar()
    }

    private fun consultar() {
        encabezado.text = encabezado.context.getString(R.string.formato_encabezado_horario, nombreGrupo, Formatos.nombreDeDia(dia))
        consulta?.cancel()
        consulta = alcance.launch {
            // Mientras carga no se ve "No hay clases" antes de tiempo.
            vacio.isVisible = false
            val clases = cargar { CetisRepositorio.horario(grupoId, Formatos.idDeDia(dia)) }.o(emptyList())
            adaptador.mostrar(clases.map(Formatos::clase))
            vacio.isVisible = clases.isEmpty()
        }
    }
}
