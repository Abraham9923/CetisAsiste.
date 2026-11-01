package com.example.cetisasiste.pantallas.prefecto

import android.view.View
import androidx.core.view.isVisible
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.PrefectoAlertasBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.comun.Llenado
import com.example.cetisasiste.pantallas.comun.PantallaBase

/**
 * Alertas de hoy (prefecto_alertas.xml), opción del menú ☰ del prefecto: cuántas van y un
 * renglón por alerta, en tiempo real (ADR-010).
 */
class AlertasActividad : PantallaBase() {

    private lateinit var enlace: PrefectoAlertasBinding
    private val adaptador = Llenado.adaptadorRenglones()

    override fun crearVista(): View {
        enlace = PrefectoAlertasBinding.inflate(layoutInflater)
        enlace.listaAlertas.adapter = adaptador
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_alertas)
        cuandoSeVea(CetisRepositorio.escucharAlertasDeHoy()) { resultado ->
            val alertas = resultado.getOrDefault(emptyList())
            val cuantas = alertas.size
            enlace.textoResumen.visibility = View.VISIBLE
            enlace.textoResumen.text = resources.getQuantityString(R.plurals.resumen_alertas, cuantas, cuantas)
            enlace.textoContador.isVisible = cuantas > 0
            enlace.textoContador.text = getString(R.string.formato_numero, cuantas)
            adaptador.mostrar(alertas.map { RegistroDeEscaneo.renglon(this, it) })
            enlace.textoVacio.isVisible = alertas.isEmpty()
        }
    }
}
