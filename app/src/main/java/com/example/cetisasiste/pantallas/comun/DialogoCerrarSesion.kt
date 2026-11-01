package com.example.cetisasiste.pantallas.comun

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.example.cetisasiste.databinding.AlumnoDialogoCerrarSesionBinding

/**
 * Diálogo "¿Cerrar sesión?" (alumno_dialogo_cerrar_sesion.xml, "LogoutDialog" de Figma).
 * No es una pantalla: se muestra encima de la pantalla abierta con la capa oscura, como en los
 * frames "Confirmar cerrar sesion" de Figma. Cancelar o tocar fuera lo quitan.
 */
object DialogoCerrarSesion {

    /** Qué tan oscura es la capa de atrás (Figma: overlay al 60 %). */
    private const val OSCURIDAD = 0.6f

    fun mostrar(actividad: Activity) {
        val enlace = AlumnoDialogoCerrarSesionBinding.inflate(actividad.layoutInflater)
        val dialogo = Dialog(actividad)
        dialogo.setContentView(enlace.root)
        dialogo.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(OSCURIDAD)
        }
        enlace.botonCancelar.setOnClickListener { dialogo.dismiss() }
        enlace.botonConfirmar.setOnClickListener {
            dialogo.dismiss()
            Navegacion.cerrarSesion(actividad)
        }
        dialogo.show()
    }
}
