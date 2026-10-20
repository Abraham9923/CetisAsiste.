// ALUMNOS (Control Escolar) y ALUMNOS POR GRUPO (Enfermería).
// Buscar por No. de control o por apellidos, o elegir turno y grupo. Al tocar un alumno se
// abre su perfil (#/alumno/{No. de control}).

import { alumnosDelGrupo, buscarAlumnos, gruposDelTurno } from '../datos.js';
import { estadoVisible, esNoControl, grupoDeId, nombreDeTurno } from '../formatos.js';
import { etiqueta, h, reemplazar, textoDeError, vacio } from '../ui.js';

// Lo que se eligió la última vez, para que al regresar del perfil se vea igual.
const elegido = {
  turno: new Date().getHours() < 13 ? 'matutino' : 'vespertino',
  grupoId: '',
  busqueda: '',
};

export function mostrarAlumnos(contenedor, { rol }) {
  let vigente = true;
  const caja = h('input', {
    type: 'search', value: elegido.busqueda, 'aria-label': 'Buscar alumno',
    placeholder: 'No. de control (14 dígitos) o apellidos: "perez ro"',
  });
  const resultados = h('div', { 'aria-live': 'polite' });
  const chipsTurno = h('div', { class: 'chips', role: 'group', 'aria-label': 'Turno' });
  const chipsGrupo = h('div', { class: 'chips', role: 'group', 'aria-label': 'Grupo' });

  const buscador = h('form', { class: 'buscador', role: 'search' }, caja, h('button', { class: 'boton', type: 'submit' }, 'Buscar'));
  buscador.addEventListener('submit', (evento) => {
    evento.preventDefault();
    elegido.busqueda = caja.value.trim();
    buscar();
  });

  reemplazar(contenedor,
    h('h1', {}, rol === 'enfermeria' ? 'Alumnos por grupo' : 'Alumnos'),
    h('div', { class: 'tarjeta' },
      h('h2', {}, 'Buscar'),
      buscador,
    ),
    h('div', { class: 'tarjeta' },
      h('h2', {}, 'Por grupo'),
      chipsTurno,
      chipsGrupo,
    ),
    resultados,
  );

  /** Tabla de alumnos: No. de control, nombre, grupo y estado (pago, salud, prácticas). */
  function mostrarLista(titulo, alumnos) {
    if (!vigente) return;
    if (alumnos.length === 0) {
      reemplazar(resultados, h('h2', {}, titulo), vacio('No se encontró ningún alumno.'));
      return;
    }
    reemplazar(resultados,
      h('h2', {}, `${titulo} (${alumnos.length})`),
      h('table', { class: 'tabla' },
        h('thead', {}, h('tr', {}, h('th', {}, 'No. de control'), h('th', {}, 'Nombre'), h('th', {}, 'Grupo'), h('th', {}, 'Estado'))),
        h('tbody', {}, alumnos.map((alumno) => {
          const estado = estadoVisible(alumno);
          return h('tr', {},
            h('td', { 'data-titulo': 'No. de control' }, alumno.id),
            h('td', { 'data-titulo': 'Nombre' }, h('a', { href: `#/alumno/${encodeURIComponent(alumno.id)}` }, alumno.nombre?.trim() || '(sin nombre)')),
            h('td', { 'data-titulo': 'Grupo' }, alumno.grupo || grupoDeId(alumno.grupoId)),
            h('td', { 'data-titulo': 'Estado' }, etiqueta(estado.texto, estado.peligro),
              alumno.estado && alumno.estado !== 'inscrito' ? [' ', etiqueta('No inscrito', true)] : null),
          );
        })),
      ),
    );
  }

  async function buscar() {
    const texto = elegido.busqueda;
    if (!texto) return;
    reemplazar(resultados, vacio('Buscando…'));
    try {
      const encontrados = await buscarAlumnos(texto);
      mostrarLista(esNoControl(texto) ? `No. de control ${texto}` : `Resultados de "${texto}"`, encontrados);
    } catch (e) {
      console.error(e);
      if (vigente) reemplazar(resultados, h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  function dibujarTurnos() {
    reemplazar(chipsTurno, ['matutino', 'vespertino'].map((turno) => h('button', {
      class: 'chip', type: 'button', 'aria-pressed': String(turno === elegido.turno),
      onclick: () => {
        elegido.turno = turno;
        elegido.grupoId = '';
        dibujarTurnos();
        cargarGrupos();
      },
    }, nombreDeTurno(turno))));
  }

  async function cargarGrupos() {
    reemplazar(chipsGrupo, vacio('Cargando grupos…'));
    try {
      const grupos = await gruposDelTurno(elegido.turno);
      if (!vigente) return;
      if (grupos.length === 0) {
        reemplazar(chipsGrupo, vacio('No hay grupos dados de alta en este turno.'));
        return;
      }
      reemplazar(chipsGrupo, grupos.map((grupo) => h('button', {
        class: 'chip', type: 'button', 'aria-pressed': String(grupo.id === elegido.grupoId),
        onclick: () => {
          elegido.grupoId = grupo.id;
          elegido.busqueda = '';
          caja.value = '';
          cargarGrupos();
        },
      }, `Grupo ${grupo.nombre}`)));
      if (elegido.grupoId) cargarAlumnos(grupos.find((g) => g.id === elegido.grupoId));
    } catch (e) {
      console.error(e);
      if (vigente) reemplazar(chipsGrupo, h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  async function cargarAlumnos(grupo) {
    if (!grupo) return;
    reemplazar(resultados, vacio('Cargando alumnos…'));
    try {
      mostrarLista(`Grupo ${grupo.nombre}`, await alumnosDelGrupo(grupo.id));
    } catch (e) {
      console.error(e);
      if (vigente) reemplazar(resultados, h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  dibujarTurnos();
  cargarGrupos();
  if (elegido.busqueda) buscar();
  return () => { vigente = false; };
}
