// Pruebas de web/js/avisos.js y web/js/formatos.js (sin navegador ni Firebase).
// Cómo correrlas, desde la carpeta web: node --test pruebas/
// Las mismas reglas que app/src/test/.../AvisosTest.kt: si una cambia, cambia las dos.

import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  ALERTAS_POR_OMISION,
  DIAS_DE_VIGENCIA,
  alPadrePorEstatus,
  alertasQueSeAvisan,
  destinatarios,
  estatusQueSeAvisan,
  nuevoAviso,
} from '../js/avisos.js';
import {
  correoDeAcceso,
  cuando,
  esNoControl,
  estadoVisible,
  fechaEnTexto,
  grupoDeId,
  hora,
  normalizar,
} from '../js/formatos.js';

test('sin configuración se usan las listas de por omisión (iguales a Avisos.kt)', () => {
  assert.deepEqual(estatusQueSeAvisan({}), ['falta', 'reporte', 'llamada_atencion', 'castigo']);
  assert.deepEqual(alertasQueSeAvisan(undefined), ['inactivo', 'salida_fuera_de_horario']);
  assert.deepEqual([...ALERTAS_POR_OMISION], ['inactivo', 'salida_fuera_de_horario']);
});

test('una lista vacía en la configuración apaga los avisos', () => {
  assert.deepEqual(estatusQueSeAvisan({ avisarAlPadreEstatus: [] }), []);
  assert.equal(alPadrePorEstatus('falta', { avisarAlPadreEstatus: [] }), false);
});

test('el pase de salud siempre se avisa; los demás según la configuración', () => {
  assert.equal(alPadrePorEstatus('pase_salud', { avisarAlPadreEstatus: [] }), true);
  assert.equal(alPadrePorEstatus('castigo', {}), true);
  assert.equal(alPadrePorEstatus('alta_practicas', {}), false);
  assert.equal(alPadrePorEstatus('reporte', { avisarAlPadreEstatus: ['falta'] }), false);
});

test('los destinatarios son los padres sin vacíos ni repetidos', () => {
  assert.deepEqual(destinatarios({ padres: ['a', '', 'b', 'a', '  '] }), ['a', 'b']);
  assert.deepEqual(destinatarios(null), []);
  assert.deepEqual(destinatarios({}), []);
});

test('un aviso nuevo tiene exactamente los campos que piden las reglas', () => {
  const ahora = new Date(2026, 9, 20, 7, 5);
  const aviso = nuevoAviso({ tipo: 'estado', titulo: '  Inactivo por pago ', mensaje: 'x', alumnoId: '00000000000001', creadaPor: 'uid', ahora });
  assert.deepEqual(Object.keys(aviso).sort(), ['alumnoId', 'creadaPor', 'fecha', 'leida', 'mensaje', 'tipo', 'titulo', 'venceEn']);
  assert.equal(aviso.titulo, 'Inactivo por pago');
  assert.equal(aviso.leida, false);
  assert.equal(Math.round((aviso.venceEn - ahora) / 86400000), DIAS_DE_VIGENCIA);
});

test('los textos largos se recortan a lo que aceptan las reglas', () => {
  const aviso = nuevoAviso({ tipo: 'estatus', titulo: 't'.repeat(300), mensaje: 'm'.repeat(900), creadaPor: 'uid' });
  assert.equal(aviso.titulo.length, 120);
  assert.equal(aviso.mensaje.length, 500);
});

test('No. de control y correo interno (como la app)', () => {
  assert.equal(esNoControl('00000000000001'), true);
  assert.equal(esNoControl('2531175'), false);
  assert.equal(esNoControl('0000000000000a'), false);
  assert.equal(correoDeAcceso(' Enfermeria.Uno ', 'cetis61.mx'), 'enfermeria.uno@cetis61.mx');
  assert.equal(correoDeAcceso('alguien@gmail.com', 'cetis61.mx'), 'alguien@gmail.com');
});

test('normalizar quita acentos y mayúsculas, como nombreBusqueda', () => {
  assert.equal(normalizar('  PÉREZ   Rodríguez Juan '), 'perez rodriguez juan');
  assert.equal(normalizar('Núñez'), 'nunez');
});

test('fechas y horas', () => {
  assert.equal(fechaEnTexto(new Date(2026, 9, 5)), '2026-10-05');
  assert.equal(hora(new Date(2026, 9, 5, 7, 5)), '7:05 am');
  assert.equal(hora(new Date(2026, 9, 5, 12, 30)), '12:30 pm');
  assert.equal(hora(new Date(2026, 9, 5, 0, 0)), '12:00 am');
  assert.equal(hora({ toDate: () => new Date(2026, 9, 5, 18, 0) }), '6:00 pm');
  const ahora = new Date(2026, 9, 20, 9, 0);
  assert.equal(cuando(new Date(2026, 9, 20, 7, 5), ahora), 'Hoy, 7:05 am');
  assert.equal(cuando(new Date(2026, 9, 18, 7, 5), ahora), '18 oct 2026, 7:05 am');
});

test('nombre del grupo desde su id y estado visible', () => {
  assert.equal(grupoDeId('2026-2027-1_102'), '102');
  assert.equal(grupoDeId(''), '');
  assert.deepEqual(estadoVisible({ activoPorPago: false, activoPorSalud: false }), { texto: 'Inactivo por pago', peligro: true });
  assert.deepEqual(estadoVisible({ activoPorSalud: false }), { texto: 'Inactivo por salud', peligro: true });
  assert.deepEqual(estadoVisible({ enPracticas: true }), { texto: 'En prácticas', peligro: false });
  assert.deepEqual(estadoVisible({}), { texto: 'Activo', peligro: false });
});
