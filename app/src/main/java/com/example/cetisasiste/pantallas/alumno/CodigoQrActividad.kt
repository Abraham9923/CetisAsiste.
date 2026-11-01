package com.example.cetisasiste.pantallas.alumno

import android.view.View
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoCodigoQrBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno

/**
 * Código QR del alumno (alumno_codigo_qr.xml). Lo abren el alumno y el padre (el de su hijo).
 * El QR lleva "CETIS61:" + el No. de control (ADR-009); es lo que lee el escáner del prefecto.
 */
class CodigoQrActividad : PantallaConAlumno() {

    private lateinit var enlace: AlumnoCodigoQrBinding
    private var noControlDibujado = ""

    override fun crearVista(): View {
        enlace = AlumnoCodigoQrBinding.inflate(layoutInflater)
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_codigo_qr)
        super.alTenerSesion(sesion)
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRolGrupo.text = getString(R.string.formato_rol_grupo, alumno.rol, alumno.grupo)
        if (documento.noControl != noControlDibujado) {
            noControlDibujado = documento.noControl
            enlace.imagenQr.setImageBitmap(Formatos.imagenQr(NoControl.contenidoQr(documento.noControl)))
        }
    }
}
