package com.example.cetisasiste.pantallas.prefecto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.StringRes
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.Alerta
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Escaneo
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.RegistroDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoAviso
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.Renglon
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException

/*
 * ESCANEO DEL PREFECTO: registra una entrada o salida (con QR o tecleando el No. de control) y
 * abre el resultado. Las reglas de las alertas están en datos/Escaneo.kt; guardar, en
 * CetisRepositorio.registrarEscaneo. Por qué así: docs/adr/ADR-009.
 */
object RegistroDeEscaneo {

    // La cámara sigue viendo el mismo QR después de registrarlo: el mismo No. de control no se
    // vuelve a registrar hasta que pasen ESPERA_MS.
    private var ultimo = ""
    private var ultimoEn = 0L
    private const val ESPERA_MS = 20_000L

    fun yaSeRegistroHaceRato(noControl: String): Boolean =
        noControl == ultimo && SystemClock.elapsedRealtime() - ultimoEn < ESPERA_MS

    /**
     * Busca al alumno, decide la alerta y guarda el registro, con el aviso a sus padres (si no
     * hubo alerta, o si la alerta es de las que el plantel pide avisar: Avisos.alPadrePorEscaneo).
     * Si se guardó, abre ResultadoActividad y devuelve true; si no, devuelve false.
     */
    suspend fun registrar(actividad: Activity, sesion: Sesion, noControl: String, tipo: String, medio: String): Boolean {
        ultimo = noControl
        ultimoEn = SystemClock.elapsedRealtime()
        return try {
            val datos = CetisRepositorio.datosDelEscaneo(noControl, tipo)
            val configuracion = CetisRepositorio.configuracion()
            val alumno = datos.alumno
            val alerta = Escaneo.alerta(datos)
            val ahora = Timestamp.now()
            val nombre = alumno?.nombre?.trim().orEmpty()
            val entrada = tipo == TipoRegistro.ENTRADA
            val registro = RegistroDoc(
                alumnoId = noControl,
                alumnoNombre = nombre,
                grupoId = alumno?.grupoId.orEmpty(),
                tipo = tipo,
                fechaHora = ahora,
                fecha = Formatos.fechaDeHoy(),
                medio = medio,
                registradoPor = sesion.idUsuario,
                registradoPorNombre = sesion.usuario.nombre.trim(),
                alerta = alerta,
            )
            // Al padre se le avisa de las entradas y salidas sin alerta y de las alertas que pida el plantel.
            val avisar = alumno != null && Avisos.destinatarios(alumno).isNotEmpty() &&
                Avisos.alPadrePorEscaneo(alerta, configuracion)
            val aviso = when {
                !avisar -> null
                alerta == null -> Avisos.nuevo(
                    tipo = tipo,
                    titulo = actividad.getString(if (entrada) R.string.resultado_entrada_registrada else R.string.resultado_salida_registrada),
                    mensaje = actividad.getString(
                        if (entrada) R.string.aviso_padre_entrada_mensaje else R.string.aviso_padre_salida_mensaje,
                        nombre, Formatos.hora(ahora),
                    ),
                    alumnoId = noControl,
                    creadaPor = sesion.idUsuario,
                    ahora = ahora,
                )
                else -> Avisos.nuevo(
                    tipo = TipoAviso.ALERTA,
                    titulo = actividad.getString(tituloDeAlerta(alerta)),
                    mensaje = actividad.getString(
                        if (entrada) R.string.aviso_padre_alerta_entrada else R.string.aviso_padre_alerta_salida,
                        nombre, Formatos.hora(ahora), actividad.getString(tituloDeAlerta(alerta)),
                    ),
                    alumnoId = noControl,
                    creadaPor = sesion.idUsuario,
                    ahora = ahora,
                )
            }
            CetisRepositorio.registrarEscaneo(registro, alumno, aviso)
            abrirResultado(actividad, noControl, alumno, tipo, alerta, ahora, avisar)
            true
        } catch (excepcion: Exception) {
            if (excepcion is CancellationException) throw excepcion
            ultimo = "" // si falló, se puede volver a intentar con el mismo QR
            false
        }
    }

    /** Abre el resultado con los textos ya armados (viajan como extras del Intent). */
    private fun abrirResultado(
        actividad: Activity,
        noControl: String,
        alumno: AlumnoDoc?,
        tipo: String,
        alerta: String?,
        ahora: Timestamp,
        seAvisoAlPadre: Boolean,
    ) {
        val grupo = alumno?.grupo?.trim().orEmpty()
        val intent = Intent(actividad, ResultadoActividad::class.java)
            .putExtra(Claves.TIPO_REGISTRO, tipo)
            .putExtra(Claves.RESULTADO_ALERTA, alerta)
            .putExtra(Claves.RESULTADO_HORA, ahora.toDate().time)
            .putExtra(Claves.RESULTADO_NOMBRE, alumno?.nombre?.trim() ?: actividad.getString(R.string.resultado_sin_alumno))
            .putExtra(Claves.RESULTADO_INICIALES, if (alumno != null) Formatos.iniciales(alumno) else actividad.getString(R.string.iniciales_desconocidas))
            .putExtra(
                Claves.RESULTADO_DETALLE,
                if (alumno != null) actividad.getString(R.string.formato_grupo_no_control, grupo, noControl) else noControl,
            )
            .putExtra(Claves.RESULTADO_ETIQUETA, etiquetaDeAlerta(actividad, alumno, alerta))
            .putExtra(Claves.RESULTADO_AVISO, actividad.getString(textoDelAviso(alerta, seAvisoAlPadre, tipo)))
        actividad.startActivity(intent)
    }

    @StringRes
    private fun textoDelAviso(alerta: String?, seAvisoAlPadre: Boolean, tipo: String): Int = when {
        alerta != null && seAvisoAlPadre -> R.string.resultado_alerta_guardada_y_aviso
        alerta != null -> R.string.resultado_alerta_guardada
        !seAvisoAlPadre -> R.string.resultado_sin_padre
        tipo == TipoRegistro.ENTRADA -> R.string.resultado_aviso_padre_entrada
        else -> R.string.resultado_aviso_padre_salida
    }

    /** Título de cada alerta ("Alumno inactivo"…). También va en el aviso al padre y en las listas. */
    @StringRes
    fun tituloDeAlerta(alerta: String?): Int = when (alerta) {
        Alerta.NO_DADO_DE_ALTA -> R.string.alerta_no_dado_de_alta
        Alerta.INACTIVO -> R.string.alerta_inactivo
        Alerta.YA_INGRESO -> R.string.alerta_ya_ingreso
        Alerta.NO_HA_INGRESADO -> R.string.alerta_no_ha_ingresado
        Alerta.CASTIGO -> R.string.alerta_castigo
        else -> R.string.alerta_salida_fuera
    }

    /** Etiqueta roja junto al alumno (StatusChip); null si no hay alerta o no existe el alumno. */
    private fun etiquetaDeAlerta(contexto: Context, alumno: AlumnoDoc?, alerta: String?): String? {
        if (alumno == null) return null
        val id = when (alerta) {
            Alerta.INACTIVO -> if (!alumno.activoPorPago) R.string.estado_inactivo_pago else R.string.estado_inactivo_salud
            Alerta.YA_INGRESO -> R.string.etiqueta_ya_ingreso
            Alerta.NO_HA_INGRESADO -> R.string.etiqueta_no_ha_ingresado
            Alerta.CASTIGO -> R.string.etiqueta_castigo
            Alerta.SALIDA_FUERA_DE_HORARIO -> R.string.etiqueta_salida_fuera
            else -> return null
        }
        return contexto.getString(id)
    }

    /**
     * Un escaneo como renglón de lista (Figma "Últimos escaneos" 78:567):
     *   sin alerta: "Juan Pérez ingresó a las 7:05 am" · "Grupo 102 · Entrada";
     *   con alerta: "Carlos Ruiz: castigo disciplinario" · "Grupo 204 · 7:12 am", en rojo.
     */
    fun renglon(contexto: Context, registro: RegistroDoc): Renglon {
        val entrada = registro.tipo == TipoRegistro.ENTRADA
        val alerta = registro.alerta
        val nombre = registro.alumnoNombre.trim().ifBlank { contexto.getString(R.string.formato_escaneo_sin_alumno, registro.alumnoId) }
        val hora = Formatos.hora(registro.fechaHora)
        val grupo = Formatos.nombreDeGrupo(registro.grupoId).takeIf { it.isNotBlank() }
            ?.let { contexto.getString(R.string.formato_grupo, it) }
        return if (alerta != null) {
            Renglon(
                icono = R.drawable.ic_x_circle,
                titulo = contexto.getString(R.string.formato_escaneo_alerta, nombre, contexto.getString(tituloDeAlerta(alerta)).lowercase()),
                detalle = Formatos.unir(grupo, hora),
                fondoIcono = R.drawable.shape_circulo_peligro,
                fondo = R.drawable.shape_aviso_peligro,
            )
        } else {
            Renglon(
                icono = if (entrada) R.drawable.ic_log_in else R.drawable.ic_log_out,
                titulo = contexto.getString(if (entrada) R.string.formato_ingreso_a_las else R.string.formato_salio_a_las, nombre, hora),
                detalle = Formatos.unir(grupo, contexto.getString(if (entrada) R.string.tipo_entrada else R.string.tipo_salida)),
                fondoIcono = if (entrada) R.drawable.shape_circulo_brand else R.drawable.shape_circulo_accent,
            )
        }
    }
}
