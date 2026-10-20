// PERFIL DEL ALUMNO (#/alumno/{No. de control}).
//
//   Enfermería:      pase de salida por salud, activar/desactivar por salud, datos de salud.
//   Control Escolar: activar/desactivar por pago, nuevo estatus, datos personales y de salud,
//                    entradas y salidas del semestre.
// Los dos ven la ficha (en vivo), los estatus y el historial de cambios de estado.
// Cada acción avisa a los padres en el mismo guardado (ADR-010).

import {
  agregarEstatus,
  cambiarEstado,
  cambiosDeEstado,
  darPaseDeSalud,
  datosDeSalud,
  datosPersonales,
  escucharAlumno,
  estatusDelAlumno,
  guardarDatosDeSalud,
  registrosDelAlumno,
} from '../datos.js';
import { destinatarios } from '../avisos.js';
import {
  NOMBRES_DE_ALERTA,
  NOMBRES_DE_ESTADO_ALUMNO,
  NOMBRES_DE_ESTATUS,
  NOMBRES_DE_UBICACION,
  cuando,
  estadoVisible,
  grupoDeId,
  nombreDeTurno,
} from '../formatos.js';
import { avisar, campo, etiqueta, h, reemplazar, textoDeError, vacio, ventana } from '../ui.js';

export function mostrarPerfil(contenedor, { sesion, rol }, noControl) {
  const esEnfermeria = rol === 'enfermeria';
  let alumno = null;

  const ficha = h('div', { class: 'tarjeta tarjeta-guinda' }, vacio('Cargando…'));
  const acciones = h('div', { class: 'tarjeta' });
  const salud = h('div', { class: 'tarjeta' });
  const personales = esEnfermeria ? null : h('div', { class: 'tarjeta' });
  const estatus = h('div', { class: 'tarjeta' });
  const historial = h('div', { class: 'tarjeta' });
  const registros = esEnfermeria ? null : h('div', { class: 'tarjeta' });

  reemplazar(contenedor,
    h('p', { class: 'ruta' }, h('a', { href: '#/alumnos' }, '← Alumnos')),
    ficha,
    acciones,
    h('div', { class: 'rejilla' }, salud, personales),
    estatus,
    historial,
    registros,
  );

  // ---------- Ficha (en vivo: si alguien la cambia, se actualiza sola) ----------

  const dejar = escucharAlumno(noControl, (datos) => {
    alumno = datos;
    if (!alumno) {
      reemplazar(ficha, h('h1', {}, 'No existe ese alumno'), h('p', { class: 'explicacion' }, `No hay ningún alumno con el No. de control ${noControl}.`));
      reemplazar(acciones);
      acciones.hidden = true;
      return;
    }
    acciones.hidden = false;
    dibujarFicha();
    dibujarAcciones();
  }, (e) => {
    console.error(e);
    reemplazar(ficha, h('p', {}, textoDeError(e)));
  });

  function dibujarFicha() {
    const estado = estadoVisible(alumno);
    const ultimo = alumno.ultimoRegistro;
    reemplazar(ficha,
      h('h1', {}, alumno.nombre?.trim() || '(sin nombre)'),
      h('p', { class: 'explicacion' }, `No. de control ${alumno.id}`),
      h('div', { class: 'chips' },
        etiqueta(estado.texto, estado.peligro),
        alumno.activoPorPago === false && alumno.activoPorSalud === false ? etiqueta('Inactivo por salud', true) : null,
        etiqueta(NOMBRES_DE_ESTADO_ALUMNO[alumno.estado] ?? alumno.estado ?? '', alumno.estado !== 'inscrito'),
      ),
      h('dl', { class: 'datos' },
        h('dt', {}, 'Grupo'), h('dd', {}, `${alumno.grupo || grupoDeId(alumno.grupoId)} · ${nombreDeTurno(alumno.turno)}`),
        h('dt', {}, 'Semestre'), h('dd', {}, alumno.semestre ? String(alumno.semestre) : '—'),
        h('dt', {}, 'Generación'), h('dd', {}, alumno.generacion || '—'),
        h('dt', {}, 'Ahora'), h('dd', {}, NOMBRES_DE_UBICACION[alumno.ubicacion] ?? '—'),
        h('dt', {}, 'Último registro'), h('dd', {}, ultimo?.fechaHora ? `${ultimo.tipo === 'entrada' ? 'Entrada' : 'Salida'} · ${cuando(ultimo.fechaHora)}` : '—'),
      ),
    );
  }

  /** "Se avisará a su padre o tutor" / "No tiene padre o tutor registrado". */
  function textoDeAviso() {
    const cuantos = destinatarios(alumno).length;
    if (cuantos === 0) return 'No tiene padre o tutor registrado: nadie recibirá aviso.';
    return cuantos === 1 ? 'Su padre o tutor recibirá un aviso en la app.' : `Sus ${cuantos} padres o tutores recibirán un aviso en la app.`;
  }

  function dibujarAcciones() {
    const botones = [];
    if (esEnfermeria) {
      botones.push(h('button', { class: 'boton', onclick: abrirPaseDeSalud }, 'Dar pase de salida por salud'));
      botones.push(botonDeEstado('salud', alumno.activoPorSalud !== false));
    } else {
      botones.push(botonDeEstado('pago', alumno.activoPorPago !== false));
      botones.push(h('button', { class: 'boton boton-claro', onclick: abrirNuevoEstatus }, 'Nuevo estatus'));
    }
    reemplazar(acciones,
      h('h2', {}, 'Acciones'),
      h('div', { class: 'botones' }, botones),
      h('p', { class: 'explicacion' }, textoDeAviso()),
    );
  }

  // ---------- Acciones ----------

  function abrirPaseDeSalud() {
    const motivo = h('textarea', { name: 'motivo', maxlength: '300', required: true });
    ventana({
      titulo: 'Pase de salida por salud',
      explicacion: `${alumno.nombre?.trim()} podrá salir hoy sin alerta. ${textoDeAviso()}`,
      campos: campo('Motivo', motivo, 'Ejemplo: dolor de cabeza; lo recoge su mamá.'),
      textoBoton: 'Dar pase',
      alGuardar: async () => {
        const texto = motivo.value.trim();
        if (!texto) return 'Escribe el motivo.';
        await darPaseDeSalud(alumno, texto, sesion);
        avisar('Se registró el pase de salida.');
        cargarEstatus();
        return null;
      },
    });
  }

  function botonDeEstado(campoDeEstado, activo) {
    const porPago = campoDeEstado === 'pago';
    return h('button', {
      class: activo ? 'boton boton-peligro' : 'boton',
      onclick: () => abrirCambioDeEstado(campoDeEstado, !activo),
    }, `${activo ? 'Desactivar' : 'Activar'} por ${porPago ? 'pago' : 'salud'}`);
  }

  function abrirCambioDeEstado(campoDeEstado, activar) {
    const motivo = h('textarea', { name: 'motivo', maxlength: '300', required: true });
    const porPago = campoDeEstado === 'pago';
    ventana({
      titulo: `${activar ? 'Activar' : 'Desactivar'} por ${porPago ? 'pago' : 'salud'}`,
      explicacion: activar
        ? `${alumno.nombre?.trim()} podrá volver a entrar sin alerta (si no tiene otra razón). ${textoDeAviso()}`
        : `Mientras esté inactivo, al escanearlo en la entrada el prefecto verá la alerta "Alumno inactivo". ${textoDeAviso()}`,
      campos: campo('Motivo', motivo, porPago ? 'Ejemplo: no ha pagado la inscripción.' : 'Ejemplo: varicela, reposo hasta el viernes.'),
      textoBoton: activar ? 'Activar' : 'Desactivar',
      peligro: !activar,
      alGuardar: async () => {
        const texto = motivo.value.trim();
        if (!texto) return 'Escribe el motivo.';
        await cambiarEstado(alumno, campoDeEstado, activar, texto, sesion);
        avisar(activar ? 'Se activó al alumno.' : 'Se desactivó al alumno.');
        cargarHistorial();
        return null;
      },
    });
  }

  function abrirNuevoEstatus() {
    const tipo = h('select', { name: 'tipo', required: true },
      h('option', { value: '' }, 'Elige el tipo…'),
      Object.entries(NOMBRES_DE_ESTATUS).map(([valor, nombre]) => h('option', { value: valor }, nombre)),
    );
    const descripcion = h('textarea', { name: 'descripcion', maxlength: '300', required: true });
    const hasta = h('input', { type: 'date', name: 'hasta' });
    ventana({
      titulo: 'Nuevo estatus',
      explicacion: 'Si el plantel avisa ese tipo de estatus al padre (Avisos al padre), recibirá un aviso con esta descripción.',
      campos: [
        campo('Tipo', tipo),
        campo('Descripción', descripcion),
        campo('Vigente hasta (opcional)', hasta, 'Si lo dejas vacío, un castigo o un pase de salud cuentan solo el día de hoy.'),
      ],
      textoBoton: 'Guardar estatus',
      alGuardar: async () => {
        if (!tipo.value) return 'Elige el tipo de estatus.';
        const texto = descripcion.value.trim();
        if (!texto) return 'Escribe una descripción.';
        // El día elegido completo: hasta las 23:59 de esa fecha (hora de esta computadora).
        const vigenteHasta = hasta.value ? new Date(`${hasta.value}T23:59:59`) : null;
        const seAviso = await agregarEstatus(alumno, tipo.value, texto, vigenteHasta, sesion);
        avisar(seAviso ? 'Se guardó el estatus y se avisó al padre o tutor.' : 'Se guardó el estatus.');
        cargarEstatus();
        return null;
      },
    });
  }

  // ---------- Datos de salud y personales ----------

  const CAMPOS_DE_SALUD = [
    ['tipoSangre', 'Tipo de sangre', false],
    ['alergias', 'Alergias', true],
    ['enfermedades', 'Enfermedades', true],
    ['medicamentos', 'Medicamentos', true],
    ['nss', 'No. de seguro social (IMSS)', false],
    ['contactoEmergencia', 'Contacto de emergencia', false],
    ['telefonoEmergencia', 'Teléfono de emergencia', false],
  ];

  const comoTexto = (valor) => (Array.isArray(valor) ? valor.join(', ') : valor ?? '');

  async function cargarSalud() {
    reemplazar(salud, h('h2', {}, 'Salud'), vacio('Cargando…'));
    try {
      const datos = (await datosDeSalud(noControl)) ?? {};
      reemplazar(salud,
        h('h2', {}, 'Salud'),
        h('dl', { class: 'datos' }, CAMPOS_DE_SALUD.map(([clave, nombre]) => [h('dt', {}, nombre), h('dd', {}, comoTexto(datos[clave]) || '—')])),
        h('div', { class: 'botones' }, h('button', { class: 'boton boton-claro', onclick: () => editarSalud(datos) }, 'Editar datos de salud')),
      );
    } catch (e) {
      console.error(e);
      reemplazar(salud, h('h2', {}, 'Salud'), h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  function editarSalud(datos) {
    const controles = Object.fromEntries(CAMPOS_DE_SALUD.map(([clave]) => [clave, h('input', { name: clave, value: comoTexto(datos[clave]) })]));
    ventana({
      titulo: 'Datos de salud',
      explicacion: 'Separa con comas las alergias, enfermedades y medicamentos.',
      campos: CAMPOS_DE_SALUD.map(([clave, nombre]) => campo(nombre, controles[clave])),
      textoBoton: 'Guardar',
      alGuardar: async () => {
        const nuevos = {};
        for (const [clave, , esLista] of CAMPOS_DE_SALUD) {
          const valor = controles[clave].value.trim();
          nuevos[clave] = esLista ? valor.split(',').map((v) => v.trim()).filter(Boolean) : valor;
        }
        await guardarDatosDeSalud(noControl, nuevos);
        avisar('Se guardaron los datos de salud.');
        cargarSalud();
        return null;
      },
    });
  }

  async function cargarPersonales() {
    if (!personales) return;
    reemplazar(personales, h('h2', {}, 'Datos personales'), vacio('Cargando…'));
    try {
      const datos = (await datosPersonales(noControl)) ?? {};
      const tutores = Array.isArray(datos.tutores) ? datos.tutores : [];
      reemplazar(personales,
        h('h2', {}, 'Datos personales'),
        h('dl', { class: 'datos' },
          h('dt', {}, 'CURP'), h('dd', {}, datos.curp || '—'),
          h('dt', {}, 'Nacimiento'), h('dd', {}, datos.fechaNacimiento || '—'),
          h('dt', {}, 'Domicilio'), h('dd', {}, datos.domicilio || '—'),
          h('dt', {}, 'Teléfono'), h('dd', {}, datos.telefono || '—'),
          h('dt', {}, 'Correo personal'), h('dd', {}, datos.correoPersonal || '—'),
          h('dt', {}, 'Tutores'), h('dd', {}, tutores.length === 0 ? '—'
            : tutores.map((t) => `${t.nombre ?? ''} (${t.parentesco ?? ''}) ${t.telefono ?? ''}`.trim()).join(' · ')),
        ),
        h('p', { class: 'explicacion' }, 'Para cambiarlos, por ahora, usa la consola de Firebase (docs/BASE_DE_DATOS.md §9).'),
      );
    } catch (e) {
      console.error(e);
      reemplazar(personales, h('h2', {}, 'Datos personales'), h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  // ---------- Listas: estatus, historial y registros ----------

  async function cargarEstatus() {
    reemplazar(estatus, h('h2', {}, 'Estatus'), vacio('Cargando…'));
    try {
      const lista = await estatusDelAlumno(noControl);
      reemplazar(estatus, h('h2', {}, 'Estatus'), lista.length === 0 ? vacio('No tiene estatus registrados.')
        : h('ul', { class: 'lista' }, lista.map((e) => h('li', {},
          h('div', {}, h('strong', {}, NOMBRES_DE_ESTATUS[e.tipo] ?? e.tipo), ' — ', e.descripcion ?? ''),
          h('div', { class: 'detalle' }, [cuando(e.fecha), e.asignadoPorNombre, e.vigenteHasta ? `vigente hasta ${cuando(e.vigenteHasta)}` : null]
            .filter(Boolean).join(' · ')),
        ))));
    } catch (e) {
      console.error(e);
      reemplazar(estatus, h('h2', {}, 'Estatus'), h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  async function cargarHistorial() {
    reemplazar(historial, h('h2', {}, 'Historial de activaciones'), vacio('Cargando…'));
    try {
      const lista = await cambiosDeEstado(noControl);
      reemplazar(historial, h('h2', {}, 'Historial de activaciones'), lista.length === 0 ? vacio('Nunca se ha activado ni desactivado.')
        : h('ul', { class: 'lista' }, lista.map((c) => h('li', {},
          h('div', {}, h('strong', {}, `${c.valor ? 'Activado' : 'Desactivado'} por ${c.campo}`), ' — ', c.motivo ?? ''),
          h('div', { class: 'detalle' }, [cuando(c.fecha), c.porNombre].filter(Boolean).join(' · ')),
        ))));
    } catch (e) {
      console.error(e);
      reemplazar(historial, h('h2', {}, 'Historial de activaciones'), h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  async function cargarRegistros() {
    if (!registros) return;
    reemplazar(registros, h('h2', {}, 'Entradas y salidas del semestre'), vacio('Cargando…'));
    try {
      const lista = await registrosDelAlumno(noControl);
      reemplazar(registros, h('h2', {}, 'Entradas y salidas del semestre'), lista.length === 0 ? vacio('No tiene entradas ni salidas este semestre.')
        : h('ul', { class: 'lista' }, lista.map((r) => h('li', {},
          h('div', {}, h('strong', {}, r.tipo === 'entrada' ? 'Entrada' : 'Salida'), ' · ', cuando(r.fechaHora),
            r.alerta ? [' ', etiqueta(NOMBRES_DE_ALERTA[r.alerta] ?? r.alerta, true)] : null),
          h('div', { class: 'detalle' }, `Registró ${r.registradoPorNombre || '—'} · ${r.medio === 'qr' ? 'QR' : 'No. de control'}`),
        ))));
    } catch (e) {
      console.error(e);
      reemplazar(registros, h('h2', {}, 'Entradas y salidas del semestre'), h('p', { class: 'error' }, textoDeError(e)));
    }
  }

  cargarSalud();
  cargarPersonales();
  cargarEstatus();
  cargarHistorial();
  cargarRegistros();
  return dejar;
}
