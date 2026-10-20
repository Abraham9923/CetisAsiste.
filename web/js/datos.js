// ÚNICO lugar de la web que habla con Firebase (como CetisRepositorio en la app Android).
//
// Las pantallas no consultan la base: le piden los datos a este archivo. Así, si cambia la
// base de datos, solo se toca aquí. La forma de cada documento está en
// docs/BASE_DE_DATOS.md; lo que las reglas dejan hacer a cada rol, en su §6.

import {
  EmailAuthProvider,
  onAuthStateChanged,
  reauthenticateWithCredential,
  signInWithEmailAndPassword,
  signOut,
  updatePassword,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import {
  collection,
  doc,
  endAt,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  startAt,
  updateDoc,
  where,
  writeBatch,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { auth, db } from './firebase.js';
import { DOMINIO_CORREO } from './config.js';
import { TipoAviso, alPadrePorEstatus, destinatarios, nuevoAviso } from './avisos.js';
import { NOMBRES_DE_ESTATUS, correoDeAcceso, esNoControl, fechaEnTexto, hora, normalizar } from './formatos.js';

/** Roles que trabajan en la web. Los demás usan la app Android. */
export const ROLES_WEB = Object.freeze(['control_escolar', 'enfermeria']);

export const TODAS_LAS_ALERTAS = Object.freeze([
  'no_dado_de_alta', 'inactivo', 'ya_ingreso', 'no_ha_ingresado', 'castigo', 'salida_fuera_de_horario',
]);

/** Lista de documentos → [{ id, ...datos }]. */
const comoLista = (resultado) => resultado.docs.map((d) => ({ id: d.id, ...d.data() }));

// ---------- Sesión ----------

/**
 * Inicia sesión con el usuario (o el correo completo) y la contraseña. Devuelve
 * { sesion } o { error } con un texto para la persona.
 */
export async function iniciarSesion(usuario, contrasena) {
  if (!String(usuario).trim() || !contrasena) return { error: 'Escribe tu usuario y tu contraseña.' };
  try {
    const { user } = await signInWithEmailAndPassword(auth, correoDeAcceso(usuario, DOMINIO_CORREO), contrasena);
    return await cargarSesion(user.uid);
  } catch (e) {
    return { error: textoDeErrorDeAcceso(e) };
  }
}

/**
 * Lee usuarios/{uid} y revisa que la cuenta esté activa y sea de un rol de la web. Si no,
 * cierra la sesión de Authentication: tener contraseña no basta.
 */
export async function cargarSesion(uid) {
  let usuario;
  try {
    usuario = (await getDoc(doc(db, 'usuarios', uid))).data();
  } catch (e) {
    await signOut(auth);
    return { error: textoDeErrorDeAcceso(e) };
  }
  if (!usuario) {
    await signOut(auth);
    return { error: 'Tu cuenta todavía no está registrada en CetisAsiste. Pide a Control Escolar que la dé de alta.' };
  }
  if (usuario.activo !== true) {
    await signOut(auth);
    return { error: 'Tu cuenta está desactivada. Habla con Control Escolar.' };
  }
  if (!ROLES_WEB.includes(usuario.rol)) {
    await signOut(auth);
    return { error: 'Esta cuenta se usa en la app CetisAsiste para Android, no en la web.' };
  }
  return { sesion: { uid, usuario } };
}

function textoDeErrorDeAcceso(e) {
  switch (e?.code) {
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
    case 'auth/user-not-found':
    case 'auth/invalid-email':
      return 'El usuario o la contraseña no son correctos.';
    case 'auth/too-many-requests':
      return 'Demasiados intentos. Espera unos minutos y vuelve a intentar.';
    case 'auth/network-request-failed':
    case 'unavailable':
      return 'No hay conexión a internet. Revisa tu conexión y vuelve a intentar.';
    default:
      return 'No se pudo iniciar sesión. Vuelve a intentar.';
  }
}

/** Escucha usuarios/{uid}: avisa con la cuenta cada vez que cambia (null si se borró o ya no se puede leer). */
export function escucharUsuario(uid, alCambiar) {
  return onSnapshot(doc(db, 'usuarios', uid), (cuenta) => alCambiar(cuenta.data() ?? null), () => alCambiar(null));
}

/** Avisa cada vez que cambia la sesión de Authentication (null = sin sesión). */
export function alCambiarLaSesion(funcion) {
  return onAuthStateChanged(auth, funcion);
}

export function cerrarSesion() {
  return signOut(auth);
}

/** ¿Debe cambiar su contraseña al entrar? (igual que la app: si se le pide o si ya venció) */
export function debeCambiarContrasena(usuario) {
  const vence = usuario?.contrasenaVence?.toDate?.();
  return usuario?.debeCambiarContrasena === true || (vence instanceof Date && vence < new Date());
}

/**
 * Cambia la contraseña de la sesión abierta. Firebase pide confirmar la actual. Después
 * anota en usuarios/{uid} cuándo vence la nueva. Devuelve null si todo salió bien o un texto
 * de error.
 */
export async function cambiarContrasena(actual, nueva) {
  const cuenta = auth.currentUser;
  if (!cuenta?.email) return 'Vuelve a iniciar sesión.';
  try {
    await reauthenticateWithCredential(cuenta, EmailAuthProvider.credential(cuenta.email, actual));
    await updatePassword(cuenta, nueva);
  } catch (e) {
    if (e?.code === 'auth/weak-password') return 'La contraseña nueva es muy débil. Usa letras y números.';
    if (e?.code === 'auth/invalid-credential' || e?.code === 'auth/wrong-password') return 'La contraseña actual no es correcta.';
    if (e?.code === 'auth/too-many-requests') return 'Demasiados intentos. Espera unos minutos.';
    if (e?.code === 'auth/network-request-failed') return 'No hay conexión a internet.';
    return 'No se pudo cambiar la contraseña. Vuelve a intentar.';
  }
  // La contraseña ya cambió. Si esto falla, solo se volverá a pedir el cambio la próxima vez.
  try {
    const dias = Number((await configuracion()).diasVenceContrasena ?? 90);
    await updateDoc(doc(db, 'usuarios', cuenta.uid), {
      debeCambiarContrasena: false,
      contrasenaVence: dias > 0 ? new Date(Date.now() + dias * 24 * 60 * 60 * 1000) : null,
      actualizadoEn: serverTimestamp(),
    });
  } catch {
    // sin conexión: no pasa nada grave
  }
  return null;
}

// ---------- Configuración del plantel ----------

let configuracionGuardada = null;

/** configuracion/plantel (se guarda 5 minutos en memoria, como en la app). */
export async function configuracion() {
  if (configuracionGuardada && Date.now() - configuracionGuardada.en < 5 * 60 * 1000) return configuracionGuardada.datos;
  const datos = (await getDoc(doc(db, 'configuracion', 'plantel'))).data() ?? {};
  configuracionGuardada = { en: Date.now(), datos };
  return datos;
}

export async function periodoActual() {
  return (await configuracion()).periodoActual ?? '';
}

/** Cambia qué estatus y qué alertas se le avisan al padre (solo Control Escolar). */
export async function guardarAvisosDelPlantel(estatus, alertas) {
  await updateDoc(doc(db, 'configuracion', 'plantel'), {
    avisarAlPadreEstatus: estatus,
    avisarAlPadreAlertas: alertas,
    actualizadoEn: serverTimestamp(),
  });
  configuracionGuardada = null;
}

// ---------- Grupos y alumnos ----------

/** Grupos del semestre en curso de un turno, por nombre. */
export async function gruposDelTurno(turno) {
  const periodo = await periodoActual();
  if (!periodo) return [];
  return comoLista(await getDocs(query(
    collection(db, 'grupos'), where('periodoId', '==', periodo), where('turno', '==', turno), orderBy('nombre'),
  )));
}

/** Alumnos inscritos de un grupo, en orden alfabético por apellidos. */
export async function alumnosDelGrupo(grupoId) {
  const lista = comoLista(await getDocs(query(
    collection(db, 'alumnos'), where('estado', '==', 'inscrito'), where('grupoId', '==', grupoId), orderBy('apellidoPaterno'),
  )));
  const orden = new Intl.Collator('es-MX', { sensitivity: 'base' });
  return lista.sort((a, b) => orden.compare(
    `${a.apellidoPaterno ?? ''} ${a.apellidoMaterno ?? ''} ${a.nombres ?? ''}`,
    `${b.apellidoPaterno ?? ''} ${b.apellidoMaterno ?? ''} ${b.nombres ?? ''}`,
  ));
}

/**
 * Busca alumnos: si se escriben 14 dígitos, por No. de control (cualquier estado); si no, por
 * el principio de sus apellidos ("perez ro…", solo inscritos), hasta 25.
 */
export async function buscarAlumnos(texto) {
  const limpio = String(texto).trim();
  if (esNoControl(limpio)) {
    const ficha = await getDoc(doc(db, 'alumnos', limpio));
    return ficha.exists() ? [{ id: ficha.id, ...ficha.data() }] : [];
  }
  const busqueda = normalizar(limpio);
  if (busqueda.length < 2) return [];
  return comoLista(await getDocs(query(
    collection(db, 'alumnos'), where('estado', '==', 'inscrito'), orderBy('nombreBusqueda'),
    startAt(busqueda), endAt(`${busqueda}`), limit(25),
  )));
}

/** Escucha la ficha del alumno: avisa cada vez que cambia (null si no existe). */
export function escucharAlumno(noControl, alCambiar, alFallar) {
  return onSnapshot(
    doc(db, 'alumnos', noControl),
    (ficha) => alCambiar(ficha.exists() ? { id: ficha.id, ...ficha.data() } : null),
    alFallar,
  );
}

/** alumnos/{n}/privado/salud (Control Escolar y Enfermería). */
export async function datosDeSalud(noControl) {
  return (await getDoc(doc(db, 'alumnos', noControl, 'privado', 'salud'))).data() ?? null;
}

/** Guarda alumnos/{n}/privado/salud (cambia solo los campos que se dan). */
export async function guardarDatosDeSalud(noControl, datos) {
  await setDoc(doc(db, 'alumnos', noControl, 'privado', 'salud'), datos, { merge: true });
}

/** alumnos/{n}/privado/datos (solo Control Escolar). */
export async function datosPersonales(noControl) {
  return (await getDoc(doc(db, 'alumnos', noControl, 'privado', 'datos'))).data() ?? null;
}

/** Estatus del alumno, del más nuevo al más viejo. */
export async function estatusDelAlumno(noControl, cuantos = 30) {
  return comoLista(await getDocs(query(
    collection(db, 'alumnos', noControl, 'estatus'), orderBy('fecha', 'desc'), limit(cuantos),
  )));
}

/** Historial de activaciones por pago y por salud, del más nuevo al más viejo. */
export async function cambiosDeEstado(noControl, cuantos = 30) {
  return comoLista(await getDocs(query(
    collection(db, 'alumnos', noControl, 'cambiosDeEstado'), orderBy('fecha', 'desc'), limit(cuantos),
  )));
}

/** Entradas y salidas del alumno en el semestre en curso (las 20 más recientes). */
export async function registrosDelAlumno(noControl) {
  const periodo = await periodoActual();
  if (!periodo) return [];
  return comoLista(await getDocs(query(
    collection(db, 'periodos', periodo, 'registros'), where('alumnoId', '==', noControl), orderBy('fechaHora', 'desc'), limit(20),
  )));
}

// ---------- Lo que guardan Enfermería y Control Escolar ----------
// Cada acción se guarda **de una vez** con los avisos a los padres (todo o nada), como en la app.

function avisarALosPadres(lote, alumno, aviso) {
  destinatarios(alumno).forEach((padre) => lote.set(doc(collection(db, 'usuarios', padre, 'notificaciones')), aviso));
}

/**
 * Enfermería da un pase de salida por salud: crea el estatus `pase_salud` (vale solo hoy) y
 * avisa a los padres. El prefecto lo ve al escanear: la salida no tiene alerta.
 */
export async function darPaseDeSalud(alumno, motivo, sesion) {
  const ahora = new Date();
  const lote = writeBatch(db);
  lote.set(doc(collection(db, 'alumnos', alumno.id, 'estatus')), {
    tipo: 'pase_salud',
    descripcion: motivo,
    fecha: ahora,
    vigenteHasta: null,
    asignadoPor: sesion.uid,
    asignadoPorNombre: sesion.usuario.nombre?.trim() ?? '',
  });
  avisarALosPadres(lote, alumno, nuevoAviso({
    tipo: TipoAviso.ESTATUS,
    titulo: 'Pase de salida por salud',
    mensaje: `Enfermería le dio a ${alumno.nombre?.trim()} un pase de salida por salud a las ${hora(ahora)}. Motivo: ${motivo}`,
    alumnoId: alumno.id,
    creadaPor: sesion.uid,
    ahora,
  }));
  await lote.commit();
}

/**
 * Activa o desactiva al alumno por pago (Control Escolar) o por salud (Enfermería): cambia
 * `activoPorPago` / `activoPorSalud`, anota el cambio en su historial y avisa a los padres.
 */
export async function cambiarEstado(alumno, campo, activo, motivo, sesion) {
  const ahora = new Date();
  const porPago = campo === 'pago';
  const quien = porPago ? 'Control Escolar' : 'Enfermería';
  const lote = writeBatch(db);
  lote.update(doc(db, 'alumnos', alumno.id), {
    [porPago ? 'activoPorPago' : 'activoPorSalud']: activo,
    actualizadoEn: serverTimestamp(),
  });
  lote.set(doc(collection(db, 'alumnos', alumno.id, 'cambiosDeEstado')), {
    campo,
    valor: activo,
    motivo,
    fecha: ahora,
    por: sesion.uid,
    porNombre: sesion.usuario.nombre?.trim() ?? '',
  });
  const nombre = alumno.nombre?.trim();
  avisarALosPadres(lote, alumno, nuevoAviso({
    tipo: TipoAviso.ESTADO,
    titulo: activo ? `Activo por ${campo}` : `Inactivo por ${campo}`,
    mensaje: activo
      ? `${quien} volvió a activar a ${nombre}. Motivo: ${motivo}`
      : `${quien} desactivó a ${nombre} por ${campo}. Motivo: ${motivo}. Mientras esté inactivo, al llegar al plantel el prefecto verá una alerta.`,
    alumnoId: alumno.id,
    creadaPor: sesion.uid,
    ahora,
  }));
  await lote.commit();
}

/**
 * Control Escolar agrega cualquier estatus. Si el plantel avisa ese tipo al padre
 * (configuracion/plantel.avisarAlPadreEstatus), se le avisa en el mismo lote.
 * `vigenteHasta` es opcional (Date o null).
 */
export async function agregarEstatus(alumno, tipo, descripcion, vigenteHasta, sesion) {
  const ahora = new Date();
  const lote = writeBatch(db);
  lote.set(doc(collection(db, 'alumnos', alumno.id, 'estatus')), {
    tipo,
    descripcion,
    fecha: ahora,
    vigenteHasta: vigenteHasta ?? null,
    asignadoPor: sesion.uid,
    asignadoPorNombre: sesion.usuario.nombre?.trim() ?? '',
  });
  const avisa = alPadrePorEstatus(tipo, await configuracion());
  if (avisa) {
    const nombreTipo = NOMBRES_DE_ESTATUS[tipo] ?? tipo;
    avisarALosPadres(lote, alumno, nuevoAviso({
      tipo: TipoAviso.ESTATUS,
      titulo: `Nuevo estatus: ${nombreTipo}`,
      mensaje: `Control Escolar le registró a ${alumno.nombre?.trim()}: ${nombreTipo.toLowerCase()}. Motivo: ${descripcion}`,
      alumnoId: alumno.id,
      creadaPor: sesion.uid,
      ahora,
    }));
  }
  await lote.commit();
  return avisa && destinatarios(alumno).length > 0;
}

// ---------- Entradas y salidas (Control Escolar) ----------

/**
 * Escucha las entradas y salidas de un día (`fecha` = "AAAA-MM-DD") del semestre en curso,
 * de la más nueva a la más vieja (hasta 200). Con `soloAlertas`, solo las que tuvieron
 * alerta (usa el índice registros: fecha, alerta, fechaHora). Devuelve la función para
 * dejar de escuchar.
 */
export function escucharRegistrosDelDia(fecha, soloAlertas, alCambiar, alFallar) {
  let dejar = () => {};
  let cancelado = false;
  periodoActual().then((periodo) => {
    if (cancelado) return;
    if (!periodo) {
      alCambiar([]);
      return;
    }
    const filtros = [where('fecha', '==', fecha)];
    if (soloAlertas) filtros.push(where('alerta', 'in', [...TODAS_LAS_ALERTAS]));
    dejar = onSnapshot(
      query(collection(db, 'periodos', periodo, 'registros'), ...filtros, orderBy('fechaHora', 'desc'), limit(200)),
      (lista) => alCambiar(comoLista(lista)),
      alFallar,
    );
  }, alFallar);
  return () => {
    cancelado = true;
    dejar();
  };
}

export const fechaDeHoy = () => fechaEnTexto(new Date());

// ---------- Avisos de la propia cuenta ----------

export function escucharNotificaciones(uid, alCambiar, alFallar) {
  return onSnapshot(
    query(collection(db, 'usuarios', uid, 'notificaciones'), orderBy('fecha', 'desc'), limit(50)),
    (lista) => alCambiar(comoLista(lista)),
    alFallar,
  );
}

/** Cuántos avisos sin leer hay (hasta 10, como la campana de la app). */
export function escucharSinLeer(uid, alCambiar) {
  return onSnapshot(
    query(collection(db, 'usuarios', uid, 'notificaciones'), where('leida', '==', false), limit(10)),
    (lista) => alCambiar(lista.size),
    () => alCambiar(0),
  );
}

export async function marcarLeidas(uid, ids) {
  if (ids.length === 0) return;
  const lote = writeBatch(db);
  ids.forEach((id) => lote.update(doc(db, 'usuarios', uid, 'notificaciones', id), { leida: true }));
  await lote.commit();
}
