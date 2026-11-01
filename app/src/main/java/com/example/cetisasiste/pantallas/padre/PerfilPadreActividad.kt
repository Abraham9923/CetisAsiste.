package com.example.cetisasiste.pantallas.padre

import android.view.View
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PadrePerfilBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.alumno.CredencialActividad
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.CambiarContrasenaActividad
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.DialogoCerrarSesion
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/** Perfil del padre (padre_perfil.xml): los datos de su hijo y sus opciones. */
class PerfilPadreActividad : PantallaConAlumno() {

    private lateinit var enlace: PadrePerfilBinding
    private var noControl = ""

    override fun crearVista(): View {
        enlace = PadrePerfilBinding.inflate(layoutInflater)
        enlace.opcionCredencial.setOnClickListener { Navegacion.abrir(this, CredencialActividad::class.java) }
        enlace.opcionEntradas.setOnClickListener {
            if (noControl.isNotBlank()) {
                Navegacion.abrir(this, EntradasSalidasActividad::class.java) { putExtra(Claves.NO_CONTROL, noControl) }
            }
        }
        enlace.opcionContrasena.setOnClickListener { Navegacion.abrir(this, CambiarContrasenaActividad::class.java) }
        enlace.opcionCerrarSesion.setOnClickListener { DialogoCerrarSesion.mostrar(this) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_perfil)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        noControl = alumno.noControl
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRolGrupo.text = getString(R.string.formato_rol_grupo, alumno.rol, alumno.grupo)
        enlace.textoNoControl.text = alumno.noControl
        enlace.textoGrupo.text = alumno.grupo
        enlace.textoCorreo.text = alumno.correo
    }
}
