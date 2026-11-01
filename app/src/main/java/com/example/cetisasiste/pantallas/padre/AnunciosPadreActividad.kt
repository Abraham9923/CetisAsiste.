package com.example.cetisasiste.pantallas.padre

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PadreAnunciosBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/** Anuncios del padre (padre_anuncios.xml): los dirigidos a "padre", con las tarjetas del boletín. */
class AnunciosPadreActividad : PantallaBase() {

    private lateinit var enlace: PadreAnunciosBinding
    private val adaptador = Llenado.adaptadorAnuncios()

    override fun crearVista(): View {
        enlace = PadreAnunciosBinding.inflate(layoutInflater)
        enlace.listaAnuncios.adapter = adaptador
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_anuncios)
        lifecycleScope.launch {
            val anuncios = cargar { CetisRepositorio.anuncios(sesion.usuario.rol) }.o(emptyList())
            adaptador.mostrar(anuncios.map(Formatos::anuncio))
            enlace.textoVacio.isVisible = anuncios.isEmpty()
        }
    }
}
