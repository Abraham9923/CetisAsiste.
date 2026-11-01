package com.example.cetisasiste.pantallas.comun

import android.view.View
import com.example.cetisasiste.R
import com.example.cetisasiste.databinding.ComunPerfilPersonalBinding
import com.example.cetisasiste.datos.Rol
import com.example.cetisasiste.datos.Sesion

/**
 * Perfil del personal (comun_perfil_personal.xml): prefecto y profesor. Sus datos de cuenta,
 * "Cambiar contraseña" y "Cerrar sesión" (con el diálogo de confirmar).
 */
class PerfilPersonalActividad : PantallaBase() {

    private lateinit var enlace: ComunPerfilPersonalBinding

    override fun crearVista(): View {
        enlace = ComunPerfilPersonalBinding.inflate(layoutInflater)
        enlace.opcionContrasena.setOnClickListener { Navegacion.abrir(this, CambiarContrasenaActividad::class.java) }
        enlace.opcionCerrarSesion.setOnClickListener { DialogoCerrarSesion.mostrar(this) }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarra(enlace.barraSuperior, R.string.titulo_perfil)
        val usuario = sesion.usuario
        val area = getString(if (usuario.rol == Rol.PREFECTO) R.string.area_prefectura else R.string.area_docente)
        val personal = Personal(usuario.nombre.trim(), area, usuario.usuario, usuario.correo)
        enlace.textoNombre.text = personal.nombre
        enlace.textoAreaPlantel.text = getString(R.string.formato_area_plantel, personal.area)
        enlace.textoUsuario.text = personal.usuario
        enlace.textoArea.text = personal.area
        enlace.textoCorreo.text = personal.correo
    }
}
