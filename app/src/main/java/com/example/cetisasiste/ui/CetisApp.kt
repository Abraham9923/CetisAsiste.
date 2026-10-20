package com.example.cetisasiste.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.MensajeLogin
import com.example.cetisasiste.datos.ResultadoLogin
import com.example.cetisasiste.datos.RolDoc
import com.example.cetisasiste.datos.Rol
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.datos.UsuarioDoc
import com.example.cetisasiste.ui.alumno.AlumnoApp
import com.example.cetisasiste.ui.comun.CambiarContrasenaXml
import com.example.cetisasiste.ui.padres.PadresApp
import com.example.cetisasiste.ui.padres.components.PrimaryButton
import com.example.cetisasiste.ui.padres.screens.BienvenidaScreen
import com.example.cetisasiste.ui.padres.screens.IniciarSesionScreen
import com.example.cetisasiste.ui.prefecto.PrefectoApp
import com.example.cetisasiste.ui.profesor.ProfesorApp
import com.example.cetisasiste.ui.theme.Brand900
import com.example.cetisasiste.ui.theme.SurfaceBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

private const val BIENVENIDA_DURATION_MS = 1500L

/**
 * Punto de entrada de toda la app.
 *
 *   Bienvenida → (¿ya había sesión?) → Iniciar sesión → pantallas según el rol.
 *
 * El rol sale del campo `rol` de usuarios/{uid} en Firestore. Nadie se registra
 * desde la app: las cuentas las crea Control Escolar.
 */
@Composable
fun CetisApp(modifier: Modifier = Modifier) {
    // false mientras se muestra Bienvenida y se revisa si ya había una sesión abierta.
    var revisado by remember { mutableStateOf(false) }
    var sesion by remember { mutableStateOf<Sesion?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<MensajeLogin?>(null) }
    // true en cuanto la persona cambia su contraseña obligatoria (aunque Firestore tarde en avisar).
    var contrasenaCambiada by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun cerrarSesion() {
        CetisRepositorio.cerrarSesion()
        sesion = null
        error = null
        contrasenaCambiada = false
    }

    if (!revisado) {
        LaunchedEffect(Unit) {
            // La bienvenida dura al menos 1.5 s; mientras, se revisa si ya había sesión.
            val inicio = System.currentTimeMillis()
            sesion = CetisRepositorio.sesionGuardada()
            delay((BIENVENIDA_DURATION_MS - (System.currentTimeMillis() - inicio)).coerceAtLeast(0))
            revisado = true
        }
        BienvenidaScreen(modifier = modifier)
        return
    }

    val actual = sesion
    if (actual == null) {
        IniciarSesionScreen(
            modifier = modifier,
            cargando = cargando,
            error = error?.let { textoDeError(it) },
            onLoginClick = { usuario, contrasena ->
                cargando = true
                error = null
                scope.launch {
                    when (val resultado = CetisRepositorio.iniciarSesion(usuario, contrasena)) {
                        is ResultadoLogin.Correcto -> sesion = resultado.sesion
                        is ResultadoLogin.Error -> error = resultado.mensaje
                    }
                    cargando = false
                }
            }
        )
        return
    }

    // La cuenta se escucha en tiempo real: si Control Escolar cambia el nombre, los hijos o
    // los grupos, la app lo ve al instante; si la desactiva o la borra, se cierra la sesión.
    LaunchedEffect(actual.uid) {
        CetisRepositorio.escucharUsuario(actual.uid).collect { resultado ->
            val usuario = resultado.getOrNull()
            when {
                resultado.isFailure -> Unit // Sin conexión o sin permiso: se queda lo último que se leyó.
                usuario == null -> {
                    cerrarSesion(); error = MensajeLogin.CuentaNoRegistrada
                }
                !usuario.activo -> {
                    cerrarSesion(); error = MensajeLogin.CuentaDesactivada
                }
                else -> sesion = actual.copy(usuario = usuario)
            }
        }
    }

    // Si Control Escolar marcó debeCambiarContrasena o la contraseña ya venció, primero se cambia.
    if (!contrasenaCambiada && debeCambiarContrasena(actual.usuario)) {
        CambiarContrasenaXml(
            obligatoria = true,
            onRegresar = {},
            onCerrarSesion = ::cerrarSesion,
            onLista = { contrasenaCambiada = true },
            modifier = modifier,
        )
        return
    }

    when (actual.usuario.rol) {
        Rol.PADRE -> PadresApp(sesion = actual, onCerrarSesion = ::cerrarSesion, modifier = modifier)
        Rol.ALUMNO -> AlumnoApp(sesion = actual, onCerrarSesion = ::cerrarSesion, modifier = modifier)
        Rol.PREFECTO -> PrefectoApp(sesion = actual, onCerrarSesion = ::cerrarSesion, modifier = modifier)
        Rol.PROFESOR -> ProfesorApp(sesion = actual, onCerrarSesion = ::cerrarSesion, modifier = modifier)
        // Enfermería y Control Escolar se usan desde la web (carpeta web/, ADR-011).
        else -> RolSinPantallas(sesion = actual, onCerrarSesion = ::cerrarSesion, modifier = modifier)
    }
}

/** true si la cuenta debe cambiar su contraseña: lo pidió Control Escolar o ya venció. */
private fun debeCambiarContrasena(usuario: UsuarioDoc): Boolean =
    usuario.debeCambiarContrasena || usuario.contrasenaVence?.toDate()?.before(Date()) == true

/**
 * Aviso para los roles sin pantallas en Android (enfermería y Control Escolar). El nombre del
 * rol y su plataforma salen del perfil roles/{rol} de Firestore; si no se puede leer, se usa
 * el nombre local.
 */
@Composable
private fun RolSinPantallas(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perfil by produceState<RolDoc?>(null, sesion.usuario.rol) {
        value = runCatching { CetisRepositorio.rol(sesion.usuario.rol) }.getOrNull()
    }
    val nombreRol = perfil?.nombre?.ifBlank { null } ?: nombreDelRol(sesion.usuario.rol)
    val soloWeb = perfil != null && "android" !in perfil!!.plataformas
    MensajeCentrado(
        texto = stringResource(
            if (soloWeb) R.string.rol_solo_web else R.string.rol_sin_pantallas,
            sesion.usuario.nombre,
            nombreRol
        ),
        modifier = modifier,
        onCerrarSesion = onCerrarSesion
    )
}

@Composable
private fun textoDeError(mensaje: MensajeLogin): String = stringResource(
    when (mensaje) {
        MensajeLogin.CamposVacios -> R.string.login_error_campos
        MensajeLogin.NoControlInvalido -> R.string.login_error_no_control
        MensajeLogin.DatosIncorrectos -> R.string.login_error_datos
        MensajeLogin.CuentaNoRegistrada -> R.string.login_error_no_registrada
        MensajeLogin.CuentaDesactivada -> R.string.login_error_desactivada
        MensajeLogin.SinConexion -> R.string.login_error_conexion
        MensajeLogin.DemasiadosIntentos -> R.string.login_error_intentos
        MensajeLogin.Desconocido -> R.string.login_error_desconocido
    }
)

private fun nombreDelRol(rol: String): String = when (rol) {
    Rol.PREFECTO -> "prefecto"
    Rol.PROFESOR -> "profesor"
    Rol.ENFERMERIA -> "enfermería"
    Rol.CONTROL_ESCOLAR -> "Control Escolar"
    else -> rol
}

/** Un texto al centro de la pantalla y, si se da [onCerrarSesion], el botón para salir. */
@Composable
fun MensajeCentrado(
    texto: String,
    modifier: Modifier = Modifier,
    onCerrarSesion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = Brand900,
            textAlign = TextAlign.Center
        )
        if (onCerrarSesion != null) {
            PrimaryButton(
                text = stringResource(R.string.perfil_option_logout),
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
