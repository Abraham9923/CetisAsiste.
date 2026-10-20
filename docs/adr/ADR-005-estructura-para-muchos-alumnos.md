# ADR-005: Estructura de la base de datos para todos los alumnos del plantel

**Estado:** Aceptada y aplicada (19 de octubre de 2026), con los ajustes de "Respuestas del
plantel"
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste junto con Control Escolar

> Describe cómo ordenar la base para que funcione con todo el plantel, semestre tras
> semestre. Lo aplicado está en `BASE_DE_DATOS.md` y en la bitácora (entrada 19-21).

## Respuestas del plantel (19 de octubre de 2026)

| Pregunta | Respuesta | Cómo quedó |
|---|---|---|
| 1. ¿Cuántos alumnos? | 2,500 | Se usan para estimar lecturas y escrituras (abajo) |
| 2. ¿Cómo se nombran los grupos? | Por ahora: matutino desde el 100, vespertino desde el 150 | Matutino x00–x49 (101, 102…), vespertino x50 en adelante (151…). El nombre no se repite entre turnos, así que el id es `{periodoId}_{nombre}` (sin turno) |
| 3. ¿Especialidades? | Solo dejar el espacio | Campo `especialidadId` vacío; reglas para `especialidades`, sin documentos |
| 4. ¿No. de control? | Fórmula pendiente; para probar, desde 25000000 | 8 dígitos; la cuenta de prueba existente es 25311754. *Actualizado por ADR-009: 14 dígitos, como la credencial* |
| 5. ¿Datos personales? | Todos los propuestos; por ahora ficticios | `privado/datos` y `privado/salud` con datos inventados |
| 6. ¿Cuánto guardar entradas y salidas? | Por semestre: cada semestre una lista nueva desde cero | `periodos/{periodoId}/registros` (y `salidasDeGrupo`); las listas viejas se conservan |

## Contexto

### Qué cambia cuando son miles de alumnos

El modelo actual se probó con 1 alumno y 1 grupo. Con el plantel completo aparecen cinco
problemas:

| # | Problema | Ejemplo |
|---|---|---|
| 1 | **Los grupos cambian cada semestre** y el id actual (`102-matutino`) no dice de qué semestre es | En febrero el 102 pasa a ser 202 y entra otro 102. Los registros viejos con `grupoId = "102-matutino"` se mezclan con los nuevos |
| 2 | **No hay historial escolar** | No se puede saber en qué grupo estuvo un alumno el año pasado ni cuándo cambió de turno |
| 3 | **Buscar por nombre** | Firestore no busca "contiene Juan". Con un solo campo `nombre` no se puede ordenar por apellido ni buscar con o sin acentos |
| 4 | **Datos personales delicados** (CURP, domicilio, teléfonos, alergias) | Las reglas protegen **documentos completos, no campos**: si se guardan en la ficha, cualquiera que pueda leer la ficha (el profesor, por ejemplo) los ve |
| 5 | **Volumen de lecturas y escrituras** | El plan gratuito tiene límites diarios (ver abajo) que se rebasan con todo el plantel |

### Cuántos datos son (estimación)

Supuesto: **2,500 alumnos**, dos turnos, 190 días de clase al año. Hay que confirmar el
número real.

| Qué | Por día | Por año |
|---|---|---|
| Escaneos (entrada + salida) → `registros` | ~5,000 escrituras | ~950,000 documentos |
| Actualizar `ubicacion` del alumno en cada escaneo | ~5,000 escrituras | — |
| Avisos al padre (`notificaciones`) | ~5,000 escrituras | — |
| Padres que abren la app (ficha + grupo + 20 registros) | ~2,500 × 22 ≈ 55,000 lecturas | — |

**Límites del plan Spark (gratuito), por día:** 50,000 lecturas, 20,000 escrituras, 20,000
borrados y 1 GB guardado. Con el plantel completo, **las lecturas se pasan del límite** y las
escrituras quedan muy cerca (~15,000 de 20,000). Cuando se rebasa, Firestore deja de
responder hasta el día siguiente.

En el plan **Blaze** los mismos límites siguen siendo gratis y lo que se pasa se cobra por
uso; para estos números serían **unos cuantos dólares al mes** (verificar en la página de
precios de Firebase antes de decidir).

## Decisión propuesta

Ordenar todo alrededor de **periodos escolares**: un grupo existe *dentro de un semestre*, y
el alumno guarda **dónde está ahora** en su ficha y **dónde estuvo** en su historial.

```
configuracion/plantel          periodoActual: "2026-2027-1"  ← el semestre en curso
periodos/{periodoId}           "2026-2027-1" = agosto 2026 – enero 2027
especialidades/{id}            catálogo: "programacion", "contabilidad"…
grupos/{periodoId}_{nombre}_{turno}         ej. 2026-2027-1_102_M
│   └── horario/{dia}          (igual que hoy)
alumnos/{noControl}            ficha ACTUAL (la que lee el escáner y la app)
│   ├── inscripciones/{periodoId}   historial: un documento por semestre cursado
│   ├── privado/datos          CURP, domicilio, teléfonos, tutores, salud (más protegido)
│   ├── estatus/{id}           (igual que hoy)
│   └── cambiosDeEstado/{id}   (igual que hoy)
registros/{id}                 + periodoId (y paginado de 20 en 20)
usuarios/{uid}, roles/{rol}, anuncios, eventos, salidasDeGrupo   (igual que hoy)
```

### 1. Periodos escolares (`periodos/{periodoId}`)

| Campo | Ejemplo |
|---|---|
| id | `2026-2027-1` (agosto–enero) y `2026-2027-2` (febrero–julio) |
| `nombre` | Agosto 2026 – Enero 2027 |
| `inicio`, `fin` | timestamps |
| `activo` | true solo en el periodo en curso |

`configuracion/plantel.periodoActual` dice cuál es el vigente. Al cambiar de semestre,
Control Escolar crea el periodo nuevo y cambia ese campo.

### 2. Grupos por periodo (`grupos/{periodoId}_{nombre}_{turno}`)

| Campo | Ejemplo | Para qué |
|---|---|---|
| `periodoId` | 2026-2027-1 | Saber de qué semestre es |
| `nombre` | 102 | Lo que se muestra |
| `semestre` | 1 | Filtrar "todos los de 1.º" |
| `turno` | matutino | Filtrar por turno |
| `especialidadId` | programacion (o vacío en tronco común) | Filtrar por carrera |
| `profesores`, `tutorId` | uid | Rutas del profesor |
| `activo` | true | Ocultar grupos de periodos pasados |

**Por qué el periodo va en el id:** el 102 de este semestre y el 102 del siguiente son grupos
distintos. Así los registros, horarios y salidas viejos siguen apuntando al grupo correcto, y
el historial no se pierde. La "M" o "V" del turno evita choques si hay un 102 en cada turno.

### 3. Ficha del alumno (`alumnos/{noControl}`) — lo de hoy, ordenado

Se queda **una sola ficha por alumno** (el id sigue siendo el No. de control, así el escáner lo
encuentra con una lectura). Cambios:

| Campo nuevo o cambiado | Ejemplo | Para qué |
|---|---|---|
| `apellidoPaterno`, `apellidoMaterno`, `nombres` | Pérez / Rodríguez / Juan | Ordenar listas como las de Control Escolar |
| `nombre` | Juan Pérez Rodríguez | Se sigue usando para mostrar |
| `nombreBusqueda` | perez rodriguez juan | Minúsculas y **sin acentos**, apellidos primero: permite buscar "perez ro…" |
| `periodoId`, `grupoId`, `semestre`, `turno`, `especialidadId` | 2026-2027-1, 2026-2027-1_102_M, 1… | Dónde está **ahora** |
| `generacion` | 2026-2029 | Año de ingreso y egreso |
| `estado` | inscrito · baja_temporal · baja_definitiva · egresado | Nunca se borra un alumno: cambia su estado |

Lo que ya existe (`activoPorPago`, `activoPorSalud`, `ubicacion`, `codigoQr`, `padres`…) se
queda igual.

### 4. Historial (`alumnos/{noControl}/inscripciones/{periodoId}`)

Un documento por semestre cursado (máximo ~6–8 por alumno):
`grupoId`, `semestre`, `turno`, `especialidadId`, `fechaAlta`, `estado` (cursando,
acreditado, baja…), `por` (quién lo inscribió). Así "¿en qué grupo estuvo en 2027?" es
**una lectura**, sin revisar miles de registros.

### 5. Datos delicados aparte (`alumnos/{noControl}/privado/datos`)

CURP, fecha de nacimiento, domicilio, teléfonos, tutores y datos de salud. Reglas: solo
**Control Escolar** (todo) y **Enfermería** (lo de salud y contactos). Ni el profesor ni el
prefecto lo leen. Al ser una escuela pública, estos datos están protegidos por la ley de
protección de datos personales; separarlos es la forma de que las reglas los cuiden.

### 6. Registros más ligeros

- Agregar `periodoId` a cada registro (reportes por semestre).
- Las pantallas piden **20 a la vez** y cargan más al deslizar (hoy piden 60).
- Opcional más adelante: borrar o exportar registros de periodos de más de N años.

### 7. Altas masivas

Cada agosto entran cientos de alumnos. Capturarlos uno por uno en la consola no es viable.
Se propone una **herramienta de importación desde Excel/CSV** (en la web de Control Escolar
o un script con la cuenta de servicio del proyecto) que cree en lotes de hasta 500: la cuenta
de Authentication, `usuarios/{uid}`, `alumnos/{noControl}` e `inscripciones/{periodoId}`.
Crear cuentas de Authentication en lote solo se puede desde un servidor o script con
permisos de administrador (no desde la app).

### 8. Índices nuevos (para las listas de Control Escolar, Enfermería y profesores)

| Colección | Campos | Para |
|---|---|---|
| `alumnos` | `estado`, `grupoId`, `apellidoPaterno` | Lista de un grupo en orden alfabético |
| `alumnos` | `estado`, `nombreBusqueda` | Buscar por nombre |
| `alumnos` | `estado`, `turno`, `semestre`, `nombreBusqueda` | Filtros por turno y semestre |
| `grupos` | `periodoId`, `turno`, `nombre` | Grupos del semestre por turno |
| `registros` | `periodoId`, `alumnoId`, `fechaHora` ↓ | Historial de un alumno en un semestre |

## Opciones consideradas

### Opción A: dejar el modelo actual

| Aspecto | Evaluación |
|---|---|
| Dificultad | Ninguna |
| Orden con miles de alumnos | Malo: grupos sin semestre, sin historial, búsqueda pobre |
| Seguridad de datos personales | Mala si se agregan CURP o teléfonos a la ficha |
| Costo | El mismo |

**A favor:** no hay que cambiar nada. **En contra:** el primer cambio de semestre mezcla los
datos, y no hay dónde guardar lo delicado.

### Opción B: periodos + ficha actual + historial (la recomendada)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Media: unos campos nuevos y 3 colecciones pequeñas (periodos, especialidades, inscripciones) |
| Orden | Bueno: cada grupo pertenece a un semestre; el historial queda por alumno |
| Rapidez para el escáner | Igual que hoy: una lectura por No. de control |
| Seguridad | Buena: datos delicados en su propio documento |
| Costo | Igual; con paginación se lee menos |

**A favor:** conserva lo que ya funciona (No. de control como id, una ficha) y agrega orden en el
tiempo. **En contra:** al cambiar de semestre hay que actualizar la ficha de cada alumno
(`grupoId`, `semestre`), con la importación masiva.

### Opción C: árbol `periodos/{p}/grupos/{g}/alumnos/{m}`

| Aspecto | Evaluación |
|---|---|
| Dificultad | Alta |
| Orden "a la vista" | Muy claro en la consola |
| Rapidez para el escáner | Mala: con solo el No. de control no se sabe en qué grupo buscar |
| Cambios de grupo | Hay que **mover** el documento (borrar y crear), y se pierden sus subcolecciones |

**A favor:** se ve ordenado. **En contra:** el caso más importante (escanear) se vuelve lento
y las reglas se complican. Descartada.

### Opción D: además, un buscador externo (Algolia o Typesense)

Búsqueda tipo Google ("juan 102"), pero necesita plan Blaze, una extensión y otro servicio
que mantener. Para ~2,500 alumnos la búsqueda por prefijo de B alcanza. Se puede agregar
después sin cambiar la estructura.

## Comparación

La pregunta clave es **qué se consulta más**: el escáner busca *un* alumno por No. de control
cientos de veces al día; Control Escolar lista *un* grupo o busca por nombre. B mantiene la
búsqueda por No. de control en una lectura (como A y a diferencia de C) y agrega lo que A no
tiene: semestres, historial, búsqueda ordenada y datos protegidos. D resuelve la búsqueda
mejor, pero es un costo que todavía no hace falta.

## Consecuencias

- **Más fácil:** reportes por semestre, listas por grupo en orden alfabético, saber la
  historia de un alumno, proteger datos personales, que el escáner siga siendo rápido.
- **Más difícil:** el cambio de semestre se vuelve un proceso (crear periodo, grupos nuevos,
  reinscribir). Hay que tener la herramienta de importación.
- **Hay que revisar después:**
  - El plan: con el plantel completo, **Spark no alcanza**; decidir Blaze antes de usarla con
    todos (ligado a ADR-002).
  - Cuántos años de `registros` se conservan.
  - Si un padre con varios hijos necesita pantalla para elegir hijo (ya pendiente).

## Preguntas para el plantel (cambian detalles, no la estructura)

1. ¿Cuántos alumnos hay en total y por turno?
2. ¿Cómo se nombran los grupos (102, 1AM, 1°A…)? ¿Se repite el nombre entre turnos?
3. ¿Qué especialidades hay y desde qué semestre se separan?
4. ¿Cómo es el No. de control (cuántos dígitos, de dónde sale)?
5. ¿Qué datos personales necesita Control Escolar (CURP, domicilio, teléfonos, tipo de
   sangre…) y cuáles Enfermería?
6. ¿Cuántos años hay que guardar las entradas y salidas?

## Cambios respecto a la propuesta original

- **Registros por semestre:** en lugar de `registros/{id}` con un campo `periodoId`, cada
  semestre tiene su propia lista `periodos/{periodoId}/registros` (pedido del plantel:
  "cada semestre una lista nueva desde cero"). Lo mismo con `salidasDeGrupo`. Por eso ya no
  hace falta el índice `periodoId + alumnoId + fechaHora`.
- **Id del grupo sin turno:** `2026-2027-1_102`, porque el nombre ya distingue el turno.
- **Fechas de periodos e inscripciones como texto** `AAAA-MM-DD` (más fácil de capturar en
  la consola que un timestamp).

## Acciones

1. [x] Contestar las preguntas de arriba.
2. [x] Aprobar este ADR (con los cambios de arriba).
3. [x] Crear `periodos/2026-2027-1` y `configuracion/plantel.periodoActual`; reglas para
   `especialidades` (sin documentos todavía).
4. [x] Campos nuevos en `alumnos` y `grupos`; subcolecciones `inscripciones` y `privado`;
   reglas, 4 índices nuevos, `Modelos.kt` y `BASE_DE_DATOS.md`.
5. [x] Datos de prueba al formato nuevo: `grupos/2026-2027-1_102` (reemplaza a
   `prueba-matutino`, que se borró).
6. [x] Entradas y salidas de 20 en 20 con "Ver más".
7. [ ] Herramienta de importación masiva (CSV/Excel).
8. [ ] Decidir el plan de Firebase (Spark o Blaze) antes de cargar a todo el plantel.
9. [ ] Receta o herramienta para el cambio de semestre.
10. [ ] Catálogo de especialidades, cuando el plantel lo dé. (El No. de control ya quedó: 14 dígitos, ADR-009.)
