package com.example.cetisasiste.pantallas.alumno

import android.view.View
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoPerfilBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.CambiarContrasenaActividad
import com.example.cetisasiste.pantallas.comun.DialogoCerrarSesion
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/**
 * Perfil del alumno (alumno_perfil.xml): sus datos, su QR, su credencial, cambiar contraseña y
 * cerrar sesión (con el diálogo "¿Cerrar sesión?" encima, como en Figma 124:1923).
 */
class PerfilAlumnoActividad : PantallaConAlumno() {

    private lateinit var enlace: AlumnoPerfilBinding

    override fun crearVista(): View {
        enlace = AlumnoPerfilBinding.inflate(layoutInflater)
        enlace.opcionQr.setOnClickListener { Navegacion.abrir(this, CodigoQrActividad::class.java) }
        enlace.opcionCredencial.setOnClickListener { Navegacion.abrir(this, CredencialActividad::class.java) }
        enlace.opcionContrasena.setOnClickListener { Navegacion.abrir(this, CambiarContrasenaActividad::class.java) }
        enlace.opcionCerrarSesion.setOnClickListener { DialogoCerrarSesion.mostrar(this) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_perfil)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRolGrupo.text = getString(R.string.formato_rol_grupo, alumno.rol, alumno.grupo)
        enlace.textoNoControl.text = alumno.noControl
        enlace.textoGrupo.text = alumno.grupo
        enlace.textoCorreo.text = alumno.correo
    }
}
