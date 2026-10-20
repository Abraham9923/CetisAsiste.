package com.example.cetisasiste

import com.example.cetisasiste.datos.Alerta
import com.example.cetisasiste.datos.AlumnoDoc
import com.example.cetisasiste.datos.Avisos
import com.example.cetisasiste.datos.ConfiguracionDoc
import com.example.cetisasiste.datos.Escaneo
import com.example.cetisasiste.datos.EstatusDoc
import com.example.cetisasiste.datos.TipoAviso
import com.example.cetisasiste.datos.TipoEstatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reglas de los avisos (datos/Avisos.kt): a quién se le avisa y cómo se arma el aviso.
 * Se corren sin teléfono: gradlew :app:testDebugUnitTest
 */
class AvisosTest {

    /** Como si configuracion/plantel no tuviera los campos de avisos: se usan los de por omisión. */
    private val porOmision = ConfiguracionDoc()

    @Test
    fun `sin alerta siempre se avisa al padre`() {
        assertTrue(Avisos.alPadrePorEscaneo(null, porOmision))
        assertTrue(Avisos.alPadrePorEscaneo(null, ConfiguracionDoc(avisarAlPadreAlertas = emptyList())))
    }

    @Test
    fun `por omision se avisan inactivo y salida fuera de horario, pero no los errores al escanear`() {
        assertTrue(Avisos.alPadrePorEscaneo(Alerta.INACTIVO, porOmision))
        assertTrue(Avisos.alPadrePorEscaneo(Alerta.SALIDA_FUERA_DE_HORARIO, porOmision))
        assertFalse(Avisos.alPadrePorEscaneo(Alerta.YA_INGRESO, porOmision))
        assertFalse(Avisos.alPadrePorEscaneo(Alerta.NO_HA_INGRESADO, porOmision))
        assertFalse(Avisos.alPadrePorEscaneo(Alerta.NO_DADO_DE_ALTA, porOmision))
    }

    @Test
    fun `el plantel puede apagar los avisos de alertas`() {
        assertFalse(Avisos.alPadrePorEscaneo(Alerta.INACTIVO, ConfiguracionDoc(avisarAlPadreAlertas = emptyList())))
    }

    @Test
    fun `por omision se avisan los cuatro estatus del profesor y el plantel puede elegir`() {
        listOf(TipoEstatus.FALTA, TipoEstatus.REPORTE, TipoEstatus.LLAMADA_ATENCION, TipoEstatus.CASTIGO).forEach {
            assertTrue(it, Avisos.alPadrePorEstatus(it, porOmision))
        }
        val soloFaltas = ConfiguracionDoc(avisarAlPadreEstatus = listOf(TipoEstatus.FALTA))
        assertTrue(Avisos.alPadrePorEstatus(TipoEstatus.FALTA, soloFaltas))
        assertFalse(Avisos.alPadrePorEstatus(TipoEstatus.REPORTE, soloFaltas))
    }

    @Test
    fun `los destinatarios son los padres sin vacios ni repetidos`() {
        assertEquals(listOf("a", "b"), Avisos.destinatarios(AlumnoDoc(padres = listOf("a", "", "b", "a", "  "))))
        assertEquals(emptyList<String>(), Avisos.destinatarios(null))
    }

    @Test
    fun `un aviso nuevo sale sin leer, firmado y con su vencimiento`() {
        val ahora = Timestamp(Date(1_000_000_000_000))
        val aviso = Avisos.nuevo(TipoAviso.ENTRADA, "  Entrada registrada ", "Juan entró.", "00000000000001", "uid-prefecto", ahora)
        assertEquals("Entrada registrada", aviso.titulo)
        assertFalse(aviso.leida)
        assertEquals("uid-prefecto", aviso.creadaPor)
        assertEquals(ahora, aviso.fecha)
        val dias = (aviso.venceEn!!.toDate().time - ahora.toDate().time) / (24L * 60 * 60 * 1000)
        assertEquals(Avisos.DIAS_DE_VIGENCIA.toLong(), dias)
    }

    @Test
    fun `los textos largos se recortan a lo que aceptan las reglas`() {
        val aviso = Avisos.nuevo(TipoAviso.ESTATUS, "t".repeat(300), "m".repeat(900), "", "uid")
        assertEquals(120, aviso.titulo.length)
        assertEquals(500, aviso.mensaje.length)
    }

    @Test
    fun `el contador de la campana`() {
        assertEquals("", Avisos.textoSinLeer(0))
        assertEquals("3", Avisos.textoSinLeer(3))
        assertEquals("9+", Avisos.textoSinLeer(10))
    }

    @Test
    fun `el resultado muestra el castigo de hoy, el mas reciente`() {
        val ahora = Date()
        val ayer = Date(ahora.time - 24L * 60 * 60 * 1000)
        val viejo = EstatusDoc(tipo = TipoEstatus.CASTIGO, descripcion = "viejo", fecha = Timestamp(ayer))
        val deHoy = EstatusDoc(tipo = TipoEstatus.CASTIGO, descripcion = "de hoy", fecha = Timestamp(ahora))
        val falta = EstatusDoc(tipo = TipoEstatus.FALTA, fecha = Timestamp(ahora))
        assertEquals("de hoy", Escaneo.castigoVigente(listOf(falta, deHoy, viejo), ahora)?.descripcion)
        assertNull(Escaneo.castigoVigente(listOf(falta, viejo), ahora))
    }
}
