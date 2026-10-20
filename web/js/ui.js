// Ayudantes para armar las pantallas de la web. No consultan la base de datos.
//
// Todo el texto se pone con textContent (nunca como HTML): así, aunque un dato de la base
// traiga "<script>", se ve como texto y no se ejecuta.

/**
 * Crea un elemento: h('button', { class: 'boton', onclick: fn }, 'Guardar').
 * Los hijos pueden ser textos, elementos, listas o null (se ignoran).
 */
export function h(etiqueta, atributos = {}, ...hijos) {
  const elemento = document.createElement(etiqueta);
  for (const [nombre, valor] of Object.entries(atributos ?? {})) {
    if (valor === null || valor === undefined || valor === false) continue;
    if (nombre.startsWith('on') && typeof valor === 'function') elemento.addEventListener(nombre.slice(2), valor);
    else if (nombre === 'class') elemento.className = valor;
    else if (valor === true) elemento.setAttribute(nombre, '');
    else elemento.setAttribute(nombre, valor);
  }
  agregar(elemento, hijos);
  return elemento;
}

function agregar(elemento, hijos) {
  for (const hijo of hijos.flat(Infinity)) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    elemento.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
  }
}

/** Cambia todo lo que hay dentro de `elemento`. */
export function reemplazar(elemento, ...hijos) {
  elemento.replaceChildren();
  agregar(elemento, hijos);
}

/** Etiqueta de color (como StatusChip de Figma): verde o roja. */
export function etiqueta(texto, peligro = false) {
  return h('span', { class: peligro ? 'etiqueta etiqueta-peligro' : 'etiqueta' }, texto);
}

/** Texto gris para "Cargando…" o "No hay…". */
export function vacio(texto) {
  return h('p', { class: 'vacio' }, texto);
}

/** Aviso corto abajo de la pantalla ("Se guardó…"); lo leen también los lectores de pantalla. */
export function avisar(texto, peligro = false) {
  const zona = document.getElementById('avisos');
  const aviso = h('div', { class: peligro ? 'aviso aviso-peligro' : 'aviso', role: 'status' }, texto);
  zona.append(aviso);
  setTimeout(() => aviso.remove(), 5000);
}

/**
 * Abre una ventana (<dialog>) con un formulario. `campos` arma el contenido; al tocar el
 * botón principal se llama a `alGuardar(formulario)`, que puede devolver un texto de error
 * (la ventana sigue abierta) o nada (se cierra). Mientras guarda, el botón se apaga.
 */
export function ventana({ titulo, explicacion, campos, textoBoton, peligro = false, alGuardar }) {
  const error = h('p', { class: 'error', role: 'alert', hidden: true });
  const boton = h('button', { type: 'submit', class: peligro ? 'boton boton-peligro' : 'boton' }, textoBoton);
  const formulario = h('form', { method: 'dialog' },
    h('h2', {}, titulo),
    explicacion ? h('p', { class: 'explicacion' }, explicacion) : null,
    campos,
    error,
    h('div', { class: 'acciones' },
      h('button', { type: 'button', class: 'boton boton-claro', onclick: () => dialogo.close() }, 'Cancelar'),
      boton,
    ),
  );
  const dialogo = h('dialog', { class: 'ventana' }, formulario);
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    boton.disabled = true;
    error.hidden = true;
    try {
      const problema = await alGuardar(formulario);
      if (problema) {
        error.textContent = problema;
        error.hidden = false;
      } else {
        dialogo.close();
      }
    } catch (e) {
      console.error(e);
      error.textContent = textoDeError(e);
      error.hidden = false;
    } finally {
      boton.disabled = false;
    }
  });
  dialogo.addEventListener('close', () => dialogo.remove());
  document.body.append(dialogo);
  dialogo.showModal();
  return dialogo;
}

let siguienteId = 0;

/**
 * Un campo con su etiqueta. `control` es el <input>, <textarea> o <select>. La `ayuda` va
 * debajo y el lector de pantalla la lee como descripción (no como parte del nombre).
 */
export function campo(texto, control, ayuda) {
  if (!control.id) control.id = `campo-${++siguienteId}`;
  const explicacion = ayuda ? h('small', { id: `${control.id}-ayuda` }, ayuda) : null;
  if (explicacion) control.setAttribute('aria-describedby', explicacion.id);
  return h('div', { class: 'campo' }, h('label', { for: control.id }, texto), control, explicacion);
}

/** Texto sencillo para un error de Firebase. */
export function textoDeError(e) {
  if (e?.code === 'permission-denied') return 'No tienes permiso para hacer esto. Si crees que es un error, avisa a Control Escolar.';
  if (e?.code === 'unavailable') return 'No hay conexión a internet. Vuelve a intentar.';
  if (e?.code === 'failed-precondition') return 'Falta crear un índice en la consola de Firebase (ver docs/BASE_DE_DATOS.md §7).';
  return 'Algo salió mal. Vuelve a intentar.';
}
