package com.example.cetisasiste.pantallas.prefecto

import android.view.View
import androidx.core.view.isVisible
import com.example.cetisasiste.databinding.PrefectoInicioBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.CalendarioActividad
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaBase

/**
 * Inicio del prefecto (prefecto_inicio.xml): saludo, "Escanear código QR", accesos a Horarios de
 * grupos y Calendario, y los últimos escaneos de hoy en tiempo real (Figma 77:499).
 */
class InicioPrefectoActividad : PantallaBase() {

    private lateinit var enlace: PrefectoInicioBinding
    private val adaptador = Llenado.adaptadorRenglones()

    override fun crearVista(): View {
        enlace = PrefectoInicioBinding.inflate(layoutInflater)
        enlace.listaEscaneos.adapter = adaptador
        val inicio = InicioPrefectoActividad::class.java
        enlace.tarjetaEscanear.setOnClickListener { Navegacion.abrirDesdeElMenu(this, EscanerActividad::class.java, inicio) }
        enlace.tarjetaHorarios.setOnClickListener { Navegacion.abrirDesdeElMenu(this, HorariosGruposActividad::class.java, inicio) }
        enlace.tarjetaCalendario.setOnClickListener { Navegacion.abrirDesdeElMenu(this, CalendarioActividad::class.java, inicio) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarraInicio(enlace.barraInicio)
        enlace.textoNombre.text = sesion.usuario.nombre.trim()
        // Últimos escaneos de hoy: solo se escuchan mientras se ve el Inicio. Si la consulta
        // falla (sin internet), se ve como si no hubiera nada.
        cuandoSeVea(CetisRepositorio.escucharEscaneosDeHoy()) { resultado ->
            val escaneos = resultado.getOrDefault(emptyList())
            adaptador.mostrar(escaneos.map { RegistroDeEscaneo.renglon(this, it) })
            enlace.textoVacioEscaneos.isVisible = escaneos.isEmpty()
        }
    }
}
