package com.example.cetisasiste.pantallas.comun

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunIniciarSesionBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.MensajeInicioSesion
import com.example.cetisasiste.datos.ResultadoInicioSesion
import kotlinx.coroutines.launch

/**
 * Iniciar sesión (comun_iniciar_sesion.xml): la primera pantalla de la app, para todos los
 * roles. El alumno escribe su No. de control; el personal y los padres, el usuario que les dio
 * Control Escolar. Si ya había una sesión abierta, pasa directo al Inicio del rol.
 */
class IniciarSesionActividad : PantallaBase() {

    override val necesitaSesion = false
    private lateinit var enlace: ComunIniciarSesionBinding
    private var cargando = false

    override fun crearVista(): View {
        enlace = ComunIniciarSesionBinding.inflate(layoutInflater)
        enlace.botonIniciarSesion.setOnClickListener { entrar() }
        enlace.campoContrasena.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) entrar()
            false
        }
        // El ojo (CheckBox): marcado muestra la contraseña, desmarcado la oculta.
        enlace.casillaMostrarContrasena.setOnCheckedChangeListener { _, mostrar ->
            mostrarContrasena(enlace.campoContrasena, mostrar)
        }
        // Si la sesión se cerró sola (cuenta desactivada o borrada), se dice por qué.
        SesionActiva.motivoDeCierre?.let { mostrarError(textoDeError(it)) }

        // ¿Ya había una sesión abierta? Entonces se pasa directo al Inicio del rol.
        lifecycleScope.launch {
            val guardada = cargar { SesionActiva.recuperar() }.o(null)
            if (guardada != null) Navegacion.abrirInicioDeLaSesion(this@IniciarSesionActividad, guardada)
        }
        return enlace.root
    }

    private fun entrar() {
        if (cargando) return
        ponerCargando(true)
        mostrarError(null)
        lifecycleScope.launch {
            val resultado = CetisRepositorio.iniciarSesion(
                enlace.campoUsuario.text.toString(),
                enlace.campoContrasena.text.toString(),
            )
            ponerCargando(false)
            when (resultado) {
                is ResultadoInicioSesion.Correcto -> {
                    SesionActiva.iniciar(resultado.sesion)
                    Navegacion.abrirInicioDeLaSesion(this@IniciarSesionActividad, resultado.sesion)
                }
                is ResultadoInicioSesion.Error -> mostrarError(textoDeError(resultado.mensaje))
            }
        }
    }

    private fun ponerCargando(si: Boolean) {
        cargando = si
        enlace.botonIniciarSesion.isEnabled = !si
        enlace.botonIniciarSesion.setText(if (si) R.string.boton_entrando else R.string.boton_iniciar_sesion)
    }

    private fun mostrarError(@StringRes texto: Int?) {
        enlace.textoError.isVisible = texto != null
        texto?.let { enlace.textoError.setText(it) }
    }

    companion object {
        /** El texto de cada error al iniciar sesión (strings.xml). */
        @StringRes
        fun textoDeError(mensaje: MensajeInicioSesion): Int = when (mensaje) {
            MensajeInicioSesion.CamposVacios -> R.string.error_inicio_sesion_campos
            MensajeInicioSesion.NoControlInvalido -> R.string.error_no_control_invalido
            MensajeInicioSesion.DatosIncorrectos -> R.string.error_inicio_sesion_datos
            MensajeInicioSesion.CuentaNoRegistrada -> R.string.error_cuenta_no_registrada
            MensajeInicioSesion.CuentaDesactivada -> R.string.error_cuenta_desactivada
            MensajeInicioSesion.SinConexion -> R.string.error_sin_conexion
            MensajeInicioSesion.DemasiadosIntentos -> R.string.error_demasiados_intentos
            MensajeInicioSesion.Desconocido -> R.string.error_inicio_sesion_desconocido
        }

        /** Muestra u oculta lo que se escribió en un campo de contraseña (lo usa el ojo). */
        fun mostrarContrasena(campo: EditText, mostrar: Boolean) {
            val cursor = campo.selectionEnd
            campo.transformationMethod =
                if (mostrar) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            campo.setSelection(cursor.coerceAtLeast(0))
        }
    }
}
