package com.example.cetisasiste.ui.prefecto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Escaneo
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Medio
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.NotificacionDoc
import com.example.cetisasiste.datos.RegistroDoc
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.ui.comun.PantallaXml
import com.example.cetisasiste.ui.comun.alTocar
import com.google.firebase.Timestamp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/*
 * ESCANEO DEL PREFECTO: escáner con la cámara, captura por No. de control y resultado
 * (prefecto_escaner.xml, prefecto_captura.xml, prefecto_resultado.xml). PrefectoApp.kt solo
 * los abre. Las reglas de las alertas están en datos/Escaneo.kt; guardar, en
 * CetisRepositorio.registrarEscaneo. Por qué así: docs/adr/ADR-009.
 */

/**
 * Lo que el escaneo recuerda mientras el prefecto pasa del escáner a la captura y al
 * resultado: Entrada o Salida, lo tecleado, el último resultado y si se está guardando.
 */
class EstadoDelEscaneo {
    var tipo by mutableStateOf(TipoRegistro.ENTRADA)
    var tecleado by mutableStateOf("")
    var guardando by mutableStateOf(false)
        private set
    /** "Buscando al alumno…" o un error; null = nada. */
    @get:StringRes
    var mensaje by mutableStateOf<Int?>(null)
    var resultado by mutableStateOf<ResultadoEscaneo?>(null)
        private set

    // La cámara sigue viendo el mismo QR después de registrarlo: el mismo No. de control no
    // se vuelve a registrar hasta que pasen ESPERA_MS.
    private var ultimo = ""
    private var ultimoEn = 0L

    fun yaSeRegistroHaceRato(noControl: String): Boolean =
        noControl == ultimo && SystemClock.elapsedRealtime() - ultimoEn < ESPERA_MS

    fun teclear(digito: Char) {
        mensaje = null
        if (tecleado.length < NoControl.DIGITOS) tecleado += digito
    }

    fun borrar() {
        mensaje = null
        tecleado = tecleado.dropLast(1)
    }

    fun limpiar() {
        mensaje = null
        tecleado = ""
    }

    /**
     * Busca al alumno, decide la alerta y guarda el registro (y el aviso a sus padres si no hubo
     * alerta). Devuelve true si se guardó; si no, deja un mensaje de error.
     */
    suspend fun registrar(contexto: Context, sesion: Sesion, noControl: String, medio: String): Boolean {
        guardando = true
        mensaje = if (medio == Medio.QR) R.string.prefecto_buscando else null
        ultimo = noControl
        ultimoEn = SystemClock.elapsedRealtime()
        return try {
            val datos = CetisRepositorio.datosDelEscaneo(noControl, tipo)
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
                registradoPor = sesion.uid,
                registradoPorNombre = sesion.usuario.nombre.trim(),
                alerta = alerta,
            )
            // Al padre solo se le avisa de las entradas y salidas sin alerta.
            val avisar = alerta == null && alumno != null && alumno.padres.any { it.isNotBlank() }
            val aviso = if (!avisar) null else NotificacionDoc(
                tipo = tipo,
                titulo = contexto.getString(if (entrada) R.string.prefecto_entrada_registrada else R.string.prefecto_salida_registrada),
                mensaje = contexto.getString(
                    if (entrada) R.string.prefecto_aviso_entrada_mensaje else R.string.prefecto_aviso_salida_mensaje,
                    nombre, Formatos.hora(ahora),
                ),
                fecha = ahora,
                leida = false,
                alumnoId = noControl,
            )
            CetisRepositorio.registrarEscaneo(registro, alumno, aviso)
            resultado = ResultadoEscaneo(noControl, alumno, alumno?.grupo?.trim().orEmpty(), tipo, alerta, ahora, avisar)
            mensaje = null
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mensaje = R.string.comun_error_guardar
            ultimo = ""  // si falló, se puede volver a intentar con el mismo QR
            false
        } finally {
            guardando = false
        }
    }

    private companion object {
        const val ESPERA_MS = 20_000L
    }
}

/**
 * Escáner (pantalla del menú ☰). Pide el permiso de la cámara al abrirse; al leer un QR de
 * CetisAsiste registra la entrada o salida y llama a [onResultado].
 */
@Composable
fun EscanerXml(
    estado: EstadoDelEscaneo,
    sesion: Sesion,
    conectarMenu: (View) -> Unit,
    onCaptura: () -> Unit,
    onResultado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val alTerminar by rememberUpdatedState(onResultado)
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    LaunchedEffect(Unit) { if (!permiso) pedirPermiso.launch(Manifest.permission.CAMERA) }

    // La cámara se enciende al mostrarse y se apaga al salir de la pantalla o de la app.
    val camara = remember { Camara() }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo, permiso) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> if (permiso) camara.encender()
                Lifecycle.Event.ON_PAUSE -> camara.apagar()
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            camara.apagar()
        }
    }

    fun alLeer(texto: String) {
        if (estado.guardando) return
        val noControl = NoControl.desdeQr(texto)
        if (noControl == null) {
            estado.mensaje = R.string.prefecto_qr_ajeno
            return
        }
        if (estado.yaSeRegistroHaceRato(noControl)) return
        scope.launch { if (estado.registrar(contexto, sesion, noControl, Medio.QR)) alTerminar() }
    }

    PantallaXml(R.layout.prefecto_escaner, modifier) { vista ->
        PrefectoVistas.llenarEscaner(vista, estado.tipo, estado.mensaje, permiso)
        vista.alTocar(R.id.opcion_entrada) { estado.tipo = TipoRegistro.ENTRADA }
        vista.alTocar(R.id.opcion_salida) { estado.tipo = TipoRegistro.SALIDA }
        vista.alTocar(R.id.btn_permiso) { pedirPermiso.launch(Manifest.permission.CAMERA) }
        vista.alTocar(R.id.btn_escribir) {
            estado.mensaje = null
            onCaptura()
        }
        camara.conectar(vista.findViewById(R.id.camara)) { alLeer(it) }
        if (permiso) camara.encender()
        conectarMenu(vista)
    }
}

/** La vista de la cámara (BarcodeView) y si está encendida, para no encenderla dos veces. */
private class Camara {
    private var vista: BarcodeView? = null
    private var encendida = false

    /** Se llama cada vez que se llena la pantalla; solo configura una vista nueva. */
    fun conectar(lector: BarcodeView, alLeer: (String) -> Unit) {
        if (vista === lector) return
        apagar()
        vista = lector
        lector.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        lector.decodeContinuous { lectura -> lectura.text?.let(alLeer) }
    }

    fun encender() {
        if (encendida) return
        vista?.resume() ?: return
        encendida = true
    }

    fun apagar() {
        if (!encendida) return
        vista?.pause()
        encendida = false
    }
}

/** Captura por No. de control: teclado de 14 dígitos y "Registrar". */
@Composable
fun CapturaXml(
    estado: EstadoDelEscaneo,
    sesion: Sesion,
    onRegresar: () -> Unit,
    onResultado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    PantallaXml(R.layout.prefecto_captura, modifier) { vista ->
        PrefectoVistas.llenarCaptura(vista, estado.tipo, estado.tecleado, estado.guardando, estado.mensaje)
        vista.alTocar(R.id.opcion_entrada) { estado.tipo = TipoRegistro.ENTRADA }
        vista.alTocar(R.id.opcion_salida) { estado.tipo = TipoRegistro.SALIDA }
        TECLAS.forEach { (id, digito) -> vista.alTocar(id) { estado.teclear(digito) } }
        vista.alTocar(R.id.tecla_borrar) { estado.borrar() }
        vista.alTocar(R.id.tecla_limpiar) { estado.limpiar() }
        vista.alTocar(R.id.btn_regresar) {
            estado.mensaje = null
            onRegresar()
        }
        vista.alTocar(R.id.btn_registrar) {
            val noControl = estado.tecleado
            when {
                estado.guardando -> Unit
                !NoControl.esValido(noControl) -> estado.mensaje = R.string.login_error_no_control
                else -> scope.launch {
                    if (estado.registrar(contexto, sesion, noControl, Medio.NO_CONTROL)) {
                        estado.limpiar()
                        onResultado()
                    }
                }
            }
        }
    }
}

/** Resultado del último escaneo. "Escanear siguiente" y la flecha regresan a donde se escaneó. */
@Composable
fun ResultadoXml(
    estado: EstadoDelEscaneo,
    onSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultado = estado.resultado
    if (resultado == null) {
        // Pasa si Android cerró la app en segundo plano: no hay nada que mostrar.
        LaunchedEffect(Unit) { onSiguiente() }
        return
    }
    PantallaXml(R.layout.prefecto_resultado, modifier) { vista ->
        PrefectoVistas.llenarResultado(vista, resultado)
        vista.alTocar(R.id.btn_siguiente, onSiguiente)
        vista.alTocar(R.id.btn_regresar, onSiguiente)
    }
}

private val TECLAS = listOf(
    R.id.tecla_0 to '0', R.id.tecla_1 to '1', R.id.tecla_2 to '2', R.id.tecla_3 to '3', R.id.tecla_4 to '4',
    R.id.tecla_5 to '5', R.id.tecla_6 to '6', R.id.tecla_7 to '7', R.id.tecla_8 to '8', R.id.tecla_9 to '9',
)
