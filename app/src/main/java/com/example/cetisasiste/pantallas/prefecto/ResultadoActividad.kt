package com.example.cetisasiste.pantallas.prefecto

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PrefectoResultadoBinding
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Resultado del último escaneo (prefecto_resultado.xml): verde si no hubo alerta; rojo con el
 * título de la alerta si la hubo. Los datos llegan como extras del Intent (RegistroDeEscaneo).
 * "Escanear siguiente" regresa a donde se escaneó.
 */
class ResultadoActividad : PantallaBase() {

    private lateinit var enlace: PrefectoResultadoBinding

    override fun crearVista(): View {
        enlace = PrefectoResultadoBinding.inflate(layoutInflater)
        enlace.botonSiguiente.setOnClickListener { finish() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_resultado)
        val datos = intent
        val alerta = datos.getStringExtra(Claves.RESULTADO_ALERTA)
        val tipo = datos.getStringExtra(Claves.TIPO_REGISTRO) ?: TipoRegistro.ENTRADA
        val sinAlerta = alerta == null
        val entrada = tipo == TipoRegistro.ENTRADA

        enlace.tarjetaResultado.setBackgroundResource(if (sinAlerta) R.drawable.shape_resultado_exito else R.drawable.shape_resultado_alerta)
        enlace.iconoResultado.setBackgroundResource(if (sinAlerta) R.drawable.shape_circulo_exito else R.drawable.shape_circulo_peligro)
        enlace.iconoResultado.setImageResource(if (sinAlerta) R.drawable.ic_check_circle else R.drawable.ic_x_circle)
        enlace.textoResultado.setText(
            when {
                !sinAlerta -> RegistroDeEscaneo.tituloDeAlerta(alerta)
                entrada -> R.string.resultado_entrada_registrada
                else -> R.string.resultado_salida_registrada
            },
        )
        enlace.textoResultado.setTextColor(
            ContextCompat.getColor(this, if (sinAlerta) R.color.cetis_status_success else R.color.cetis_status_danger),
        )
        val hora = Formatos.hora(Timestamp(Date(datos.getLongExtra(Claves.RESULTADO_HORA, System.currentTimeMillis()))))
        val textoTipo = getString(if (entrada) R.string.tipo_entrada else R.string.tipo_salida)
        // Figma: sin alerta "Hoy · 7:05 am"; con alerta "Hoy · 7:12 am · Entrada".
        enlace.textoCuando.text = if (sinAlerta) {
            getString(R.string.formato_dos_partes, getString(R.string.texto_hoy), hora)
        } else {
            getString(R.string.formato_hoy_hora_tipo, hora, textoTipo)
        }

        enlace.textoIniciales.text = datos.getStringExtra(Claves.RESULTADO_INICIALES).orEmpty()
        enlace.textoNombre.text = datos.getStringExtra(Claves.RESULTADO_NOMBRE).orEmpty()
        enlace.textoDetalleAlumno.text = datos.getStringExtra(Claves.RESULTADO_DETALLE).orEmpty()
        val etiqueta = datos.getStringExtra(Claves.RESULTADO_ETIQUETA)
        enlace.textoEtiquetaAlerta.isVisible = etiqueta != null
        enlace.textoEtiquetaAlerta.text = etiqueta.orEmpty()

        enlace.textoAviso.setBackgroundResource(if (sinAlerta) R.drawable.shape_aviso_exito else R.drawable.shape_aviso_peligro)
        enlace.textoAviso.text = datos.getStringExtra(Claves.RESULTADO_AVISO).orEmpty()
    }
}
