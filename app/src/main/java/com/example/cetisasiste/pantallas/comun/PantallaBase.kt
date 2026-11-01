package com.example.cetisasiste.pantallas.comun

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.databinding.ComunBarraInicioBinding
import com.example.cetisasiste.databinding.ComunBarraSuperiorBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Lo que tienen todas las pantallas (Activities) de la app:
 *   - dibujan de borde a borde: la barra guinda queda debajo de la hora y la batería, y el
 *     contenido termina arriba de la barra de gestos (o del teclado, si está abierto);
 *   - necesitan la sesión abierta: si no la hay, regresan a Iniciar sesión, y si se cierra
 *     (Cerrar sesión, cuenta desactivada) también;
 *   - conectan la barra de arriba: el menú ☰ (comun_menu.xml) y la campana de notificaciones.
 *
 * Cada pantalla solo dice qué vista XML usa ([crearVista], con su clase de ViewBinding) y qué
 * hace cuando ya hay sesión ([alTenerSesion]).
 */
abstract class PantallaBase : ComponentActivity() {

    /** false solo en Iniciar sesión. */
    protected open val necesitaSesion: Boolean = true

    /** La sesión abierta. Se puede usar desde [alTenerSesion]. */
    protected lateinit var sesion: Sesion
        private set

    /** true en cuanto ya hay sesión (antes de eso no se debe usar [sesion]). */
    protected val haySesion: Boolean get() = ::sesion.isInitialized

    /** La vista raíz de la pantalla (la de su XML). */
    protected lateinit var raiz: View
        private set

    /** Cuánto mide la barra de estado (hora y batería) de este teléfono, en píxeles. */
    var altoBarraDeEstado: Int = 0
        private set

    /** Infla la vista XML con su clase de ViewBinding y devuelve su raíz. */
    protected abstract fun crearVista(): View

    /** Se llama una vez, en cuanto hay sesión: aquí se llenan los datos y se conectan los botones. */
    protected open fun alTenerSesion(sesion: Sesion) = Unit

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        // Íconos claros arriba (el encabezado es guinda) y oscuros abajo (el fondo es claro).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        raiz = crearVista()
        setContentView(raiz)
        aplicarBordes(raiz)
        if (!necesitaSesion) return
        val actual = SesionActiva.sesion.value
        if (actual != null) {
            empezar(actual)
        } else {
            // Android pudo cerrar la app en segundo plano: se recupera la sesión de Firebase.
            lifecycleScope.launch {
                val recuperada = cargar { SesionActiva.recuperar() }.o(null)
                if (recuperada == null) Navegacion.irAIniciarSesion(this@PantallaBase) else empezar(recuperada)
            }
        }
    }

    private fun empezar(actual: Sesion) {
        sesion = actual
        alTenerSesion(actual)
        // Mientras la pantalla se ve: si la sesión se cierra, se regresa a Iniciar sesión; si la
        // cuenta cambia en Firestore (por ejemplo, el nombre), se usa la nueva; y si Control
        // Escolar pide cambiar la contraseña (o venció), no se puede seguir sin cambiarla.
        cuandoSeVea(SesionActiva.sesion) { nueva ->
            when {
                nueva == null -> Navegacion.irAIniciarSesion(this)
                this !is CambiarContrasenaActividad && SesionActiva.debeCambiarContrasena(nueva.usuario) ->
                    Navegacion.abrirInicioDeLaSesion(this, nueva)
                else -> sesion = nueva
            }
        }
    }

    /** El contenido no queda debajo de la barra de gestos ni del teclado. */
    private fun aplicarBordes(vista: View) {
        val abajoOriginal = vista.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(vista) { _, bordes ->
            val sistema = bordes.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
            altoBarraDeEstado = bordes.getInsets(WindowInsetsCompat.Type.statusBars()).top
            vista.setPadding(vista.paddingLeft, vista.paddingTop, vista.paddingRight, abajoOriginal + sistema.bottom)
            bordes
        }
    }

    /** Escucha [flujo] solo mientras la pantalla se ve (así no se gastan lecturas de Firestore). */
    protected fun <T> cuandoSeVea(flujo: Flow<T>, accion: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { flujo.collect { accion(it) } }
        }
    }

    // ---------- Barra de arriba ----------

    /** Barra de las pantallas internas (comun_barra_superior.xml): título, ☰ y campana. */
    protected fun conectarBarra(barra: ComunBarraSuperiorBinding, @StringRes titulo: Int) =
        conectarBarra(barra, getString(titulo))

    protected fun conectarBarra(barra: ComunBarraSuperiorBinding, titulo: String) {
        barra.textoTitulo.text = titulo
        conectarBotones(barra.botonMenu, barra.botonNotificaciones)
    }

    /** Barra de los Inicio (comun_barra_inicio.xml): ☰, "CetisAsiste" y campana. */
    protected fun conectarBarraInicio(barra: ComunBarraInicioBinding) =
        conectarBotones(barra.botonMenu, barra.botonNotificaciones)

    private fun conectarBotones(botonMenu: ImageButton, botonCampana: ImageButton) {
        botonMenu.setOnClickListener { abrirMenu() }
        botonCampana.setOnClickListener {
            if (this !is NotificacionesActividad) Navegacion.abrir(this, NotificacionesActividad::class.java)
        }
        // La campana lleva punto rojo solo si hay avisos sin leer.
        cuandoSeVea(SesionActiva.sinLeer) { sinLeer -> marcarCampana(botonCampana, sinLeer) }
    }

    /** Abre el menú ☰ del rol. En el cambio de contraseña obligatorio solo trae "Cerrar sesión". */
    protected open fun abrirMenu() {
        if (!::sesion.isInitialized) return
        MenuLateral.mostrar(this, raiz.parent as ViewGroup, sesion, soloCerrarSesion = false)
    }

    private fun marcarCampana(campana: ImageButton, sinLeer: Int) {
        campana.setImageResource(if (sinLeer > 0) R.drawable.ic_bell_badge else R.drawable.ic_bell)
        val texto = getString(R.string.titulo_notificaciones)
        campana.contentDescription =
            if (sinLeer > 0) getString(R.string.formato_campana_sin_leer, texto, Avisos.textoSinLeer(sinLeer)) else texto
    }

    // ---------- Ayudantes ----------

    /** Mensaje corto abajo de la pantalla ("Se guardó el estatus."). */
    protected fun avisar(@StringRes texto: Int) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}
