# Traspaso — Estado de CetisAsiste para el siguiente agente

**Fecha:** 19 de octubre de 2026 (actualizado el 20 de octubre: avisos, revisión de la arquitectura y web)
**Para quién:** el siguiente agente (o persona) que continúe el proyecto.
**Léelo primero.** Resume qué es la app, qué se le hizo, cómo está hoy, qué se quiere lograr y
qué reglas de trabajo hay que respetar. El detalle está en los documentos que se enlazan.

---

## 1. Qué es CetisAsiste y qué se quiere conseguir

**CetisAsiste** es la app del **CETis 61 (Cd. Juárez)** para **controlar las entradas y
salidas de los alumnos** (unos **2,500**, en dos turnos).

**La meta final:**

1. El **prefecto** escanea el QR del alumno (o teclea su No. de control) en la puerta. El sistema
   revisa si puede entrar o salir y, si hay algo raro, muestra una **alerta** (no dado de
   alta, inactivo por pago o salud, ya ingresó, no ha ingresado, castigo, salida fuera de
   horario).
2. Cada entrada y salida queda guardada y **el padre recibe un aviso** en su teléfono.
3. El **profesor** cambia el estatus de sus alumnos y **libera al grupo** en la última clase
   del turno, para que el prefecto no reciba alertas al verlos salir.
4. **Enfermería** da pases de salida por salud y activa o desactiva al alumno por salud.
5. **Control Escolar** administra todo (alumnos, grupos, horarios, pagos, estatus) desde una
   **web** que todavía no existe.
6. El **alumno** y el **padre** solo consultan: QR, credencial, horario, boletín, entradas
   y salidas, calendario.

La fuente de esta meta es el documento **"CetisAsiste — Pantallas por rol"** (7 oct 2026, del
usuario): [referencias/PANTALLAS_POR_ROL.md](referencias/PANTALLAS_POR_ROL.md) (copia en
texto, con diagramas UML y una comparación con la app al final; el Word original está junto a
ella). También está el diseño de **Figma**:
<https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste> ("CETis 61 - Inicio
(Android)"), con páginas **Padre, Alumno, Prefecto, Profesor y Componentes**. Enlace de cada
página, lista de pantallas y diferencias con la app: [referencias/FIGMA.md](referencias/FIGMA.md).
**Ojo:** la herramienta de Figma no lista todas las páginas; ábrelas por su `node-id`.

---

## 2. Cómo está hoy (20 de octubre de 2026)

| Parte | Estado |
|---|---|
| Base de datos (Cloud Firestore) | **Lista** con la estructura por semestres, reglas por rol (con 21 pruebas en `firebase/pruebas-reglas`) y 11 índices. Vacía de datos reales; solo hay datos de prueba. **Las reglas del 19 y 20 oct no están publicadas** (PENDIENTES §1) |
| Inicio de sesión | **Funciona** con Firebase Authentication (No. de control o usuario → correo interno) |
| Pantallas de **alumno** (vistas XML) | **Funcionan** con datos reales |
| Pantallas de **padre** (vistas XML desde el 19 oct; antes Compose) | **Hechas** con datos reales. Falta probarlas en el teléfono con `padre.prueba` |
| Pantallas de **prefecto** (vistas XML) | **Hechas**, con escáner QR, captura por No. de control, resultado y alertas (ADR-009). Falta probarlas con `prefecto.prueba` |
| Pantallas de **profesor** (vistas XML) | **Hechas**; guarda estatus y salidas de grupo. Falta probarlas con `profesor.prueba` |
| Cambiar contraseña (todos) | **Hecha**; obligatoria al entrar si `debeCambiarContrasena = true` o venció |
| Enfermería y Control Escolar | **Web, primera versión hecha** (ADR-011, carpeta `web/`), probada con el emulador y registrada en Firebase; **falta publicarla**. En Android ven un aviso |
| Escaneo de QR y alertas | **Hecho** (ADR-009); el prefecto ve "Últimos escaneos" y "Alertas de hoy" (ADR-010) |
| Avisos (notificaciones) | **Hechos** (ADR-010): un buzón por cuenta; el padre recibe entradas, salidas, estatus y algunas alertas; campana en vivo. Son **dentro de la app**: el **push** no existe (necesita Blaze) |
| Plan de Firebase | **Spark (gratis)**: sin Cloud Functions y con límites diarios que **no alcanzan** para 2,500 alumnos |

---

## 3. Lo que se le hizo a la app (en orden)

Detalle completo, paso por paso: [BITACORA.md](BITACORA.md) (entradas 19-01 a 19-31).

1. **Vistas de alumno:** el contenido ya no queda debajo de la barra inferior y la barra
   tiene 12 dp de margen (ADR-001; desde el punto 12 ya no hay barra).
2. **Base de datos en Firebase:** se diseñó el modelo, se publicaron reglas de seguridad por
   rol (antes la base estaba abierta a cualquiera) y se crearon índices (ADR-002, ADR-004).
3. **Sin datos de ejemplo:** se borró `SampleData.kt`; las pantallas reciben sus datos como
   parámetros y muestran un texto de "vacío" si no hay nada. **El usuario no quiere datos de
   ejemplo** ni en la app ni en la base.
4. **Inicio de sesión real:** Firebase Authentication con correo interno
   `noControl@cetis61.mx` / `usuario@cetis61.mx` (ADR-003). Después se lee
   `usuarios/{uid}` y el `rol` decide qué se ve.
5. **Pantallas por rol:** `CetisApp` manda a cada rol a su `*App.kt` (`AlumnoApp`,
   `PadresApp`, `PrefectoApp`, `ProfesorApp`), que muestran vistas XML dentro de Compose con
   `AndroidView`; enfermería y Control Escolar ven un aviso. (Hasta el 19 oct el padre usaba
   pantallas Compose; ver el punto 11.)
6. **Perfiles de usuario:** colección `roles` con los 6 perfiles (pantallas y permisos).
7. **QR real:** se dibuja con ZXing. Desde el 19 oct lleva `CETIS61:` + el No. de control
   (ADR-009); antes llevaba un texto al azar (`codigoQr`).
8. **Tiempo real:** la cuenta, la ficha del alumno y su grupo se **escuchan**
   (`addSnapshotListener`); un cambio en la consola se ve al instante. Si la cuenta se
   desactiva o se borra, se cierra la sesión.
9. **Estructura para todo el plantel (ADR-005):** todo se ordena por **semestre**
   (`periodos/{periodoId}`); grupos con el semestre en su id; historial por alumno;
   datos personales aparte; entradas y salidas en una **lista nueva por semestre**,
   leídas de 20 en 20.
10. **Documentación:** todo en `docs/`, en español sencillo, con versión Word.
11. **Vistas XML para padre, prefecto y profesor (ADR-006):** el padre pasó de Compose a XML;
    se hicieron las vistas del prefecto (sin escaneo) y del profesor (con cambiar estatus y
    dar salida al grupo); vistas comunes `comun_*.xml`; barra inferior por rol puesta por el
    código; "Cambiar contraseña" para todos. Prefecto y profesor se armaron con el estilo del
    alumno porque sus páginas de Figma no se veían; **después se encontraron** y su diseño es
    distinto (pendiente de ajustar: [referencias/FIGMA.md](referencias/FIGMA.md)).
12. **Menú ☰ en lugar de la barra inferior (ADR-007):** ningún rol tiene barra inferior; sus
    pantallas principales llevan el ☰, que abre un menú común (`comun_menu.xml`) con el nombre
    y el rol, una opción por pantalla principal y "Cerrar sesión" hasta abajo.
13. **Revisión de la estructura (ADR-008, propuesta):** qué está bien y qué conviene cambiar
    (código muerto, carpetas, una sola navegación, pruebas, `ViewModel`, paquete propio). El
    usuario decide qué se aplica.
14. **No. de control y escaneo (ADR-009):** `matricula` pasó a `noControl` (14 dígitos) en
    todo; el QR lleva `CETIS61:` + el No. de control; el prefecto escanea, captura y ve el
    resultado con alertas. **Faltan** publicar las reglas y volver a crear al alumno de prueba.
15. **Avisos (ADR-010, 20 oct):** buzón único `usuarios/{uid}/notificaciones` y `datos/Avisos.kt`;
    el padre recibe estatus del profesor y alertas (según `configuracion/plantel`); campana con
    punto rojo solo si hay sin leer; "Alertas de hoy" del prefecto; reglas del buzón más
    estrictas con pruebas en el emulador.
16. **Revisión de la arquitectura (20 oct):** [ARQUITECTURA.md](ARQUITECTURA.md), qué mejorar
    por prioridad (escaneo sin señal, hora del servidor, App Check, script de administración…).
17. **Web (ADR-011, 20 oct):** carpeta `web/` para Control Escolar y Enfermería. Ya está
    registrada en Firebase (`config.js` lleno, usa la base real); en esta PC, sin Node.js, se
    abre con `docs/herramientas/servidor_web.ps1` (o la configuración "web" de
    `.claude/launch.json`). Falta publicarla con Hosting.

---

## 4. Mapa del código (lo que importa)

`app/src/main/java/com/example/cetisasiste/`

| Archivo | Qué hace |
|---|---|
| `Main.kt` | `MainActivity` abre `CetisApp()`. Abajo hay código viejo (`MainAppContainer`, contraseñas en Firestore) **que ya no se usa**; no lo reactives |
| `ui/CetisApp.kt` | Bienvenida → inicio de sesión → pantallas según el rol. Escucha `usuarios/{uid}` en vivo |
| `datos/CetisRepositorio.kt` | **Único lugar que habla con Firebase**: sesión, consultas, escuchas en vivo, `periodoActual()`, `registros()` paginado, grupos, alumnos del grupo, estatus, salidas de grupo, cambiar contraseña |
| `datos/Modelos.kt` | Una clase por tipo de documento (`UsuarioDoc`, `AlumnoDoc`, `GrupoDoc`, `PeriodoDoc`…), nombres de colecciones, roles, estados, `TipoEstatus`, `Turno` |
| `datos/Formatos.kt` | Fechas y horas al estilo Figma ("Hoy · 5 oct", "7:00 – 7:50 am"), semestre, iniciales, documento → variables de pantalla, dibujo del QR |
| `ui/comun/` | Lo que comparten los roles: `PantallaXml`, menú ☰ (`conectarMenu`, `Pestana`), diálogo de cerrar sesión, listas (`VistasComunes.kt`); calendario, notificaciones, horario, perfil del personal y cambiar contraseña (`PantallasComunes.kt`); `Carga`, `alumnoDeLaSesionEnVivo()`, `nombreDeGrupoEnVivo()` (`Carga.kt`) |
| `ui/padres/PadresApp.kt` + `PadresVistas.kt` | Navegación y datos del padre sobre `padre_*.xml` |
| `ui/padres/screens/` | Solo quedan `BienvenidaScreen` e `IniciarSesionScreen` (Compose, de todos los roles) |
| `ui/prefecto/` y `ui/profesor/` | `*App.kt` (navegación y datos) y `*Vistas.kt` (llenan los XML) de cada rol |
| `ui/alumno/AlumnoApp.kt` | Navegación del alumno sobre `res/layout/alumno_*.xml` |
| `AlumnoVariables.kt` / `AlumnoVistas.kt` | Datos que necesitan las vistas XML del alumno y cómo se colocan |
| `datos/Avisos.kt` y `datos/Escaneo.kt` | A quién se avisa y qué alerta toca al escanear (sin Firebase, con pruebas) |

**La web** (`web/`, ver [web/README.md](../web/README.md)): `js/datos.js` es el único archivo
que habla con Firebase; `js/avisos.js` repite `Avisos.kt` (**si cambias uno, cambia el otro**);
una pantalla por archivo en `js/pantallas/`.

Otros: `firebase/firestore.rules` y `firestore.indexes.json` (**copias** de lo publicado en la
consola), `firebase/roles.json` (copia de los perfiles), `docs/herramientas/md_a_word.ps1`
(genera los `.docx`).

---

## 5. La base de datos en una mirada

Detalle completo: [BASE_DE_DATOS.md](BASE_DE_DATOS.md). Proyecto `appcetis-7c79d`.

```
usuarios/{uid}               cuenta y rol (+ notificaciones)
alumnos/{noControl}          ficha actual (+ inscripciones, privado/datos, privado/salud,
                             estatus, cambiosDeEstado)
periodos/{2026-2027-1}       semestre (+ registros, salidasDeGrupo: lista nueva cada semestre)
grupos/{periodoId}_{nombre}  grupo del semestre (+ horario/{dia})
anuncios, eventos, roles, especialidades, configuracion/plantel (periodoActual)
```

- **Grupos:** matutino del x00 al x49 (101, 102…), vespertino del x50 en adelante (151…).
- **No. de control:** 14 dígitos, el de la credencial (antes "matrícula"; el campo es `noControl`).
- **Nombres:** el del alumno sale de `alumnos/{m}.nombre`; el del grupo, de
  `grupos/{id}.nombre`. Hay copias en otros documentos que **no** se sincronizan solas.

### Cuentas que existen

| Quién | Entra con | Notas |
|---|---|---|
| Alumno de prueba (Jesusito Pérez Prueba) | `25311754` | **Ya no entra** (8 dígitos): hay que volver a crearlo con 14 (BASE_DE_DATOS §10.1) |
| Padre de prueba | `padre.prueba` | Hijo: 25311754 (cambiar al alumno nuevo) |
| Profesor de prueba | `profesor.prueba` | Grupo `2026-2027-1_102` |
| Prefecto de prueba | `prefecto.prueba` | Creado el 19 oct; entra directo (`debeCambiarContrasena = false`) |
| Control Escolar (real) | `abrahamemiliomorales@gmail.com` | Es la cuenta del usuario. **No es de prueba** |

Las contraseñas **solo las tiene el usuario**. No las pidas.

---

## 6. Reglas de trabajo con este usuario (importante)

1. **Todo en español** y explicado **lo más sencillo posible** (el equipo tiene poca
   experiencia en Android).
2. **Documentar absolutamente todo**, incluso los cambios pequeños: agregar una entrada en
   [BITACORA.md](BITACORA.md) (lo más nuevo arriba), actualizar `BASE_DE_DATOS.md`,
   `MANUAL_TECNICO.md` o `DECISIONES.md` si cambia lo que describen, y un ADR en `docs/adr/`
   para decisiones importantes.
3. **Versiones Word:** después de editar un `.md`, correr
   `powershell -ExecutionPolicy Bypass -File docs\herramientas\md_a_word.ps1 docs\ARCHIVO.md`
   (un archivo por llamada; usa el Word instalado). No se editan los `.docx` a mano.
4. **Nada de datos de ejemplo** en la app ni en la base. Datos de prueba solo si el usuario
   los pide, marcados como "Prueba".
5. **Cuentas:** el agente **no crea cuentas** de Authentication ni pone contraseñas. Se le
   dan los pasos al usuario y el agente hace lo demás (perfiles en Firestore).
6. **Seguridad:** no abrir las reglas de Firestore "un rato" para cargar datos; el sistema lo
   bloquea y es correcto. Los datos se cargan desde la consola (que escribe como
   administrador).
7. **No borrar datos** que no hayas creado tú. Las colecciones viejas `CetisAsiste` y los 2
   documentos viejos de `usuarios` (con contraseñas a la vista) los debe borrar el usuario.

---

## 7. Cómo trabajar en esta computadora

- **Compilar:** `export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` y luego
  `./gradlew.bat :app:assembleDebug` desde la raíz. No hay `node`, `python`, `pandoc` ni
  emulador.
- **Probar:** el teléfono del usuario (Samsung, Android 16) por USB con
  `C:/Users/PC/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Es su teléfono personal:
  antes de tocar la pantalla revisa que la app esté al frente. Para entrar a la app hacen
  falta contraseñas: eso lo prueba el usuario.
- **Consola de Firebase:** se maneja desde Chrome (extensión Claude in Chrome).
  - El editor de reglas es CodeMirror 6: el texto se reemplaza con
    `document.querySelector('.cm-content').cmView.view.dispatch(...)` y se confirma
    recargando la página.
  - Los formularios de "Add document" se pueden llenar con JavaScript (campos
    `fire-input-field`, `mat-select`); hay ayudantes guardados en `localStorage['__fsSrc']`
    de la consola.
  - **Cuidado:** "Start collection" aparece también en el documento abierto; revisa siempre
    la ruta ("Parent path") antes de guardar.
- **Ya está en GitHub** (`Abraham9923/cetisasistevv2`, desde el 20 de octubre): hay historial
  para deshacer. Los agentes en la nube trabajan en ramas `claude/…`.
- **En una sesión en la nube** (Linux, sin teléfono ni Word): se puede compilar con el SDK de
  Android, correr las pruebas de la app, las de las reglas y la web con los emuladores
  (ver `firebase/pruebas-reglas/README.md` y `web/README.md`). Los `.docx` no se pueden
  generar ahí: anótalo en la bitácora para correr `md_a_word.ps1` después.

---

## 8. Qué sigue (en orden sugerido)

Todo lo que falta está en [PENDIENTES.md](PENDIENTES.md): lo más urgente (sección 1), el
orden sugerido de trabajo (sección 2), las preguntas para el plantel, las decisiones del
usuario y los pendientes de la app, el servidor, la web, los datos y las pruebas. Empieza
por ahí.

---

## 9. Documentos de referencia

| Documento | Para qué |
|---|---|
| [PENDIENTES.md](PENDIENTES.md) | Todo lo que falta, junto y sin repetir, con quién tiene que actuar |
| [ARQUITECTURA.md](ARQUITECTURA.md) | Revisión de todo el sistema y qué mejorar, por prioridad (20 oct) |
| [web/README.md](../web/README.md) | Cómo usar, probar y publicar la web |
| [referencias/PANTALLAS_POR_ROL.md](referencias/PANTALLAS_POR_ROL.md) | El documento de roles del usuario: pantallas, alertas, estatus, preguntas abiertas, UML y arquitectura |
| [referencias/FIGMA.md](referencias/FIGMA.md) | Enlace del Figma, qué páginas tiene y qué falta |
| [BASE_DE_DATOS.md](BASE_DE_DATOS.md) | Cada colección, campo, regla e índice; recetas para dar de alta datos |
| [BITACORA.md](BITACORA.md) | Todo lo que se hizo, en orden, con archivos y cómo se comprobó |
| [adr/](adr/) | Decisiones: ADR-001 barra inferior (reemplazado) · 002 reglas sin Cloud Functions · 003 inicio de sesión · 004 modelo de datos · 005 estructura por semestres · 006 vistas XML para todos los roles · 007 menú ☰ · 008 mejoras a la estructura (propuesta) · 009 No. de control y escaneo · **010 notificaciones, alertas y avisos** · **011 web de Control Escolar y Enfermería** |
| [MANUAL_TECNICO.md](MANUAL_TECNICO.md) | Dónde está cada cosa del código y cómo cambiarla |
| [DECISIONES.md](DECISIONES.md) | El porqué de las pantallas (sección K: vistas XML) |
| [VISTAS_ALUMNO.md](VISTAS_ALUMNO.md) | Las vistas XML del alumno |
| [VISTAS_POR_ROL.md](VISTAS_POR_ROL.md) | Las vistas XML de padre, prefecto, profesor y las comunes |
