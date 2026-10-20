package com.example.cetisasiste.ui.profesor

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.ClaseDoc
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.GrupoDoc
import com.example.cetisasiste.datos.SalidaDeGrupoDoc
import com.example.cetisasiste.datos.TipoEstatus
import com.example.cetisasiste.ui.comun.AlumnoDeLista
import com.example.cetisasiste.ui.comun.EstadoVisible
import com.example.cetisasiste.ui.comun.Renglon
import com.example.cetisasiste.ui.comun.VistasComunes
import com.example.cetisasiste.ui.comun.mostrar
import com.example.cetisasiste.ui.comun.texto

/*
 * Coloca las VARIABLES dentro de las vistas XML del profesor (res/layout/profesor_*.xml).
 * Solo pone textos en su lugar. No consulta la base de datos y no navega.
 */
object ProfesorVistas {

    /** Los cuatro estatus que puede poner el profesor, con su chip en profesor_cambiar_estatus.xml. */
    val CHIPS_DE_ESTATUS = listOf(
        R.id.chip_falta to TipoEstatus.FALTA,
        R.id.chip_reporte to TipoEstatus.REPORTE,
        R.id.chip_llamada_atencion to TipoEstatus.LLAMADA_ATENCION,
        R.id.chip_castigo to TipoEstatus.CASTIGO,
    )

    /** profesor_inicio.xml. [grupos] es null mientras carga. */
    fun llenarInicio(
        vista: View,
        nombre: String,
        numeroDeGrupos: Int,
        grupos: List<Pair<String, GrupoDoc>>?,
        alTocarGrupo: (String, GrupoDoc) -> Unit,
    ) {
        vista.texto(R.id.txt_nombre, nombre)
        vista.texto(
            R.id.txt_grupos,
            vista.resources.getQuantityString(R.plurals.profesor_numero_grupos, numeroDeGrupos, numeroDeGrupos),
        )
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

    /**
     * profesor_grupo.xml: título, aviso si ya salió hoy y la lista de alumnos
     * ([alumnos] es null mientras carga). Al tocar un alumno se llama a [alTocarAlumno].
     */
    fun llenarGrupo(
        vista: View,
        nombreGrupo: String,
        salidaDeHoy: SalidaDeGrupoDoc?,
        alumnos: List<AlumnoDoc>?,
        alTocarAlumno: (String) -> Unit,
    ) {
        vista.texto(R.id.txt_titulo, vista.context.getString(R.string.comun_grupo_titulo, nombreGrupo))
        llenarAvisoDeSalida(vista, salidaDeHoy)
        vista.texto(R.id.txt_seccion_alumnos, vista.context.getString(R.string.profesor_seccion_alumnos, alumnos?.size ?: 0))
        val lista = alumnos.orEmpty().map(::alumnoDeLista)
        VistasComunes.llenarLista(vista.findViewById(R.id.lista_alumnos), lista, R.layout.profesor_item_alumno) { fila, alumno ->
            fila.texto(R.id.txt_iniciales, alumno.iniciales)
            fila.texto(R.id.txt_nombre, alumno.nombre)
            fila.texto(R.id.txt_detalle, alumno.noControl)
            VistasComunes.llenarEstado(fila, alumno.estado)
            fila.setOnClickListener { alTocarAlumno(alumno.noControl) }
        }
        vista.mostrar(R.id.txt_vacio, alumnos?.isEmpty() == true)
    }

    /**
     * profesor_alumno.xml. [alumno] y [estatus] son null mientras cargan.
     */
    fun llenarAlumno(vista: View, alumno: AlumnoDoc?, nombreGrupo: String, estatus: List<EstatusDoc>?) {
        if (alumno != null) {
            vista.texto(R.id.txt_nombre, alumno.nombre.trim())
            vista.texto(R.id.txt_rol_grupo, vista.context.getString(R.string.role_and_group, Formatos.ROL_ALUMNO, nombreGrupo))
            vista.texto(R.id.txt_no_control, alumno.noControl)
            vista.texto(R.id.txt_turno, Formatos.turno(alumno.turno))
            vista.texto(R.id.txt_correo, alumno.correo)
            VistasComunes.llenarEstado(vista, estadoVisible(alumno))
        }
        vista.mostrar(R.id.chip_estado, alumno != null)
        VistasComunes.llenarLista(vista.findViewById(R.id.lista_estatus), estatus.orEmpty(), R.layout.comun_item_lista) { fila, uno ->
            VistasComunes.llenarRenglon(
                fila,
                Renglon(
                    icono = R.drawable.ic_flag,
                    titulo = vista.context.getString(nombreDeEstatus(uno.tipo)),
                    // "No entregó la práctica · Lun 5 oct · Pedro Pozo"
                    detalle = listOf(uno.descripcion, Formatos.fechaCorta(uno.fecha), uno.asignadoPorNombre)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                ),
            )
        }
        vista.mostrar(R.id.txt_vacio, estatus?.isEmpty() == true)
    }

    /**
     * profesor_cambiar_estatus.xml: a quién, qué tipo está elegido, si se está guardando y el
     * mensaje de error (si hay). La descripción la escribe el profesor y no se toca aquí.
     */
    fun llenarCambiarEstatus(
        vista: View,
        alumno: AlumnoDoc?,
        nombreGrupo: String,
        tipoElegido: String?,
        guardando: Boolean,
        @StringRes mensaje: Int?,
    ) {
        vista.texto(R.id.txt_nombre, alumno?.nombre?.trim().orEmpty())
        vista.texto(
            R.id.txt_detalle,
            listOf(alumno?.noControl.orEmpty(), vista.context.getString(R.string.comun_grupo_titulo, nombreGrupo))
                .filter { it.isNotBlank() }.joinToString(" · "),
        )
        CHIPS_DE_ESTATUS.forEach { (id, tipo) ->
            VistasComunes.marcarChip(vista.findViewById<TextView>(id), tipo == tipoElegido)
        }
        llenarMensaje(vista, mensaje)
        llenarBoton(vista, R.id.btn_guardar, habilitado = !guardando)
    }

    /**
     * profesor_dar_salida.xml. [ultimaClase] es la última clase de hoy (null si hoy no hay
     * clases); [salidaDeHoy], la salida ya registrada hoy (si existe, el botón se apaga).
     * [revisando] es true mientras se revisa o se guarda: el botón espera apagado.
     */
    fun llenarDarSalida(
        vista: View,
        nombreGrupo: String,
        hoy: String,
        ultimaClase: ClaseDoc?,
        salidaDeHoy: SalidaDeGrupoDoc?,
        revisando: Boolean,
        @StringRes mensaje: Int?,
    ) {
        val contexto = vista.context
        vista.texto(R.id.txt_grupo, nombreGrupo)
        vista.texto(R.id.txt_fecha, hoy)
        vista.texto(R.id.txt_materia, ultimaClase?.materia ?: contexto.getString(R.string.profesor_salida_sin_clases))
        vista.texto(R.id.txt_horario, ultimaClase?.let { Formatos.rangoDeHoras(it.horaInicio, it.horaFin) }.orEmpty())
        vista.mostrar(R.id.txt_horario, ultimaClase != null)
        vista.texto(R.id.txt_explicacion, contexto.getString(R.string.profesor_salida_explicacion, nombreGrupo))
        llenarAvisoDeSalida(vista, salidaDeHoy)
        llenarMensaje(vista, mensaje)
        llenarBoton(vista, R.id.btn_dar_salida, habilitado = !revisando && salidaDeHoy == null)
    }

    // ---------- Ayudantes ----------

    /** AlumnoDoc (Firestore) → AlumnoDeLista (renglón de la lista del grupo). */
    private fun alumnoDeLista(doc: AlumnoDoc) = AlumnoDeLista(
        noControl = doc.noControl,
        iniciales = Formatos.iniciales(doc),
        nombre = doc.nombre.trim(),
        estado = estadoVisible(doc),
    )

    /** El estado que se ve en la etiqueta de color: primero el pago, luego la salud, luego prácticas. */
    private fun estadoVisible(doc: AlumnoDoc): EstadoVisible = when {
        !doc.activoPorPago -> EstadoVisible.InactivoPorPago
        !doc.activoPorSalud -> EstadoVisible.InactivoPorSalud
        doc.enPracticas -> EstadoVisible.EnPracticas
        else -> EstadoVisible.Activo
    }

    @StringRes
    private fun nombreDeEstatus(tipo: String): Int = when (tipo) {
        TipoEstatus.FALTA -> R.string.estatus_falta
        TipoEstatus.REPORTE -> R.string.estatus_reporte
        TipoEstatus.LLAMADA_ATENCION -> R.string.estatus_llamada_atencion
        TipoEstatus.CASTIGO -> R.string.estatus_castigo
        TipoEstatus.PASE_SALUD -> R.string.estatus_pase_salud
        else -> R.string.estatus_alta_practicas
    }

    /** Recuadro verde "El grupo salió hoy a las 11:40 am (autorizó …)". */
    private fun llenarAvisoDeSalida(vista: View, salida: SalidaDeGrupoDoc?) {
        vista.mostrar(R.id.aviso_salida, salida != null)
        if (salida != null) {
            vista.texto(
                R.id.txt_aviso_salida,
                vista.context.getString(R.string.profesor_salida_hecha, Formatos.hora(salida.fechaHora), salida.autorizadaPorNombre),
            )
        }
    }

    private fun llenarMensaje(vista: View, @StringRes mensaje: Int?) {
        vista.mostrar(R.id.txt_mensaje, mensaje != null)
        if (mensaje != null) vista.findViewById<TextView>(R.id.txt_mensaje).setText(mensaje)
    }

    /** Un botón apagado se ve más claro y no responde. */
    private fun llenarBoton(vista: View, id: Int, habilitado: Boolean) {
        val boton = vista.findViewById<Button>(id)
        boton.isEnabled = habilitado
        boton.alpha = if (habilitado) 1f else 0.5f
    }
}
