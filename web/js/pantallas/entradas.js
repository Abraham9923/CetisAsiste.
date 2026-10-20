// ENTRADAS Y SALIDAS (Control Escolar): los escaneos de un día, en vivo, con sus alertas.
// Sale de periodos/{semestre}/registros. "Solo alertas" usa el índice (fecha, alerta, fechaHora).

import { escucharRegistrosDelDia, fechaDeHoy } from '../datos.js';
import { NOMBRES_DE_ALERTA, grupoDeId, hora } from '../formatos.js';
import { campo, etiqueta, h, reemplazar, textoDeError, vacio } from '../ui.js';

export function mostrarEntradas(contenedor) {
  const dia = h('input', { type: 'date', value: fechaDeHoy(), max: fechaDeHoy() });
  const soloAlertas = h('input', { type: 'checkbox' });
  const resumen = h('p', { class: 'explicacion', 'aria-live': 'polite' });
  const tabla = h('div');
  let dejar = () => {};

  reemplazar(contenedor,
    h('h1', {}, 'Entradas y salidas'),
    h('div', { class: 'tarjeta' },
      h('div', { class: 'rejilla' },
        campo('Día', dia),
        h('label', { class: 'casilla' }, soloAlertas, 'Solo las que tuvieron alerta'),
      ),
      resumen,
    ),
    tabla,
  );

  function escuchar() {
    dejar();
    reemplazar(tabla, vacio('Cargando…'));
    resumen.textContent = '';
    dejar = escucharRegistrosDelDia(dia.value, soloAlertas.checked, dibujar, (e) => {
      console.error(e);
      reemplazar(tabla, h('p', { class: 'error' }, textoDeError(e)));
    });
  }

  function dibujar(registros) {
    const entradas = registros.filter((r) => r.tipo === 'entrada').length;
    const conAlerta = registros.filter((r) => r.alerta).length;
    resumen.textContent = soloAlertas.checked
      ? `${registros.length} alertas${registros.length === 200 ? ' (se muestran las 200 más recientes)' : ''}. La lista se actualiza sola.`
      : `${registros.length} registros: ${entradas} entradas, ${registros.length - entradas} salidas, ${conAlerta} con alerta${registros.length === 200 ? ' (se muestran los 200 más recientes)' : ''}. La lista se actualiza sola.`;
    if (registros.length === 0) {
      reemplazar(tabla, vacio(soloAlertas.checked ? 'No hubo alertas ese día.' : 'No hay entradas ni salidas ese día.'));
      return;
    }
    reemplazar(tabla, h('table', { class: 'tabla' },
      h('thead', {}, h('tr', {}, ['Hora', 'Alumno', 'Grupo', 'Tipo', 'Alerta', 'Registró'].map((t) => h('th', {}, t)))),
      h('tbody', {}, registros.map((r) => h('tr', { class: r.alerta ? 'con-alerta' : null },
        h('td', { 'data-titulo': 'Hora' }, hora(r.fechaHora)),
        h('td', { 'data-titulo': 'Alumno' }, r.alumnoNombre
          ? h('a', { href: `#/alumno/${encodeURIComponent(r.alumnoId)}` }, r.alumnoNombre)
          : `No. de control ${r.alumnoId}`),
        h('td', { 'data-titulo': 'Grupo' }, grupoDeId(r.grupoId) || '—'),
        h('td', { 'data-titulo': 'Tipo' }, r.tipo === 'entrada' ? 'Entrada' : 'Salida'),
        h('td', { 'data-titulo': 'Alerta' }, r.alerta ? etiqueta(NOMBRES_DE_ALERTA[r.alerta] ?? r.alerta, true) : '—'),
        h('td', { 'data-titulo': 'Registró' }, `${r.registradoPorNombre || '—'} (${r.medio === 'qr' ? 'QR' : 'No. de control'})`),
      ))),
    ));
  }

  dia.addEventListener('change', escuchar);
  soloAlertas.addEventListener('change', escuchar);
  escuchar();
  return () => dejar();
}
