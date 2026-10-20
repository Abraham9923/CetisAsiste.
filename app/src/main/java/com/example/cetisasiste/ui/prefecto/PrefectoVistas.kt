package com.example.cetisasiste.ui.prefecto

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.Alerta
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.SalidaDeGrupoDoc
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.datos.Turno
import com.google.firebase.Timestamp
import com.example.cetisasiste.ui.comun.Renglon
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.mostrar
import com.example.cetisasiste.ui.comun.texto

/*
 * Coloca las VARIABLES dentro de las vistas XML del prefecto (res/layout/prefecto_*.xml).
 * Solo pone textos en su lugar. No consulta la base de datos y no navega.
 */
object PrefectoVistas {

    /**
     * prefecto_inicio.xml. [salidas] son los grupos que ya salieron hoy; null mientras carga.
     * [hoy] es la fecha como se lee ("Hoy · lunes 19 de octubre").
     */
    fun llenarInicio(vista: View, nombre: String, hoy: String, salidas: List<SalidaDeGrupoDoc>?) {
        vista.texto(R.id.txt_nombre, nombre)
        vista.texto(R.id.txt_fecha, hoy)
        VistasComunes.llenarLista(vista.findViewById(R.id.lista_salidas), salidas.orEmpty(), R.layout.comun_item_lista) { fila, salida ->
            VistasComunes.llenarRenglon(
                fila,
                Renglon(
                    icono = R.drawable.ic_flag,
                    titulo = vista.context.getString(R.string.prefecto_salida_titulo, salida.grupo),
                    // "11:40 am · Tecnologías de la información · Pedro Pozo"
                    detalle = listOf(Formatos.hora(salida.fechaHora), salida.materia, salida.autorizadaPorNombre)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                ),
            )
        }
        vista.mostrar(R.id.txt_vacio_salidas, salidas?.isEmpty() == true)
    }

    /**
     * prefecto_horarios.xml: el chip del [turno] elegido y un renglón por grupo
     * ([grupos] es null mientras carga). Al tocar un grupo se llama a [alTocarGrupo].
     */
    fun llenarHorarios(
        vista: View,
        turno: String,
        grupos: List<Pair<String, GrupoDoc>>?,
        alTocarGrupo: (String, GrupoDoc) -> Unit,
    ) {
        VistasComunes.marcarChip(vista.findViewById<TextView>(R.id.chip_matutino), turno == Turno.MATUTINO)
        VistasComunes.marcarChip(vista.findViewById<TextView>(R.id.chip_vespertino), turno == Turno.VESPERTINO)
        VistasComunes.llenarLista(vista.findViewById(R.id.lista_grupos), grupos.orEmpty(), R.layout.comun_item_lista) { fila, (id, grupo) ->
            VistasComunes.llenarRenglon(
                fila,
                Renglon(
                    icono = R.drawable.ic_users,
                    titulo = vista.context.getString(R.string.comun_grupo_titulo, grupo.nombre.trim()),
                    detalle = Formatos.detalleDeGrupo(grupo),
                ),
                alTocar = { alTocarGrupo(id, grupo) },
            )
        }
        vista.mostrar(R.id.txt_vacio, grupos?.isEmpty() == true)
    }

    // ---------- Escaneo ----------

    /** Marca Entrada o Salida en el selector (opcion_entrada / opcion_salida). */
    private fun marcarTipo(vista: View, tipo: String) {
        listOf(R.id.opcion_entrada to TipoRegistro.ENTRADA, R.id.opcion_salida to TipoRegistro.SALIDA).forEach { (id, t) ->
            val opcion = vista.findViewById<TextView>(id)
            val elegida = t == tipo
            opcion.isSelected = elegida
            opcion.setBackgroundResource(if (elegida) R.drawable.shape_chip_activo else 0)
            opcion.setTextColor(color(vista, if (elegida) R.color.cetis_text_on_brand else R.color.cetis_brand_900))
        }
    }

    private fun textoDeTipo(vista: View, tipo: String): String =
        vista.context.getString(if (tipo == TipoRegistro.ENTRADA) R.string.prefecto_entrada else R.string.prefecto_salida)

    /**
     * prefecto_escaner.xml. [mensaje] es "Buscando al alumno…" o un error (null = nada);
     * sin [permiso] de cámara se tapa la cámara con el aviso para pedirlo.
     */
    fun llenarEscaner(vista: View, tipo: String, @StringRes mensaje: Int?, permiso: Boolean) {
        marcarTipo(vista, tipo)
        vista.texto(R.id.txt_registrando, vista.context.getString(R.string.prefecto_registrando, textoDeTipo(vista, tipo)))
        vista.mostrar(R.id.txt_estado, mensaje != null)
        mensaje?.let { vista.findViewById<TextView>(R.id.txt_estado).setText(it) }
        vista.mostrar(R.id.aviso_permiso, !permiso)
    }

    /** prefecto_captura.xml. [mensaje] es un error que va en rojo debajo del número (null = la ayuda). */
    fun llenarCaptura(vista: View, tipo: String, tecleado: String, guardando: Boolean, @StringRes mensaje: Int?) {
        marcarTipo(vista, tipo)
        vista.texto(R.id.txt_tecleado, tecleado)
        val ayuda = vista.findViewById<TextView>(R.id.txt_ayuda)
        ayuda.setText(mensaje ?: R.string.prefecto_captura_ayuda)
        ayuda.setTextColor(color(vista, if (mensaje != null) R.color.cetis_status_danger else R.color.cetis_text_secondary))
        val boton = vista.findViewById<Button>(R.id.btn_registrar)
        boton.isEnabled = !guardando
        boton.alpha = if (guardando) 0.6f else 1f
        boton.setText(
            when {
                guardando -> R.string.prefecto_guardando
                tipo == TipoRegistro.ENTRADA -> R.string.prefecto_registrar_entrada
                else -> R.string.prefecto_registrar_salida
            },
        )
    }

    /** prefecto_resultado.xml: verde si no hubo alerta, rojo con el título de la alerta si la hubo. */
    fun llenarResultado(vista: View, resultado: ResultadoEscaneo) {
        val contexto = vista.context
        val ok = resultado.alerta == null
        val alumno = resultado.alumno
        vista.findViewById<View>(R.id.tarjeta_resultado)
            .setBackgroundResource(if (ok) R.drawable.shape_resultado_exito else R.drawable.shape_resultado_alerta)
        vista.findViewById<View>(R.id.circulo_resultado)
            .setBackgroundResource(if (ok) R.drawable.shape_circulo_exito else R.drawable.shape_circulo_peligro)
        vista.findViewById<ImageView>(R.id.img_resultado).setImageResource(if (ok) R.drawable.ic_check_circle else R.drawable.ic_x_circle)

        val titulo = vista.findViewById<TextView>(R.id.txt_resultado)
        titulo.setText(
            when {
                !ok -> tituloDeAlerta(resultado.alerta)
                resultado.tipo == TipoRegistro.ENTRADA -> R.string.prefecto_entrada_registrada
                else -> R.string.prefecto_salida_registrada
            },
        )
        titulo.setTextColor(color(vista, if (ok) R.color.cetis_status_success else R.color.cetis_status_danger))
        vista.texto(R.id.txt_cuando, contexto.getString(R.string.prefecto_resultado_cuando, Formatos.hora(resultado.fechaHora), textoDeTipo(vista, resultado.tipo)))

        vista.texto(R.id.txt_iniciales, if (alumno != null) Formatos.iniciales(alumno) else "?")
        vista.texto(R.id.txt_nombre, alumno?.nombre?.trim() ?: contexto.getString(R.string.prefecto_resultado_sin_alumno))
        vista.texto(R.id.txt_detalle, if (alumno != null) contexto.getString(R.string.prefecto_resultado_grupo, resultado.nombreGrupo) else "")
        vista.mostrar(R.id.txt_detalle, alumno != null)
        vista.texto(R.id.txt_no_control, resultado.noControl)

        val etiqueta = etiquetaDeAlerta(contexto, resultado)
        vista.mostrar(R.id.chip_alerta, etiqueta != null)
        etiqueta?.let { vista.texto(R.id.txt_alerta, it) }

        val aviso = vista.findViewById<TextView>(R.id.txt_aviso)
        aviso.setBackgroundResource(if (ok) R.drawable.shape_aviso_exito else R.drawable.shape_aviso_peligro)
        aviso.setText(
            when {
                !ok -> R.string.prefecto_alerta_guardada
                !resultado.seAvisoAlPadre -> R.string.prefecto_sin_padre
                resultado.tipo == TipoRegistro.ENTRADA -> R.string.prefecto_aviso_padre_entrada
                else -> R.string.prefecto_aviso_padre_salida
            },
        )
    }

    @StringRes
    private fun tituloDeAlerta(alerta: String?): Int = when (alerta) {
        Alerta.NO_DADO_DE_ALTA -> R.string.prefecto_alerta_no_dado_de_alta
        Alerta.INACTIVO -> R.string.prefecto_alerta_inactivo
        Alerta.YA_INGRESO -> R.string.prefecto_alerta_ya_ingreso
        Alerta.NO_HA_INGRESADO -> R.string.prefecto_alerta_no_ha_ingresado
        Alerta.CASTIGO -> R.string.prefecto_alerta_castigo
        else -> R.string.prefecto_alerta_salida_fuera
    }

    /** Etiqueta roja junto al alumno; null si no hay alerta o si no hay alumno. */
    private fun etiquetaDeAlerta(contexto: Context, resultado: ResultadoEscaneo): String? {
        val alumno = resultado.alumno ?: return null
        val id = when (resultado.alerta) {
            Alerta.INACTIVO -> if (!alumno.activoPorPago) R.string.estado_inactivo_pago else R.string.estado_inactivo_salud
            Alerta.YA_INGRESO -> R.string.prefecto_etiqueta_ya_ingreso
            Alerta.NO_HA_INGRESADO -> R.string.prefecto_etiqueta_no_ha_ingresado
            Alerta.CASTIGO -> R.string.prefecto_etiqueta_castigo
            Alerta.SALIDA_FUERA_DE_HORARIO -> R.string.prefecto_etiqueta_salida_fuera
            else -> return null
        }
        return contexto.getString(id)
    }

    private fun color(vista: View, @ColorRes id: Int): Int = ContextCompat.getColor(vista.context, id)
}

/** Lo que muestra prefecto_resultado.xml después de guardar un escaneo. */
data class ResultadoEscaneo(
    val noControl: String,
    /** null si no hay ningún alumno con ese No. de control. */
    val alumno: AlumnoDoc?,
    val nombreGrupo: String,
    val tipo: String,
    /** null = sin alerta. */
    val alerta: String?,
    val fechaHora: Timestamp,
    val seAvisoAlPadre: Boolean,
)
