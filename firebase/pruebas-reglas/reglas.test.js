// Pruebas de las reglas de seguridad (firebase/firestore.rules) con el emulador de Firestore.
// Cómo correrlas: ver README.md de esta carpeta. No tocan la base real: todo pasa en el emulador.
//
// Cada prueba dice en español qué se espera: "puede" (assertSucceeds) o "no puede" (assertFails).

import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  Timestamp,
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

// ---------- Datos de las pruebas (solo existen en el emulador) ----------

const PERIODO = '2026-2027-1';
const GRUPO_1 = `${PERIODO}_102`;
const GRUPO_2 = `${PERIODO}_151`;
const ALUMNO_1 = '00000000000001'; // grupo 102, hijo de "padre"
const ALUMNO_2 = '00000000000002'; // grupo 151, hijo de "otroPadre"

const usuarios = {
  alumno: { rol: 'alumno', activo: true, noControl: ALUMNO_1 },
  padre: { rol: 'padre', activo: true, hijos: [ALUMNO_1] },
  otroPadre: { rol: 'padre', activo: true, hijos: [ALUMNO_2] },
  prefecto: { rol: 'prefecto', activo: true },
  prefectoInactivo: { rol: 'prefecto', activo: false },
  profesor: { rol: 'profesor', activo: true, grupos: [GRUPO_1] },
  enfermera: { rol: 'enfermeria', activo: true },
  control: { rol: 'control_escolar', activo: true },
};

const alumnos = {
  [ALUMNO_1]: { noControl: ALUMNO_1, nombre: 'Juan Prueba', grupoId: GRUPO_1, padres: ['padre'], estado: 'inscrito',
    activoPorPago: true, activoPorSalud: true, ubicacion: 'fuera' },
  [ALUMNO_2]: { noControl: ALUMNO_2, nombre: 'Ana Prueba', grupoId: GRUPO_2, padres: ['otroPadre'], estado: 'inscrito',
    activoPorPago: true, activoPorSalud: true, ubicacion: 'fuera' },
};

/** Un aviso con la forma correcta, firmado por [creadaPor]. */
function aviso(creadaPor, cambios = {}) {
  return {
    tipo: 'entrada',
    titulo: 'Entrada registrada',
    mensaje: 'Juan Prueba entró al plantel a las 7:05 am.',
    fecha: Timestamp.now(),
    leida: false,
    alumnoId: ALUMNO_1,
    creadaPor,
    venceEn: null,
    ...cambios,
  };
}

let entorno;

/** La base de datos vista por la cuenta [uid] (con sesión iniciada). */
const como = (uid) => entorno.authenticatedContext(uid).firestore();

before(async () => {
  entorno = await initializeTestEnvironment({
    projectId: 'demo-cetisasiste',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await entorno?.cleanup();
});

beforeEach(async () => {
  await entorno.clearFirestore();
  await entorno.withSecurityRulesDisabled(async (contexto) => {
    const db = contexto.firestore();
    for (const [uid, datos] of Object.entries(usuarios)) await setDoc(doc(db, 'usuarios', uid), datos);
    for (const [id, datos] of Object.entries(alumnos)) await setDoc(doc(db, 'alumnos', id), datos);
    await setDoc(doc(db, 'configuracion', 'plantel'), { periodoActual: PERIODO });
    await setDoc(doc(db, 'periodos', PERIODO), { activo: true });
    await setDoc(doc(db, 'usuarios', 'padre', 'notificaciones', 'n1'), aviso('prefecto'));
  });
});

// ---------- Escaneo del prefecto ----------

describe('Escaneo del prefecto', () => {
  test('puede guardar el escaneo completo en un lote: registro, ubicación y aviso al padre', async () => {
    const db = como('prefecto');
    const lote = writeBatch(db);
    lote.set(doc(collection(db, 'periodos', PERIODO, 'registros')), {
      alumnoId: ALUMNO_1, tipo: 'entrada', fechaHora: Timestamp.now(), fecha: '2026-10-20', alerta: null, registradoPor: 'prefecto',
    });
    lote.update(doc(db, 'alumnos', ALUMNO_1), { ubicacion: 'dentro', ultimoRegistro: { tipo: 'entrada' }, actualizadoEn: Timestamp.now() });
    lote.set(doc(collection(db, 'usuarios', 'padre', 'notificaciones')), aviso('prefecto'));
    await assertSucceeds(lote.commit());
  });

  test('puede avisar al padre de una alerta (tipo alerta)', async () => {
    await assertSucceeds(setDoc(doc(collection(como('prefecto'), 'usuarios', 'padre', 'notificaciones')),
      aviso('prefecto', { tipo: 'alerta', titulo: 'Salida fuera de horario' })));
  });

  test('no puede avisar a quien no es padre del alumno', async () => {
    await assertFails(setDoc(doc(collection(como('prefecto'), 'usuarios', 'otroPadre', 'notificaciones')), aviso('prefecto')));
  });

  test('no puede mandar un aviso firmado con otro uid', async () => {
    await assertFails(setDoc(doc(collection(como('prefecto'), 'usuarios', 'padre', 'notificaciones')), aviso('control')));
  });

  test('no puede mandar un aviso ya leído, de tipo desconocido o con campos de más', async () => {
    const buzon = collection(como('prefecto'), 'usuarios', 'padre', 'notificaciones');
    await assertFails(setDoc(doc(buzon), aviso('prefecto', { leida: true })));
    await assertFails(setDoc(doc(buzon), aviso('prefecto', { tipo: 'chisme' })));
    await assertFails(setDoc(doc(buzon), aviso('prefecto', { extra: 'x' })));
    await assertFails(setDoc(doc(buzon), aviso('prefecto', { titulo: '' })));
  });

  test('con la cuenta desactivada no puede avisar', async () => {
    await assertFails(setDoc(doc(collection(como('prefectoInactivo'), 'usuarios', 'padre', 'notificaciones')),
      aviso('prefectoInactivo')));
  });

  test('puede ver los escaneos de hoy y solo las alertas de hoy', async () => {
    const registros = collection(como('prefecto'), 'periodos', PERIODO, 'registros');
    await assertSucceeds(getDocs(query(registros, where('fecha', '==', '2026-10-20'), orderBy('fechaHora', 'desc'), limit(10))));
    await assertSucceeds(getDocs(query(registros, where('fecha', '==', '2026-10-20'),
      where('alerta', 'in', ['inactivo', 'castigo']), orderBy('fechaHora', 'desc'))));
  });
});

// ---------- Profesor ----------

describe('Estatus del profesor', () => {
  test('puede poner un estatus a un alumno de su grupo y avisar a su padre en el mismo lote', async () => {
    const db = como('profesor');
    const lote = writeBatch(db);
    lote.set(doc(collection(db, 'alumnos', ALUMNO_1, 'estatus')), {
      tipo: 'falta', descripcion: 'No entró a Química', fecha: Timestamp.now(), asignadoPor: 'profesor',
    });
    lote.set(doc(collection(db, 'usuarios', 'padre', 'notificaciones')),
      aviso('profesor', { tipo: 'estatus', titulo: 'Falta registrada' }));
    await assertSucceeds(lote.commit());
  });

  test('no puede poner un estatus firmado por otra persona', async () => {
    await assertFails(setDoc(doc(collection(como('profesor'), 'alumnos', ALUMNO_1, 'estatus')), {
      tipo: 'falta', descripcion: 'x', fecha: Timestamp.now(), asignadoPor: 'control',
    }));
  });

  test('no puede avisar al padre de un alumno que no es de sus grupos', async () => {
    await assertFails(setDoc(doc(collection(como('profesor'), 'usuarios', 'otroPadre', 'notificaciones')),
      aviso('profesor', { tipo: 'estatus', alumnoId: ALUMNO_2 })));
  });

  test('puede leer la configuración del plantel (qué estatus se avisan al padre)', async () => {
    await assertSucceeds(getDoc(doc(como('profesor'), 'configuracion', 'plantel')));
  });
});

// ---------- Enfermería (web) ----------

describe('Enfermería', () => {
  test('puede dar un pase de salud y avisar al padre', async () => {
    const db = como('enfermera');
    const lote = writeBatch(db);
    lote.set(doc(collection(db, 'alumnos', ALUMNO_1, 'estatus')), {
      tipo: 'pase_salud', descripcion: 'Dolor de cabeza', fecha: Timestamp.now(), asignadoPor: 'enfermera',
    });
    lote.set(doc(collection(db, 'usuarios', 'padre', 'notificaciones')),
      aviso('enfermera', { tipo: 'estatus', titulo: 'Pase de salida por salud' }));
    await assertSucceeds(lote.commit());
  });

  test('no puede poner otro tipo de estatus', async () => {
    await assertFails(setDoc(doc(collection(como('enfermera'), 'alumnos', ALUMNO_1, 'estatus')), {
      tipo: 'castigo', descripcion: 'x', fecha: Timestamp.now(), asignadoPor: 'enfermera',
    }));
  });

  test('puede desactivar por salud con su historial, pero no tocar el pago', async () => {
    const db = como('enfermera');
    const lote = writeBatch(db);
    lote.update(doc(db, 'alumnos', ALUMNO_1), { activoPorSalud: false, actualizadoEn: serverTimestamp() });
    lote.set(doc(collection(db, 'alumnos', ALUMNO_1, 'cambiosDeEstado')), {
      campo: 'salud', valor: false, motivo: 'Varicela', fecha: Timestamp.now(), por: 'enfermera',
    });
    await assertSucceeds(lote.commit());
    await assertFails(updateDoc(doc(db, 'alumnos', ALUMNO_1), { activoPorPago: false }));
    await assertFails(setDoc(doc(collection(db, 'alumnos', ALUMNO_1, 'cambiosDeEstado')), {
      campo: 'pago', valor: false, motivo: 'x', fecha: Timestamp.now(), por: 'enfermera',
    }));
  });

  test('puede leer los datos de salud, pero no los datos personales', async () => {
    const db = como('enfermera');
    await assertSucceeds(getDoc(doc(db, 'alumnos', ALUMNO_1, 'privado', 'salud')));
    await assertFails(getDoc(doc(db, 'alumnos', ALUMNO_1, 'privado', 'datos')));
  });
});

// ---------- Control Escolar (web) ----------

describe('Control Escolar', () => {
  test('puede desactivar por pago con su historial y avisar a cualquier cuenta', async () => {
    const db = como('control');
    const lote = writeBatch(db);
    lote.update(doc(db, 'alumnos', ALUMNO_1), { activoPorPago: false, actualizadoEn: serverTimestamp() });
    lote.set(doc(collection(db, 'alumnos', ALUMNO_1, 'cambiosDeEstado')), {
      campo: 'pago', valor: false, motivo: 'Sin pago de inscripción', fecha: Timestamp.now(), por: 'control',
    });
    lote.set(doc(collection(db, 'usuarios', 'padre', 'notificaciones')), aviso('control', { tipo: 'estado' }));
    lote.set(doc(collection(db, 'usuarios', 'prefecto', 'notificaciones')), aviso('control', { tipo: 'anuncio', alumnoId: '' }));
    await assertSucceeds(lote.commit());
  });

  test('no puede anotar un cambio de estado firmado por otra persona', async () => {
    await assertFails(setDoc(doc(collection(como('control'), 'alumnos', ALUMNO_1, 'cambiosDeEstado')), {
      campo: 'pago', valor: true, motivo: 'x', fecha: Timestamp.now(), por: 'enfermera',
    }));
  });
});

// ---------- Padre y alumno ----------

describe('Buzón del padre', () => {
  test('puede leer sus avisos sin leer (contador de la campana) y marcarlos como leídos', async () => {
    const db = como('padre');
    await assertSucceeds(getDocs(query(collection(db, 'usuarios', 'padre', 'notificaciones'), where('leida', '==', false), limit(10))));
    await assertSucceeds(updateDoc(doc(db, 'usuarios', 'padre', 'notificaciones', 'n1'), { leida: true }));
  });

  test('no puede cambiar el texto de un aviso, crear avisos ni leer los de otra cuenta', async () => {
    const db = como('padre');
    await assertFails(updateDoc(doc(db, 'usuarios', 'padre', 'notificaciones', 'n1'), { titulo: 'Otro' }));
    await assertFails(setDoc(doc(collection(db, 'usuarios', 'padre', 'notificaciones')), aviso('padre')));
    await assertFails(getDocs(collection(db, 'usuarios', 'otroPadre', 'notificaciones')));
  });

  test('puede ver las entradas y salidas de su hijo, pero no las de otro alumno', async () => {
    const registros = collection(como('padre'), 'periodos', PERIODO, 'registros');
    await assertSucceeds(getDocs(query(registros, where('alumnoId', '==', ALUMNO_1), orderBy('fechaHora', 'desc'))));
    await assertFails(getDocs(query(registros, where('alumnoId', '==', ALUMNO_2), orderBy('fechaHora', 'desc'))));
  });

  test('el alumno puede leer su propia ficha', async () => {
    await assertSucceeds(getDoc(doc(como('alumno'), 'alumnos', ALUMNO_1)));
    await assertFails(getDoc(doc(como('alumno'), 'alumnos', ALUMNO_2)));
  });
});
