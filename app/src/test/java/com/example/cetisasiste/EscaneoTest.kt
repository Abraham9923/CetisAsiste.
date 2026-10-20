package com.example.cetisasiste

import com.example.cetisasiste.datos.Alerta
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.DatosDelEscaneo
import com.example.cetisasiste.datos.Escaneo
import com.example.cetisasiste.datos.EstadoAlumno
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.NoControl
import com.example.cetisasiste.datos.TipoEstatus
import com.example.cetisasiste.datos.TipoRegistro
import com.example.cetisasiste.datos.Ubicacion
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reglas del escaneo (datos/Escaneo.kt) y del No. de control (datos/Modelos.kt).
 * Se corren sin teléfono: gradlew :app:testDebugUnitTest
 */
class EscaneoTest {

    private val ahora = Date()
    private val ayer = Date(ahora.time - 24L * 60 * 60 * 1000)
    private val manana = Date(ahora.time + 24L * 60 * 60 * 1000)

    private fun alumno(ubicacion: String = Ubicacion.FUERA) = AlumnoDoc(noControl = "00000000000001", ubicacion = ubicacion)

    private fun datos(
        alumno: AlumnoDoc?,
        tipo: String,
        estatus: List<EstatusDoc> = emptyList(),
        grupoYaSalio: Boolean = false,
    ) = DatosDelEscaneo(alumno, tipo, estatus, grupoYaSalio, ahora)

    private fun estatus(tipo: String, fecha: Date, hasta: Date? = null) =
        EstatusDoc(tipo = tipo, fecha = Timestamp(fecha), vigenteHasta = hasta?.let { Timestamp(it) })

    // ---------- Entrada ----------

    @Test fun entradaNormal_sinAlerta() =
        assertNull(Escaneo.alerta(datos(alumno(), TipoRegistro.ENTRADA)))

    @Test fun noExiste_noDadoDeAlta() =
        assertEquals(Alerta.NO_DADO_DE_ALTA, Escaneo.alerta(datos(null, TipoRegistro.ENTRADA)))

    @Test fun dadoDeBaja_noDadoDeAlta() =
        assertEquals(Alerta.NO_DADO_DE_ALTA, Escaneo.alerta(datos(alumno().copy(estado = EstadoAlumno.BAJA_DEFINITIVA), TipoRegistro.ENTRADA)))

    @Test fun inactivoPorPago_alEntrar() =
        assertEquals(Alerta.INACTIVO, Escaneo.alerta(datos(alumno().copy(activoPorPago = false), TipoRegistro.ENTRADA)))

    @Test fun inactivoPorSalud_alEntrar() =
        assertEquals(Alerta.INACTIVO, Escaneo.alerta(datos(alumno().copy(activoPorSalud = false), TipoRegistro.ENTRADA)))

    @Test fun yaEstaDentro_yaIngreso() =
        assertEquals(Alerta.YA_INGRESO, Escaneo.alerta(datos(alumno(Ubicacion.DENTRO), TipoRegistro.ENTRADA)))

    @Test fun castigoDeHoy_alEntrar() =
        assertEquals(Alerta.CASTIGO, Escaneo.alerta(datos(alumno(), TipoRegistro.ENTRADA, listOf(estatus(TipoEstatus.CASTIGO, ahora)))))

    @Test fun castigoDeAyerSinFechaDeFin_yaNoCuenta() =
        assertNull(Escaneo.alerta(datos(alumno(), TipoRegistro.ENTRADA, listOf(estatus(TipoEstatus.CASTIGO, ayer)))))

    @Test fun castigoDeAyerHastaManana_siCuenta() =
        assertEquals(Alerta.CASTIGO, Escaneo.alerta(datos(alumno(), TipoRegistro.ENTRADA, listOf(estatus(TipoEstatus.CASTIGO, ayer, manana)))))

    @Test fun inactivoGanaACastigo() =
        assertEquals(
            Alerta.INACTIVO,
            Escaneo.alerta(datos(alumno().copy(activoPorPago = false), TipoRegistro.ENTRADA, listOf(estatus(TipoEstatus.CASTIGO, ahora)))),
        )

    // ---------- Salida ----------

    @Test fun salidaSinEntrada_noHaIngresado() =
        assertEquals(Alerta.NO_HA_INGRESADO, Escaneo.alerta(datos(alumno(Ubicacion.FUERA), TipoRegistro.SALIDA, grupoYaSalio = true)))

    @Test fun salidaConGrupoLiberado_sinAlerta() =
        assertNull(Escaneo.alerta(datos(alumno(Ubicacion.DENTRO), TipoRegistro.SALIDA, grupoYaSalio = true)))

    @Test fun salidaAntesDeQueSalgaElGrupo_fueraDeHorario() =
        assertEquals(Alerta.SALIDA_FUERA_DE_HORARIO, Escaneo.alerta(datos(alumno(Ubicacion.DENTRO), TipoRegistro.SALIDA)))

    @Test fun salidaConPaseDeSaludDeHoy_sinAlerta() =
        assertNull(Escaneo.alerta(datos(alumno(Ubicacion.DENTRO), TipoRegistro.SALIDA, listOf(estatus(TipoEstatus.PASE_SALUD, ahora)))))

    @Test fun salidaEnPracticas_sinAlerta() =
        assertNull(Escaneo.alerta(datos(alumno(Ubicacion.DENTRO).copy(enPracticas = true), TipoRegistro.SALIDA)))

    @Test fun salidaConCastigo_ganaACastigo() =
        assertEquals(
            Alerta.CASTIGO,
            Escaneo.alerta(datos(alumno(Ubicacion.DENTRO), TipoRegistro.SALIDA, listOf(estatus(TipoEstatus.CASTIGO, ahora)), grupoYaSalio = true)),
        )

    @Test fun ubicacionDespues() {
        assertEquals(Ubicacion.DENTRO, Escaneo.ubicacionDespues(TipoRegistro.ENTRADA))
        assertEquals(Ubicacion.FUERA, Escaneo.ubicacionDespues(TipoRegistro.SALIDA))
    }

    // ---------- No. de control y QR ----------

    @Test fun noControl_14Digitos() {
        assertTrue(NoControl.esValido("12345678901234"))
        assertFalse(NoControl.esValido("25311754"))
        assertFalse(NoControl.esValido("1234567890123A"))
        assertFalse(NoControl.esValido("123456789012345"))
    }

    @Test fun qr_idaYVuelta() {
        val texto = NoControl.contenidoQr("12345678901234")
        assertEquals("CETIS61:12345678901234", texto)
        assertEquals("12345678901234", NoControl.desdeQr(texto))
    }

    @Test fun qr_ajenoOIncompleto() {
        assertNull(NoControl.desdeQr("https://ejemplo.com"))
        assertNull(NoControl.desdeQr("12345678901234"))
        assertNull(NoControl.desdeQr("CETIS61:123"))
    }
}
