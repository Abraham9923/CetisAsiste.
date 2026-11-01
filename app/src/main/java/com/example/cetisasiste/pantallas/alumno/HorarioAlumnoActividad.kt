package com.example.cetisasiste.pantallas.alumno

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoHorarioBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.DiaSemana
import com.example.cetisasiste.pantallas.comun.HorarioEnPantalla
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/** Horario del alumno (alumno_horario.xml): las clases de su grupo, día por día. */
class HorarioAlumnoActividad : PantallaConAlumno() {

    private lateinit var enlace: AlumnoHorarioBinding
    private lateinit var horario: HorarioEnPantalla
    private var grupoMostrado: Pair<String, String>? = null

    override fun crearVista(): View {
        enlace = AlumnoHorarioBinding.inflate(layoutInflater)
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
        conectarBarra(enlace.barraSuperior, R.string.titulo_mi_horario)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        // Solo se vuelve a pedir el horario si cambió el grupo.
        val grupo = documento.grupoId to alumno.grupo
        if (grupo != grupoMostrado) {
            grupoMostrado = grupo
            horario.mostrar(grupo.first, grupo.second)
        }
    }
}
