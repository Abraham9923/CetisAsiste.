package com.example.cetisasiste.datos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/*
 * MODELOS DE LA BASE DE DATOS (Cloud Firestore).
 *
 * Cada clase es la forma de UN documento de una colección. Los nombres de los
 * campos son exactamente los de Firestore, para que la conversión sea automática:
 *     documento.toObject(UsuarioDoc::class.java)
 *
 * Todos los campos son `var` con valor por defecto porque Firestore necesita
 * crear el objeto vacío y luego llenarlo.
 *
 * La explicación de cada colección está en docs/BASE_DE_DATOS.md.
 */

/** Nombres de las colecciones. Se escriben una sola vez para no equivocarse. */
object Colecciones {
    const val USUARIOS = "usuarios"
    const val NOTIFICACIONES = "notificaciones" // subcolección de usuarios/{uid}
    const val ALUMNOS = "alumnos"
    const val ESTATUS = "estatus" // subcolección de alumnos/{noControl}
    const val CAMBIOS_DE_ESTADO = "cambiosDeEstado" // subcolección de alumnos/{noControl}
    const val INSCRIPCIONES = "inscripciones" // subcolección de alumnos/{noControl}
    const val PRIVADO = "privado" // subcolección de alumnos/{noControl}: documentos "datos" y "salud"
    const val GRUPOS = "grupos"
    const val HORARIO = "horario" // subcolección de grupos/{grupoId}
    const val PERIODOS = "periodos"
    const val REGISTROS = "registros" // subcolección de periodos/{periodoId}
    const val SALIDAS_DE_GRUPO = "salidasDeGrupo" // subcolección de periodos/{periodoId}
    const val ESPECIALIDADES = "especialidades"
    const val ANUNCIOS = "anuncios"
    const val EVENTOS = "eventos"
    const val CONFIGURACION = "configuracion"
    const val ROLES = "roles"
}

/** Valores del campo `estado` de alumnos/{noControl}. Un alumno nunca se borra: cambia de estado. */
object EstadoAlumno {
    const val INSCRITO = "inscrito"
    const val BAJA_TEMPORAL = "baja_temporal"
    const val BAJA_DEFINITIVA = "baja_definitiva"
    const val EGRESADO = "egresado"
}

/** Valores del campo `tipo` de alumnos/{noControl}/estatus. */
object TipoEstatus {
    const val FALTA = "falta"
    const val REPORTE = "reporte"
    const val LLAMADA_ATENCION = "llamada_atencion"
    const val CASTIGO = "castigo"
    const val PASE_SALUD = "pase_salud"
    const val ALTA_PRACTICAS = "alta_practicas"
}

/** Valores del campo `turno` de alumnos y grupos. */
object Turno {
    const val MATUTINO = "matutino"
    const val VESPERTINO = "vespertino"
}

/** Valores del campo `rol` de usuarios/{uid}. */
object Rol {
    const val ALUMNO = "alumno"
    const val PADRE = "padre"
    const val PREFECTO = "prefecto"
    const val PROFESOR = "profesor"
    const val ENFERMERIA = "enfermeria"
    const val CONTROL_ESCOLAR = "control_escolar"
}

/**
 * El No. de control del alumno: 14 dígitos, como en la credencial del plantel. Es el id de
 * alumnos/{noControl}, con él inicia sesión el alumno y es lo que lleva su código QR.
 */
object NoControl {
    const val DIGITOS = 14

    /** Lo que va antes del número dentro del QR, para reconocer los QR de CetisAsiste. */
    const val PREFIJO_QR = "CETIS61:"

    fun esValido(texto: String): Boolean = texto.length == DIGITOS && texto.all { it in '0'..'9' }

    /** Texto que lleva el QR del alumno: "CETIS61:" + su No. de control. */
    fun contenidoQr(noControl: String): String = if (noControl.isBlank()) "" else PREFIJO_QR + noControl

    /** Del texto leído en un QR saca el No. de control, o null si no es un QR de CetisAsiste. */
    fun desdeQr(texto: String): String? =
        texto.trim().removePrefix(PREFIJO_QR).takeIf { texto.trim().startsWith(PREFIJO_QR) && esValido(it) }
}

/** Valores del campo `medio` de los registros: cómo se identificó al alumno. */
object Medio {
    const val QR = "qr"
    const val NO_CONTROL = "no_control"
}

/** Valores del campo `tipo` de los registros y de `ultimoRegistro`. */
object TipoRegistro {
    const val ENTRADA = "entrada"
    const val SALIDA = "salida"
}

/** Valores del campo `ubicacion` de alumnos/{noControl}. */
object Ubicacion {
    const val FUERA = "fuera"
    const val DENTRO = "dentro"
    const val SALIDA_AUTORIZADA = "salida_autorizada"
}

/** Valores del campo `alerta` de los registros (null = sin alerta). Ver docs/BASE_DE_DATOS.md §5.3. */
object Alerta {
    const val NO_DADO_DE_ALTA = "no_dado_de_alta"
    const val INACTIVO = "inactivo"
    const val YA_INGRESO = "ya_ingreso"
    const val NO_HA_INGRESADO = "no_ha_ingresado"
    const val CASTIGO = "castigo"
    const val SALIDA_FUERA_DE_HORARIO = "salida_fuera_de_horario"
}

/** usuarios/{uid} — una cuenta por persona. El id es el uid de Firebase Authentication. */
data class UsuarioDoc(
    var nombre: String = "",
    /** Con lo que inicia sesión: el No. de control (alumno) o su nombre de usuario (personal y padres). */
    var usuario: String = "",
    /** Correo de la cuenta de Authentication: usuario@cetis61.mx */
    var correo: String = "",
    var rol: String = "",
    /** Si es false, la cuenta no puede usar la app (las reglas le niegan todo). */
    var activo: Boolean = false,
    var debeCambiarContrasena: Boolean = false,
    var contrasenaVence: Timestamp? = null,
    /** Solo alumno: su No. de control (id de alumnos/{noControl}). */
    var noControl: String = "",
    /** Solo padre: números de control de sus hijos. */
    var hijos: List<String> = emptyList(),
    /** Solo profesor: ids de los grupos donde da clase. */
    var grupos: List<String> = emptyList(),
    var fotoUrl: String = "",
    /** Tokens de Firebase Cloud Messaging para avisos push (futuro). */
    var tokensFcm: List<String> = emptyList(),
    var creadoEn: Timestamp? = null,
    var actualizadoEn: Timestamp? = null,
)

/**
 * alumnos/{noControl} — la ficha del alumno. El id es el No. de control.
 * @IgnoreExtraProperties: si un documento viejo trae campos que ya no se usan (`codigoQr`,
 * `matricula`), se ignoran sin avisos.
 */
@IgnoreExtraProperties
data class AlumnoDoc(
    var noControl: String = "",
    /** Nombre completo, tal como se muestra ("Juan Pérez Rodríguez"). */
    var nombre: String = "",
    var apellidoPaterno: String = "",
    var apellidoMaterno: String = "",
    var nombres: String = "",
    /** Apellidos y nombres en minúsculas y sin acentos ("perez rodriguez juan"), para buscar. */
    var nombreBusqueda: String = "",
    /** inscrito, baja_temporal, baja_definitiva o egresado (ver EstadoAlumno). */
    var estado: String = EstadoAlumno.INSCRITO,
    /** Semestre en curso del plantel (periodos/{periodoId}). */
    var periodoId: String = "",
    var semestre: Long = 0,
    /** Vacío mientras no existan especialidades (tronco común). */
    var especialidadId: String = "",
    /** Años de ingreso y egreso ("2026-2029"). */
    var generacion: String = "",
    /** uid de su cuenta en usuarios/. */
    var usuarioId: String = "",
    /** uid de las cuentas de sus padres o tutores. */
    var padres: List<String> = emptyList(),
    var grupoId: String = "",
    /** Nombre corto del grupo, tal como se muestra ("102"). */
    var grupo: String = "",
    var turno: String = "",
    var correo: String = "",
    /** Ciclo escolar de la credencial ("Ciclo 2026–2027"). */
    var vigencia: String = "",
    var fotoUrl: String = "",
    var activoPorPago: Boolean = true,
    var activoPorSalud: Boolean = true,
    var enPracticas: Boolean = false,
    /** Dónde está: "fuera", "dentro" o "salida_autorizada". */
    var ubicacion: String = "fuera",
    var ultimoRegistro: UltimoRegistro? = null,
    var creadoEn: Timestamp? = null,
    var actualizadoEn: Timestamp? = null,
)

/** Campo `ultimoRegistro` dentro de alumnos/{noControl}. */
data class UltimoRegistro(
    /** "entrada" o "salida". */
    var tipo: String = "",
    var fechaHora: Timestamp? = null,
)

/** alumnos/{noControl}/estatus/{id} — castigos, faltas, reportes, pases… */
data class EstatusDoc(
    /** castigo, falta, reporte, llamada_atencion, pase_salud o alta_practicas. */
    var tipo: String = "",
    var descripcion: String = "",
    var fecha: Timestamp? = null,
    /** Hasta cuándo aplica (null = no vence o no aplica). */
    var vigenteHasta: Timestamp? = null,
    var asignadoPor: String = "",
    var asignadoPorNombre: String = "",
)

/** alumnos/{noControl}/cambiosDeEstado/{id} — historial de activaciones por pago o salud. */
data class CambioDeEstadoDoc(
    /** "pago" o "salud". */
    var campo: String = "",
    /** true = activado, false = desactivado. */
    var valor: Boolean = true,
    var motivo: String = "",
    var fecha: Timestamp? = null,
    var por: String = "",
    var porNombre: String = "",
)

/**
 * grupos/{periodoId}_{nombre} — por ejemplo grupos/2026-2027-1_102.
 * Nombres: matutino del x00 al x49 (101, 102…), vespertino del x50 en adelante (151…).
 */
data class GrupoDoc(
    var nombre: String = "",
    var periodoId: String = "",
    var turno: String = "",
    var semestre: Long = 0,
    var especialidadId: String = "",
    /** uid del profesor tutor del grupo (vacío si no hay). */
    var tutorId: String = "",
    /** false cuando el semestre del grupo ya terminó. */
    var activo: Boolean = true,
    var cicloEscolar: String = "",
    /** uid de los profesores que dan clase en el grupo. */
    var profesores: List<String> = emptyList(),
    var actualizadoEn: Timestamp? = null,
)

/** grupos/{grupoId}/horario/{dia} — un documento por día (lunes … viernes). */
data class HorarioDoc(
    var dia: String = "",
    /** 1 = lunes … 5 = viernes. */
    var orden: Long = 0,
    var clases: List<ClaseDoc> = emptyList(),
    var actualizadoEn: Timestamp? = null,
)

/** Una clase dentro de HorarioDoc.clases. */
data class ClaseDoc(
    var materia: String = "",
    /** "07:00" (24 horas). */
    var horaInicio: String = "",
    var horaFin: String = "",
    var profesorId: String = "",
    var profesor: String = "",
    var aula: String = "",
    /** true en la última clase del turno: ahí el profesor da salida al grupo. */
    var ultimaDelTurno: Boolean = false,
)

/** registros/{id} — cada escaneo de entrada o salida. Nadie lo edita ni lo borra. */
data class RegistroDoc(
    /** No. de control del alumno (o lo que se tecleó, si la alerta es no_dado_de_alta). */
    var alumnoId: String = "",
    var alumnoNombre: String = "",
    var grupoId: String = "",
    /** "entrada" o "salida". */
    var tipo: String = "",
    var fechaHora: Timestamp? = null,
    /** Día en texto "2026-10-05", para buscar por fecha sin cálculos. */
    var fecha: String = "",
    /** "qr" o "no_control". */
    var medio: String = "",
    var registradoPor: String = "",
    var registradoPorNombre: String = "",
    /** null si todo estuvo bien; si no, el tipo de alerta (ver docs/BASE_DE_DATOS.md). */
    var alerta: String? = null,
)

/** salidasDeGrupo/{id} — el profesor libera al grupo en la última clase. */
data class SalidaDeGrupoDoc(
    var grupoId: String = "",
    var grupo: String = "",
    var fecha: String = "",
    var fechaHora: Timestamp? = null,
    var autorizadaPor: String = "",
    var autorizadaPorNombre: String = "",
    var materia: String = "",
)

/** anuncios/{id} — boletín de noticias. */
data class AnuncioDoc(
    /** Quién lo publica, se muestra como etiqueta: "Prefectura", "Control escolar"… */
    var area: String = "",
    var fecha: Timestamp? = null,
    var titulo: String = "",
    var texto: String = "",
    /** true = aparece como "Información importante" en Inicio. */
    var destacado: Boolean = false,
    /** Roles que lo ven: ["alumno", "padre", …]. */
    var dirigidoA: List<String> = emptyList(),
    var publicadoPor: String = "",
    var publicadoPorNombre: String = "",
)

/** eventos/{id} — calendario escolar. */
data class EventoDoc(
    var titulo: String = "",
    var inicio: Timestamp? = null,
    var fin: Timestamp? = null,
    var lugar: String = "",
    var area: String = "",
    var detalle: String = "",
)

/**
 * usuarios/{uid}/notificaciones/{id} — avisos personales (el "buzón" de cada cuenta, ADR-010).
 * Las reglas exigen exactamente estos campos: no agregues uno sin cambiar también
 * firebase/firestore.rules (función avisoValido). Se arman con Avisos.nuevo().
 */
data class NotificacionDoc(
    /** Ver TipoAviso: entrada, salida, alerta, estatus, estado, anuncio… */
    var tipo: String = "",
    var titulo: String = "",
    var mensaje: String = "",
    var fecha: Timestamp? = null,
    var leida: Boolean = false,
    /** No. de control del alumno al que se refiere (vacío si no trata de un alumno). */
    var alumnoId: String = "",
    /** uid de quien lo mandó. Las reglas exigen que sea el de la sesión. */
    var creadaPor: String = "",
    /** Desde cuándo se puede borrar (para limpiar la base; ver BASE_DE_DATOS §4.1). */
    var venceEn: Timestamp? = null,
)

/** Valores del campo `tipo` de usuarios/{uid}/notificaciones (los mismos que aceptan las reglas). */
object TipoAviso {
    /** El alumno entró sin alerta (prefecto). */
    const val ENTRADA = "entrada"
    /** El alumno salió sin alerta (prefecto). */
    const val SALIDA = "salida"
    /** Al escanear al alumno hubo una alerta (prefecto). */
    const val ALERTA = "alerta"
    /** Le pusieron un estatus: falta, reporte, castigo… (profesor) o pase de salud (enfermería). */
    const val ESTATUS = "estatus"
    /** Lo activaron o desactivaron por pago (Control Escolar) o por salud (Enfermería). */
    const val ESTADO = "estado"
    const val ANUNCIO = "anuncio"
    const val RECORDATORIO = "recordatorio"
    const val CREDENCIAL = "credencial"
    const val SALIDA_GRUPO = "salida_grupo"
}

/**
 * roles/{rol} — perfil de un tipo de usuario (alumno, padre, prefecto, profesor,
 * enfermeria, control_escolar). El id es el mismo valor que usuarios/{uid}.rol.
 * Copia en firebase/roles.json.
 */
data class RolDoc(
    var orden: Long = 0,
    /** "Padre de familia" */
    var nombre: String = "",
    /** Texto que se muestra en tarjetas y credencial ("Estudiante"). */
    var etiqueta: String = "",
    var descripcion: String = "",
    /** "android" y/o "web". */
    var plataformas: List<String> = emptyList(),
    /** false mientras el plantel no confirme la plataforma (Enfermería). */
    var plataformaConfirmada: Boolean = false,
    /** Con qué inicia sesión: "no_control" o "usuario". */
    var inicioDeSesion: String = "",
    /** Campos extra de usuarios/{uid} que usa este rol: noControl, hijos o grupos. */
    var camposDelUsuario: List<String> = emptyList(),
    var cambioDeContrasenaObligatorio: Boolean = false,
    /** Nombres de las pantallas del rol (documento "Pantallas por rol"). */
    var pantallas: List<String> = emptyList(),
    /** Qué puede hacer: "ver_mi_qr", "escanear_qr", "activar_por_pago"… */
    var permisos: List<String> = emptyList(),
    var activo: Boolean = true,
)

/**
 * periodos/{periodoId} — un semestre. Ej. "2026-2027-1" (agosto–enero) y "2026-2027-2"
 * (febrero–julio). Sus subcolecciones `registros` y `salidasDeGrupo` empiezan vacías cada semestre.
 */
data class PeriodoDoc(
    var nombre: String = "",
    /** "AAAA-MM-DD" */
    var inicio: String = "",
    var fin: String = "",
    /** true solo en el semestre en curso. */
    var activo: Boolean = false,
)

/** especialidades/{id} — catálogo de carreras técnicas (todavía sin datos). */
data class EspecialidadDoc(
    var nombre: String = "",
    var clave: String = "",
    var activa: Boolean = true,
)

/** alumnos/{noControl}/inscripciones/{periodoId} — historial: un documento por semestre cursado. */
data class InscripcionDoc(
    var grupoId: String = "",
    var grupo: String = "",
    var semestre: Long = 0,
    var turno: String = "",
    var especialidadId: String = "",
    /** "AAAA-MM-DD" */
    var fechaAlta: String = "",
    /** cursando, acreditado, baja… */
    var estado: String = "",
    var por: String = "",
    var porNombre: String = "",
)

/** alumnos/{noControl}/privado/datos — solo Control Escolar. */
data class DatosPrivadosDoc(
    var curp: String = "",
    /** "AAAA-MM-DD" */
    var fechaNacimiento: String = "",
    var domicilio: String = "",
    var telefono: String = "",
    var correoPersonal: String = "",
    var tutores: List<TutorDoc> = emptyList(),
)

/** Un padre, madre o tutor dentro de DatosPrivadosDoc.tutores. */
data class TutorDoc(
    var nombre: String = "",
    var parentesco: String = "",
    var telefono: String = "",
)

/** alumnos/{noControl}/privado/salud — Control Escolar y Enfermería. */
data class DatosSaludDoc(
    var tipoSangre: String = "",
    var alergias: List<String> = emptyList(),
    var enfermedades: List<String> = emptyList(),
    var medicamentos: List<String> = emptyList(),
    /** Número de seguro social (IMSS). */
    var nss: String = "",
    var contactoEmergencia: String = "",
    var telefonoEmergencia: String = "",
)

/** configuracion/plantel — ajustes generales del plantel. */
data class ConfiguracionDoc(
    var nombre: String = "",
    /** Semestre en curso, por ejemplo "2026-2027-1" (id en periodos). */
    var periodoActual: String = "",
    var ciudad: String = "",
    var cicloEscolar: String = "",
    var turnos: List<String> = emptyList(),
    var horaCierrePuerta: String = "",
    var diasVenceContrasena: Long = 90,
    var dominioCorreo: String = "",
    /**
     * Estatus que se le avisan al padre cuando el profesor los pone (ADR-010). Si el campo no
     * existe en Firestore, se usa Avisos.ESTATUS_POR_OMISION; una lista vacía no avisa ninguno.
     */
    var avisarAlPadreEstatus: List<String> = Avisos.ESTATUS_POR_OMISION,
    /**
     * Alertas del escaneo que se le avisan al padre (ADR-010). Las entradas y salidas sin alerta
     * siempre se avisan. Si el campo no existe, se usa Avisos.ALERTAS_POR_OMISION.
     */
    var avisarAlPadreAlertas: List<String> = Avisos.ALERTAS_POR_OMISION,
    var actualizadoEn: Timestamp? = null,
)
