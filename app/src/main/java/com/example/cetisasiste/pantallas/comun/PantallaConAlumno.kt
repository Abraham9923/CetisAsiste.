package com.example.cetisasiste.pantallas.comun

import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.CetisRepositorio
import com.example.cetisasiste.datos.Formatos
import com.example.cetisasiste.datos.Sesion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Pantalla que muestra la ficha del alumno de la sesión: el propio alumno o el primer hijo del
 * padre (usuarios/{uid}.hijos). La ficha y el nombre de su grupo se escuchan en tiempo real:
 * si cambian en Firestore, la pantalla se vuelve a llenar sola.
 */
abstract class PantallaConAlumno : PantallaBase() {

    /** Se llama cada vez que llega (o cambia) la ficha del alumno. */
    protected abstract fun mostrarAlumno(alumno: Alumno, documento: AlumnoDoc)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun alTenerSesion(sesion: Sesion) {
        val ficha = SesionActiva.alumno.flatMapLatest { carga ->
            val documento = (carga as? Carga.Listo)?.datos
            if (documento == null) {
                flowOf(carga to null)
            } else {
                CetisRepositorio.escucharGrupo(documento.grupoId).map { grupo -> carga to grupo.getOrNull()?.nombre }
            }
        }
        cuandoSeVea(ficha) { (carga, nombreGrupo) ->
            when (carga) {
                Carga.Cargando -> Unit
                Carga.Fallo -> Navegacion.abrirMensaje(this, TipoMensaje.ErrorAlCargar)
                is Carga.Listo -> {
                    val documento = carga.datos
                    if (documento == null) {
                        Navegacion.abrirMensaje(this, TipoMensaje.SinAlumnoVinculado)
                    } else {
                        mostrarAlumno(Formatos.alumno(documento, nombreGrupo), documento)
                    }
                }
            }
        }
    }
}
