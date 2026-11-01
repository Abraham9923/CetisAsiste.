package com.example.cetisasiste.pantallas.alumno

import android.view.View
import com.example.cetisasiste.databinding.AlumnoInicioBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/** Inicio del alumno (alumno_inicio.xml): su tarjeta y los cuatro accesos rápidos. */
class InicioAlumnoActividad : PantallaConAlumno() {

    private lateinit var enlace: AlumnoInicioBinding

    override fun crearVista(): View {
        enlace = AlumnoInicioBinding.inflate(layoutInflater)
        enlace.tarjetaQr.setOnClickListener { Navegacion.abrir(this, CodigoQrActividad::class.java) }
        enlace.tarjetaCredencial.setOnClickListener { Navegacion.abrir(this, CredencialActividad::class.java) }
        enlace.tarjetaHorario.setOnClickListener {
            Navegacion.abrirDesdeElMenu(this, HorarioAlumnoActividad::class.java, InicioAlumnoActividad::class.java)
        }
        enlace.tarjetaBoletin.setOnClickListener {
            Navegacion.abrirDesdeElMenu(this, BoletinAlumnoActividad::class.java, InicioAlumnoActividad::class.java)
        }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarraInicio(enlace.barraInicio)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRol.text = alumno.rol
        enlace.textoGrupo.text = alumno.grupo
    }
}
