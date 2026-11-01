package com.example.cetisasiste.pantallas.profesor

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ProfesorAlumnoBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoEstatus
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/**
 * Perfil del alumno para el profesor (profesor_alumno.xml, Figma 78:1085): sus datos y último
 * estatus, tres acciones para cambiar su estatus y el historial.
 * Extras: [Claves.NO_CONTROL], [Claves.GRUPO_ID] y [Claves.NOMBRE_GRUPO].
 */
class AlumnoProfesorActividad : PantallaBase() {

    private lateinit var enlace: ProfesorAlumnoBinding
    private val adaptadorHistorial = Llenado.adaptadorRenglones()
    private val noControl: String get() = intent.getStringExtra(Claves.NO_CONTROL).orEmpty()
    private val nombreGrupo: String get() = intent.getStringExtra(Claves.NOMBRE_GRUPO).orEmpty()

    override fun crearVista(): View {
        enlace = ProfesorAlumnoBinding.inflate(layoutInflater)
        enlace.listaHistorial.adapter = adaptadorHistorial
        enlace.opcionFalta.setOnClickListener { cambiarEstatus(TipoEstatus.FALTA) }
        enlace.opcionReporte.setOnClickListener { cambiarEstatus(TipoEstatus.REPORTE) }
        enlace.opcionLlamada.setOnClickListener { cambiarEstatus(TipoEstatus.LLAMADA_ATENCION) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_perfil_alumno)
    }

    // Cada vez que se regresa (por ejemplo, después de poner un estatus) se vuelve a leer.
    override fun onStart() {
        super.onStart()
        recargar()
    }

    private fun cambiarEstatus(tipo: String) {
        Navegacion.abrir(this, CambiarEstatusActividad::class.java) {
            putExtra(Claves.NO_CONTROL, noControl)
            putExtra(Claves.NOMBRE_GRUPO, nombreGrupo)
            putExtra(Claves.TIPO_ESTATUS, tipo)
        }
    }

    private fun recargar() {
        lifecycleScope.launch {
            val alumno = cargar { CetisRepositorio.alumno(noControl) }.o(null)
            if (alumno != null) {
                enlace.textoIniciales.text = Formatos.iniciales(alumno)
                enlace.textoNombre.text = alumno.nombre.trim()
                enlace.textoDetalle.text = getString(R.string.formato_grupo_no_control, nombreGrupo, alumno.noControl)
            }
            val estatus = cargar { CetisRepositorio.estatus(noControl) }.o(emptyList())
            val (texto, color) = Estatus.etiqueta(this@AlumnoProfesorActividad, estatus.firstOrNull())
            Llenado.llenarEstatus(enlace.textoEstatus, texto, color)
            enlace.textoEstatus.visibility = View.VISIBLE
            adaptadorHistorial.mostrar(estatus.map { Estatus.renglon(this@AlumnoProfesorActividad, it) })
            enlace.textoVacio.isVisible = estatus.isEmpty()
        }
    }
}
