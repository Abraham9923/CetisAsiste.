package com.example.cetisasiste.pantallas.comun

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunItemNotificacionBinding
import com.example.cetisasiste.databinding.ComunNotificacionesBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.NotificacionDoc
import com.example.cetisasiste.datos.Rol
import com.example.cetisasiste.datos.SalidaDeGrupoDoc
import com.example.cetisasiste.datos.Sesion
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Notificaciones (comun_notificaciones.xml), para todos los roles: la abren la campana y el
 * menú ☰. Son los avisos de la cuenta (usuarios/{uid}/notificaciones), separados en hoy y
 * anteriores, en tiempo real. El prefecto ve además los grupos que ya salieron hoy, como en
 * Figma ("Grupo 102 ya salió de clases").
 */
class NotificacionesActividad : PantallaBase() {

    private lateinit var enlace: ComunNotificacionesBinding
    private val adaptador = AdaptadorConEncabezados<Notificacion, ComunItemNotificacionBinding>(
        ComunItemNotificacionBinding::inflate,
        Llenado::llenarNotificacion,
    )
    /** Ids de los avisos sin leer (para "Marcar todas como leídas"). */
    private var sinLeer: List<String> = emptyList()

    override fun crearVista(): View {
        enlace = ComunNotificacionesBinding.inflate(layoutInflater)
        enlace.listaNotificaciones.adapter = adaptador
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_notificaciones)
        enlace.textoMarcarLeidas.setOnClickListener {
            val ids = sinLeer
            lifecycleScope.launch { cargar { CetisRepositorio.marcarLeidas(sesion.idUsuario, ids) } }
        }
        val salidas = if (sesion.usuario.rol == Rol.PREFECTO) {
            CetisRepositorio.escucharSalidasDeHoy()
        } else {
            flowOf(Result.success(emptyList()))
        }
        // Si falla (sin internet), se ve como "No hay notificaciones de hoy".
        cuandoSeVea(CetisRepositorio.escucharNotificaciones(sesion.idUsuario).combine(salidas) { avisos, deGrupos ->
            avisos.getOrNull().orEmpty() to deGrupos.getOrNull().orEmpty()
        }) { (avisos, deGrupos) -> mostrar(avisos, deGrupos) }
    }

    private fun mostrar(avisos: List<Pair<String, NotificacionDoc>>, salidasDeHoy: List<SalidaDeGrupoDoc>) {
        sinLeer = avisos.filter { !it.second.leida }.map { it.first }
        val (deHoy, anteriores) = avisos.partition { Formatos.esHoy(it.second.fecha) }
        val hoy = salidasDeHoy.map(::avisoDeSalida) + deHoy.map { Llenado.notificacion(it.second) }
        val elementos = buildList<ElementoLista<Notificacion>> {
            hoy.forEach { add(ElementoLista.Fila(it)) }
            if (anteriores.isNotEmpty()) {
                add(ElementoLista.Encabezado(getString(R.string.seccion_anteriores)))
                anteriores.forEach { add(ElementoLista.Fila(Llenado.notificacion(it.second))) }
            }
        }
        adaptador.mostrar(elementos)
        enlace.textoVacioHoy.isVisible = hoy.isEmpty()
        enlace.textoMarcarLeidas.isVisible = sinLeer.isNotEmpty()
    }

    /** Un grupo que ya salió de clases hoy, como aviso del prefecto. */
    private fun avisoDeSalida(salida: SalidaDeGrupoDoc) = Notificacion(
        tipo = TipoNotificacion.SalidaDeGrupo,
        titulo = getString(R.string.formato_grupo_ya_salio, salida.grupo.trim()),
        hora = Formatos.hora(salida.fechaHora),
        texto = if (salida.materia.isBlank()) "" else getString(R.string.formato_quien_dio_salida, salida.materia, salida.autorizadaPorNombre.trim()),
        leida = true,
    )
}
