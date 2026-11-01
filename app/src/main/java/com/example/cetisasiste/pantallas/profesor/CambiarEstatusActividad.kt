package com.example.cetisasiste.pantallas.profesor

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ProfesorCambiarEstatusBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.ConfiguracionDoc
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoAviso
import com.example.cetisasiste.datos.TipoEstatus
import com.example.cetisasiste.pantallas.comun.Carga
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.PantallaBase
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

/**
 * Cambiar estatus (profesor_cambiar_estatus.xml, Figma 78:1086): el profesor elige UN tipo
 * (RadioButton: Falta, Reporte o Llamada de atención), escribe el motivo y guarda. Si el plantel
 * lo pide (configuracion/plantel), el padre recibe un aviso (ADR-010).
 * Extras: [Claves.NO_CONTROL], [Claves.NOMBRE_GRUPO] y [Claves.TIPO_ESTATUS] (el elegido de antemano).
 */
class CambiarEstatusActividad : PantallaBase() {

    private lateinit var enlace: ProfesorCambiarEstatusBinding
    private var alumno: AlumnoDoc? = null
    private var configuracion: ConfiguracionDoc? = null
    private var guardando = false
    private val noControl: String get() = intent.getStringExtra(Claves.NO_CONTROL).orEmpty()

    /** El tipo elegido en el RadioGroup (o null si no hay ninguno). */
    private val tipoElegido: String?
        get() = when (enlace.grupoTipoEstatus.checkedRadioButtonId) {
            enlace.chipFalta.id -> TipoEstatus.FALTA
            enlace.chipReporte.id -> TipoEstatus.REPORTE
            enlace.chipLlamada.id -> TipoEstatus.LLAMADA_ATENCION
            else -> null
        }

    override fun crearVista(): View {
        enlace = ProfesorCambiarEstatusBinding.inflate(layoutInflater)
        when (intent.getStringExtra(Claves.TIPO_ESTATUS)) {
            TipoEstatus.FALTA -> enlace.chipFalta.isChecked = true
            TipoEstatus.REPORTE -> enlace.chipReporte.isChecked = true
            TipoEstatus.LLAMADA_ATENCION -> enlace.chipLlamada.isChecked = true
        }
        enlace.botonGuardar.setOnClickListener { guardar() }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_cambiar_estatus)
        val nombreGrupo = intent.getStringExtra(Claves.NOMBRE_GRUPO).orEmpty()
        lifecycleScope.launch {
            alumno = cargar { CetisRepositorio.alumno(noControl) }.o(null)
            alumno?.let {
                enlace.textoIniciales.text = Formatos.iniciales(it)
                enlace.textoNombre.text = it.nombre.trim()
                enlace.textoDetalle.text = getString(R.string.formato_grupo_no_control, nombreGrupo, it.noControl)
            }
            // Qué estatus se le avisan al padre (configuracion/plantel). Sin configuración, no se avisa.
            configuracion = cargar { CetisRepositorio.configuracion() }.o(null)
        }
    }

    /** ¿Se le avisa al padre de este tipo de estatus? */
    private fun seAvisa(tipo: String): Boolean {
        val reglas = configuracion ?: return false
        return Avisos.destinatarios(alumno).isNotEmpty() && Avisos.alPadrePorEstatus(tipo, reglas)
    }

    private fun guardar() {
        if (guardando) return
        val motivo = enlace.campoMotivo.text.toString().trim()
        val tipo = tipoElegido
        val error = when {
            tipo == null -> R.string.error_estatus_tipo
            motivo.isEmpty() -> R.string.error_estatus_motivo
            else -> null
        }
        mostrarMensaje(error)
        if (tipo == null || error != null) return
        ponerGuardando(true)
        lifecycleScope.launch {
            val ahora = Timestamp.now()
            val nombreProfesor = sesion.usuario.nombre.trim()
            val nuevo = EstatusDoc(
                tipo = tipo,
                descripcion = motivo,
                fecha = ahora,
                vigenteHasta = null,
                asignadoPor = sesion.idUsuario,
                asignadoPorNombre = nombreProfesor,
            )
            val nombreTipo = getString(Estatus.nombre(tipo))
            val aviso = if (!seAvisa(tipo)) null else Avisos.nuevo(
                tipo = TipoAviso.ESTATUS,
                titulo = getString(R.string.aviso_estatus_titulo, nombreTipo),
                mensaje = getString(
                    R.string.aviso_estatus_mensaje,
                    nombreTipo.lowercase(), alumno?.nombre?.trim().orEmpty(), nombreProfesor, motivo,
                ),
                alumnoId = noControl,
                creadaPor = sesion.idUsuario,
                ahora = ahora,
            )
            val resultado = cargar { CetisRepositorio.agregarEstatus(noControl, nuevo, aviso, Avisos.destinatarios(alumno)) }
            ponerGuardando(false)
            if (resultado is Carga.Listo) {
                avisar(R.string.aviso_estatus_guardado)
                finish()
            } else {
                mostrarMensaje(R.string.error_guardar)
            }
        }
    }

    private fun ponerGuardando(si: Boolean) {
        guardando = si
        enlace.botonGuardar.isEnabled = !si
        enlace.botonGuardar.alpha = if (si) ALFA_APAGADO else 1f
        enlace.botonGuardar.setText(if (si) R.string.texto_guardando else R.string.boton_guardar_estatus)
    }

    private fun mostrarMensaje(@StringRes texto: Int?) {
        enlace.textoMensaje.isVisible = texto != null
        texto?.let { enlace.textoMensaje.setText(it) }
    }

    private companion object {
        const val ALFA_APAGADO = 0.5f
    }
}
