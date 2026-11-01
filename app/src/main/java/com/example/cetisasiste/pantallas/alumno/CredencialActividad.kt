package com.example.cetisasiste.pantallas.alumno

import android.view.View
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoCredencialDigitalBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/** Credencial digital (alumno_credencial_digital.xml). La abren el alumno y el padre (la de su hijo). */
class CredencialActividad : PantallaConAlumno() {

    private lateinit var enlace: AlumnoCredencialDigitalBinding

    override fun crearVista(): View {
        enlace = AlumnoCredencialDigitalBinding.inflate(layoutInflater)
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_credencial)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRol.text = alumno.rol
        enlace.textoGrupo.text = alumno.grupo
        enlace.textoNoControl.text = alumno.noControl
        enlace.textoVigencia.text = alumno.vigencia
    }
}
