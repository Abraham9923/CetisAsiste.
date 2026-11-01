package com.example.cetisasiste.pantallas.alumno

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.AlumnoBoletinBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/** Boletín de noticias del alumno (alumno_boletin.xml): los anuncios dirigidos a "alumno". */
class BoletinAlumnoActividad : PantallaBase() {

    private lateinit var enlace: AlumnoBoletinBinding
    private val adaptador = Llenado.adaptadorAnuncios()

    override fun crearVista(): View {
        enlace = AlumnoBoletinBinding.inflate(layoutInflater)
        enlace.listaAnuncios.adapter = adaptador
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_boletin)
        lifecycleScope.launch {
            val anuncios = cargar { CetisRepositorio.anuncios(sesion.usuario.rol) }.o(emptyList())
            adaptador.mostrar(anuncios.map(Formatos::anuncio))
            enlace.textoVacio.isVisible = anuncios.isEmpty()
        }
    }
}
