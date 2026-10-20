// Iniciar sesión y cambiar contraseña (la obligatoria al entrar y la de la opción del menú).
// Las mismas reglas que la app: comun_cambiar_contrasena.xml y CambiarContrasenaXml.

import { cambiarContrasena, iniciarSesion } from '../datos.js';
import { avisar, campo, h, reemplazar } from '../ui.js';

/** Mínimo de caracteres de una contraseña nueva (Firebase pide 6; el plantel, 8). */
const MINIMO_CONTRASENA = 8;

/**
 * Pantalla de iniciar sesión. `antesDeEntrar` se llama antes de pedirle la sesión a
 * Firebase y `alEntrar(resultado)` al terminar ({ sesion } o { error }).
 */
export function mostrarAcceso(raiz, { error, usaEmulador, antesDeEntrar, alEntrar }) {
  const usuario = h('input', { name: 'usuario', autocomplete: 'username', required: true, autocapitalize: 'none' });
  const contrasena = h('input', { name: 'contrasena', type: 'password', autocomplete: 'current-password', required: true });
  const mensaje = h('p', { class: 'error', role: 'alert', hidden: !error }, error ?? '');
  const boton = h('button', { type: 'submit', class: 'boton boton-ancho' }, 'Iniciar sesión');

  const formulario = h('form', { novalidate: true },
    campo('Usuario', usuario, 'El que te dio Control Escolar (sin @cetis61.mx).'),
    campo('Contraseña', contrasena),
    mensaje,
    boton,
  );
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    boton.disabled = true;
    boton.textContent = 'Entrando…';
    mensaje.hidden = true;
    antesDeEntrar();
    const resultado = await iniciarSesion(usuario.value, contrasena.value);
    boton.disabled = false;
    boton.textContent = 'Iniciar sesión';
    if (resultado.error) {
      mensaje.textContent = resultado.error;
      mensaje.hidden = false;
    }
    alEntrar(resultado);
  });

  reemplazar(raiz, h('div', { class: 'acceso' }, h('div', { class: 'acceso-tarjeta' },
    h('img', { src: 'img/logo_cetis61.png', alt: 'CETis 61' }),
    h('h1', {}, 'CetisAsiste'),
    h('p', { class: 'explicacion' }, 'Control Escolar y Enfermería'),
    usaEmulador ? h('p', { class: 'franja-emulador' }, 'Emulador: datos de prueba') : null,
    formulario,
  )));
  usuario.focus();
}

/** Revisa los tres campos antes de llamar a Firebase. Devuelve el texto del error o null. */
function errorDeContrasena(actual, nueva, repetir) {
  if (!actual || !nueva || !repetir) return 'Llena los tres campos.';
  if (nueva.length < MINIMO_CONTRASENA) return `La contraseña nueva debe tener al menos ${MINIMO_CONTRASENA} caracteres.`;
  if (nueva !== repetir) return 'La contraseña nueva no coincide en los dos campos.';
  if (nueva === actual) return 'La contraseña nueva debe ser distinta de la actual.';
  return null;
}

/** El formulario de cambiar contraseña. `alListo` se llama cuando ya cambió. */
function formularioDeContrasena(alListo) {
  const actual = h('input', { type: 'password', autocomplete: 'current-password' });
  const nueva = h('input', { type: 'password', autocomplete: 'new-password' });
  const repetir = h('input', { type: 'password', autocomplete: 'new-password' });
  const mensaje = h('p', { class: 'error', role: 'alert', hidden: true });
  const boton = h('button', { type: 'submit', class: 'boton boton-ancho' }, 'Guardar contraseña');
  const formulario = h('form', { novalidate: true },
    campo('Contraseña actual', actual),
    campo('Contraseña nueva', nueva, `Al menos ${MINIMO_CONTRASENA} caracteres.`),
    campo('Repite la contraseña nueva', repetir),
    mensaje,
    boton,
  );
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const problema = errorDeContrasena(actual.value, nueva.value, repetir.value);
    if (problema) {
      mensaje.textContent = problema;
      mensaje.hidden = false;
      return;
    }
    mensaje.hidden = true;
    boton.disabled = true;
    boton.textContent = 'Guardando…';
    const error = await cambiarContrasena(actual.value, nueva.value);
    boton.disabled = false;
    boton.textContent = 'Guardar contraseña';
    if (error) {
      mensaje.textContent = error;
      mensaje.hidden = false;
      return;
    }
    formulario.reset();
    avisar('Se cambió tu contraseña.');
    alListo();
  });
  return formulario;
}

/** Cambio obligatorio al entrar (debeCambiarContrasena o venció): solo se puede cambiar o salir. */
export function mostrarCambioObligatorio(raiz, { alTerminar, alCerrarSesion }) {
  reemplazar(raiz, h('div', { class: 'acceso' }, h('div', { class: 'acceso-tarjeta' },
    h('h1', {}, 'Cambia tu contraseña'),
    h('p', { class: 'explicacion' }, 'Tu contraseña venció o es la primera vez que entras. Para continuar, elige una nueva.'),
    formularioDeContrasena(alTerminar),
    h('div', { class: 'botones' }, h('button', { class: 'boton boton-claro boton-ancho', onclick: alCerrarSesion }, 'Cerrar sesión')),
  )));
}

/** Opción "Cambiar contraseña" del menú. */
export function mostrarCambiarContrasena(contenedor) {
  reemplazar(contenedor,
    h('h1', {}, 'Cambiar contraseña'),
    h('div', { class: 'tarjeta', style: 'max-width: 460px' }, formularioDeContrasena(() => {})),
  );
}
