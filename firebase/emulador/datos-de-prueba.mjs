// Carga DATOS DE PRUEBA en el EMULADOR de Firebase (nunca en la base real del plantel).
//
// Sirve para probar la web (y las reglas) en tu computadora. Todo dice "Prueba".
// Cómo usarlo: ver web/README.md ("Probar en tu computadora"). En corto:
//   1. En la carpeta firebase/pruebas-reglas:  npx firebase emulators:start --project demo-cetisasiste --config ../firebase.json
//   2. En otra terminal, en la carpeta firebase:  node emulador/datos-de-prueba.mjs
//   3. Abre http://127.0.0.1:5000 y entra con una de las cuentas de abajo.
//
// Cuentas (contraseña de todas: prueba1234):
//   control.prueba     Control Escolar
//   enfermeria.prueba  Enfermería
//   cambio.prueba      Enfermería que debe cambiar su contraseña al entrar
//   padre.prueba       Padre (no puede entrar a la web: usa la app)

const PROYECTO = 'demo-cetisasiste';
const FIRESTORE = `http://127.0.0.1:8080/v1/projects/${PROYECTO}/databases/(default)/documents`;
const AUTH = `http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/projects/${PROYECTO}`;
const CONTRASENA = 'prueba1234';
// "owner" le dice al emulador que es un administrador: se salta las reglas (solo en el emulador).
const COMO_ADMINISTRADOR = { Authorization: 'Bearer owner', 'Content-Type': 'application/json' };

const PERIODO = '2026-2027-1';
const G102 = `${PERIODO}_102`;
const G151 = `${PERIODO}_151`;
const JUAN = '00000000000001';
const ANA = '00000000000002';
const LUIS = '00000000000003';

const hoy = new Date();
const dia = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
const aLas = (horas, minutos) => new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate(), horas, minutos);

/** Un valor de JavaScript → el formato de la API REST de Firestore. */
function valor(v) {
  if (v === null || v === undefined) return { nullValue: null };
  if (v instanceof Date) return { timestampValue: v.toISOString() };
  if (Array.isArray(v)) return { arrayValue: { values: v.map(valor) } };
  if (typeof v === 'boolean') return { booleanValue: v };
  if (typeof v === 'number') return Number.isInteger(v) ? { integerValue: String(v) } : { doubleValue: v };
  if (typeof v === 'object') return { mapValue: { fields: campos(v) } };
  return { stringValue: String(v) };
}
const campos = (objeto) => Object.fromEntries(Object.entries(objeto).map(([k, v]) => [k, valor(v)]));

async function guardar(ruta, datos) {
  const respuesta = await fetch(`${FIRESTORE}/${ruta}`, { method: 'PATCH', headers: COMO_ADMINISTRADOR, body: JSON.stringify({ fields: campos(datos) }) });
  if (!respuesta.ok) throw new Error(`${ruta}: ${respuesta.status} ${await respuesta.text()}`);
}

async function crearCuenta(uid, usuario) {
  const respuesta = await fetch(`${AUTH}/accounts`, {
    method: 'POST', headers: COMO_ADMINISTRADOR,
    body: JSON.stringify({ localId: uid, email: `${usuario}@cetis61.mx`, password: CONTRASENA }),
  });
  if (!respuesta.ok) throw new Error(`cuenta ${usuario}: ${respuesta.status} ${await respuesta.text()}`);
}

async function borrarTodo() {
  await fetch(`http://127.0.0.1:8080/emulator/v1/projects/${PROYECTO}/databases/(default)/documents`, { method: 'DELETE' });
  await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${PROYECTO}/accounts`, { method: 'DELETE' });
}

const cuentas = [
  ['uid-control', 'control.prueba', { nombre: 'Control Escolar Prueba', rol: 'control_escolar' }],
  ['uid-enfermeria', 'enfermeria.prueba', { nombre: 'Enfermera Prueba', rol: 'enfermeria' }],
  ['uid-cambio', 'cambio.prueba', { nombre: 'Enfermera Nueva Prueba', rol: 'enfermeria', debeCambiarContrasena: true }],
  ['uid-padre', 'padre.prueba', { nombre: 'Padre de Prueba', rol: 'padre', hijos: [JUAN] }],
  ['uid-prefecto', 'prefecto.prueba', { nombre: 'Prefecto de Prueba', rol: 'prefecto' }],
  ['uid-profesor', 'profesor.prueba', { nombre: 'Profesor de Prueba', rol: 'profesor', grupos: [G102] }],
];

function alumno(noControl, paterno, materno, nombres, grupoId, grupo, turno, extra = {}) {
  const nombre = `${nombres} ${paterno} ${materno}`;
  const busqueda = `${paterno} ${materno} ${nombres}`.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  return {
    noControl, nombre, apellidoPaterno: paterno, apellidoMaterno: materno, nombres, nombreBusqueda: busqueda,
    estado: 'inscrito', periodoId: PERIODO, semestre: 1, especialidadId: '', generacion: '2026-2029',
    usuarioId: '', padres: [], grupoId, grupo, turno, correo: `${noControl}@cetis61.mx`, vigencia: 'Ciclo 2026–2027',
    activoPorPago: true, activoPorSalud: true, enPracticas: false, ubicacion: 'fuera', ultimoRegistro: null, ...extra,
  };
}

function registro(alumnoId, alumnoNombre, grupoId, tipo, fechaHora, alerta) {
  return {
    alumnoId, alumnoNombre, grupoId, tipo, fechaHora, fecha: dia, medio: 'qr',
    registradoPor: 'uid-prefecto', registradoPorNombre: 'Prefecto de Prueba', alerta,
  };
}

await borrarTodo();
for (const [uid, usuario, datos] of cuentas) {
  await crearCuenta(uid, usuario);
  await guardar(`usuarios/${uid}`, { usuario, correo: `${usuario}@cetis61.mx`, activo: true, debeCambiarContrasena: false, ...datos });
}
await guardar('configuracion/plantel', {
  nombre: 'CETis 61', ciudad: 'Cd. Juárez', cicloEscolar: 'Ciclo 2026–2027', periodoActual: PERIODO,
  turnos: ['matutino', 'vespertino'], horaCierrePuerta: '07:15', diasVenceContrasena: 90, dominioCorreo: 'cetis61.mx',
});
await guardar(`periodos/${PERIODO}`, { nombre: 'Agosto 2026 – enero 2027', inicio: '2026-08-24', fin: '2027-01-22', activo: true });
await guardar(`grupos/${G102}`, { nombre: '102', periodoId: PERIODO, turno: 'matutino', semestre: 1, activo: true, profesores: ['uid-profesor'] });
await guardar(`grupos/${G151}`, { nombre: '151', periodoId: PERIODO, turno: 'vespertino', semestre: 1, activo: true, profesores: [] });
await guardar(`alumnos/${JUAN}`, alumno(JUAN, 'Pérez', 'Prueba', 'Juan', G102, '102', 'matutino', {
  padres: ['uid-padre'], ubicacion: 'dentro', ultimoRegistro: { tipo: 'entrada', fechaHora: aLas(7, 5) },
}));
await guardar(`alumnos/${ANA}`, alumno(ANA, 'López', 'Prueba', 'Ana', G102, '102', 'matutino', { activoPorPago: false }));
await guardar(`alumnos/${LUIS}`, alumno(LUIS, 'Núñez', 'Prueba', 'Luis', G151, '151', 'vespertino'));
await guardar(`alumnos/${JUAN}/privado/salud`, { tipoSangre: 'O+', alergias: [], enfermedades: [], medicamentos: [], nss: '', contactoEmergencia: 'Padre de Prueba', telefonoEmergencia: '6560000000' });
await guardar(`alumnos/${JUAN}/privado/datos`, { curp: 'XEXX090315HCHXXX00', fechaNacimiento: '2009-03-15', domicilio: 'Calle Ficticia 123', telefono: '6560000000', correoPersonal: '', tutores: [{ nombre: 'Padre de Prueba', parentesco: 'Padre', telefono: '6560000000' }] });
await guardar(`periodos/${PERIODO}/registros/r1`, registro(JUAN, 'Juan Pérez Prueba', G102, 'entrada', aLas(7, 5), null));
await guardar(`periodos/${PERIODO}/registros/r2`, registro(ANA, 'Ana López Prueba', G102, 'entrada', aLas(7, 9), 'inactivo'));
await guardar(`periodos/${PERIODO}/registros/r3`, registro('99999999999999', '', '', 'entrada', aLas(7, 12), 'no_dado_de_alta'));
await guardar('usuarios/uid-control/notificaciones/n1', {
  tipo: 'anuncio', titulo: 'Bienvenida a la web', mensaje: 'Este es un aviso de prueba.', fecha: aLas(7, 0),
  leida: false, alumnoId: '', creadaPor: 'uid-control', venceEn: null,
});
console.log(`Listo: ${cuentas.length} cuentas (contraseña ${CONTRASENA}), 3 alumnos, 2 grupos y 3 registros de hoy (${dia}).`);
