package com.example.cetisasiste.pantallas.comun

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.example.cetisasiste.R
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.Rol
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.databinding.ComunItemMenuBinding
import com.example.cetisasiste.databinding.ComunMenuBinding
import com.example.cetisasiste.pantallas.alumno.BoletinAlumnoActividad
import com.example.cetisasiste.pantallas.alumno.HorarioAlumnoActividad
import com.example.cetisasiste.pantallas.alumno.InicioAlumnoActividad
import com.example.cetisasiste.pantallas.alumno.PerfilAlumnoActividad
import com.example.cetisasiste.pantallas.padre.AnunciosPadreActividad
import com.example.cetisasiste.pantallas.padre.InicioPadreActividad
import com.example.cetisasiste.pantallas.padre.PerfilPadreActividad
import com.example.cetisasiste.pantallas.prefecto.AlertasActividad
import com.example.cetisasiste.pantallas.prefecto.EscanerActividad
import com.example.cetisasiste.pantallas.prefecto.HorariosGruposActividad
import com.example.cetisasiste.pantallas.prefecto.InicioPrefectoActividad
import com.example.cetisasiste.pantallas.profesor.BoletinProfesorActividad
import com.example.cetisasiste.pantallas.profesor.InicioProfesorActividad

/** Una opción del menú ☰: la pantalla que abre, su ícono y su nombre. */
class OpcionMenu(
    val destino: Class<out Activity>,
    @param:DrawableRes val icono: Int,
    @param:StringRes val texto: Int,
)

/**
 * Menú hamburguesa (lateral) de todas las pantallas (comun_menu.xml, ADR-007). Reemplaza a la
 * barra inferior de Figma. No es una pantalla: se pone encima de la pantalla abierta, con una
 * capa oscura, y entra deslizándose desde la izquierda. Tocar fuera o "atrás" lo cierran.
 */
object MenuLateral {

    /** Lo que tarda el menú en entrar o salir (milisegundos). */
    private const val DURACION = 220L

    /** Pantallas principales de cada rol: son las opciones del menú. La primera es su Inicio. */
    fun opcionesDe(rol: String): List<OpcionMenu> = when (rol) {
        Rol.ALUMNO -> listOf(
            OpcionMenu(InicioAlumnoActividad::class.java, R.drawable.ic_home, R.string.menu_inicio),
            OpcionMenu(BoletinAlumnoActividad::class.java, R.drawable.ic_announcements, R.string.menu_boletin),
            OpcionMenu(HorarioAlumnoActividad::class.java, R.drawable.ic_clock, R.string.menu_horario),
            OpcionMenu(PerfilAlumnoActividad::class.java, R.drawable.ic_profile, R.string.menu_perfil),
        )
        Rol.PADRE -> listOf(
            OpcionMenu(InicioPadreActividad::class.java, R.drawable.ic_home, R.string.menu_inicio),
            OpcionMenu(AnunciosPadreActividad::class.java, R.drawable.ic_announcements, R.string.menu_anuncios),
            OpcionMenu(CalendarioActividad::class.java, R.drawable.ic_calendar, R.string.menu_calendario),
            OpcionMenu(PerfilPadreActividad::class.java, R.drawable.ic_profile, R.string.menu_perfil),
        )
        Rol.PREFECTO -> listOf(
            OpcionMenu(InicioPrefectoActividad::class.java, R.drawable.ic_home, R.string.menu_inicio),
            OpcionMenu(EscanerActividad::class.java, R.drawable.ic_scan, R.string.menu_escanear),
            OpcionMenu(AlertasActividad::class.java, R.drawable.ic_warning, R.string.menu_alertas),
            OpcionMenu(HorariosGruposActividad::class.java, R.drawable.ic_clock, R.string.menu_horarios),
            OpcionMenu(CalendarioActividad::class.java, R.drawable.ic_calendar, R.string.menu_calendario),
            OpcionMenu(PerfilPersonalActividad::class.java, R.drawable.ic_profile, R.string.menu_perfil),
        )
        Rol.PROFESOR -> listOf(
            OpcionMenu(InicioProfesorActividad::class.java, R.drawable.ic_home, R.string.menu_inicio),
            OpcionMenu(BoletinProfesorActividad::class.java, R.drawable.ic_announcements, R.string.menu_boletin),
            OpcionMenu(PerfilPersonalActividad::class.java, R.drawable.ic_profile, R.string.menu_perfil),
        )
        else -> emptyList()
    }

    /** Cómo se llama el rol debajo del nombre ("Alumno", "Padre de familia"…). */
    @StringRes
    fun nombreDelRol(rol: String): Int = when (rol) {
        Rol.ALUMNO -> R.string.rol_alumno
        Rol.PADRE -> R.string.rol_padre
        Rol.PREFECTO -> R.string.rol_prefecto
        Rol.PROFESOR -> R.string.rol_profesor
        Rol.ENFERMERIA -> R.string.rol_enfermeria
        else -> R.string.rol_control_escolar
    }

    /**
     * Pone el menú encima de [contenedor] (el lugar de la pantalla donde está su vista XML).
     * Si [soloCerrarSesion] es true (cambio de contraseña obligatorio), no trae las pantallas.
     */
    fun mostrar(actividad: PantallaBase, contenedor: ViewGroup, sesion: Sesion, soloCerrarSesion: Boolean) {
        if (contenedor.children.any { it.tag == ETIQUETA }) return // ya está abierto
        val ancho = actividad.resources.getDimension(R.dimen.cetis_menu_width)
        val capa = View(actividad).apply {
            setBackgroundColor(ContextCompat.getColor(actividad, R.color.cetis_overlay))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = 0f
        }
        val menu = ComunMenuBinding.inflate(LayoutInflater.from(actividad), contenedor, false)
        menu.root.tag = ETIQUETA
        ViewCompat.setAccessibilityPaneTitle(menu.root, actividad.getString(R.string.accion_menu))
        menu.textoNombre.text = sesion.usuario.nombre.trim()
        menu.textoRol.setText(nombreDelRol(sesion.usuario.rol))
        // El encabezado empieza debajo de la hora y la batería que dibuja Android.
        menu.encabezadoMenu.setPadding(
            menu.encabezadoMenu.paddingLeft,
            menu.encabezadoMenu.paddingTop + actividad.altoBarraDeEstado,
            menu.encabezadoMenu.paddingRight,
            menu.encabezadoMenu.paddingBottom,
        )

        lateinit var cerrar: () -> Unit
        val opciones = if (soloCerrarSesion) emptyList() else opcionesDe(sesion.usuario.rol)
        val inicio = opciones.firstOrNull()?.destino
        menu.listaOpciones.adapter = AdaptadorSimple<OpcionMenu, ComunItemMenuBinding>(ComunItemMenuBinding::inflate) { fila, opcion, _ ->
            val esActiva = opcion.destino == actividad.javaClass
            fila.iconoOpcion.setImageResource(opcion.icono)
            fila.textoOpcion.setText(opcion.texto)
            fila.root.isSelected = esActiva
            if (esActiva) fila.root.setBackgroundResource(R.drawable.shape_tarjeta) else fila.root.background = null
            fila.root.setOnClickListener {
                cerrar()
                if (!esActiva && inicio != null) Navegacion.abrirDesdeElMenu(actividad, opcion.destino, inicio)
            }
        }.also { it.mostrar(opciones) }

        // "Notificaciones" con cuántos avisos sin leer hay.
        val sinLeer = SesionActiva.sinLeer.value
        menu.opcionNotificaciones.isVisible = !soloCerrarSesion
        menu.textoSinLeer.isVisible = sinLeer > 0
        menu.textoSinLeer.text = Avisos.textoSinLeer(sinLeer)
        if (sinLeer > 0) {
            menu.opcionNotificaciones.contentDescription = actividad.getString(
                R.string.formato_campana_sin_leer, actividad.getString(R.string.titulo_notificaciones), Avisos.textoSinLeer(sinLeer),
            )
        }
        menu.opcionNotificaciones.setOnClickListener {
            cerrar()
            if (actividad !is NotificacionesActividad) Navegacion.abrir(actividad, NotificacionesActividad::class.java)
        }
        menu.opcionCerrarSesion.setOnClickListener {
            cerrar()
            DialogoCerrarSesion.mostrar(actividad)
        }

        // Mientras el menú está abierto, "atrás" lo cierra (en lugar de regresar de pantalla).
        val atras = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cerrar()
        }
        (actividad as ComponentActivity).onBackPressedDispatcher.addCallback(actividad, atras)

        cerrar = {
            atras.remove()
            capa.animate().alpha(0f).setDuration(DURACION).start()
            menu.root.animate().translationX(-ancho).setDuration(DURACION).withEndAction {
                contenedor.removeView(menu.root)
                contenedor.removeView(capa)
            }.start()
        }
        capa.setOnClickListener { cerrar() }

        val todo = ViewGroup.LayoutParams.MATCH_PARENT
        contenedor.addView(capa, FrameLayout.LayoutParams(todo, todo))
        contenedor.addView(menu.root)
        menu.root.translationX = -ancho
        capa.animate().alpha(1f).setDuration(DURACION).start()
        menu.root.animate().translationX(0f).setDuration(DURACION).start()
    }

    /** Marca para saber si el menú ya está puesto. */
    private const val ETIQUETA = "menuLateral"
}
