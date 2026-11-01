package com.example.cetisasiste.pantallas.prefecto

import android.view.View
import android.widget.Button
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PrefectoCapturaBinding
import com.example.cetisasiste.datos.Medio
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.PantallaBase
import kotlinx.coroutines.launch

/**
 * Captura por No. de control (prefecto_captura.xml): teclado de 14 dígitos y "Registrar".
 * Extra: [Claves.TIPO_REGISTRO] (lo que estaba elegido en el escáner).
 */
class CapturaActividad : PantallaBase() {

    private lateinit var enlace: PrefectoCapturaBinding
    private var tecleado = ""
    private var guardando = false

    private val tipo: String
        get() = if (enlace.opcionSalida.isChecked) TipoRegistro.SALIDA else TipoRegistro.ENTRADA

    override fun crearVista(): View {
        enlace = PrefectoCapturaBinding.inflate(layoutInflater)
        if (intent.getStringExtra(Claves.TIPO_REGISTRO) == TipoRegistro.SALIDA) enlace.opcionSalida.isChecked = true
        enlace.grupoTipo.setOnCheckedChangeListener { _, _ -> mostrar(null) }
        val teclas: List<Pair<Button, Char>> = listOf(
            enlace.tecla0 to '0', enlace.tecla1 to '1', enlace.tecla2 to '2', enlace.tecla3 to '3', enlace.tecla4 to '4',
            enlace.tecla5 to '5', enlace.tecla6 to '6', enlace.tecla7 to '7', enlace.tecla8 to '8', enlace.tecla9 to '9',
        )
        teclas.forEach { (tecla, digito) ->
            tecla.setOnClickListener {
                if (tecleado.length < NoControl.DIGITOS) tecleado += digito
                mostrar(null)
            }
        }
        enlace.teclaBorrar.setOnClickListener { tecleado = tecleado.dropLast(1); mostrar(null) }
        enlace.teclaLimpiar.setOnClickListener { tecleado = ""; mostrar(null) }
        enlace.botonRegistrar.setOnClickListener { registrar() }
        mostrar(null)
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_captura)
    }

    override fun onResume() {
        super.onResume()
        // Al regresar del resultado se empieza de nuevo.
        if (!guardando) mostrar(null)
    }

    private fun registrar() {
        if (guardando || !haySesion) return
        val noControl = tecleado
        if (!NoControl.esValido(noControl)) {
            mostrar(R.string.error_no_control_invalido)
            return
        }
        guardando = true
        mostrar(null)
        lifecycleScope.launch {
            val listo = RegistroDeEscaneo.registrar(this@CapturaActividad, sesion, noControl, tipo, Medio.NO_CONTROL)
            guardando = false
            if (listo) tecleado = ""
            mostrar(if (listo) null else R.string.error_guardar)
        }
    }

    /** [mensaje] es un error que va en rojo debajo del número (null = la ayuda). */
    private fun mostrar(@StringRes mensaje: Int?) {
        enlace.textoTecleado.text = tecleado
        enlace.textoAyuda.setText(mensaje ?: R.string.ayuda_captura)
        enlace.textoAyuda.setTextColor(
            ContextCompat.getColor(this, if (mensaje != null) R.color.cetis_status_danger else R.color.cetis_text_secondary),
        )
        enlace.botonRegistrar.isEnabled = !guardando
        enlace.botonRegistrar.alpha = if (guardando) ALFA_APAGADO else 1f
        enlace.botonRegistrar.setText(
            when {
                guardando -> R.string.texto_guardando
                tipo == TipoRegistro.ENTRADA -> R.string.boton_registrar_entrada
                else -> R.string.boton_registrar_salida
            },
        )
    }

    private companion object {
        const val ALFA_APAGADO = 0.6f
    }
}
