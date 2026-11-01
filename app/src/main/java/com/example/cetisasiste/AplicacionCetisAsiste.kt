package com.example.cetisasiste

import android.app.Application
import com.example.cetisasiste.datos.Formatos

/**
 * La aplicación. Lo único que hace al abrirse es preparar los formatos de fechas y textos
 * (Formatos lee sus palabras de strings.xml). La primera pantalla es Iniciar sesión
 * (IniciarSesionActividad, ver AndroidManifest.xml).
 */
class AplicacionCetisAsiste : Application() {
    override fun onCreate() {
        super.onCreate()
        Formatos.iniciar(this)
    }
}
