package com.example.cetisasiste.pantallas.profesor

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ProfesorDarSalidaBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.ClaseDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.SalidaDeGrupoDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Carga
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

/**
 * Dar salida al grupo (profesor_dar_salida.xml, Figma 78:1087): el profesor libera al grupo en
 * la última clase del turno; el prefecto ya no recibe alertas al verlos salir.
 * Extras: [Claves.GRUPO_ID] y [Claves.NOMBRE_GRUPO].
 */
class DarSalidaActividad : PantallaBase() {

    private lateinit var enlace: ProfesorDarSalidaBinding
    private var ultimaClase: ClaseDoc? = null
    private var guardando = false
    private val grupoId: String get() = intent.getStringExtra(Claves.GRUPO_ID).orEmpty()
    private val nombreGrupo: String get() = intent.getStringExtra(Claves.NOMBRE_GRUPO).orEmpty()

    override fun crearVista(): View {
        enlace = ProfesorDarSalidaBinding.inflate(layoutInflater)
        enlace.textoGrupo.text = getString(R.string.formato_grupo, nombreGrupo)
        // Mientras se revisa si el grupo ya salió hoy, el botón espera apagado (así no se registra dos veces).
        habilitarBoton(false)
        enlace.botonDarSalida.setOnClickListener { darSalida() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_dar_salida)
        lifecycleScope.launch {
            val dia = Formatos.diaEscolarDeHoy()
            val clases = if (dia == null) emptyList() else cargar { CetisRepositorio.horario(grupoId, Formatos.idDeDia(dia)) }.o(emptyList())
            ultimaClase = clases.lastOrNull { it.ultimaDelTurno } ?: clases.lastOrNull()
            val clase = ultimaClase
            enlace.textoMateria.text = clase?.materia ?: getString(R.string.texto_sin_clases_hoy)
            enlace.textoUltimaClase.isVisible = clase != null
            clase?.let {
                enlace.textoUltimaClase.text = getString(R.string.formato_ultima_clase, Formatos.rangoDeHoras(it.horaInicio, it.horaFin))
            }
            val salida = cargar { CetisRepositorio.salidasDeGrupoDeHoy() }.o(emptyList()).firstOrNull { it.grupoId == grupoId }
            mostrarSalida(salida)
            habilitarBoton(salida == null)
        }
    }

    private fun darSalida() {
        if (guardando) return
        guardando = true
        habilitarBoton(false)
        lifecycleScope.launch {
            val salida = SalidaDeGrupoDoc(
                grupoId = grupoId,
                grupo = nombreGrupo,
                fecha = Formatos.fechaDeHoy(),
                fechaHora = Timestamp.now(),
                autorizadaPor = sesion.idUsuario,
                autorizadaPorNombre = sesion.usuario.nombre.trim(),
                materia = ultimaClase?.materia.orEmpty(),
            )
            val resultado = cargar { CetisRepositorio.darSalidaAlGrupo(salida) }
            guardando = false
            if (resultado is Carga.Listo) {
                avisar(R.string.aviso_salida_guardada)
                finish()
            } else {
                mostrarError(R.string.error_guardar)
                habilitarBoton(true)
            }
        }
    }

    /** Recuadro verde "El grupo salió hoy a las 11:40 am (autorizó …)". */
    private fun mostrarSalida(salida: SalidaDeGrupoDoc?) {
        enlace.textoMensaje.isVisible = salida != null
        enlace.textoMensaje.setBackgroundResource(R.drawable.shape_aviso_exito)
        salida?.let {
            enlace.textoMensaje.text = getString(R.string.formato_salida_hecha, Formatos.hora(it.fechaHora), it.autorizadaPorNombre)
        }
    }

    private fun mostrarError(@StringRes texto: Int) {
        enlace.textoMensaje.isVisible = true
        enlace.textoMensaje.setBackgroundResource(R.drawable.shape_aviso_peligro)
        enlace.textoMensaje.setText(texto)
    }

    /** Un botón apagado se ve más claro y no responde. */
    private fun habilitarBoton(si: Boolean) {
        enlace.botonDarSalida.isEnabled = si
        enlace.botonDarSalida.alpha = if (si) 1f else ALFA_APAGADO
    }

    private companion object {
        const val ALFA_APAGADO = 0.5f
    }
}
