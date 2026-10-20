// RECORRIDO DE PUNTA A PUNTA de la web contra el emulador (Auth + Firestore + Hosting).
// Abre Chromium, entra con cada cuenta de prueba, hace cada acción y revisa en el emulador
// lo que quedó guardado (pasando por las reglas reales de firebase/firestore.rules).
//
// Cómo correrlo (ver web/README.md, "Pruebas"):
//   1. Emuladores encendidos y datos de prueba cargados (firebase/emulador/datos-de-prueba.mjs).
//   2. En la carpeta web:  npm install  y luego  npm run recorrido
// Variables opcionales: CHROMIUM (ruta de un Chromium ya instalado) y SDK_LOCAL (carpeta con
// firebase-app.js, firebase-auth.js y firebase-firestore.js, si la computadora no llega a gstatic).
// Las capturas quedan en web/pruebas/capturas/ (no se suben a git).

import { chromium } from 'playwright';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const CAPTURAS = fileURLToPath(new URL('./capturas', import.meta.url));
const WEB = 'http://127.0.0.1:5000';
const FS = 'http://127.0.0.1:8080/v1/projects/demo-cetisasiste/databases/(default)/documents';
const OWNER = { Authorization: 'Bearer owner' };
mkdirSync(CAPTURAS, { recursive: true });

async function leer(ruta) {
  const r = await fetch(`${FS}/${ruta}`, { headers: OWNER });
  return r.ok ? r.json() : null;
}
async function listar(ruta) {
  const r = await fetch(`${FS}/${ruta}?pageSize=100`, { headers: OWNER });
  return (await r.json()).documents ?? [];
}

const navegador = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const errores = [];

async function nuevaPagina(ancho = 1280, alto = 900) {
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: alto }, locale: 'es-MX' });
  if (process.env.SDK_LOCAL) {
    await contexto.route('https://www.gstatic.com/firebasejs/**', (ruta) => {
      const archivo = ruta.request().url().split('/').pop();
      ruta.fulfill({ body: readFileSync(`${process.env.SDK_LOCAL}/${archivo}`), contentType: 'text/javascript' });
    });
  }
  const pagina = await contexto.newPage();
  pagina.on('pageerror', (e) => errores.push(`pageerror: ${e.message}`));
  pagina.on('console', (m) => { if (m.type() === 'error') errores.push(`console: ${m.text()}`); });
  pagina.on('dialog', (d) => d.accept());
  pagina.on('response', (r) => { if (r.status() >= 400) errores.push(`HTTP ${r.status()} ${r.url().split('?')[0]}`); });
  return pagina;
}

async function entrar(pagina, usuario) {
  await pagina.goto(WEB);
  await pagina.getByLabel('Usuario').fill(usuario);
  await pagina.getByLabel('Contraseña').fill('prueba1234');
  await pagina.getByRole('button', { name: 'Iniciar sesión' }).click();
}

const paso = (t) => console.log(`✔ ${t}`);

// ---------- 1. Un padre no puede entrar a la web ----------
{
  const p = await nuevaPagina();
  await entrar(p, 'padre.prueba');
  await p.getByText('Esta cuenta se usa en la app CetisAsiste para Android').waitFor();
  paso('el padre ve "Esta cuenta se usa en la app…" y no entra');
  await entrar(p, 'nadie');
  await p.getByText('El usuario o la contraseña no son correctos.').waitFor();
  paso('usuario inexistente: "El usuario o la contraseña no son correctos."');
  await p.screenshot({ path: `${CAPTURAS}/01-acceso.png` });
  await p.context().close();
}

// ---------- 2. Enfermería: grupo → perfil → pase de salud → desactivar por salud ----------
{
  const p = await nuevaPagina();
  await entrar(p, 'enfermeria.prueba');
  await p.getByRole('heading', { name: 'Alumnos por grupo' }).waitFor();
  await p.getByRole('button', { name: 'Matutino' }).click();
  await p.getByRole('button', { name: 'Grupo 102' }).click();
  await p.getByRole('heading', { name: 'Grupo 102 (2)' }).waitFor();
  paso('Enfermería ve el grupo 102 con 2 alumnos');
  await p.screenshot({ path: `${CAPTURAS}/02-enfermeria-grupo.png`, fullPage: true });
  await p.getByRole('link', { name: 'Juan Pérez Prueba' }).click();
  await p.getByRole('heading', { name: 'Juan Pérez Prueba' }).waitFor();
  await p.getByText('Su padre o tutor recibirá un aviso en la app.').waitFor();
  assert.equal(await p.getByRole('button', { name: /por pago/ }).count(), 0, 'Enfermería no debe ver el botón de pago');
  await p.getByText('O+').waitFor();
  paso('perfil: ficha, aviso al padre y datos de salud (O+); sin botón de pago');

  await p.getByRole('button', { name: 'Dar pase de salida por salud' }).click();
  await p.getByRole('dialog').getByLabel('Motivo').fill('Dolor de cabeza; lo recoge su papá');
  await p.getByRole('dialog').getByRole('button', { name: 'Dar pase' }).click();
  await p.getByText('Se registró el pase de salida.').waitFor();
  await p.getByText('Dolor de cabeza; lo recoge su papá').waitFor();
  paso('pase de salud guardado y visible en Estatus');

  await p.getByRole('button', { name: 'Desactivar por salud' }).click();
  await p.getByRole('dialog').getByLabel('Motivo').fill('Varicela');
  await p.screenshot({ path: `${CAPTURAS}/03-enfermeria-desactivar.png` });
  await p.getByRole('dialog').getByRole('button', { name: 'Desactivar' }).click();
  await p.getByText('Se desactivó al alumno.').waitFor();
  await p.getByRole('button', { name: 'Activar por salud' }).waitFor();
  await p.locator('.tarjeta-guinda').getByText('Inactivo por salud').waitFor();
  await p.getByText('Desactivado por salud').waitFor();
  paso('desactivado por salud: la ficha cambia sola y el historial lo muestra');

  await p.getByRole('button', { name: 'Editar datos de salud' }).click();
  await p.getByRole('dialog').getByLabel('Alergias').fill('Penicilina, nuez');
  await p.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
  await p.getByText('Penicilina, nuez').waitFor();
  paso('datos de salud editados');
  await p.screenshot({ path: `${CAPTURAS}/04-enfermeria-perfil.png`, fullPage: true });

  // Buscar por apellido
  await p.goto(`${WEB}/#/alumnos`);
  await p.getByLabel('Buscar alumno').fill('nunez');
  await p.getByRole('button', { name: 'Buscar' }).click();
  await p.getByRole('link', { name: 'Luis Núñez Prueba' }).waitFor();
  paso('búsqueda por apellido sin acento ("nunez") encuentra a Luis Núñez');
  await p.context().close();

  // Revisión en la base
  const juan = await leer('alumnos/00000000000001');
  assert.equal(juan.fields.activoPorSalud.booleanValue, false);
  const estatus = await listar('alumnos/00000000000001/estatus');
  assert.equal(estatus.length, 1);
  assert.equal(estatus[0].fields.tipo.stringValue, 'pase_salud');
  assert.equal(estatus[0].fields.asignadoPor.stringValue, 'uid-enfermeria');
  const avisos = await listar('usuarios/uid-padre/notificaciones');
  assert.equal(avisos.length, 2, 'el padre debe tener 2 avisos (pase y desactivación)');
  const tipos = avisos.map((a) => a.fields.tipo.stringValue).sort();
  assert.deepEqual(tipos, ['estado', 'estatus']);
  assert.ok(avisos.every((a) => a.fields.creadaPor.stringValue === 'uid-enfermeria' && a.fields.leida.booleanValue === false));
  const salud = await leer('alumnos/00000000000001/privado/salud');
  assert.deepEqual(salud.fields.alergias.arrayValue.values.map((v) => v.stringValue), ['Penicilina', 'nuez']);
  paso('en la base: activoPorSalud=false, estatus pase_salud firmado, 2 avisos al padre, alergias guardadas');
}

// ---------- 3. Cambio de contraseña obligatorio ----------
{
  const p = await nuevaPagina();
  await entrar(p, 'cambio.prueba');
  await p.getByRole('heading', { name: 'Cambia tu contraseña' }).waitFor();
  await p.getByLabel('Contraseña actual').fill('prueba1234');
  await p.getByLabel('Contraseña nueva', { exact: true }).fill('corta');
  await p.getByLabel('Repite la contraseña nueva').fill('corta');
  await p.getByRole('button', { name: 'Guardar contraseña' }).click();
  await p.getByRole('alert').getByText('al menos 8 caracteres').waitFor();
  await p.getByLabel('Contraseña nueva', { exact: true }).fill('nuevaClave2026');
  await p.getByLabel('Repite la contraseña nueva').fill('nuevaClave2026');
  await p.getByRole('button', { name: 'Guardar contraseña' }).click();
  await p.getByRole('heading', { name: 'Alumnos por grupo' }).waitFor();
  const cuenta = await leer('usuarios/uid-cambio');
  assert.equal(cuenta.fields.debeCambiarContrasena.booleanValue, false);
  assert.ok(cuenta.fields.contrasenaVence.timestampValue);
  paso('contraseña obligatoria: valida, cambia, entra y anota debeCambiarContrasena=false');
  await p.context().close();
}

// ---------- 4. Control Escolar ----------
{
  const p = await nuevaPagina();
  await entrar(p, 'control.prueba');
  await p.getByRole('heading', { name: 'Alumnos', exact: true }).waitFor();
  const contador = p.locator('.menu a[data-ruta="notificaciones"] .contador');
  await contador.waitFor();
  assert.equal(await contador.textContent(), '1');
  paso('Control Escolar entra; el menú dice 1 notificación sin leer');

  await p.getByLabel('Buscar alumno').fill('00000000000002');
  await p.getByRole('button', { name: 'Buscar' }).click();
  await p.getByRole('link', { name: 'Ana López Prueba' }).click();
  await p.getByRole('heading', { name: 'Ana López Prueba' }).waitFor();
  await p.getByText('No tiene padre o tutor registrado').waitFor();
  await p.getByRole('button', { name: 'Activar por pago' }).click();
  await p.getByRole('dialog').getByLabel('Motivo').fill('Ya pagó la inscripción');
  await p.getByRole('dialog').getByRole('button', { name: 'Activar' }).click();
  await p.getByText('Se activó al alumno.').waitFor();
  await p.getByRole('button', { name: 'Desactivar por pago' }).waitFor();
  paso('activar por pago (Ana, sin padres): se guarda y el botón cambia a "Desactivar por pago"');

  await p.goto(`${WEB}/#/alumno/00000000000001`);
  await p.getByRole('heading', { name: 'Juan Pérez Prueba' }).waitFor();
  await p.getByText('XEXX090315HCHXXX00').waitFor();
  await p.getByRole('button', { name: 'Nuevo estatus' }).click();
  await p.getByRole('dialog').getByLabel('Tipo').selectOption('castigo');
  await p.getByRole('dialog').getByLabel('Descripción').fill('Pelea en el patio');
  await p.getByRole('dialog').getByRole('button', { name: 'Guardar estatus' }).click();
  await p.getByText('Se guardó el estatus y se avisó al padre o tutor.').waitFor();
  await p.getByText('Pelea en el patio').waitFor();
  await p.locator('.lista').getByText('Hoy, 7:05 am').waitFor();
  paso('perfil de Control Escolar: datos personales, nuevo estatus con aviso, entradas del semestre');
  await p.screenshot({ path: `${CAPTURAS}/05-control-perfil.png`, fullPage: true });

  await p.getByRole('link', { name: 'Entradas y salidas' }).click();
  await p.getByText('3 registros: 3 entradas, 0 salidas, 2 con alerta').waitFor();
  await p.getByLabel('Solo las que tuvieron alerta').check();
  await p.getByText('2 alertas').waitFor();
  await p.getByText('No. de control 99999999999999').waitFor();
  paso('Entradas y salidas: 3 registros, 2 con alerta; "Solo alertas" deja 2');
  await p.screenshot({ path: `${CAPTURAS}/06-control-entradas.png`, fullPage: true });

  await p.getByRole('link', { name: 'Avisos al padre' }).click();
  await p.getByText('Ahora se usa lo de por omisión').waitFor();
  await p.getByLabel('Reporte').uncheck();
  await p.getByLabel('Alumno con castigo disciplinario').check();
  await p.getByRole('button', { name: 'Guardar' }).click();
  await p.getByText('Se guardó.').waitFor();
  const config = await leer('configuracion/plantel');
  assert.deepEqual(config.fields.avisarAlPadreEstatus.arrayValue.values.map((v) => v.stringValue), ['falta', 'llamada_atencion', 'castigo']);
  assert.deepEqual(config.fields.avisarAlPadreAlertas.arrayValue.values.map((v) => v.stringValue), ['inactivo', 'castigo', 'salida_fuera_de_horario']);
  paso('Avisos al padre: se guardan las listas en configuracion/plantel');
  await p.screenshot({ path: `${CAPTURAS}/07-control-avisos.png`, fullPage: true });

  await p.getByRole('link', { name: 'Notificaciones' }).click();
  await p.getByText('Bienvenida a la web').waitFor();
  await p.getByRole('button', { name: 'Marcar todas como leídas' }).click();
  await contador.waitFor({ state: 'hidden' });
  paso('Notificaciones: marcar como leídas apaga el contador del menú');
  await p.context().close();

  const avisosPadre = await listar('usuarios/uid-padre/notificaciones');
  assert.equal(avisosPadre.length, 3, 'el padre debe tener 3 avisos (2 de enfermería + castigo)');
  const pagos = await listar('alumnos/00000000000002/cambiosDeEstado');
  assert.equal(pagos[0].fields.campo.stringValue, 'pago');
  assert.equal(pagos[0].fields.por.stringValue, 'uid-control');
  paso('en la base: aviso del castigo al padre y cambio de pago firmado');
}

// ---------- 5. En el teléfono (pantalla angosta) ----------
{
  const p = await nuevaPagina(390, 844);
  await entrar(p, 'control.prueba');
  await p.getByRole('heading', { name: 'Alumnos', exact: true }).waitFor();
  await p.getByRole('button', { name: 'Abrir el menú' }).click();
  await p.getByRole('link', { name: 'Entradas y salidas' }).click();
  await p.getByText('3 registros').waitFor();
  const desborde = await p.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
  assert.equal(desborde, false, 'no debe haber scroll horizontal en el teléfono');
  paso('en un teléfono (390 px): el menú ☰ abre y no hay scroll horizontal');
  await p.waitForTimeout(400);
  await p.screenshot({ path: `${CAPTURAS}/08-telefono-entradas.png`, fullPage: true });
  await p.context().close();
}

await navegador.close();
const reales = errores.filter((e) => !e.includes('PERMISSION_DENIED') && !e.includes('favicon'));
console.log(reales.length ? `\nErrores en la consola:\n${reales.join('\n')}` : '\nSin errores en la consola del navegador.');
