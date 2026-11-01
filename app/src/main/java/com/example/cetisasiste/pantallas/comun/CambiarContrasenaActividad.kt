package com.example.cetisasiste.pantallas.comun

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunCambiarContrasenaBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.ResultadoContrasena
import com.example.cetisasiste.datos.Sesion
import kotlinx.coroutines.launch

/**
 * Cambiar contraseña (comun_cambiar_contrasena.xml), para todos los roles.
 * Extra [Claves.CONTRASENA_OBLIGATORIA]: true si Control Escolar pidió cambiarla o ya venció.
 * Entonces se ve el aviso amarillo y el menú ☰ solo trae "Cerrar sesión" (no se puede usar la
 * app sin cambiarla). Al guardar, pasa al Inicio del rol; si no era obligatoria, regresa.
 */
class CambiarContrasenaActividad : PantallaBase() {

    private lateinit var enlace: ComunCambiarContrasenaBinding
    private val obligatoria: Boolean get() = intent.getBooleanExtra(Claves.CONTRASENA_OBLIGATORIA, false)
    private var guardando = false

    override fun crearVista(): View {
        enlace = ComunCambiarContrasenaBinding.inflate(layoutInflater)
        enlace.avisoVencida.isVisible = obligatoria
        // Los ojos (CheckBox) muestran u ocultan cada contraseña.
        enlace.casillaMostrarActual.setOnCheckedChangeListener { _, mostrar ->
            IniciarSesionActividad.mostrarContrasena(enlace.campoActual, mostrar)
        }
        enlace.casillaMostrarNueva.setOnCheckedChangeListener { _, mostrar ->
            IniciarSesionActividad.mostrarContrasena(enlace.campoNueva, mostrar)
        }
        enlace.casillaMostrarRepetir.setOnCheckedChangeListener { _, mostrar ->
            IniciarSesionActividad.mostrarContrasena(enlace.campoRepetir, mostrar)
        }
        enlace.botonGuardar.setOnClickListener { guardar() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_cambiar_contrasena)
    }

    override fun abrirMenu() {
        if (obligatoria) MenuLateral.mostrar(this, raiz.parent as ViewGroup, sesion, soloCerrarSesion = true) else super.abrirMenu()
    }

    private fun guardar() {
        if (guardando) return
        val actual = enlace.campoActual.text.toString()
        val nueva = enlace.campoNueva.text.toString()
        val error = errorDeContrasena(actual, nueva, enlace.campoRepetir.text.toString())
        mostrarMensaje(error)
        if (error != null) return
        ponerGuardando(true)
        lifecycleScope.launch {
            val resultado = CetisRepositorio.cambiarContrasena(actual, nueva)
            ponerGuardando(false)
            if (resultado == ResultadoContrasena.Lista) {
                avisar(R.string.aviso_contrasena_cambiada)
                SesionActiva.contrasenaCambiada = true
                if (obligatoria) Navegacion.abrirInicioDeLaSesion(this@CambiarContrasenaActividad, sesion) else finish()
            } else {
                mostrarMensaje(textoDeResultado(resultado))
            }
        }
    }

    private fun ponerGuardando(si: Boolean) {
        guardando = si
        enlace.botonGuardar.isEnabled = !si
        enlace.botonGuardar.alpha = if (si) ALFA_APAGADO else 1f
        enlace.botonGuardar.setText(if (si) R.string.texto_guardando else R.string.boton_guardar_continuar)
    }

    private fun mostrarMensaje(@StringRes texto: Int?) {
        enlace.textoMensaje.isVisible = texto != null
        texto?.let { enlace.textoMensaje.setText(it) }
    }

    /** Revisa los tres campos antes de llamar a Firebase. Devuelve el texto del error o null. */
    @StringRes
    private fun errorDeContrasena(actual: String, nueva: String, repetir: String): Int? = when {
        actual.isEmpty() || nueva.isEmpty() || repetir.isEmpty() -> R.string.error_contrasena_vacios
        nueva.length < MINIMO_CONTRASENA -> R.string.error_contrasena_corta
        nueva != repetir -> R.string.error_contrasena_no_coinciden
        nueva == actual -> R.string.error_contrasena_igual
        else -> null
    }

    @StringRes
    private fun textoDeResultado(resultado: ResultadoContrasena): Int = when (resultado) {
        ResultadoContrasena.ActualIncorrecta -> R.string.error_contrasena_actual
        ResultadoContrasena.Debil -> R.string.error_contrasena_debil
        ResultadoContrasena.SinConexion -> R.string.error_sin_conexion
        ResultadoContrasena.DemasiadosIntentos -> R.string.error_demasiados_intentos
        ResultadoContrasena.Lista, ResultadoContrasena.Desconocido -> R.string.error_contrasena_desconocido
    }

    private companion object {
        /** Mínimo de caracteres de una contraseña nueva (Firebase pide 6; el plantel, 8). */
        const val MINIMO_CONTRASENA = 8
        const val ALFA_APAGADO = 0.6f
    }
}
