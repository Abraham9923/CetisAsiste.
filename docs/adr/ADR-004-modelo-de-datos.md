# ADR-004: Forma de la base de datos (colecciones, ids y datos sin ejemplos)

**Estado:** Aceptada (aplicada el 19 de octubre de 2026)
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste

## Contexto

- El documento "Pantallas por rol" define 10 clases (Usuario, Alumno, Grupo, ClaseHorario,
  Estatus, Registro, SalidaDeGrupo, Anuncio, Evento, Notificación) y dice: "en Firestore cada
  clase sería una colección y las relaciones se guardan como el id".
- El "Proceso 8" del manual técnico propone `usuarios/{uid}`, `grupos/{grupo}/horario/{dia}`
  y `anuncios/{id}`.
- Firestore cobra (y tarda) **por documento leído**, no permite "joins" y cada búsqueda que
  filtra y ordena por campos distintos necesita un índice.
- El usuario pidió **no tener datos de ejemplo**: todo lo cargará Control Escolar.

## Decisión

| Decisión | Motivo |
|---|---|
| Colecciones de primer nivel para lo que se busca entre muchos alumnos: `registros`, `salidasDeGrupo`, `anuncios`, `eventos` | El prefecto y Control Escolar buscan en todos a la vez ("¿quién entró hoy?") sin índices especiales de "grupo de colecciones" |
| Subcolecciones para lo que siempre se consulta de **un** dueño: `alumnos/{m}/estatus`, `usuarios/{uid}/notificaciones`, `grupos/{g}/horario` | Las reglas quedan más simples ("el dueño lee lo suyo") |
| `alumnos` usa la **No. de control** como id | El escáner lo encuentra con una sola lectura directa |
| `usuarios` usa el **uid** de Authentication como id | Las reglas pueden revisar el rol con `get(usuarios/uid)` |
| `alumnos` separado de `usuarios` | El alumno existe aunque todavía no tenga cuenta, y el prefecto necesita datos (pago, salud, ubicación) que no son de la cuenta |
| Horario: **un documento por día** con la lista de clases | Una lectura llena la pantalla y la comparten los 40 alumnos del grupo |
| Algunos datos repetidos a propósito (`grupo`, `alumnoNombre`, `…PorNombre`) | Mostrar una lista sin leer otro documento por cada renglón |
| QR con un texto al azar (`codigoQr`), no el No. de control — *reemplazado por ADR-009: el QR lleva el No. de control* | No se puede falsificar y se puede cambiar si se pierde el teléfono |
| La base **arranca vacía** (solo `configuracion/plantel`) | Pedido del usuario: los datos reales los da de alta Control Escolar |
| Colección `roles` con los 6 perfiles de usuario (agregada el mismo día) | Que cada tipo de usuario esté descrito en la base: plataforma, pantallas y permisos. Son configuración, no datos de personas. Los permisos **describen**; quien protege los datos siguen siendo las reglas |

## Opciones consideradas

### Opción A: todo en subcolecciones del alumno (`alumnos/{m}/registros`)

Más ordenado a la vista, pero "registros de hoy de todo el plantel" necesitaría consultas de
grupo de colecciones con índices especiales. Descartada para `registros`.

### Opción B: un solo documento por alumno con todo adentro (listas de registros)

Una lectura por alumno, pero los documentos tienen límite de 1 MB y cada escaneo reescribiría
el documento completo. Descartada.

### Opción C: la elegida (mezcla de A y colecciones de primer nivel)

Ver tabla de decisión.

## Consecuencias

- **Más fácil:** cada pantalla se llena con una o dos consultas; las reglas son legibles.
- **Más difícil:** los datos repetidos (por ejemplo el nombre del alumno dentro de cada
  registro) no se actualizan solos si cambia el original.
- **Datos de ejemplo:** se cargaron 32 documentos de prueba y se quitaron el mismo día a
  pedido del usuario (ver `BITACORA.md`). La app ya no tiene `SampleData.kt`.
- **Hay que revisar después:** si un padre tiene varios hijos, la app muestra el primero.

## Acciones

1. [x] Crear `datos/Modelos.kt` con una clase por tipo de documento.
2. [x] Crear los 6 índices compuestos.
3. [x] Documentar todo en `BASE_DE_DATOS.md`.
4. [ ] Pantalla para elegir hijo cuando un padre tenga varios.
