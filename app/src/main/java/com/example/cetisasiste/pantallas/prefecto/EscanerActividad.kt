package com.example.cetisasiste.pantallas.prefecto

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PrefectoEscanerBinding
import com.example.cetisasiste.datos.Medio
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.launch

/**
 * Escáner (prefecto_escaner.xml), pantalla del menú ☰ del prefecto. Pide el permiso de la
 * cámara al abrirse; al leer un QR de CetisAsiste registra la entrada o salida y abre el
 * resultado. "Escribir No. de control" abre la captura.
 */
class EscanerActividad : PantallaBase() {

    private lateinit var enlace: PrefectoEscanerBinding
    private var guardando = false
    private var permiso = false
    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permiso = concedido
        mostrarPermiso()
        if (concedido) enlace.camara.resume()
    }

    /** "entrada" o "salida": lo que esté elegido en el selector (RadioButton). */
    private val tipo: String
        get() = if (enlace.opcionSalida.isChecked) TipoRegistro.SALIDA else TipoRegistro.ENTRADA

    override fun crearVista(): View {
        enlace = PrefectoEscanerBinding.inflate(layoutInflater)
        if (intent.getStringExtra(Claves.TIPO_REGISTRO) == TipoRegistro.SALIDA) enlace.opcionSalida.isChecked = true
        enlace.grupoTipo.setOnCheckedChangeListener { _, _ -> mostrarTipo() }
        enlace.botonPermiso.setOnClickListener { pedirPermiso.launch(Manifest.permission.CAMERA) }
        enlace.botonEscribir.setOnClickListener {
            mostrarMensaje(null)
            Navegacion.abrir(this, CapturaActividad::class.java) { putExtra(Claves.TIPO_REGISTRO, tipo) }
        }
        enlace.camara.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        enlace.camara.decodeContinuous { lectura -> lectura.text?.let(::alLeer) }
        permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        mostrarTipo()
        mostrarPermiso()
        if (!permiso) pedirPermiso.launch(Manifest.permission.CAMERA)
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_escanear)
    }

    // La cámara se enciende al mostrarse la pantalla y se apaga al salir de ella o de la app.
    override fun onResume() {
        super.onResume()
        if (permiso) enlace.camara.resume()
    }

    override fun onPause() {
        enlace.camara.pause()
        super.onPause()
    }

    private fun alLeer(texto: String) {
        if (guardando || !haySesion) return
        val noControl = NoControl.desdeQr(texto)
        if (noControl == null) {
            mostrarMensaje(R.string.mensaje_qr_ajeno)
            return
        }
        if (RegistroDeEscaneo.yaSeRegistroHaceRato(noControl)) return
        guardando = true
        mostrarMensaje(R.string.mensaje_buscando_alumno)
        lifecycleScope.launch {
            val listo = RegistroDeEscaneo.registrar(this@EscanerActividad, sesion, noControl, tipo, Medio.QR)
            guardando = false
            mostrarMensaje(if (listo) null else R.string.error_guardar)
        }
    }

    private fun mostrarTipo() {
        enlace.textoRegistrando.text = getString(
            R.string.formato_registrando,
            getString(if (tipo == TipoRegistro.ENTRADA) R.string.tipo_entrada else R.string.tipo_salida),
        )
    }

    /** Sin permiso de cámara se tapa la cámara con el aviso para pedirlo. */
    private fun mostrarPermiso() {
        enlace.textoPermiso.isVisible = !permiso
        enlace.botonPermiso.isVisible = !permiso
    }

    private fun mostrarMensaje(@StringRes texto: Int?) {
        enlace.textoEstado.isVisible = texto != null
        texto?.let { enlace.textoEstado.setText(it) }
    }
}
