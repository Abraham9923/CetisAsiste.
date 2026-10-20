package com.example.cetisasiste.ui.comun

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.Avisos
import java.util.Locale

/*
 * Lo que comparten las vistas XML de todos los roles:
 *   - PantallaXml: muestra un layout XML dentro de Compose (la app es una sola actividad).
 *   - El menú ☰ de cada rol, el diálogo de cerrar sesión y las listas.
 *   - Cómo se llenan las vistas comunes: calendario, notificaciones, renglones, estado.
 *
 * Igual que AlumnoVistas.kt: solo pone textos e imágenes en su lugar. No consulta la base
 * de datos y no decide a dónde navegar (eso lo hace el *App.kt de cada rol).
 */

/**
 * Muestra el layout XML [layout] a pantalla completa.
 * [llenar] pone los datos y conecta los botones; se vuelve a llamar cada vez que cambian.
 */
@Composable
fun PantallaXml(
    @LayoutRes layout: Int,
    modifier: Modifier = Modifier,
    llenar: (View) -> Unit,
) {
    // La app dibuja de borde a borde: el contenido termina arriba de la barra de gestos de Android.
    val abajo = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    // key(): cuando cambia la pantalla se infla su XML desde cero.
    key(layout) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { contexto -> LayoutInflater.from(contexto).inflate(layout, null) },
            update = { vista ->
                // El espacio va en la columna (primer hijo), no en la raíz: así el fondo, el
                // diálogo y el menú sí llegan hasta abajo.
                (vista as? ViewGroup)?.getChildAt(0)?.let { it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, abajo) }
                llenar(vista)
            },
        )
    }
}

/** Una pantalla principal del rol. Cada una sale como opción del menú ☰ (comun_menu.xml). */
class Pestana<T>(val destino: T, @param:DrawableRes val icono: Int, @param:StringRes val texto: Int)

object VistasComunes {

    /** Lo que tarda el menú en entrar o salir (milisegundos). */
    private const val DURACION_MENU = 220L

    // ---------- Menú ☰ ----------

    /**
     * Conecta el botón ☰ (btn_menu) de [pantalla]: abre el menú lateral con [nombre] y [rol]
     * arriba y una opción por cada una de las [pestanas]; la de [activa] se marca. Tocar una
     * opción cierra el menú y llama a [irA]. "Notificaciones" (debajo de las opciones) llama a
     * [onNotificaciones], lo mismo que la campana de Inicio. "Cerrar sesión", hasta abajo,
     * pregunta antes (diálogo de cerrar sesión) y luego llama a [onCerrarSesion]. Si la
     * pantalla no tiene ☰, no hace nada. [sinLeer] es cuántos avisos sin leer hay: sale como
     * contador junto a "Notificaciones".
     */
    fun <T> conectarMenu(
        pantalla: View,
        nombre: String,
        rol: String,
        pestanas: List<Pestana<T>>,
        activa: T,
        irA: (T) -> Unit,
        onNotificaciones: () -> Unit,
        onCerrarSesion: () -> Unit,
        sinLeer: Int = 0,
    ) {
        pantalla.alTocar(R.id.btn_menu) {
            mostrarMenu(pantalla as FrameLayout, nombre, rol, pestanas, activa, irA, onNotificaciones, onCerrarSesion, sinLeer)
        }
    }

    /**
     * La campana de Inicio (btn_notificaciones): con punto rojo solo si hay avisos sin leer, y
     * para quien usa lector de pantalla dice cuántos ("Notificaciones, 3 sin leer").
     */
    fun marcarCampana(pantalla: View, sinLeer: Int) {
        val campana = pantalla.findViewById<ImageView>(R.id.btn_notificaciones) ?: return
        campana.setImageResource(if (sinLeer > 0) R.drawable.ic_bell_badge else R.drawable.ic_bell)
        val texto = pantalla.context.getString(R.string.alumno_notificaciones)
        campana.contentDescription =
            if (sinLeer > 0) pantalla.context.getString(R.string.comun_campana_sin_leer, texto, Avisos.textoSinLeer(sinLeer)) else texto
    }

    /**
     * Pone encima de la pantalla una capa oscura y el menú (entra deslizándose desde la
     * izquierda). Tocar fuera o el botón "atrás" de Android lo cierran.
     */
    private fun <T> mostrarMenu(
        raiz: FrameLayout,
        nombre: String,
        rol: String,
        pestanas: List<Pestana<T>>,
        activa: T,
        irA: (T) -> Unit,
        onNotificaciones: () -> Unit,
        onCerrarSesion: () -> Unit,
        sinLeer: Int,
    ) {
        if (raiz.findViewById<View>(R.id.menu) != null) return  // ya está abierto
        val contexto = raiz.context
        val ancho = raiz.resources.getDimension(R.dimen.cetis_menu_width)
        val capa = View(contexto).apply {
            setBackgroundColor(ContextCompat.getColor(contexto, R.color.cetis_overlay))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = 0f
        }
        val menu = LayoutInflater.from(contexto).inflate(R.layout.comun_menu, raiz, false)
        ViewCompat.setAccessibilityPaneTitle(menu, contexto.getString(R.string.action_menu))
        menu.texto(R.id.txt_nombre, nombre)
        menu.texto(R.id.txt_rol, rol)
        // El menú llega de arriba abajo de la pantalla: el encabezado empieza debajo de la hora
        // y la batería, y el pie termina arriba de la barra de gestos.
        val bordes = ViewCompat.getRootWindowInsets(raiz)
            ?.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        val encabezado = menu.findViewById<View>(R.id.encabezado_menu)
        encabezado.setPadding(0, encabezado.paddingTop + (bordes?.top ?: 0), 0, 0)
        menu.setPadding(menu.paddingLeft, menu.paddingTop, menu.paddingRight, bordes?.bottom ?: 0)

        lateinit var cerrar: () -> Unit
        llenarLista(menu.findViewById(R.id.lista_opciones), pestanas, R.layout.comun_item_menu) { fila, pestana ->
            val esActiva = pestana.destino == activa
            fila.findViewById<ImageView>(R.id.img_icono).setImageResource(pestana.icono)
            fila.findViewById<TextView>(R.id.txt_texto).setText(pestana.texto)
            fila.isSelected = esActiva
            if (esActiva) fila.setBackgroundResource(R.drawable.shape_tarjeta)
            fila.setOnClickListener {
                cerrar()
                if (!esActiva) irA(pestana.destino)
            }
        }
        menu.mostrar(R.id.txt_sin_leer, sinLeer > 0)
        menu.texto(R.id.txt_sin_leer, Avisos.textoSinLeer(sinLeer))
        if (sinLeer > 0) {
            menu.findViewById<View>(R.id.opcion_notificaciones).contentDescription = contexto.getString(
                R.string.comun_campana_sin_leer, contexto.getString(R.string.alumno_notificaciones), Avisos.textoSinLeer(sinLeer),
            )
        }
        menu.alTocar(R.id.opcion_notificaciones) {
            cerrar()
            onNotificaciones()
        }
        menu.alTocar(R.id.opcion_cerrar_sesion) {
            cerrar()
            mostrarDialogoCerrarSesion(raiz, onCerrarSesion)
        }

        // Mientras el menú está abierto, "atrás" lo cierra (en lugar de regresar de pantalla).
        val atras = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cerrar()
        }
        raiz.findViewTreeOnBackPressedDispatcherOwner()?.onBackPressedDispatcher?.addCallback(atras)
        raiz.doOnDetach { atras.remove() }

        cerrar = {
            atras.remove()
            capa.animate().alpha(0f).setDuration(DURACION_MENU).start()
            menu.animate().translationX(-ancho).setDuration(DURACION_MENU).withEndAction {
                raiz.removeView(menu)
                raiz.removeView(capa)
            }.start()
        }
        capa.setOnClickListener { cerrar() }

        raiz.addView(capa, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz.addView(menu)
        menu.translationX = -ancho
        capa.animate().alpha(1f).setDuration(DURACION_MENU).start()
        menu.animate().translationX(0f).setDuration(DURACION_MENU).start()
    }

    // ---------- Diálogo "¿Cerrar sesión?" ----------

    /**
     * Pone encima de la pantalla la capa oscura y el diálogo de cerrar sesión
     * (alumno_dialogo_cerrar_sesion.xml, "LogoutDialog" de Figma). Cancelar o tocar fuera lo quita.
     */
    fun mostrarDialogoCerrarSesion(pantalla: View, onConfirmar: () -> Unit) {
        val raiz = pantalla as FrameLayout
        val contexto = raiz.context
        val capa = View(contexto).apply {
            setBackgroundColor(ContextCompat.getColor(contexto, R.color.cetis_overlay))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val dialogo = LayoutInflater.from(contexto).inflate(R.layout.alumno_dialogo_cerrar_sesion, raiz, false)
        (dialogo.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        raiz.addView(capa, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz.addView(dialogo)

        fun cerrar() {
            raiz.removeView(dialogo)
            raiz.removeView(capa)
        }
        capa.setOnClickListener { cerrar() }
        dialogo.alTocar(R.id.btn_cancelar) { cerrar() }
        dialogo.alTocar(R.id.btn_confirmar) {
            cerrar()
            onConfirmar()
        }
    }

    // ---------- Listas ----------

    /**
     * Llena un contenedor vertical con una fila [fila] por cada elemento. Se usa dentro de las
     * pantallas con scroll, donde las listas son cortas (eventos, grupos, alumnos de un grupo).
     */
    fun <T> llenarLista(contenedor: ViewGroup, elementos: List<T>, @LayoutRes fila: Int, llenar: (View, T) -> Unit) {
        contenedor.removeAllViews()
        val inflador = LayoutInflater.from(contenedor.context)
        elementos.forEach { elemento ->
            contenedor.addView(inflador.inflate(fila, contenedor, false).also { llenar(it, elemento) })
        }
    }

    /** comun_item_lista.xml. Si se da [alTocar], el renglón lleva flecha y se puede tocar. */
    fun llenarRenglon(fila: View, renglon: Renglon, alTocar: (() -> Unit)? = null) {
        fila.findViewById<View>(R.id.circulo).setBackgroundResource(renglon.fondoIcono)
        fila.findViewById<ImageView>(R.id.img_icono).setImageResource(renglon.icono)
        fila.texto(R.id.txt_titulo, renglon.titulo)
        fila.texto(R.id.txt_detalle, renglon.detalle)
        fila.mostrar(R.id.txt_detalle, renglon.detalle.isNotBlank())
        fila.mostrar(R.id.img_flecha, alTocar != null)
        if (alTocar != null) {
            fila.setOnClickListener { alTocar() }
        } else {
            fila.setOnClickListener(null)
            fila.isClickable = false
        }
    }

    // ---------- Chips y estado ----------

    /** Pinta un chip como elegido (guinda con letra blanca) o no (como en Figma). */
    fun marcarChip(chip: TextView, elegido: Boolean) {
        chip.isSelected = elegido
        chip.setBackgroundResource(if (elegido) R.drawable.shape_chip_activo else R.drawable.shape_chip)
        chip.setTextColor(
            ContextCompat.getColor(chip.context, if (elegido) R.color.cetis_text_on_brand else R.color.cetis_brand_900),
        )
    }

    /** Etiqueta de estado del alumno (StatusChip): verde si está activo, roja si no. */
    fun llenarEstado(vista: View, estado: EstadoVisible) {
        val activo = estado == EstadoVisible.Activo || estado == EstadoVisible.EnPracticas
        vista.findViewById<View>(R.id.chip_estado)
            .setBackgroundResource(if (activo) R.drawable.shape_estatus_exito else R.drawable.shape_estatus_peligro)
        vista.findViewById<View>(R.id.punto_estado)
            .setBackgroundResource(if (activo) R.drawable.shape_punto_exito else R.drawable.shape_punto_peligro)
        val texto = vista.findViewById<TextView>(R.id.txt_estado)
        texto.setText(
            when (estado) {
                EstadoVisible.Activo -> R.string.estado_activo
                EstadoVisible.InactivoPorPago -> R.string.estado_inactivo_pago
                EstadoVisible.InactivoPorSalud -> R.string.estado_inactivo_salud
                EstadoVisible.EnPracticas -> R.string.estado_en_practicas
            },
        )
        texto.setTextColor(
            ContextCompat.getColor(vista.context, if (activo) R.color.cetis_status_success else R.color.cetis_status_danger),
        )
    }

    // ---------- Calendario (comun_calendario.xml) ----------

    fun llenarCalendario(vista: View, mes: MesCalendario, eventos: List<Evento>) {
        val contexto = vista.context
        vista.texto(R.id.txt_mes, mes.titulo)

        val letras = vista.resources.getStringArray(R.array.calendario_weekdays)
        val encabezado = vista.findViewById<ViewGroup>(R.id.dias_semana)
        letras.forEachIndexed { i, letra -> (encabezado.getChildAt(i) as? TextView)?.text = letra }

        // Casillas del mes: null = vacía, número = día. Se parten en renglones de 7 (lunes a domingo).
        val casillas: List<Int?> = List(mes.casillasVaciasAlInicio) { null } + (1..mes.dias)
        val semanas = vista.findViewById<ViewGroup>(R.id.calendario_semanas)
        semanas.removeAllViews()
        val inflador = LayoutInflater.from(contexto)
        casillas.chunked(DIAS_POR_SEMANA).forEachIndexed { numeroDeSemana, semana ->
            val renglon = LinearLayout(contexto).apply { orientation = LinearLayout.HORIZONTAL }
            for (columna in 0 until DIAS_POR_SEMANA) {
                val casilla = inflador.inflate(R.layout.comun_item_dia, renglon, false)
                val dia = semana.getOrNull(columna)
                if (dia != null) llenarDia(casilla, dia, esFinDeSemana = columna >= 5, mes)
                renglon.addView(casilla)
            }
            val medidas = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (numeroDeSemana > 0) medidas.topMargin = vista.resources.getDimensionPixelSize(R.dimen.cetis_space_8)
            semanas.addView(renglon, medidas)
        }

        llenarLista(vista.findViewById(R.id.lista_eventos), eventos, R.layout.comun_item_lista) { fila, evento ->
            llenarRenglon(fila, Renglon(R.drawable.ic_calendar_event, evento.titulo, evento.detalle))
        }
        vista.mostrar(R.id.txt_vacio, eventos.isEmpty())
    }

    private fun llenarDia(casilla: View, dia: Int, esFinDeSemana: Boolean, mes: MesCalendario) {
        val numero = casilla.findViewById<TextView>(R.id.txt_dia)
        numero.text = String.format(Locale.getDefault(), "%d", dia)
        val color = when {
            dia == mes.diaDeHoy -> {
                // Hoy: círculo guinda, letra blanca y más gruesa.
                TextViewCompat.setTextAppearance(numero, R.style.Cetis_Texto_TituloTarjeta)
                numero.setBackgroundResource(R.drawable.shape_circulo_brand)
                R.color.cetis_text_on_brand
            }
            esFinDeSemana -> R.color.cetis_text_secondary
            else -> R.color.cetis_brand_900
        }
        numero.setTextColor(ContextCompat.getColor(casilla.context, color))
        casilla.findViewById<View>(R.id.img_evento).visibility =
            if (dia in mes.diasConEvento) View.VISIBLE else View.INVISIBLE
    }

    private const val DIAS_POR_SEMANA = 7

    // ---------- Notificaciones (comun_notificaciones.xml) ----------

    /** comun_notificaciones.xml. "Marcar todas como leídas" solo sale si hay alguna sin leer. */
    fun llenarNotificaciones(vista: View, deHoy: List<Notificacion>, anteriores: List<Notificacion>) {
        llenarLista(vista.findViewById(R.id.lista_hoy), deHoy, R.layout.comun_item_notificacion, ::llenarNotificacion)
        vista.mostrar(R.id.txt_vacio_hoy, deHoy.isEmpty())
        vista.mostrar(R.id.txt_marcar_leidas, (deHoy + anteriores).any { !it.leida })
        llenarLista(vista.findViewById(R.id.lista_anteriores), anteriores, R.layout.comun_item_notificacion, ::llenarNotificacion)
        vista.mostrar(R.id.txt_anteriores, anteriores.isNotEmpty())
    }

    // Las notificaciones sin leer tienen fondo más oscuro y un punto rojo a la derecha.
    private fun llenarNotificacion(fila: View, notificacion: Notificacion) {
        val icono = when (notificacion.tipo) {
            TipoNotificacion.Entrada -> R.drawable.ic_log_in
            TipoNotificacion.Salida -> R.drawable.ic_log_out
            TipoNotificacion.Anuncio -> R.drawable.ic_announcements
            TipoNotificacion.Recordatorio -> R.drawable.ic_calendar
            TipoNotificacion.Credencial -> R.drawable.ic_id_card
            TipoNotificacion.Alerta -> R.drawable.ic_warning
            TipoNotificacion.Estatus -> R.drawable.ic_flag
            TipoNotificacion.Estado -> R.drawable.ic_id_card
            TipoNotificacion.SalidaDeGrupo -> R.drawable.ic_flag
            TipoNotificacion.Aviso -> R.drawable.ic_announcements
        }
        fila.findViewById<ImageView>(R.id.img_icono).setImageResource(icono)
        fila.findViewById<View>(R.id.circulo).setBackgroundResource(
            when (notificacion.tipo) {
                TipoNotificacion.Alerta -> R.drawable.shape_circulo_peligro
                TipoNotificacion.Salida, TipoNotificacion.SalidaDeGrupo -> R.drawable.shape_circulo_accent
                else -> R.drawable.shape_circulo_brand
            },
        )
        fila.setBackgroundResource(if (notificacion.leida) R.drawable.shape_tarjeta else R.drawable.shape_tarjeta_sin_leer)
        fila.texto(R.id.txt_titulo, notificacion.titulo)
        fila.texto(R.id.txt_hora, notificacion.hora)
        fila.texto(R.id.txt_texto, notificacion.texto)
        fila.mostrar(R.id.img_sin_leer, !notificacion.leida)
    }

    // ---------- Perfil del personal (comun_perfil_personal.xml) ----------

    fun llenarPerfilPersonal(vista: View, personal: Personal) {
        vista.texto(R.id.txt_nombre, personal.nombre)
        vista.texto(R.id.txt_rol, personal.rol)
        vista.texto(R.id.txt_usuario, personal.usuario)
        vista.texto(R.id.txt_correo, personal.correo)
    }
}

// ---------- Ayudantes cortos para cualquier vista ----------

/** Pone [valor] en el TextView con ese id. */
fun View.texto(@IdRes id: Int, valor: CharSequence) {
    findViewById<TextView>(id).text = valor
}

/** Muestra u oculta (sin dejar hueco) la vista con ese id. */
fun View.mostrar(@IdRes id: Int, visible: Boolean) {
    findViewById<View>(id)?.isVisible = visible
}

/** Conecta un toque, solo si esa vista existe en el XML de la pantalla. */
fun View.alTocar(@IdRes id: Int, accion: () -> Unit) {
    findViewById<View>(id)?.setOnClickListener { accion() }
}

/** Mensaje corto abajo de la pantalla ("Se guardó el estatus."). */
fun View.avisar(@StringRes texto: Int) {
    Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
}
