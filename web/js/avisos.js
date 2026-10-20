// REGLAS DE LOS AVISOS (notificaciones dentro de la app). Por qué así: docs/adr/ADR-010.
//
// Es la versión web de app/.../datos/Avisos.kt: **si cambias algo aquí, cámbialo también
// allá** (y al revés). Aquí no se usa Firebase: solo se decide a quién se avisa y se arma el
// documento del buzón usuarios/{uid}/notificaciones. Pruebas: web/pruebas/avisos.test.js.

/** Valores del campo `tipo` de un aviso (los mismos que aceptan las reglas). */
export const TipoAviso = Object.freeze({
  ENTRADA: 'entrada',
  SALIDA: 'salida',
  ALERTA: 'alerta',
  ESTATUS: 'estatus',
  ESTADO: 'estado',
  ANUNCIO: 'anuncio',
  RECORDATORIO: 'recordatorio',
  CREDENCIAL: 'credencial',
  SALIDA_GRUPO: 'salida_grupo',
});

/** Estatus que se avisan al padre si configuracion/plantel no dice otra cosa. */
export const ESTATUS_POR_OMISION = Object.freeze(['falta', 'reporte', 'llamada_atencion', 'castigo']);

/** Alertas del escaneo que se avisan al padre si configuracion/plantel no dice otra cosa. */
export const ALERTAS_POR_OMISION = Object.freeze(['inactivo', 'salida_fuera_de_horario']);

/** Días que se guarda un aviso antes de poder borrarlo (campo `venceEn`). */
export const DIAS_DE_VIGENCIA = 60;

// Los mismos límites que firebase/firestore.rules (avisoValido).
const MAXIMO_TITULO = 120;
const MAXIMO_MENSAJE = 500;
const UN_DIA_MS = 24 * 60 * 60 * 1000;

/** Lista de estatus que se avisan al padre según la configuración del plantel. */
export function estatusQueSeAvisan(configuracion) {
  return Array.isArray(configuracion?.avisarAlPadreEstatus) ? configuracion.avisarAlPadreEstatus : ESTATUS_POR_OMISION;
}

/** Lista de alertas que se avisan al padre según la configuración del plantel. */
export function alertasQueSeAvisan(configuracion) {
  return Array.isArray(configuracion?.avisarAlPadreAlertas) ? configuracion.avisarAlPadreAlertas : ALERTAS_POR_OMISION;
}

/** ¿Se le avisa al padre de este estatus? El pase de salud se avisa siempre. */
export function alPadrePorEstatus(tipo, configuracion) {
  return tipo === 'pase_salud' || estatusQueSeAvisan(configuracion).includes(tipo);
}

/** Padres y tutores del alumno a quienes se les puede avisar (sin repetidos ni vacíos). */
export function destinatarios(alumno) {
  const padres = Array.isArray(alumno?.padres) ? alumno.padres : [];
  return [...new Set(padres.filter((uid) => typeof uid === 'string' && uid.trim() !== ''))];
}

/**
 * Un aviso nuevo, sin leer, firmado por `creadaPor` (el uid de la sesión, como piden las
 * reglas) y con su fecha de vencimiento. Las fechas van como Date: Firestore las guarda como
 * timestamp.
 */
export function nuevoAviso({ tipo, titulo, mensaje, alumnoId = '', creadaPor, ahora = new Date() }) {
  return {
    tipo,
    titulo: String(titulo).trim().slice(0, MAXIMO_TITULO),
    mensaje: String(mensaje).trim().slice(0, MAXIMO_MENSAJE),
    fecha: ahora,
    leida: false,
    alumnoId,
    creadaPor,
    venceEn: new Date(ahora.getTime() + DIAS_DE_VIGENCIA * UN_DIA_MS),
  };
}
