// TEXTOS, FECHAS Y NOMBRES que muestra la web. Versión web de lo que hace
// app/.../datos/Formatos.kt. Aquí no se usa Firebase. Pruebas: web/pruebas/formatos.test.js.

/** No. de control: 14 dígitos, como en la credencial (NoControl.esValido en la app). */
export const DIGITOS_NO_CONTROL = 14;

export function esNoControl(texto) {
  return typeof texto === 'string' && texto.length === DIGITOS_NO_CONTROL && /^[0-9]+$/.test(texto);
}

/**
 * "usuario" → "usuario@cetis61.mx". Si ya trae @, se deja igual (la cuenta real de Control
 * Escolar es un correo completo). Igual que CetisRepositorio.correoDeAcceso.
 */
export function correoDeAcceso(usuario, dominio) {
  const limpio = String(usuario).trim().toLowerCase();
  return limpio.includes('@') ? limpio : `${limpio}@${dominio}`;
}

/**
 * Minúsculas y sin acentos: "Pérez Rodríguez" → "perez rodriguez". Es como se guarda
 * `alumnos.nombreBusqueda`, para poder buscar por nombre.
 */
export function normalizar(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

const dosDigitos = (n) => String(n).padStart(2, '0');

/** "2026-10-20": el día en texto, como el campo `fecha` de los registros (hora de esta computadora). */
export function fechaEnTexto(fecha = new Date()) {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;
}

/** Acepta Date o un Timestamp de Firestore (que tiene toDate()). */
export function aFecha(valor) {
  if (!valor) return null;
  if (valor instanceof Date) return valor;
  if (typeof valor.toDate === 'function') return valor.toDate();
  return null;
}

/** "7:05 am", "12:30 pm" (como Formatos.hora en la app). */
export function hora(valor) {
  const fecha = aFecha(valor);
  if (!fecha) return '';
  const h = fecha.getHours();
  const sufijo = h < 12 ? 'am' : 'pm';
  const doce = h % 12 === 0 ? 12 : h % 12;
  return `${doce}:${dosDigitos(fecha.getMinutes())} ${sufijo}`;
}

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/** "20 oct 2026, 7:05 am" */
export function fechaYHora(valor) {
  const fecha = aFecha(valor);
  if (!fecha) return '';
  return `${fecha.getDate()} ${MESES[fecha.getMonth()]} ${fecha.getFullYear()}, ${hora(fecha)}`;
}

/** "Hoy, 7:05 am" o "18 oct 2026, 7:05 am". */
export function cuando(valor, ahora = new Date()) {
  const fecha = aFecha(valor);
  if (!fecha) return '';
  return fechaEnTexto(fecha) === fechaEnTexto(ahora) ? `Hoy, ${hora(fecha)}` : fechaYHora(fecha);
}

/** Nombre del grupo a partir de su id: "2026-2027-1_102" → "102". */
export function grupoDeId(grupoId) {
  const texto = String(grupoId ?? '');
  return texto.includes('_') ? texto.slice(texto.lastIndexOf('_') + 1) : texto;
}

export const NOMBRES_DE_ESTATUS = Object.freeze({
  falta: 'Falta',
  reporte: 'Reporte',
  llamada_atencion: 'Llamada de atención',
  castigo: 'Castigo',
  pase_salud: 'Pase de salud',
  alta_practicas: 'Alta para prácticas',
});

/** Los mismos títulos que ve el prefecto en la app (strings_prefecto.xml). */
export const NOMBRES_DE_ALERTA = Object.freeze({
  no_dado_de_alta: 'Alumno no dado de alta',
  inactivo: 'Alumno inactivo',
  ya_ingreso: 'El alumno ya ingresó',
  no_ha_ingresado: 'El alumno no ha ingresado',
  castigo: 'Alumno con castigo disciplinario',
  salida_fuera_de_horario: 'Salida fuera de horario',
});

export const NOMBRES_DE_ESTADO_ALUMNO = Object.freeze({
  inscrito: 'Inscrito',
  baja_temporal: 'Baja temporal',
  baja_definitiva: 'Baja definitiva',
  egresado: 'Egresado',
});

export const NOMBRES_DE_UBICACION = Object.freeze({
  fuera: 'Fuera del plantel',
  dentro: 'Dentro del plantel',
  salida_autorizada: 'Dentro, con salida autorizada',
});

export function nombreDeTurno(turno) {
  return turno === 'matutino' ? 'Matutino' : turno === 'vespertino' ? 'Vespertino' : '';
}

/**
 * Estado que se ve en la etiqueta de color, en el mismo orden que la app del profesor
 * (ProfesorVistas.estadoVisible): pago, salud, prácticas, activo.
 */
export function estadoVisible(alumno) {
  if (alumno?.activoPorPago === false) return { texto: 'Inactivo por pago', peligro: true };
  if (alumno?.activoPorSalud === false) return { texto: 'Inactivo por salud', peligro: true };
  if (alumno?.enPracticas) return { texto: 'En prácticas', peligro: false };
  return { texto: 'Activo', peligro: false };
}
