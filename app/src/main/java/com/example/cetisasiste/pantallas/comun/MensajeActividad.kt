package com.example.cetisasiste.pantallas.comun

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunMensajeBinding
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Sesion
import kotlinx.coroutines.launch

/**
 * Mensaje a mitad de la pantalla con "Cerrar sesión" (comun_mensaje.xml). Se abre cuando la
 * cuenta no tiene pantallas en Android (enfermería y Control Escolar usan la web, ADR-011),
 * cuando el padre o el alumno no tienen alumno vinculado o si no se pudieron cargar los datos.
 * Extra: [Claves.TIPO_MENSAJE].
 */
class MensajeActividad : PantallaBase() {

    private lateinit var enlace: ComunMensajeBinding

    override fun crearVista(): View {
        enlace = ComunMensajeBinding.inflate(layoutInflater)
        enlace.botonCerrarSesion.setOnClickListener { Navegacion.cerrarSesion(this) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        val tipo = intent.getStringExtra(Claves.TIPO_MENSAJE)?.let { nombre -> TipoMensaje.entries.firstOrNull { it.name == nombre } }
        when (tipo) {
            TipoMensaje.SinAlumnoVinculado -> enlace.textoMensaje.setText(R.string.mensaje_sin_alumno_vinculado)
            TipoMensaje.ErrorAlCargar -> enlace.textoMensaje.setText(R.string.mensaje_error_cargar_datos)
            else -> mostrarRolSinPantallas(sesion)
        }
    }

    /**
     * El nombre del rol y su plataforma salen del perfil roles/{rol} de Firestore; si no se
     * puede leer, se usa el nombre de strings.xml.
     */
    private fun mostrarRolSinPantallas(sesion: Sesion) {
        val usuario = sesion.usuario
        val nombreLocal = getString(MenuLateral.nombreDelRol(usuario.rol))
        enlace.textoMensaje.text = getString(R.string.mensaje_rol_sin_pantallas, usuario.nombre, nombreLocal)
        lifecycleScope.launch {
            val perfil = cargar { CetisRepositorio.rol(usuario.rol) }.o(null) ?: return@launch
            val nombreRol = perfil.nombre.ifBlank { nombreLocal }
            val soloWeb = "android" !in perfil.plataformas
            enlace.textoMensaje.text = getString(
                if (soloWeb) R.string.mensaje_rol_solo_web else R.string.mensaje_rol_sin_pantallas,
                usuario.nombre,
                nombreRol,
            )
        }
    }
}
