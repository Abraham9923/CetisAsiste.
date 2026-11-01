package com.example.cetisasiste.pantallas.profesor

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ProfesorInicioBinding
import com.example.cetisasiste.databinding.ProfesorItemAlumnoBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.AdaptadorSimple
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/**
 * Inicio del profesor (profesor_inicio.xml, Figma 78:1083): saludo, "Dar salida al grupo" (el
 * grupo donde da la última clase del turno hoy), "Mis grupos" y el acceso al Boletín.
 */
class InicioProfesorActividad : PantallaBase() {

    private lateinit var enlace: ProfesorInicioBinding
    /** Grupo de la última clase de hoy (id y nombre), o null si hoy no le toca. */
    private var grupoDeSalida: Pair<String, String>? = null

    private val adaptadorGrupos = AdaptadorSimple<Pair<String, GrupoDoc>, ProfesorItemAlumnoBinding>(ProfesorItemAlumnoBinding::inflate) { fila, (grupoId, grupo), _ ->
        val nombre = grupo.nombre.trim()
        // En "Mis grupos" el círculo es guinda con el número del grupo y no lleva etiqueta.
        fila.textoIniciales.text = nombre
        fila.textoIniciales.setBackgroundResource(R.drawable.shape_circulo_brand)
        fila.textoIniciales.setTextColor(ContextCompat.getColor(this, R.color.cetis_text_on_brand))
        fila.textoNombre.text = getString(R.string.formato_grupo, nombre)
        fila.textoDetalle.text = Formatos.detalleDeGrupo(grupo)
        fila.textoEstatus.isVisible = false
        fila.root.setOnClickListener {
            Navegacion.abrir(this, GrupoActividad::class.java) {
                putExtra(Claves.GRUPO_ID, grupoId)
                putExtra(Claves.NOMBRE_GRUPO, nombre)
            }
        }
    }

    override fun crearVista(): View {
        enlace = ProfesorInicioBinding.inflate(layoutInflater)
        enlace.listaGrupos.adapter = adaptadorGrupos
        enlace.tarjetaDarSalida.setOnClickListener {
            val (grupoId, nombre) = grupoDeSalida ?: return@setOnClickListener avisar(R.string.aviso_sin_ultima_clase)
            Navegacion.abrir(this, DarSalidaActividad::class.java) {
                putExtra(Claves.GRUPO_ID, grupoId)
                putExtra(Claves.NOMBRE_GRUPO, nombre)
            }
        }
        enlace.tarjetaBoletin.setOnClickListener {
            Navegacion.abrirDesdeElMenu(this, BoletinProfesorActividad::class.java, InicioProfesorActividad::class.java)
        }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarraInicio(enlace.barraInicio)
        enlace.textoNombre.text = sesion.usuario.nombre.trim()
        enlace.tarjetaDarSalidaDetalle.setText(R.string.detalle_buscando_ultima_clase)
        lifecycleScope.launch {
            val grupos = cargar { CetisRepositorio.grupos(sesion.usuario.grupos) }.o(emptyList())
            adaptadorGrupos.mostrar(grupos)
            enlace.textoVacio.isVisible = grupos.isEmpty()
            buscarUltimaClase(sesion, grupos)
        }
    }

    /** El grupo donde el profesor da hoy la última clase del turno (para "Dar salida al grupo"). */
    private suspend fun buscarUltimaClase(sesion: Sesion, grupos: List<Pair<String, GrupoDoc>>) {
        val dia = Formatos.diaEscolarDeHoy()
        grupoDeSalida = if (dia == null) null else grupos.firstOrNull { (grupoId, _) ->
            cargar { CetisRepositorio.horario(grupoId, Formatos.idDeDia(dia)) }.o(emptyList())
                .any { it.ultimaDelTurno && (it.profesorId.isBlank() || it.profesorId == sesion.idUsuario) }
        }?.let { (grupoId, grupo) -> grupoId to grupo.nombre.trim() }
        enlace.tarjetaDarSalidaDetalle.text = grupoDeSalida?.let { getString(R.string.detalle_dar_salida, it.second) }
            ?: getString(R.string.detalle_sin_ultima_clase)
    }
}
