# Base de datos — CetisAsiste (Firebase)

**Proyecto de Firebase:** `appcetis-7c79d` (nombre visible: *appcetis*), plan **Spark** (gratuito)
**Base de datos:** Cloud Firestore, base `(default)`
**Inicio de sesión:** Firebase Authentication, método **Correo/Contraseña**
**Fecha:** 19 de octubre de 2026

Este documento explica **qué hay en la base de datos, quién puede ver o cambiar cada cosa y
cómo se dan de alta los datos**. El *porqué* de las decisiones está en los ADR de
[`docs/adr/`](adr/) y lo que se hizo, paso por paso, en [BITACORA.md](BITACORA.md).

---

## 1. Resumen en 30 segundos

1. **Cada persona tiene una cuenta** en Authentication y un documento en `usuarios/{uid}`
   que dice su **rol**: alumno, padre, prefecto, profesor, enfermería o control escolar.
2. **Nadie se registra desde la app.** Control Escolar da de alta a cada persona.
3. **El alumno entra con su No. de control** (14 dígitos, el de su credencial) y el personal
   con su usuario. La app los convierte en un correo interno (`noControl@cetis61.mx`) que la
   persona no necesita conocer. Su **código QR** lleva `CETIS61:` + su No. de control
   (ADR-009).
4. **Las contraseñas no están en Firestore.** Las guarda Authentication, cifradas.
5. **El alumno es el centro:** `alumnos/{No. de control}` guarda su grupo, si está activo por pago
   o salud y dónde está; sus entradas y salidas van en la lista del semestre
   (`periodos/{semestre}/registros`), que empieza vacía cada semestre.
6. **Las reglas de seguridad deciden quién ve qué.** Un alumno solo ve lo suyo, un padre
   lo de su hijo, un profesor lo de sus grupos y Control Escolar todo.
7. **La base empezó vacía**, sin datos de ejemplo. Hoy tiene los ajustes del plantel
   (`configuracion/plantel`), los **6 perfiles de usuario** (`roles`), el semestre en curso
   (`periodos/2026-2027-1`) y unos pocos datos de prueba que dicen "Prueba" (§10.1).

---

## 2. Dónde está cada cosa

| Qué | Dónde |
|---|---|
| Consola de Firebase | [console.firebase.google.com → appcetis](https://console.firebase.google.com/project/appcetis-7c79d/overview) |
| Datos (colecciones) | Consola → *Firestore* → pestaña **Data** |
| Reglas de seguridad publicadas | Consola → *Firestore* → pestaña **Rules** |
| Índices | Consola → *Firestore* → pestaña **Indexes** → *Manual* |
| Cuentas | Consola → *Authentication* → pestaña **Users** |
| Copia de las reglas en el proyecto | `firebase/firestore.rules` |
| Copia de los índices en el proyecto | `firebase/firestore.indexes.json` |
| Copia de los perfiles de usuario | `firebase/roles.json` |
| Configuración para la herramienta `firebase` (futuro) | `firebase/firebase.json` y `firebase/.firebaserc` |
| Código de la app que habla con Firebase | `app/src/main/java/com/example/cetisasiste/datos/` |

> **Importante:** si cambias las reglas en la consola, copia el mismo texto en
> `firebase/firestore.rules` (y al revés). Así el proyecto siempre tiene la versión vigente.

---

## 3. Mapa de la base de datos

```
Firestore (default)
│
├── usuarios/{uid}                 ← una cuenta por persona (id = uid de Authentication)
│   └── notificaciones/{id}        ← avisos personales de esa cuenta
│
├── alumnos/{noControl}            ← ficha ACTUAL del alumno (id = No. de control)
│   ├── inscripciones/{periodoId}  ← historial: un documento por semestre cursado
│   ├── privado/datos              ← CURP, domicilio, teléfonos, tutores (solo Control Escolar)
│   ├── privado/salud              ← tipo de sangre, alergias, NSS… (y Enfermería)
│   ├── estatus/{id}               ← castigos, faltas, reportes, pases de salud, prácticas
│   └── cambiosDeEstado/{id}       ← historial de activado/desactivado por pago o salud
│
├── periodos/{periodoId}           ← un semestre: 2026-2027-1 = agosto 2026 – enero 2027
│   ├── registros/{id}             ← entradas y salidas DE ESE SEMESTRE (empieza vacía)
│   └── salidasDeGrupo/{id}        ← grupos liberados DE ESE SEMESTRE
│
├── grupos/{periodoId}_{nombre}    ← por ejemplo grupos/2026-2027-1_102
│   └── horario/{dia}              ← lunes, martes, miercoles, jueves, viernes
│
├── especialidades/{id}            ← catálogo de carreras técnicas (todavía vacío)
├── anuncios/{id}                  ← boletín de noticias
├── eventos/{id}                   ← calendario escolar
├── roles/{rol}                    ← perfil de cada tipo de usuario (6 documentos)
└── configuracion/plantel          ← ajustes generales; periodoActual = semestre en curso
```

**Todo se ordena por semestre** (ADR-005): cada grupo pertenece a un periodo, y cada periodo
tiene su propia lista de entradas y salidas, que **empieza desde cero** al iniciar el
semestre. Las listas de semestres anteriores se conservan.

`usuarios/{uid}.rol` dice **qué perfil** tiene la persona: su valor es el id de un
documento de `roles` (por ejemplo `rol = "padre"` → `roles/padre`).

Cómo se relacionan (las relaciones se guardan **como el id** del otro documento):

```
usuarios/{uid} ──(noControl)──► alumnos/{noControl} ◄──(hijos[])── usuarios/{uid del padre}
                                     │
                     (grupoId)       │        (alumnoId)
grupos/{grupoId} ◄───────────────────┤◄──────────────── periodos/{periodo}/registros/{id}
       │  (periodoId)                ├── inscripciones/{periodoId}
       └──────────► periodos/{id}    ├── privado/datos · privado/salud
                                     ├── estatus/{id}
                                     └── cambiosDeEstado/{id}
configuracion/plantel.periodoActual ──► periodos/{id} (el semestre en curso)
```

---

## 4. Las colecciones, una por una

**Tipos de dato** (como aparecen en la consola): *string* = texto, *boolean* = sí/no,
*number* = número, *timestamp* = fecha y hora, *array* = lista, *map* = grupo de campos,
*null* = vacío.

### 4.1 `usuarios/{uid}` — cuentas

**Para qué:** saber quién inició sesión y qué rol tiene. **Id:** el *User UID* que muestra
Authentication al crear la cuenta.

| Campo | Tipo | Ejemplo | Notas |
|---|---|---|---|
| `nombre` | string | Juan Perez Rodriguez | Nombre completo |
| `usuario` | string | 00000000000000 | Con lo que inicia sesión: No. de control (alumno) o usuario (los demás) |
| `correo` | string | 00000000000000@cetis61.mx | El correo de su cuenta en Authentication |
| `rol` | string | alumno | Ver [valores de rol](#51-roles) |
| `activo` | boolean | true | Si es `false`, la app no lo deja entrar y las reglas le niegan todo |
| `debeCambiarContrasena` | boolean | false | `true` = al entrar, la app le pide cambiar su contraseña antes de continuar |
| `contrasenaVence` | timestamp | 17 ene 2027 | Cuándo vence la contraseña; si ya pasó, la app pide cambiarla. Al cambiarla, la app pone hoy + `diasVenceContrasena` |
| `noControl` | string | 00000000000000 | **Solo alumno.** Id de su ficha en `alumnos` |
| `hijos` | array de string | ["00000000000000"] | **Solo padre.** Números de control de sus hijos |
| `grupos` | array de string | ["2026-2027-1_102"] | **Solo profesor.** Grupos donde da clase **este semestre** (se actualiza cada semestre) |
| `fotoUrl` | string | "" | Opcional |
| `tokensFcm` | array de string | [] | Para avisos push (futuro). Lo llena la app |
| `creadoEn` / `actualizadoEn` | timestamp | | Fechas de control |

**Quién puede:** cada quien **lee** su propio documento y solo puede cambiar `tokensFcm`,
`debeCambiarContrasena`, `contrasenaVence` y `actualizadoEn`. **Control Escolar** lee y
cambia todos.

#### Subcolección `usuarios/{uid}/notificaciones/{id}` — el buzón de avisos

Todos los avisos de una persona van aquí, sin importar quién los mande (ADR-010). Las reglas
exigen **exactamente** estos campos (ni uno más):

| Campo | Tipo | Ejemplo |
|---|---|---|
| `tipo` | string | `entrada`, `salida`, `alerta`, `estatus`, `estado`, `anuncio`, `recordatorio`, `credencial` o `salida_grupo` |
| `titulo` | string | Entrada registrada (de 1 a 120 letras) |
| `mensaje` | string | Juan entró al plantel a las 7:05 am. (hasta 500 letras) |
| `fecha` | timestamp | |
| `leida` | boolean | false (siempre false al crearlo) |
| `alumnoId` | string | No. de control del alumno del que trata (vacío si no trata de un alumno) |
| `creadaPor` | string | uid de quien lo mandó (debe ser el de su sesión) |
| `venceEn` | timestamp | `fecha` + 60 días: desde ahí se puede borrar |

Qué avisos hay y quién los manda:

| Evento | Lo manda | `tipo` | ¿Siempre? |
|---|---|---|---|
| Entrada o salida sin alerta | Prefecto | `entrada` / `salida` | Sí |
| Alerta al escanear | Prefecto | `alerta` | Solo las de `configuracion/plantel.avisarAlPadreAlertas` (§4.8) |
| Falta, reporte, llamada de atención, castigo | Profesor | `estatus` | Solo los de `avisarAlPadreEstatus` (§4.8) |
| Pase de salida por salud | Enfermería (web) | `estatus` | Sí |
| Activado o desactivado por salud o por pago | Enfermería o Control Escolar (web) | `estado` | Sí |

**Quién puede:** el dueño los lee, los borra y los marca como leídos (solo el campo
`leida`). Los **crean**: Control Escolar, a cualquier cuenta; prefecto y Enfermería, solo a
los **padres del alumno** del aviso (`alumnoId`); el profesor, solo a los padres de un alumno
**de sus grupos**. En el futuro también los creará una Cloud Function (y mandará el push).

**Espacio:** con todo el plantel se juntan miles de avisos al día. Conviene activar una
política **TTL** (borrado automático) sobre `venceEn` y excluir `titulo` y `mensaje` de los
índices automáticos (consola → *Firestore* → *Indexes* → *Single field*). Ver ADR-010.

### 4.2 `alumnos/{noControl}` — ficha del alumno

**Para qué:** todo lo que el prefecto necesita al escanear y lo que ven alumno y padre.
**Id:** el No. de control (así el escáner la encuentra directo, sin buscar).

| Campo | Tipo | Ejemplo | Quién lo cambia |
|---|---|---|---|
| `noControl` | string | 00000000000001 (**14 dígitos**, el No. de control de la credencial; antes este campo se llamaba `matricula`) | Control Escolar |
| `nombre` | string | Juan Pérez Rodríguez (completo, para mostrar) | Control Escolar |
| `apellidoPaterno` / `apellidoMaterno` / `nombres` | string | Pérez / Rodríguez / Juan | Control Escolar (para ordenar listas). **Siempre llenar `apellidoPaterno`:** sin él, el alumno no aparece en la lista del profesor |
| `nombreBusqueda` | string | perez rodriguez juan (minúsculas, **sin acentos**, apellidos primero) | Control Escolar (para buscar) |
| `estado` | string | inscrito · baja_temporal · baja_definitiva · egresado | Control Escolar. **Un alumno nunca se borra**: cambia su estado |
| `periodoId` | string | 2026-2027-1 (semestre en curso) | Control Escolar |
| `semestre` | number | 1 | Control Escolar |
| `especialidadId` | string | "" (vacío por ahora) | Control Escolar |
| `generacion` | string | 2026-2029 | Control Escolar |
| `usuarioId` | string | uid de su cuenta | Control Escolar |
| `padres` | array de string | [uid del padre] | Control Escolar |
| `grupoId` | string | 2026-2027-1_102 (grupo **actual**) | Control Escolar |
| `grupo` | string | 102 | Control Escolar. Copia de respaldo: la app muestra `grupos/{grupoId}.nombre` |
| `turno` | string | matutino | Control Escolar |
| `correo` | string | correo@institucional.mx | Control Escolar |
| `vigencia` | string | Ciclo 2026–2027 | Control Escolar (credencial) |
| `fotoUrl` | string | "" | Control Escolar (opcional) |
| `activoPorPago` | boolean | true | Control Escolar |
| `activoPorSalud` | boolean | true | Enfermería |
| `enPracticas` | boolean | false | Control Escolar |
| `ubicacion` | string | fuera | Prefecto (al escanear): fuera, dentro o salida_autorizada |
| `ultimoRegistro` | map `{tipo, fechaHora}` | {salida, 5 oct 2:01 pm} | Prefecto (al escanear) |
| `creadoEn` / `actualizadoEn` | timestamp | | |

> **¿Qué lleva el QR?** `CETIS61:` + el No. de control (por ejemplo `CETIS61:00000000000001`).
> La app lo dibuja sola: **no hay que guardar nada para el QR**. Antes había un campo
> `codigoQr` con un texto al azar; ya no se usa y, si un documento viejo lo trae, se ignora.
> Por qué: [ADR-009](adr/ADR-009-no-de-control-y-escaneo.md).

**Quién puede leer:** prefecto, enfermería y Control Escolar (todos); el propio alumno; sus
padres; los profesores que dan clase en su grupo.

#### Subcolección `alumnos/{noControl}/inscripciones/{periodoId}` — historial escolar

Un documento por semestre cursado (máximo ~6–8 por alumno). Así "¿en qué grupo estuvo en
2027?" es una sola lectura.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `grupoId` / `grupo` | string | 2026-2027-1_102 / 102 |
| `semestre` | number | 1 |
| `turno` | string | matutino |
| `especialidadId` | string | "" |
| `fechaAlta` | string | 2026-08-01 (AAAA-MM-DD) |
| `estado` | string | cursando, acreditado, baja… |
| `por` / `porNombre` | string | uid y nombre de quien lo inscribió |

**Quién puede:** lo leen prefecto, enfermería, Control Escolar, el alumno y su padre; lo
escribe Control Escolar.

#### Subcolección `alumnos/{noControl}/privado` — datos delicados

Las reglas protegen **documentos completos, no campos sueltos**. Por eso lo delicado va en
dos documentos aparte y **no** en la ficha (que leen el profesor y el prefecto):

| Documento | Campos | Quién lo lee y escribe |
|---|---|---|
| `privado/datos` | `curp`, `fechaNacimiento` (AAAA-MM-DD), `domicilio`, `telefono`, `correoPersonal`, `tutores` (lista de `{nombre, parentesco, telefono}`) | Solo Control Escolar |
| `privado/salud` | `tipoSangre`, `alergias`, `enfermedades`, `medicamentos` (listas), `nss` (seguro social), `contactoEmergencia`, `telefonoEmergencia` | Control Escolar y Enfermería |

Son datos personales de menores protegidos por ley: no copiarlos a otros documentos.

#### Subcolección `alumnos/{noControl}/estatus/{id}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| `tipo` | string | castigo, falta, reporte, llamada_atencion, pase_salud, alta_practicas |
| `descripcion` | string | Falta notificada en Matemáticas |
| `fecha` | timestamp | |
| `vigenteHasta` | timestamp o null | Hasta cuándo aplica (castigos, pases) |
| `asignadoPor` / `asignadoPorNombre` | string | uid y nombre de quien lo puso |

**Quién puede:** lo leen prefecto, enfermería, Control Escolar y los profesores del grupo.
Lo crean: **profesor** (falta, reporte, llamada_atencion, castigo), **enfermería**
(pase_salud) y **Control Escolar** (cualquiera). Solo Control Escolar lo edita o borra.
Por ahora **el alumno y el padre no los ven** (pregunta abierta del documento de roles).

#### Subcolección `alumnos/{noControl}/cambiosDeEstado/{id}`

Historial de activaciones (el documento de roles pide "historial de cambios" del pago).

| Campo | Tipo | Ejemplo |
|---|---|---|
| `campo` | string | pago o salud |
| `valor` | boolean | false = se desactivó, true = se activó |
| `motivo` | string | Adeudo de inscripción |
| `fecha` | timestamp | |
| `por` / `porNombre` | string | uid y nombre de quien hizo el cambio |

**Quién puede:** Control Escolar crea los de `pago`; Enfermería, los de `salud`. Nadie los
edita ni borra (es historial).

### 4.3 `grupos/{periodoId}_{nombre}` y su horario

**Id del grupo:** el semestre y el nombre, por ejemplo `2026-2027-1_102`. El 102 de este
semestre y el 102 del siguiente son **grupos distintos**: así los horarios y registros
viejos no se mezclan con los nuevos.

**Cómo se nombran (por ahora):** matutino del **x00 al x49** (101, 102… 301…), vespertino del
**x50 en adelante** (151, 152… 351…). Así un nombre nunca se repite entre turnos.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `nombre` | string | 102 |
| `periodoId` | string | 2026-2027-1 |
| `turno` | string | matutino |
| `semestre` | number | 1 |
| `especialidadId` | string | "" (vacío por ahora) |
| `tutorId` | string | uid del profesor tutor ("" si no hay) |
| `activo` | boolean | true (false cuando termina el semestre) |
| `cicloEscolar` | string | Ciclo 2026–2027 |
| `profesores` | array de string | uid de sus profesores |

**`grupos/{grupoId}/horario/{dia}`** — un documento por día, con id `lunes`, `martes`,
`miercoles`, `jueves` o `viernes` (sin acento):

| Campo | Tipo | Ejemplo |
|---|---|---|
| `dia` | string | miercoles |
| `orden` | number | 3 (1 = lunes … 5 = viernes) |
| `clases` | array de map | ver abajo |

Cada clase dentro de `clases`:

| Campo | Tipo | Ejemplo |
|---|---|---|
| `materia` | string | Matemáticas |
| `horaInicio` / `horaFin` | string | 07:00 / 07:50 (formato de 24 horas) |
| `profesorId` / `profesor` | string | uid y nombre del profesor |
| `aula` | string | B-12 (opcional) |
| `ultimaDelTurno` | boolean | true solo en la última clase: ahí el profesor da salida al grupo |

> **¿Por qué un documento por día?** Toda la pantalla de Horario se llena con **una sola
> lectura**, y los 40 alumnos de un grupo leen el mismo documento.

**Quién puede:** cualquier cuenta activa lo lee; solo Control Escolar lo cambia.

### 4.4 `periodos/{periodoId}` y sus `registros` — entradas y salidas por semestre

**`periodos/{periodoId}`** es un semestre: `2026-2027-1` (agosto–enero) y `2026-2027-2`
(febrero–julio).

| Campo | Tipo | Ejemplo |
|---|---|---|
| `nombre` | string | Agosto 2026 – Enero 2027 |
| `inicio` / `fin` | string | 2026-08-01 / 2027-01-31 (AAAA-MM-DD) |
| `activo` | boolean | true solo en el semestre en curso |

`configuracion/plantel.periodoActual` dice cuál es el semestre en curso; la app lo usa para
saber en qué lista leer.

**`periodos/{periodoId}/registros/{id}`** — cada vez que el prefecto escanea un QR o teclea
un No. de control se crea **un** registro, aunque haya alerta. Es una **bitácora**: nadie la
edita ni la borra. **Cada semestre empieza con la lista vacía**; las de semestres
anteriores se conservan.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `alumnoId` | string | No. de control (o lo que se tecleó, si no existe) |
| `alumnoNombre` | string | Juan Pérez Rodríguez |
| `grupoId` | string | 2026-2027-1_102 |
| `tipo` | string | entrada o salida |
| `fechaHora` | timestamp | 5 oct 2026 7:05 am |
| `fecha` | string | 2026-10-05 (para buscar por día sin cálculos) |
| `medio` | string | qr o no_control (tecleado por el prefecto) |
| `registradoPor` / `registradoPorNombre` | string | uid y nombre del prefecto |
| `alerta` | string o null | null si todo bien; si no, ver [alertas](#53-alertas-del-escaneo) |

**Quién puede:** lo leen prefecto, enfermería, Control Escolar, el alumno (los suyos), el
padre (los de sus hijos) y el profesor (los de sus grupos). Solo el **prefecto** lo crea, y
con su propio uid en `registradoPor`.

### 4.5 `periodos/{periodoId}/salidasDeGrupo/{id}` — el profesor libera al grupo

También una lista por semestre.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `grupoId` / `grupo` | string | 2026-2027-1_102 / 102 |
| `fecha` | string | 2026-10-05 |
| `fechaHora` | timestamp | |
| `autorizadaPor` / `autorizadaPorNombre` | string | uid y nombre del profesor |
| `materia` | string | Tecnologías de la información |

**Quién puede:** lo crea un profesor **de ese grupo**; lo leen prefecto, profesores y
Control Escolar.

### 4.6 `anuncios/{id}` — boletín de noticias

| Campo | Tipo | Ejemplo |
|---|---|---|
| `area` | string | Prefectura (se muestra como etiqueta) |
| `fecha` | timestamp | |
| `titulo` | string | Horario de acceso |
| `texto` | string | Recuerda que la puerta principal cierra a las 7:15 am… |
| `destacado` | boolean | true = aparece en "Información importante" del Inicio del padre |
| `dirigidoA` | array de string | ["alumno", "padre", "profesor", …] — **si un rol no está, no lo ve** |
| `publicadoPor` / `publicadoPorNombre` | string | uid y nombre |

**Quién puede:** lo lee cualquier cuenta activa; lo escribe Control Escolar.

### 4.7 `eventos/{id}` — calendario

| Campo | Tipo | Ejemplo |
|---|---|---|
| `titulo` | string | Junta de padres de familia |
| `inicio` / `fin` | timestamp | 9 oct 6:00 pm / 9 oct 7:30 pm |
| `lugar` | string | Auditorio |
| `area` | string | Dirección |
| `detalle` | string | Texto opcional |

**Quién puede:** lo lee cualquier cuenta activa; lo escribe Control Escolar.

### 4.8 `configuracion/plantel` — ajustes generales

Ya existe, cargado el 19 de octubre.

| Campo | Valor actual |
|---|---|
| `nombre` | CETis 61 |
| `ciudad` | Cd. Juárez |
| `cicloEscolar` | Ciclo 2026–2027 |
| `periodoActual` | 2026-2027-1 (el semestre en curso; id en `periodos`) |
| `turnos` | ["matutino", "vespertino"] |
| `horaCierrePuerta` | 07:15 |
| `diasVenceContrasena` | 90 (días que dura una contraseña nueva; 0 = no vence) |
| `dominioCorreo` | cetis61.mx |
| `avisarAlPadreEstatus` | *(no existe todavía)* Estatus del profesor que se le avisan al padre. Si no existe: `["falta", "reporte", "llamada_atencion", "castigo"]`. `[]` = ninguno (ADR-010) |
| `avisarAlPadreAlertas` | *(no existe todavía)* Alertas del escaneo que se le avisan al padre. Si no existe: `["inactivo", "salida_fuera_de_horario"]`. `[]` = ninguna. Las entradas y salidas sin alerta se avisan siempre (ADR-010) |

> Los dos últimos solo hay que crearlos si el plantel quiere algo distinto de lo de por
> omisión: consola → `configuracion/plantel` → *Add field* → tipo **array** de **string**.

### 4.9 `roles/{rol}` — perfiles de usuario

**Para qué:** describir cada **tipo de usuario** de la app: cómo se llama, en qué plataforma
trabaja, con qué inicia sesión, qué pantallas tiene y qué puede hacer. No son personas: son
los "moldes" que Control Escolar asigna al dar de alta una cuenta (campo `usuarios.rol`).
**Id:** el valor del rol. Ya están cargados los 6 (19 de octubre).

| Campo | Tipo | Ejemplo (`roles/padre`) |
|---|---|---|
| `orden` | int64 | 2 (para listarlos en orden) |
| `nombre` | string | Padre de familia |
| `etiqueta` | string | Padre de familia (texto en tarjetas; el alumno dice "Estudiante") |
| `descripcion` | string | Sigue las entradas y salidas de su hijo, los avisos y el calendario del plantel. |
| `plataformas` | array de string | ["android"] o ["web"] |
| `plataformaConfirmada` | boolean | true (Enfermería: false, "web por confirmar") |
| `inicioDeSesion` | string | `noControl` (alumno) o `usuario` (los demás) |
| `camposDelUsuario` | array de string | ["hijos"] — campos extra que lleva `usuarios/{uid}` con este rol |
| `cambioDeContrasenaObligatorio` | boolean | false (prefecto: true) |
| `pantallas` | array de string | Inicio, Entradas y salidas, Notificaciones, … |
| `permisos` | array de string | ver_perfil_hijo, ver_qr_hijo, … |
| `activo` | boolean | true |

Resumen de los 6 perfiles:

| Id | Nombre | Plataforma | Inicia sesión con | Campo extra en `usuarios` | Cambio de contraseña obligatorio | Pantallas | Permisos |
|---|---|---|---|---|---|---|---|
| `alumno` | Alumno ("Estudiante") | Android | No. de control | `noControl` | No | 6 | 6 |
| `padre` | Padre de familia | Android | usuario | `hijos` | No | 9 | 8 |
| `prefecto` | Prefecto | Android | usuario | — | **Sí** | 8 | 9 |
| `profesor` | Profesor | Android | usuario | `grupos` | No | 6 | 6 |
| `enfermeria` | Enfermería | Web (por confirmar) | usuario | — | No | 4 | 4 |
| `control_escolar` | Control Escolar | Web | usuario | — | No | 8 | 11 |

Los permisos de cada uno (salen del documento "Pantallas por rol",
[referencias/PANTALLAS_POR_ROL.md](referencias/PANTALLAS_POR_ROL.md)):

| Perfil | Permisos |
|---|---|
| `alumno` | ver_mi_qr, ver_mi_credencial, ver_mi_perfil, ver_mi_horario, ver_boletin, ver_notificaciones |
| `padre` | ver_perfil_hijo, ver_qr_hijo, ver_credencial_hijo, ver_entradas_salidas_hijo, ver_horario_hijo, ver_boletin, ver_calendario, ver_notificaciones |
| `prefecto` | escanear_qr, capturar_no_control, registrar_entrada_salida, ver_alertas, ver_todos_los_alumnos, ver_horarios_grupos, ver_salidas_de_grupo, ver_calendario, ver_notificaciones |
| `profesor` | ver_mis_grupos, ver_alumnos_de_mis_grupos, cambiar_estatus_alumno, dar_salida_grupo, ver_boletin, ver_notificaciones |
| `enfermeria` | ver_todos_los_alumnos, dar_pase_salud, activar_por_salud, ver_notificaciones |
| `control_escolar` | administrar_usuarios, administrar_alumnos, administrar_grupos, administrar_horarios, activar_por_pago, cambiar_cualquier_estatus, registrar_practicas, ver_todos_los_alumnos, ver_entradas_salidas, publicar_anuncios, publicar_eventos |

> **Importante — los permisos describen, las reglas deciden.** La lista `permisos` sirve
> para que las apps sepan qué mostrar y para documentar. **Lo que realmente protege los
> datos son las reglas de seguridad** (sección 6), que revisan `usuarios.rol`. Si cambias
> un permiso aquí, revisa que las reglas digan lo mismo.

**Quién puede:** cualquier persona con sesión los lee; solo Control Escolar los cambia.
**Para editarlos:** consola → *Firestore* → `roles` → el documento → lápiz del campo. Después
copia el cambio en `firebase/roles.json`.

---

## 5. Valores permitidos

Se escriben **exactamente así** (minúsculas, sin acentos, con guion bajo).

### 5.1 Roles

Son los ids de la colección `roles` (§4.9).

| Valor de `rol` | Quién es | ¿Tiene pantallas en la app hoy? |
|---|---|---|
| `alumno` | Alumno | Sí (vistas XML de alumno) |
| `padre` | Padre de familia o tutor | Sí (vistas XML de padre; antes Compose) |
| `prefecto` | Prefecto | Sí (vistas XML), con escáner QR, captura por No. de control, resultado y alertas |
| `profesor` | Profesor | Sí (vistas XML): grupos, alumnos, estatus y salida del grupo |
| `enfermeria` | Enfermería | No (web, por confirmar): ve un aviso |
| `control_escolar` | Control Escolar | No (web): ve un aviso |

### 5.2 Estatus (`alumnos/{m}/estatus.tipo`)

`castigo` · `falta` · `reporte` · `llamada_atencion` · `pase_salud` · `alta_practicas`

### 5.3 Alertas del escaneo (`registros.alerta`)

| Valor | Cuándo |
|---|---|
| `no_dado_de_alta` | El QR o el No. de control no corresponde a ningún alumno |
| `inactivo` | Al entrar, `activoPorPago` o `activoPorSalud` es false |
| `ya_ingreso` | Entrada cuando ya tenía una entrada registrada |
| `no_ha_ingresado` | Salida sin entrada registrada |
| `castigo` | Tiene un estatus `castigo` vigente |
| `salida_fuera_de_horario` | Sale antes de que su profesor libere al grupo o sin pase de salud |

### 5.4 Otros

| Campo | Valores |
|---|---|
| `alumnos.ubicacion` | `fuera` · `dentro` · `salida_autorizada` |
| `registros.tipo`, `ultimoRegistro.tipo` | `entrada` · `salida` |
| `registros.medio` | `qr` · `no_control` |
| `cambiosDeEstado.campo` | `pago` · `salud` |

---

## 6. Reglas de seguridad (quién ve y cambia qué)

Están en `firebase/firestore.rules` y publicadas en la consola. Resumen:

| Colección | Alumno | Padre | Prefecto | Profesor | Enfermería | Control Escolar |
|---|---|---|---|---|---|---|
| `usuarios` (el suyo) | Lee | Lee | Lee | Lee | Lee | **Todo** |
| `usuarios/…/notificaciones` (las suyas) | Lee, marca leída | Lee, marca leída | Lee, marca leída | Lee, marca leída | Lee, marca leída | Lee, marca leída |
| `usuarios/…/notificaciones` (crear en otra cuenta) | — | — | A los padres del alumno | A los padres de sus alumnos | A los padres del alumno | A cualquiera |
| `alumnos` | El suyo | Sus hijos | Lee todos; cambia `ubicacion` | Sus grupos | Lee todos; cambia `activoPorSalud` | **Todo** |
| `alumnos/…/estatus` | — | — | Lee | Lee y crea (sus grupos) | Lee; crea `pase_salud` | **Todo** |
| `alumnos/…/cambiosDeEstado` | — | — | — | — | Lee; crea `salud` | Lee; crea `pago` |
| `alumnos/…/inscripciones` | La suya | De sus hijos | Lee | — | Lee | **Todo** |
| `alumnos/…/privado/datos` | — | — | — | — | — | **Todo** |
| `alumnos/…/privado/salud` | — | — | — | — | Lee y escribe | **Todo** |
| `grupos` y `horario` | Lee | Lee | Lee | Lee | Lee | **Todo** |
| `periodos` | Lee | Lee | Lee | Lee | Lee | **Todo** |
| `periodos/…/registros` | Los suyos | De sus hijos | Lee todos; **crea** | De sus grupos | Lee todos | Lee todos |
| `periodos/…/salidasDeGrupo` | — | — | Lee | Lee; crea (sus grupos) | — | **Todo** |
| `anuncios`, `eventos`, `configuracion`, `roles`, `especialidades` | Lee | Lee | Lee | Lee | Lee | **Todo** |

Además:

- **Sin sesión no se puede leer ni escribir nada.** (Se comprobó: una lectura sin sesión
  responde *403 Permission denied*.)
- **Si `activo` es false**, la cuenta no puede hacer nada aunque tenga contraseña.
- **Lo que se crea va firmado:** los avisos (`creadaPor`), los estatus (`asignadoPor`), los
  cambios de estado (`por`), los registros (`registradoPor`) y las salidas de grupo
  (`autorizadaPor`) deben llevar el uid de quien los crea; si no, se rechazan.
- **Pruebas:** las reglas tienen 21 pruebas automáticas con el emulador
  ([firebase/pruebas-reglas](../firebase/pruebas-reglas/README.md)). Córrelas antes de publicar
  un cambio.
- **Cualquier colección que no esté en la tabla queda cerrada**, incluidas las viejas
  (`CetisAsiste` y los documentos antiguos de `usuarios` con contraseña).

---

## 7. Índices

Firestore necesita un "índice compuesto" para las búsquedas que filtran por un campo y
ordenan por otro. Hay 11 (consola → *Indexes* → *Manual*); el de `fecha`, `alerta`,
`fechaHora` es del 20 de octubre (ADR-010) y **hay que crearlo en la consola**. Los de `registros` y
`salidasDeGrupo` sirven para la lista de **cualquier** semestre, porque Firestore los aplica a
toda colección con ese nombre.

| Colección | Campos | Para qué búsqueda |
|---|---|---|
| `registros` | `alumnoId` ↑, `fechaHora` ↓ | Entradas y salidas de un alumno (padre, alumno) |
| `registros` | `grupoId` ↑, `fechaHora` ↓ | Registros de un grupo (profesor) |
| `registros` | `fecha` ↑, `fechaHora` ↓ | Registros de un día (Control Escolar, prefecto: "Últimos escaneos") |
| `registros` | `fecha` ↑, `alerta` ↑, `fechaHora` ↓ | Alertas de un día (prefecto: "Alertas de hoy"; Control Escolar) |
| `salidasDeGrupo` | `fecha` ↑, `fechaHora` ↓ | Grupos que ya salieron hoy (prefecto) |
| `anuncios` | `dirigidoA` (arreglo), `fecha` ↓ | Boletín según el rol |
| `alumnos` | `grupoId` ↑, `nombre` ↑ | Lista de alumnos de un grupo (profesor, enfermería) |
| `alumnos` | `estado` ↑, `grupoId` ↑, `apellidoPaterno` ↑ | Lista de un grupo en orden alfabético (solo inscritos) |
| `alumnos` | `estado` ↑, `nombreBusqueda` ↑ | Buscar alumno por nombre ("perez ro…") |
| `alumnos` | `estado` ↑, `turno` ↑, `semestre` ↑, `nombreBusqueda` ↑ | Filtrar por turno y semestre |
| `grupos` | `periodoId` ↑, `turno` ↑, `nombre` ↑ | Grupos del semestre por turno |

Si un día la app muestra un error que dice *"The query requires an index"*, el mensaje trae
un enlace: al abrirlo, Firebase crea el índice que falta. Después agrégalo también a
`firebase/firestore.indexes.json`.

---

## 8. Cómo entra una persona a la app

```
La persona escribe:   No. de control o usuario  +  contraseña
        │
        ▼
La app arma el correo:  00000000000000@cetis61.mx
        │
        ▼
Authentication revisa la contraseña ──── incorrecta ──► "No. de control o contraseña incorrectas."
        │ correcta
        ▼
La app lee usuarios/{uid} ─── no existe ──► "Tu cuenta todavía no está dada de alta…"
        │                  └─ activo = false ► "Tu cuenta está desactivada…"
        ▼
¿debeCambiarContrasena = true o contrasenaVence ya pasó? ── sí ──► Cambiar contraseña
        │ no                                                      (y luego sigue)
        ▼
Según el rol:  padre → vistas de padre        alumno   → vistas de alumno
               prefecto → vistas de prefecto  profesor → vistas de profesor
               enfermería, Control Escolar → aviso "se usa desde la web" + Cerrar sesión
```

La sesión se queda guardada: si la persona cierra la app sin tocar "Cerrar sesión", al
abrirla de nuevo entra directo.

---

## 9. Recetas: dar de alta datos (mientras no exista la web de Control Escolar)

Todo se hace en la **consola de Firebase** con una cuenta de Google que tenga acceso al
proyecto. Para crear un documento: *Firestore → Data →* elige la colección *→ Add document*,
escribe el **Document ID** y agrega cada campo con su **tipo** (string, boolean, timestamp…).

### 9.1 Primera vez: crear la cuenta de Control Escolar

Las reglas solo dejan escribir a Control Escolar, así que **la primera cuenta se crea a mano**:

1. *Authentication → Users → Add user.* Email: `control.escolar@cetis61.mx` (o el usuario
   que elijan + `@cetis61.mx`). Contraseña: una segura. *Add user.*
2. Copia el **User UID** que aparece en la lista.
3. *Firestore → Data → usuarios → Add document.* Document ID: **pega el UID**. Campos:
   - `nombre` (string): nombre de la persona
   - `usuario` (string): `control.escolar`
   - `correo` (string): `control.escolar@cetis61.mx`
   - `rol` (string): `control_escolar`
   - `activo` (boolean): `true`
   - `debeCambiarContrasena` (boolean): `false`

### 9.2 Dar de alta un grupo y su horario

1. *Firestore → grupos → Add document.* ID: `{periodoActual}_{nombre}`, por ejemplo
   `2026-2027-1_102`. Campos: `nombre` = `102`, `periodoId` = `2026-2027-1`,
   `turno` = `matutino` (nombres x00–x49) o `vespertino` (x50 en adelante),
   `semestre` (number) = `1`, `especialidadId` = "" , `tutorId` = "", `activo` = `true`,
   `cicloEscolar` = `Ciclo 2026–2027`, `profesores` (array) = uid de sus profesores.
2. Dentro del grupo: *Start collection* → `horario`. Un documento por día (`lunes` …
   `viernes`) con `dia`, `orden` (number) y `clases` (array de **map** con `materia`,
   `horaInicio`, `horaFin`, `profesorId`, `profesor`, `aula`, `ultimaDelTurno`).

### 9.3 Dar de alta un alumno

1. **Cuenta:** *Authentication → Add user.* Email: `NOCONTROL@cetis61.mx` con sus **14
   dígitos** (por ejemplo `00000000000001@cetis61.mx`) y una contraseña inicial. Copia el
   **User UID**.
2. **`usuarios/{UID}`:** `nombre`, `usuario` = el No. de control, `correo` = el email del paso 1,
   `rol` = `alumno`, `activo` = `true`, `noControl` = el No. de control.
3. **`alumnos/{NOCONTROL}`** (el Document ID es el No. de control): `noControl`, `nombre`
   (completo), `apellidoPaterno`, `apellidoMaterno`, `nombres`, `nombreBusqueda` (apellidos
   y nombres en minúsculas sin acentos), `estado` = `inscrito`, `periodoId` = el periodo
   actual, `semestre` (number), `especialidadId` = "", `generacion` (ej. `2026-2029`),
   `usuarioId` = el UID, `grupoId` = `2026-2027-1_102`, `grupo` = `102`, `turno`, `correo`,
   `vigencia`, `activoPorPago` = `true`,
   `activoPorSalud` = `true`, `enPracticas` = `false`, `ubicacion` = `fuera`,
   `padres` (array, vacío por ahora).
4. **Historial:** dentro del alumno, *Start collection* → `inscripciones`, documento con id
   = el periodo (`2026-2027-1`): `grupoId`, `grupo`, `semestre`, `turno`, `especialidadId`,
   `fechaAlta` (AAAA-MM-DD), `estado` = `cursando`, `por`, `porNombre`.
5. **Datos delicados:** *Start collection* → `privado`, documentos `datos` y `salud` con los
   campos de §4.2.
6. Entrégale al alumno su No. de control y su contraseña inicial.

> Con 2,500 alumnos esto no se hace a mano: hace falta la herramienta de importación masiva
> (ADR-005, acción 7).

> **El QR no se captura:** la app lo arma con el No. de control (ADR-009).

### 9.4 Dar de alta a un padre o tutor

1. *Authentication → Add user* con `usuario.que.elijan@cetis61.mx`. Copia el UID.
2. `usuarios/{UID}`: `nombre`, `usuario`, `correo`, `rol` = `padre`, `activo` = `true`,
   `hijos` (array) = números de control de sus hijos.
3. En `alumnos/{No. de control}` de cada hijo, agrega el UID del padre al arreglo `padres`.

### 9.5 Dar de alta personal (prefecto, profesor, enfermería)

Igual que el padre, con `rol` = `prefecto`, `profesor` o `enfermeria` y
`debeCambiarContrasena` = `true`. Al **profesor** agrégale `grupos` (array con los
`grupoId`) y su uid en `grupos/{grupoId}.profesores`.

### 9.6 Publicar un anuncio o un evento

- `anuncios` → *Add document* → *Auto-ID*: `area`, `fecha` (timestamp), `titulo`, `texto`,
  `destacado` (boolean), `dirigidoA` (array con los roles que lo verán, por ejemplo
  `alumno` y `padre`).
- `eventos` → *Auto-ID*: `titulo`, `inicio` y `fin` (timestamp), `lugar`, `area`.

### 9.7 Desactivar a alguien

- **Cuenta completa:** `usuarios/{uid}.activo` = `false`.
- **Alumno por pago:** `alumnos/{m}.activoPorPago` = `false` y agrega un documento en
  `alumnos/{m}/cambiosDeEstado` con `campo` = `pago`, `valor` = `false`, `motivo`, `fecha`.

### 9.8 Cambiar la contraseña de alguien que la olvidó

La consola no deja escribir una contraseña nueva: su opción *Reset password* manda un
correo, y a los correos internos `@cetis61.mx` **no les llega nada**. Mientras no exista la
web de Control Escolar, el camino es:

1. *Authentication → Users →* ⋮ de la cuenta → *Delete account*.
2. *Add user* con **el mismo email** y una contraseña nueva. Copia el **nuevo UID**.
3. En *Firestore*, crea `usuarios/{nuevo UID}` con los mismos campos que tenía el
   documento viejo y borra el viejo.
4. Si es alumno, cambia `alumnos/{m}.usuarioId`; si es padre, cambia su UID en el arreglo
   `padres` de cada hijo.

Es laborioso a propósito. Desde el 19 de octubre la app tiene la pantalla **"Cambiar
contraseña"** (Perfil), pero pide la contraseña actual: sirve para cambiarla, no para
recuperarla. Para una contraseña olvidada sigue haciendo falta este camino o la web
(ver [PENDIENTES.md](PENDIENTES.md)).

### 9.9 Pedirle a alguien que cambie su contraseña

En `usuarios/{uid}` pon `debeCambiarContrasena` = `true`. La próxima vez que entre, la app
le mostrará "Cambiar contraseña" antes que nada. Al cambiarla, la app regresa el campo a
`false` sola.

---

## 10. La app y la base de datos

| Archivo | Qué hace |
|---|---|
| `datos/Modelos.kt` | La forma de cada documento (`UsuarioDoc`, `AlumnoDoc`, `RegistroDoc`…), los nombres de las colecciones (`Colecciones`) y los roles (`Rol`) |
| `datos/CetisRepositorio.kt` | **Único lugar que habla con Firebase:** inicia y cierra sesión, y hace todas las consultas |
| `datos/Avisos.kt` | A quién se le avisa de qué y cómo se arma un aviso del buzón (sin Firebase; pruebas en `AvisosTest`) |
| `datos/Escaneo.kt` | Qué alerta corresponde a un escaneo (sin Firebase; pruebas en `EscaneoTest`) |
| `datos/Formatos.kt` | Convierte fechas y documentos en los textos de Figma ("Hoy · 5 oct", "7:00 – 7:50 am") y dibuja el QR |
| `ui/CetisApp.kt` | Bienvenida → inicio de sesión → (cambio de contraseña obligatorio) → pantallas según el rol |
| `ui/padres/PadresApp.kt` | Pide los datos del hijo y muestra las vistas XML del padre |
| `ui/alumno/AlumnoApp.kt` | Muestra las vistas XML del alumno y las llena con sus datos |
| `ui/prefecto/PrefectoApp.kt` | Vistas XML del prefecto |
| `ui/profesor/ProfesorApp.kt` | Vistas XML del profesor; guarda estatus y salidas de grupo |
| `ui/comun/` | Pantallas que comparten varios roles (calendario, notificaciones, horario, perfil, contraseña) |

**En tiempo real.** La cuenta (`usuarios/{uid}`), la ficha del alumno
(`alumnos/{No. de control}`), su grupo (`grupos/{grupoId}`), los **avisos sin leer** (para la
campana), la pantalla de **Notificaciones** mientras está abierta y, en el prefecto, las
salidas de grupo, los últimos escaneos y las alertas de hoy se **escuchan**: si alguien las
cambia, la app se actualiza sola en uno o dos segundos, sin cerrar sesión. Si la cuenta se
desactiva (`activo = false`) o se borra, la app cierra la sesión y lo dice. Las demás listas
(anuncios, eventos, registros del alumno, horario, grupos, alumnos, estatus) se leen cada vez
que se abre su pantalla. `configuracion/plantel` se guarda 5 minutos en memoria para no leerla
en cada escaneo.

**¿De dónde sale cada nombre?** Ojo, hay dos campos `nombre` y **no se sincronizan solos**:

| Lo que ves en la app | Sale de | Para cambiarlo, edita |
|---|---|---|
| Nombre del alumno (tarjeta de Inicio, Perfil, Credencial, QR) — lo ve el alumno **y** su padre | La ficha del alumno | `alumnos/{No. de control}.nombre` |
| Grupo del alumno ("102") en tarjetas, Perfil, Credencial y Horario | El grupo | `grupos/{grupoId}.nombre` (la copia `alumnos/{m}.grupo` solo se usa si el grupo no se puede leer) |
| Nombre del personal (Inicio y Perfil del prefecto y del profesor; aviso de enfermería y Control Escolar) | La cuenta | `usuarios/{uid}.nombre` |
| Quién puso un estatus o dio salida a un grupo | Se copia al guardar | `asignadoPorNombre` / `autorizadaPorNombre` (no cambia si después se corrige el nombre) |
| Nombre del padre | Hoy ninguna pantalla de padre lo muestra (todas hablan del hijo) | `usuarios/{uid}.nombre` |

Por eso, si cambias el nombre de un alumno, cámbialo en `alumnos/{No. de control}` (y, para que
coincida, también en su `usuarios/{uid}`). La futura web de Control Escolar debe cambiar
ambos a la vez.

Qué lee cada pantalla:

| Pantalla | Lee de Firestore |
|---|---|
| Inicio (padre) | `alumnos/{hijo}` y el anuncio destacado más reciente |
| Anuncios / Boletín | `anuncios` donde `dirigidoA` contiene su rol, del más nuevo al más viejo |
| Calendario | `eventos` que empiezan en el mes actual |
| Perfil, Credencial, Código QR | `alumnos/{No. de control}` (el QR se dibuja con `CETIS61:` + el No. de control) |
| Entradas y salidas | `periodos/{periodoActual}/registros` del alumno: los 20 más recientes del semestre, agrupados por día; "Ver más" trae otros 20 |
| Notificaciones | `usuarios/{uid}/notificaciones`, las 50 más recientes, **en tiempo real** (el prefecto ve arriba, además, los grupos que salieron hoy) |
| Campana y menú ☰ (todos) | `usuarios/{uid}/notificaciones` donde `leida` = false (hasta 10), **en tiempo real** |
| Horario (alumno, padre, y el de cualquier grupo para prefecto y profesor) | `grupos/{grupoId}/horario/{día elegido}` |
| Aviso para roles sin pantallas | `roles/{rol}` (nombre del perfil y si es solo web) |
| Inicio (prefecto) | `periodos/{periodoActual}/salidasDeGrupo` donde `fecha` = hoy, y `registros` donde `fecha` = hoy (los 8 más recientes), **en tiempo real** |
| Alertas de hoy (prefecto; también el contador del Inicio) | `periodos/{periodoActual}/registros` donde `fecha` = hoy y `alerta` es una de las seis, del más nuevo al más viejo, **en tiempo real** |
| Horarios de grupos (prefecto) | `grupos` donde `periodoId` = semestre en curso y `turno` = el elegido, por `nombre` |
| Inicio (profesor) | `grupos/{id}` de cada id en `usuarios/{uid}.grupos` |
| Grupo (profesor) | `alumnos` donde `estado` = inscrito y `grupoId` = el grupo, por `apellidoPaterno`; y las salidas de hoy |
| Perfil del alumno (profesor) | `alumnos/{No. de control}` y `alumnos/{No. de control}/estatus` (los 30 más recientes) |
| Dar salida al grupo (profesor) | `grupos/{grupoId}/horario/{hoy}` (última clase) y las salidas de hoy |
| Escáner y captura (prefecto) | `alumnos/{No. de control}`, sus 10 estatus más recientes, `configuracion/plantel` (qué alertas se avisan) y, si es salida, las salidas de grupo de hoy |
| Cambiar estatus (profesor) | `configuracion/plantel` (qué estatus se avisan al padre) |

Qué **escribe** cada pantalla (todo lo permiten las reglas de §6):

| Pantalla | Escribe | Campos |
|---|---|---|
| Cambiar estatus (profesor) | **Todo de una vez:** crea `alumnos/{m}/estatus/{auto}` y, si el estatus se avisa (§4.8), un aviso en `usuarios/{padre}/notificaciones` para cada padre | Estatus: `tipo` (falta, reporte, llamada_atencion o castigo), `descripcion`, `fecha`, `vigenteHasta` = null, `asignadoPor`, `asignadoPorNombre`. Aviso: `tipo` = estatus (campos en §4.1) |
| Dar salida al grupo (profesor) | Crea `periodos/{semestre}/salidasDeGrupo/{auto}` | `grupoId`, `grupo`, `fecha` (AAAA-MM-DD), `fechaHora`, `autorizadaPor`, `autorizadaPorNombre`, `materia` |
| Cambiar contraseña (todos) | Cambia la contraseña en Authentication y actualiza `usuarios/{uid}` | `debeCambiarContrasena` = false, `contrasenaVence`, `actualizadoEn` |
| Notificaciones (todos) | Actualiza `usuarios/{uid}/notificaciones/{id}` | `leida` = true |
| Escáner y captura (prefecto) | **Todo de una vez:** crea `periodos/{semestre}/registros/{auto}` (siempre); si no hubo alerta, actualiza `alumnos/{n}`; y crea `usuarios/{padre}/notificaciones/{auto}` para cada padre si no hubo alerta o si la alerta se avisa (§4.8) | Registro: ver §4.4. Alumno: `ubicacion` (dentro o fuera), `ultimoRegistro`, `actualizadoEn`. Aviso: `tipo` = entrada, salida o alerta (campos en §4.1) |

---

## 10.1 Cuentas y datos de prueba (para revisar la app en Android)

Creados el 19 de octubre de 2026 a pedido del usuario. **Todo lo de prueba dice "Prueba"**
para no confundirlo con datos reales. Las contraseñas las eligió el usuario y no están
anotadas en ningún documento.

| Quién | Inicia sesión con | Cuenta (Authentication) | UID / documento |
|---|---|---|---|
| Alumno (Jesusito Pérez Prueba) — **hay que volver a crearlo** (ver abajo) | `25311754` (8 dígitos: ya no entra) | `25311754@cetis61.mx` | `usuarios/OQjoqdYtkGSGWSLDVbiCkOTVsy72` y `alumnos/25311754` |
| Padre de Prueba | `padre.prueba` | `padre.prueba@cetis61.mx` | `usuarios/kyddI1TM9faff42rkWDzKnYrgqG3` (hijo: 25311754; cambiar al nuevo) |
| Profesor de Prueba | `profesor.prueba` | `profesor.prueba@cetis61.mx` | `usuarios/fIdvWaDFZ7MQvP4I73Mh0Ro7j8t1` (grupo: 2026-2027-1_102) |
| Prefecto de Prueba | `prefecto.prueba` | `prefecto.prueba@cetis61.mx` | `usuarios/QpWWIb6e1Ze2zlzhmwygFnoRJss1` (`debeCambiarContrasena = false`) |

### Pasar los datos de prueba al No. de control de 14 dígitos (ADR-009)

Desde el 19 de octubre la app exige **14 dígitos** y la base usa el campo **`noControl`** (antes
`matricula`). El alumno de prueba tiene 8 dígitos, así que **ya no puede entrar**. Pasos, en la
consola de Firebase y en este orden (los hace el usuario):

1. **Publicar las reglas nuevas:** *Firestore → Reglas*, pegar todo `firebase/firestore.rules`
   y *Publicar*. (Las reglas viejas buscan `matricula`; sin este paso el alumno nuevo no ve sus
   datos.)
2. **Perfiles:** en `roles/alumno` cambiar `inicioDeSesion` a `no_control` y
   `camposDelUsuario` a `["noControl"]`; en `roles/prefecto`, los textos y el permiso
   `capturar_no_control` (todo está en `firebase/roles.json`).
3. **Cuenta nueva:** *Authentication → Add user* con un No. de control **inventado** de 14
   dígitos, por ejemplo `00000000000001@cetis61.mx`, y una contraseña. Copia el **UID**.
4. **Ficha:** en `alumnos`, documento con id `00000000000001` y los mismos campos que
   `alumnos/25311754`, pero con `noControl` = `00000000000001`, `usuarioId` = el UID nuevo y
   **sin** `matricula` ni `codigoQr`. Si quieres, su `inscripciones/2026-2027-1` y `privado`.
5. **Cuenta en la base:** `usuarios/{UID nuevo}` con `nombre` = Jesusito Pérez Prueba,
   `usuario` y `noControl` = `00000000000001`, `correo`, `rol` = `alumno`, `activo` = `true`.
6. **Padre de prueba:** en su `usuarios/{uid}`, `hijos` = `["00000000000001"]`.
7. **Borrar lo viejo:** la cuenta `25311754@cetis61.mx`, `usuarios/OQjoqdYtkGSGWSLDVbiCkOTVsy72`
   y `alumnos/25311754` (con sus subcolecciones). Los registros viejos, si hubiera, se quedan.

Además (ya con la estructura por semestre de ADR-005):

- **Semestre** `periodos/2026-2027-1` (activo) y `configuracion/plantel.periodoActual =
  "2026-2027-1"`.
- **Grupo** `grupos/2026-2027-1_102` (nombre 102, matutino, 1.er semestre) con horario de
  **lunes a viernes**: las 5 clases del diseño de Figma, con `profesorId` del profesor de
  prueba; la última (Tecnologías de la información, 10:50–11:40) es la del fin de turno.
- **Ficha** `alumnos/25311754` con apellidos, `nombreBusqueda`, `estado = inscrito`,
  `semestre = 1`, `generacion = 2026-2029`; su historial `inscripciones/2026-2027-1`; y datos
  **ficticios** en `privado/datos` (CURP `XEXX090315HCHXXX00`, domicilio y teléfonos
  inventados, tutor "Juan Pérez Ficticio") y `privado/salud` (O+, sin alergias).
- El profesor está en `grupos/2026-2027-1_102.profesores`; el padre, en
  `alumnos/25311754.padres`.
- **Sin anuncios, eventos, registros ni notificaciones**: esas pantallas se ven vacías.
- El grupo viejo `grupos/prueba-matutino` se borró al pasar a la estructura nueva.

### Cuenta de Control Escolar (real, no de prueba)

| Quién | Inicia sesión con | Documento |
|---|---|---|
| Abraham Emilio Morales | `abrahamemiliomorales@gmail.com` (el correo completo) | `usuarios/QUgw5x0tkvRx6Dz3Nb0S2zYlnmG2`, rol `control_escolar` |

- Es la **primera cuenta de Control Escolar** (la de la receta §9.1). Con ella, las reglas
  dejan crear y cambiar todo: alumnos, grupos, anuncios, perfiles… cuando exista la web.
- En la app Android ve el aviso "El perfil de Control Escolar se usa desde la web…", porque
  ese rol no tiene pantallas en Android.
- Se escribe el correo completo porque no es `@cetis61.mx`: la app lo deja tal cual.
- **No la borres** al limpiar los datos de prueba.

**Para borrar todo lo de prueba** (consola de Firebase): las 4 cuentas en *Authentication*;
los 4 documentos de `usuarios` de la tabla; `alumnos/25311754` (con sus `inscripciones` y
`privado`); y `grupos/2026-2027-1_102` (al borrarlo, la consola también borra su `horario`).
El semestre `periodos/2026-2027-1` y `periodoActual` **no** son de prueba: se quedan.

---

## 11. Pendientes

Todos los pendientes del proyecto, también los de la base de datos (plan de Firebase,
Cloud Functions, importación masiva, cambio de semestre, borrar las colecciones viejas…),
están juntos y sin repetir en [PENDIENTES.md](PENDIENTES.md), con quién tiene que actuar.
