package com.example.cetisasiste.pantallas.profesor

import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ProfesorGrupoBinding
import com.example.cetisasiste.databinding.ProfesorItemAlumnoBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.AdaptadorSimple
import com.example.cetisasiste.pantallas.comun.AlumnoDeLista
import com.example.cetisasiste.pantallas.comun.Carga
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.ColorEstatus
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * Lista de alumnos de un grupo (profesor_grupo.xml, Figma 78:1084): buscador y un renglón por
 * alumno con su último estatus. Extras: [Claves.GRUPO_ID] y [Claves.NOMBRE_GRUPO].
 */
class GrupoActividad : PantallaBase() {

    private lateinit var enlace: ProfesorGrupoBinding
    private var alumnos: List<AlumnoDeLista> = emptyList()
    private var cargado = false
    private val grupoId: String get() = intent.getStringExtra(Claves.GRUPO_ID).orEmpty()
    private val nombreGrupo: String get() = intent.getStringExtra(Claves.NOMBRE_GRUPO).orEmpty()

    private val adaptador = AdaptadorSimple<AlumnoDeLista, ProfesorItemAlumnoBinding>(ProfesorItemAlumnoBinding::inflate) { fila, alumno, _ ->
        fila.textoIniciales.text = alumno.iniciales
        fila.textoNombre.text = alumno.nombre
        fila.textoDetalle.text = getString(R.string.formato_no_control, alumno.noControl)
        fila.textoEstatus.isVisible = alumno.estatus != null
        alumno.estatus?.let { Llenado.llenarEstatus(fila.textoEstatus, it, alumno.color) }
        fila.root.setOnClickListener {
            Navegacion.abrir(this, AlumnoProfesorActividad::class.java) {
                putExtra(Claves.NO_CONTROL, alumno.noControl)
                putExtra(Claves.GRUPO_ID, grupoId)
                putExtra(Claves.NOMBRE_GRUPO, nombreGrupo)
            }
        }
    }

    override fun crearVista(): View {
        enlace = ProfesorGrupoBinding.inflate(layoutInflater)
        enlace.listaAlumnos.adapter = adaptador
        enlace.campoBuscar.doAfterTextChanged { filtrar() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, getString(R.string.formato_grupo, nombreGrupo))
    }

    // Cada vez que se regresa a la lista (por ejemplo, después de poner un estatus) se vuelve a leer.
    override fun onStart() {
        super.onStart()
        recargar()
    }

    private fun recargar() {
        lifecycleScope.launch {
            val documentos = cargar { CetisRepositorio.alumnosDelGrupo(grupoId) }.o(emptyList())
            // Primero la lista; luego, el último estatus de cada alumno (todos a la vez).
            alumnos = documentos.map { alDeLista(it, null) }
            cargado = true
            filtrar()
            val conEstatus = documentos.map { documento ->
                async { alDeLista(documento, cargar { CetisRepositorio.ultimoEstatus(documento.noControl) }) }
            }.awaitAll()
            alumnos = conEstatus
            filtrar()
        }
    }

    private fun alDeLista(documento: AlumnoDoc, ultimo: Carga<EstatusDoc?>?): AlumnoDeLista {
        val etiqueta = ultimo?.let { Estatus.etiqueta(this, it.o(null)) }
        return AlumnoDeLista(
            noControl = documento.noControl,
            iniciales = Formatos.iniciales(documento),
            nombre = documento.nombre.trim(),
            estatus = etiqueta?.first,
            color = etiqueta?.second ?: ColorEstatus.Exito,
        )
    }

    /** Muestra los alumnos cuyo nombre o No. de control tienen lo escrito en el buscador. */
    private fun filtrar() {
        val buscado = sinAcentos(enlace.campoBuscar.text.toString())
        val visibles = if (buscado.isBlank()) alumnos else alumnos.filter {
            sinAcentos(it.nombre).contains(buscado) || it.noControl.contains(buscado)
        }
        adaptador.mostrar(visibles)
        enlace.textoVacio.isVisible = cargado && visibles.isEmpty()
    }

    private fun sinAcentos(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}
