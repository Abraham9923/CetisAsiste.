package com.example.cetisasiste.pantallas.padre

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cetisasiste.databinding.PadreInicioBinding
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Sesion
import com.example.cetisasiste.pantallas.alumno.CodigoQrActividad
import com.example.cetisasiste.pantallas.alumno.CredencialActividad
import com.example.cetisasiste.pantallas.comun.Alumno
import com.example.cetisasiste.pantallas.comun.Claves
import com.example.cetisasiste.pantallas.comun.HorarioGrupoActividad
import com.example.cetisasiste.pantallas.comun.Navegacion
import com.example.cetisasiste.pantallas.comun.PantallaConAlumno
import com.example.cetisasiste.pantallas.comun.cargar
import com.example.cetisasiste.pantallas.comun.o
import kotlinx.coroutines.launch

/**
 * Inicio del padre (padre_inicio.xml): la tarjeta de su hijo, el anuncio destacado
 * ("Información Importante") y los cuatro accesos rápidos.
 */
class InicioPadreActividad : PantallaConAlumno() {

    private lateinit var enlace: PadreInicioBinding
    private var alumno: Alumno? = null
    private var grupoId = ""

    override fun crearVista(): View {
        enlace = PadreInicioBinding.inflate(layoutInflater)
        // El encabezado del padre en Figma (3:33) no lleva "CetisAsiste".
        enlace.barraInicio.textoNombreApp.isVisible = false
        enlace.tarjetaImportante.setOnClickListener {
            Navegacion.abrirDesdeElMenu(this, AnunciosPadreActividad::class.java, InicioPadreActividad::class.java)
        }
        enlace.tarjetaQr.setOnClickListener { Navegacion.abrir(this, CodigoQrActividad::class.java) }
        enlace.tarjetaCredencial.setOnClickListener { Navegacion.abrir(this, CredencialActividad::class.java) }
        enlace.tarjetaEntradas.setOnClickListener {
            val hijo = alumno ?: return@setOnClickListener
            Navegacion.abrir(this, EntradasSalidasActividad::class.java) { putExtra(Claves.NO_CONTROL, hijo.noControl) }
        }
        enlace.tarjetaHorario.setOnClickListener {
            val hijo = alumno ?: return@setOnClickListener
            Navegacion.abrir(this, HorarioGrupoActividad::class.java) {
                putExtra(Claves.GRUPO_ID, grupoId)
                putExtra(Claves.NOMBRE_GRUPO, hijo.grupo)
            }
        }
        return enlace.root
    }

    override fun alTenerSesion(sesion: Sesion) {
        conectarBarraInicio(enlace.barraInicio)
        super.alTenerSesion(sesion)
        // "Información Importante": el anuncio destacado más nuevo (si no hay, no se ve el recuadro).
        lifecycleScope.launch {
            val destacado = cargar { CetisRepositorio.anuncios(sesion.usuario.rol) }.o(emptyList()).firstOrNull { it.destacado }
            enlace.tarjetaImportante.isVisible = destacado != null
            destacado?.let { enlace.textoImportante.text = it.titulo }
        }
    }

    override fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc) {
        this.alumno = alumno
        grupoId = documento.grupoId
        enlace.textoNombre.text = alumno.nombre
        enlace.textoRol.text = alumno.rol
        enlace.textoGrupo.text = alumno.grupo
    }
}
