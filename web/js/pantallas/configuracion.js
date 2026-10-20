// AVISOS AL PADRE (Control Escolar): qué estatus y qué alertas del escaneo se le avisan al
// padre. Guarda configuracion/plantel.avisarAlPadreEstatus y avisarAlPadreAlertas (ADR-010).
// Las entradas y salidas sin alerta y los pases de salud se avisan siempre.

import { configuracion, guardarAvisosDelPlantel } from '../datos.js';
import { alertasQueSeAvisan, estatusQueSeAvisan } from '../avisos.js';
import { NOMBRES_DE_ALERTA, NOMBRES_DE_ESTATUS } from '../formatos.js';
import { avisar, h, reemplazar, textoDeError, vacio } from '../ui.js';

/** Los estatus que pone el profesor (los que se pueden elegir aquí). */
const ESTATUS_DEL_PROFESOR = ['falta', 'reporte', 'llamada_atencion', 'castigo'];

export function mostrarAvisosDelPlantel(contenedor) {
  reemplazar(contenedor, h('h1', {}, 'Avisos al padre'), vacio('Cargando…'));
  let vigente = true;

  configuracion().then((actual) => {
    if (!vigente) return;
    const estatus = estatusQueSeAvisan(actual);
    const alertas = alertasQueSeAvisan(actual);
    const casillas = (opciones, nombres, elegidas) => opciones.map((valor) => {
      const casilla = h('input', { type: 'checkbox', value: valor, checked: elegidas.includes(valor) });
      return { valor, casilla, elemento: h('label', { class: 'casilla' }, casilla, nombres[valor] ?? valor) };
    });
    const deEstatus = casillas(ESTATUS_DEL_PROFESOR, NOMBRES_DE_ESTATUS, estatus);
    const deAlertas = casillas(Object.keys(NOMBRES_DE_ALERTA), NOMBRES_DE_ALERTA, alertas);
    const boton = h('button', { class: 'boton', type: 'submit' }, 'Guardar');
    const porOmision = !Array.isArray(actual.avisarAlPadreEstatus) || !Array.isArray(actual.avisarAlPadreAlertas);

    const notaPorOmision = h('p', { class: 'explicacion', hidden: !porOmision }, 'Ahora se usa lo de por omisión (todavía no se ha guardado nada aquí).');
    const formulario = h('form', {},
      notaPorOmision,
      h('div', { class: 'rejilla' },
        h('fieldset', { class: 'tarjeta' },
          h('legend', {}, h('h2', {}, 'Estatus que pone el profesor')),
          h('p', { class: 'explicacion' }, 'El padre recibe el tipo de estatus y la descripción que escribió el profesor.'),
          deEstatus.map((c) => c.elemento),
        ),
        h('fieldset', { class: 'tarjeta' },
          h('legend', {}, h('h2', {}, 'Alertas al escanear')),
          h('p', { class: 'explicacion' }, '"Ya ingresó" y "No ha ingresado" suelen ser errores al escanear; "No dado de alta" no tiene a quién avisar.'),
          deAlertas.map((c) => c.elemento),
        ),
      ),
      h('p', { class: 'explicacion' }, 'Siempre se avisan: cada entrada y salida sin alerta, los pases de salida por salud y los cambios de estado por pago o salud.'),
      boton,
    );
    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      boton.disabled = true;
      try {
        await guardarAvisosDelPlantel(
          deEstatus.filter((c) => c.casilla.checked).map((c) => c.valor),
          deAlertas.filter((c) => c.casilla.checked).map((c) => c.valor),
        );
        notaPorOmision.hidden = true;
        avisar('Se guardó. La app lo usa en menos de 5 minutos.');
      } catch (e) {
        console.error(e);
        avisar(textoDeError(e), true);
      } finally {
        boton.disabled = false;
      }
    });
    reemplazar(contenedor, h('h1', {}, 'Avisos al padre'),
      h('p', { class: 'explicacion' }, 'Elige de qué se le avisa al padre o tutor en su app.'), formulario);
  }, (e) => {
    console.error(e);
    if (vigente) reemplazar(contenedor, h('h1', {}, 'Avisos al padre'), h('p', { class: 'error' }, textoDeError(e)));
  });
  return () => { vigente = false; };
}
