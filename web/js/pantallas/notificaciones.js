// NOTIFICACIONES de la propia cuenta (el mismo buzón que en la app: usuarios/{uid}/notificaciones),
// en vivo. "Marcar todas como leídas" apaga el contador del menú.

import { escucharNotificaciones, marcarLeidas } from '../datos.js';
import { cuando } from '../formatos.js';
import { h, reemplazar, textoDeError, vacio } from '../ui.js';

export function mostrarNotificaciones(contenedor, { sesion }) {
  const lista = h('div', { class: 'tarjeta' }, vacio('Cargando…'));
  const boton = h('button', { class: 'boton boton-claro', hidden: true }, 'Marcar todas como leídas');
  reemplazar(contenedor, h('h1', {}, 'Notificaciones'), h('div', { class: 'botones' }, boton), lista);
  let sinLeer = [];

  boton.addEventListener('click', async () => {
    boton.disabled = true;
    try {
      await marcarLeidas(sesion.uid, sinLeer);
    } catch (e) {
      console.error(e);
    } finally {
      boton.disabled = false;
    }
  });

  return escucharNotificaciones(sesion.uid, (avisos) => {
    sinLeer = avisos.filter((a) => !a.leida).map((a) => a.id);
    boton.hidden = sinLeer.length === 0;
    reemplazar(lista, avisos.length === 0 ? vacio('No tienes notificaciones.')
      : h('ul', { class: 'lista' }, avisos.map((a) => h('li', { class: a.leida ? null : 'sin-leer' },
        h('div', {}, a.titulo ?? '', a.leida ? null : h('span', { class: 'contador', style: 'margin-left: 8px' }, 'Nuevo')),
        h('div', { class: 'detalle' }, `${cuando(a.fecha)} · ${a.mensaje ?? ''}`),
      ))));
  }, (e) => {
    console.error(e);
    reemplazar(lista, h('p', { class: 'error' }, textoDeError(e)));
  });
}
