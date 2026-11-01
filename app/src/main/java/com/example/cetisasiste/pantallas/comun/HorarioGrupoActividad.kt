package com.example.cetisasiste.pantallas.comun

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunHorarioBinding
import com.example.cetisasiste.datos.Sesion

/**
 * Horario de un grupo (comun_horario.xml). Lo abre el padre (el grupo de su hijo).
 * Extras: [Claves.GRUPO_ID] y [Claves.NOMBRE_GRUPO].
 */
class HorarioGrupoActividad : PantallaBase() {

    private lateinit var enlace: ComunHorarioBinding

    override fun crearVista(): View {
        enlace = ComunHorarioBinding.inflate(layoutInflater)
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_horario)
        val horario = HorarioEnPantalla(
            lifecycleScope, enlace.grupoDias,
            mapOf(
                DiaSemana.LUNES to enlace.chipLunes, DiaSemana.MARTES to enlace.chipMartes,
                DiaSemana.MIERCOLES to enlace.chipMiercoles, DiaSemana.JUEVES to enlace.chipJueves,
                DiaSemana.VIERNES to enlace.chipViernes,
            ),
            enlace.textoEncabezado, enlace.listaClases, enlace.textoVacio,
        )
        horario.mostrar(
            intent.getStringExtra(Claves.GRUPO_ID).orEmpty(),
            intent.getStringExtra(Claves.NOMBRE_GRUPO).orEmpty(),
        )
    }
}
