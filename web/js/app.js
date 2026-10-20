// ARRANQUE DE LA WEB: sesión → (cambio de contraseña obligatorio) → pantallas según el rol.
//
// Es la versión web de ui/CetisApp.kt. Solo entran Control Escolar y Enfermería; los demás
// roles usan la app Android. Cada pantalla vive en js/pantallas/ y se elige con la dirección
// (#/alumnos, #/alumno/00000000000001, #/entradas…).

import { listaParaUsarse, usaEmulador } from './firebase.js';
import {
  alCambiarLaSesion,
  cargarSesion,
  cerrarSesion,
  debeCambiarContrasena,
  escucharSinLeer,
  escucharUsuario,
} from './datos.js';
import { h, reemplazar } from './ui.js';
import { mostrarAcceso, mostrarCambioObligatorio, mostrarCambiarContrasena } from './pantallas/acceso.js';
import { mostrarAlumnos } from './pantallas/alumnos.js';
import { mostrarPerfil } from './pantallas/perfil.js';
import { mostrarEntradas } from './pantallas/entradas.js';
import { mostrarAvisosDelPlantel } from './pantallas/configuracion.js';
import { mostrarNotificaciones } from './pantallas/notificaciones.js';

const raiz = document.getElementById('app');

/** Nombre de cada rol, como lo ve la persona. */
const NOMBRE_DEL_ROL = { control_escolar: 'Control Escolar', enfermeria: 'Enfermería' };

/**
 * Pantallas de cada rol. Las que tienen `menu` salen en el menú de la izquierda.
 * `mostrar(contenedor, contexto, parametro)` dibuja la pantalla y puede devolver una
 * función para limpiar (dejar de escuchar Firestore) al salir de ella.
 */
const PANTALLAS = {
  control_escolar: {
    alumnos: { menu: 'Alumnos', mostrar: mostrarAlumnos },
    alumno: { mostrar: mostrarPerfil },
    entradas: { menu: 'Entradas y salidas', mostrar: mostrarEntradas },
    avisos: { menu: 'Avisos al padre', mostrar: mostrarAvisosDelPlantel },
    notificaciones: { menu: 'Notificaciones', mostrar: mostrarNotificaciones },
    contrasena: { menu: 'Cambiar contraseña', mostrar: mostrarCambiarContrasena },
  },
  enfermeria: {
    alumnos: { menu: 'Alumnos por grupo', mostrar: mostrarAlumnos },
    alumno: { mostrar: mostrarPerfil },
    notificaciones: { menu: 'Notificaciones', mostrar: mostrarNotificaciones },
    contrasena: { menu: 'Cambiar contraseña', mostrar: mostrarCambiarContrasena },
  },
};

let limpiarPantalla = () => {};
let limpiarSesion = () => {};
/** true mientras el formulario de acceso está iniciando sesión (él dibuja el resultado). */
let entrando = false;

function arrancar() {
  if (!listaParaUsarse) {
    reemplazar(raiz, h('div', { class: 'acceso' }, h('div', { class: 'acceso-tarjeta' },
      h('h1', {}, 'Falta configurar la web'),
      h('p', { class: 'explicacion' }, 'Llena apiKey y appId en web/js/config.js con los datos de la consola de Firebase (ver web/README.md).'),
    )));
    return;
  }
  alCambiarLaSesion(async (cuenta) => {
    if (entrando) return;
    if (!cuenta) {
      salir();
      return;
    }
    const resultado = await cargarSesion(cuenta.uid);
    if (resultado.sesion) entrar(resultado.sesion);
    else salir(resultado.error);
  });
}

/** Pantalla de iniciar sesión (con un error, si lo hay). */
function salir(error) {
  limpiarPantalla();
  limpiarSesion();
  limpiarPantalla = () => {};
  limpiarSesion = () => {};
  window.removeEventListener('hashchange', alCambiarRuta);
  mostrarAcceso(raiz, {
    error,
    usaEmulador,
    antesDeEntrar: () => { entrando = true; },
    alEntrar: (resultado) => {
      entrando = false;
      if (resultado.sesion) entrar(resultado.sesion);
    },
  });
}

let sesionActual = null;

function entrar(sesion) {
  sesionActual = sesion;
  limpiarSesion();
  // La cuenta se escucha en vivo: si Control Escolar la desactiva o la borra, se cierra la sesión.
  const dejarCuenta = escucharUsuario(sesion.uid, (usuario) => {
    if (!usuario || usuario.activo !== true) {
      cerrarSesion();
      return;
    }
    sesion.usuario = usuario;
  });
  limpiarSesion = dejarCuenta;

  if (debeCambiarContrasena(sesion.usuario)) {
    mostrarCambioObligatorio(raiz, {
      // Ya cambió: no se espera a que llegue la cuenta actualizada para dejarla pasar.
      alTerminar: () => {
        sesion.usuario = { ...sesion.usuario, debeCambiarContrasena: false, contrasenaVence: null };
        entrar(sesion);
      },
      alCerrarSesion: cerrarSesion,
    });
    return;
  }
  dibujarEstructura(sesion);
}

/** Barra superior, menú de la izquierda y el lugar de las pantallas. */
function dibujarEstructura(sesion) {
  const rol = sesion.usuario.rol;
  const menu = h('nav', { class: 'menu', id: 'menu', 'aria-label': 'Menú principal' });
  const contenido = h('main', { id: 'contenido', tabindex: '-1' });
  const botonMenu = h('button', {
    class: 'boton-menu', 'aria-label': 'Abrir el menú', 'aria-controls': 'menu', 'aria-expanded': 'false',
    onclick: () => {
      const abierto = menu.classList.toggle('abierto');
      botonMenu.setAttribute('aria-expanded', String(abierto));
    },
  }, '☰');

  const contadores = {};
  for (const [ruta, pantalla] of Object.entries(PANTALLAS[rol])) {
    if (!pantalla.menu) continue;
    const contador = h('span', { class: 'contador', hidden: true });
    contadores[ruta] = contador;
    menu.append(h('a', { href: `#/${ruta}`, 'data-ruta': ruta, onclick: () => menu.classList.remove('abierto') },
      h('span', {}, pantalla.menu), contador));
  }
  menu.append(h('div', { class: 'separador' }));
  menu.append(h('a', { href: '#/salir', onclick: (e) => { e.preventDefault(); if (confirm('¿Cerrar sesión?')) cerrarSesion(); } }, 'Cerrar sesión'));

  // En el teléfono, tocar la página cierra el menú.
  contenido.addEventListener('click', () => {
    menu.classList.remove('abierto');
    botonMenu.setAttribute('aria-expanded', 'false');
  });

  reemplazar(raiz,
    usaEmulador ? h('div', { class: 'franja-emulador' }, 'Emulador: datos de prueba, no es la base real del plantel') : null,
    h('header', { class: 'barra' },
      botonMenu,
      h('span', { class: 'marca' }, 'CetisAsiste'),
      h('span', { class: 'persona' }, sesion.usuario.nombre?.trim() ?? '', h('small', {}, NOMBRE_DEL_ROL[rol])),
    ),
    h('div', { class: 'cuerpo' }, menu, contenido),
  );

  // Avisos sin leer, en vivo: contador junto a "Notificaciones".
  const dejarSinLeer = escucharSinLeer(sesion.uid, (cuantos) => {
    contadores.notificaciones.hidden = cuantos === 0;
    contadores.notificaciones.textContent = cuantos >= 10 ? '9+' : String(cuantos);
  });
  const dejarCuenta = limpiarSesion;
  limpiarSesion = () => { dejarCuenta(); dejarSinLeer(); };

  window.addEventListener('hashchange', alCambiarRuta);
  alCambiarRuta();
}

/** Lee la dirección (#/alumno/0000…) y muestra esa pantalla. */
function alCambiarRuta() {
  const sesion = sesionActual;
  const contenido = document.getElementById('contenido');
  if (!sesion || !contenido) return;
  const [ruta, parametro] = location.hash.replace(/^#\/?/, '').split('/');
  const pantallas = PANTALLAS[sesion.usuario.rol];
  if (!pantallas[ruta]) {
    location.replace('#/alumnos');
    return;
  }
  document.querySelectorAll('.menu a[data-ruta]').forEach((a) => {
    if (a.dataset.ruta === ruta || (ruta === 'alumno' && a.dataset.ruta === 'alumnos')) a.setAttribute('aria-current', 'page');
    else a.removeAttribute('aria-current');
  });
  limpiarPantalla();
  reemplazar(contenido);
  const contexto = { sesion, rol: sesion.usuario.rol, navegar: (destino) => { location.hash = destino; } };
  limpiarPantalla = pantallas[ruta].mostrar(contenido, contexto, decodeURIComponent(parametro ?? '')) ?? (() => {});
  contenido.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

arrancar();
