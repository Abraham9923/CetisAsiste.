# Manual técnico — Pantallas de CetisAsiste

**Proyecto:** CetisAsiste (Android, Kotlin; pantallas en vistas XML dentro de Jetpack Compose)
**Diseño de origen:** [Figma — CetisAsiste](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste)
(páginas "Alumno" y "Componentes")
**Fecha:** 5 de octubre de 2026 · **Actualizado:** 19 de octubre de 2026 (vistas XML para
padre, prefecto y profesor)

Este manual explica **qué se construyó, dónde está cada cosa y cómo modificarla**.
Si quieres saber *por qué* se hizo así, lee [DECISIONES.md](DECISIONES.md) y los ADR de
[`adr/`](adr/). La base de datos tiene su propio documento: [BASE_DE_DATOS.md](BASE_DE_DATOS.md).
Cada vista, con sus variables: [VISTAS_ALUMNO.md](VISTAS_ALUMNO.md) y
[VISTAS_POR_ROL.md](VISTAS_POR_ROL.md). Lo que cambia día a día: [BITACORA.md](BITACORA.md).

> **Cambio importante (19 oct 2026):** las pantallas del padre eran Compose; ahora son vistas
> XML, como las del alumno, el prefecto y el profesor ([ADR-006](adr/ADR-006-vistas-xml-para-todos-los-roles.md)).
> Solo **Bienvenida** e **Iniciar sesión** siguen en Compose.

---

## 1. Qué hay y qué no hay

**Sí hay:**

- **Inicio de sesión real** con Firebase Authentication. Nadie se registra desde la app: las
  cuentas las crea Control Escolar.
- **Pantallas por rol**, con datos reales de Firestore:
  - **Alumno:** Inicio, Boletín, Horario, Perfil, Código QR, Credencial.
  - **Padre:** Inicio, Anuncios, Calendario, Perfil, Código QR y Credencial del hijo,
    Entradas y salidas, Horario, Notificaciones.
  - **Prefecto:** Inicio (con los grupos que salieron hoy), Horarios de todos los grupos,
    Calendario, Perfil, Notificaciones.
  - **Profesor:** Inicio (sus grupos), lista de alumnos del grupo, Perfil del alumno,
    Cambiar estatus, Dar salida al grupo, Boletín, Perfil, Notificaciones.
- **Cambiar contraseña** para todos (y obligatorio al entrar si Control Escolar lo pide).
- **No hay datos de ejemplo.** Si algo no se ha dado de alta, la pantalla lo dice.

**No hay (todavía):** escáner QR, captura por No. de control y alertas del prefecto; avisos push;
recuperar una contraseña olvidada; la web de Control Escolar y Enfermería (en Android ven un
aviso y el botón de cerrar sesión).

---

## 2. Cómo ver las pantallas

**Opción A — Correr la app.** En Android Studio presiona ▶ *Run*. Verás la bienvenida y,
1.5 segundos después, el inicio de sesión. Para entrar necesitas una **cuenta dada de alta**
(recetas en [BASE_DE_DATOS.md §9](BASE_DE_DATOS.md#9-recetas-dar-de-alta-datos-mientras-no-exista-la-web-de-control-escolar)).
Cada rol entra a sus propias pantallas.

**Opción B — Sin correr nada (vista previa).** Abre cualquier archivo de `res/layout/` y elige
la vista *Design* o *Split* (arriba a la derecha). Los datos aparecen como `{alumno.nombre}`.
Las pestañas de padre, prefecto y profesor se ven **sin la barra de abajo** (la pone el
código; ver §4).

---

## 3. Mapa de pantallas

```
Bienvenida ──(1.5 s)──► Iniciar sesión ──► ¿debe cambiar su contraseña? ──sí──► Cambiar contraseña
                                                     │ no                              │
                                                     ▼                                 ▼
             según usuarios/{uid}.rol:  alumno → AlumnoApp     padre → PadresApp
                                        prefecto → PrefectoApp profesor → ProfesorApp
                                        enfermería, Control Escolar → aviso "se usa desde la web"
```

Menú ☰ de cada rol (ya no hay barra inferior, [ADR-007](adr/ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md)).
El botón ☰ está en la barra superior de cada pantalla principal y abre `comun_menu.xml`
(nombre y rol de la persona, las opciones del rol, "Notificaciones" y, hasta abajo, "Cerrar
sesión"). La campana de Inicio sigue en su lugar y abre las mismas notificaciones:

| Rol | Opciones del menú | Dónde se define |
|---|---|---|
| Alumno | Inicio · Boletín · Horario · Perfil | `PESTANAS` en `ui/alumno/AlumnoApp.kt` |
| Padre | Inicio · Anuncios · Calendario · Perfil | `PESTANAS` en `ui/padres/PadresApp.kt` |
| Prefecto | Inicio · Escanear · Horarios · Calendario · Perfil | `PESTANAS` en `ui/prefecto/PrefectoApp.kt` |
| Profesor | Inicio · Boletín · Perfil | `PESTANAS` en `ui/profesor/ProfesorApp.kt` |

Las tablas completas (cada pantalla, su XML, sus variables y cómo se llega) están en
[VISTAS_ALUMNO.md](VISTAS_ALUMNO.md) y [VISTAS_POR_ROL.md](VISTAS_POR_ROL.md).

Navegación del padre, como ejemplo:

```
INICIO ─┬─ campana ─────────────► Notificaciones
        ├─ recuadro guinda ─────► Anuncios
        ├─ tarjeta QR ──────────► Código QR
        ├─ tarjeta credencial ──► Credencial digital
        ├─ tarjeta entradas ────► Entradas y salidas
        └─ tarjeta horario ─────► Horario
Menú ☰: Inicio │ Anuncios │ Calendario │ Perfil
                                            ├─ Mi credencial digital ──► Credencial digital
                                            ├─ Mis entradas y salidas ─► Entradas y salidas
                                            ├─ Cambiar contraseña ────► Cambiar contraseña
                                            └─ Cerrar sesión ─────────► (pregunta) → Iniciar sesión
```

La flecha ← de arriba y el botón "atrás" del teléfono regresan a la pantalla anterior. Las
pantallas del menú no se apilan: debajo de cualquiera queda solo Inicio. Con el menú
abierto, "atrás" solo lo cierra.

---

## 4. Estructura de carpetas

```
app/src/main/
├── java/com/example/cetisasiste/
│   ├── Main.kt                    ← Arranca la app y muestra CetisApp()
│   ├── AlumnoVariables.kt         ← Qué datos necesitan las vistas del alumno
│   ├── AlumnoVistas.kt            ← Pone esos datos en las vistas del alumno
│   ├── datos/                     ← LA BASE DE DATOS (único lugar que habla con Firebase)
│   │   ├── Modelos.kt             ← Forma de cada documento de Firestore
│   │   ├── CetisRepositorio.kt    ← Sesión, consultas, escuchas, guardar estatus/salidas, contraseña
│   │   ├── Formatos.kt            ← Fechas "Hoy · 5 oct", horas "7:05 am", semestre, QR
│   │   ├── Escaneo.kt             ← Qué alerta toca al escanear (sin Firebase; tiene pruebas)
│   │   └── Avisos.kt              ← A quién se le avisa de qué y cómo se arma un aviso (sin Firebase; tiene pruebas)
│   └── ui/
│       ├── CetisApp.kt            ← Bienvenida → iniciar sesión → pantallas según el rol
│       ├── comun/                 ← LO QUE COMPARTEN LOS ROLES
│       │   ├── VistasComunes.kt   ← PantallaXml, menú ☰, diálogo, listas, calendario…
│       │   ├── PantallasComunes.kt← Calendario, Notificaciones, Horario, Perfil, Contraseña
│       │   ├── VariablesComunes.kt← Qué datos necesitan las vistas comunes y de otros roles
│       │   └── Carga.kt           ← Estado de una consulta (Cargando / Fallo / Listo)
│       ├── alumno/AlumnoApp.kt    ← Navegación y datos del alumno
│       ├── padres/
│       │   ├── PadresApp.kt       ← Navegación y datos del padre
│       │   ├── PadresVistas.kt    ← Pone los datos en las vistas del padre
│       │   ├── components/        ← AppIcon, PrimaryButton, SchoolLogo (Compose, de Bienvenida e Iniciar sesión)
│       │   └── screens/           ← BienvenidaScreen e IniciarSesionScreen (Compose)
│       ├── prefecto/              ← PrefectoApp.kt, PrefectoVistas.kt y EscaneoPrefecto.kt (escáner, captura, resultado)
│       ├── profesor/              ← ProfesorApp.kt y ProfesorVistas.kt
│       └── theme/                 ← Estilo de las dos pantallas Compose
└── res/
    ├── layout/                    ← LAS VISTAS XML: alumno_*, padre_*, prefecto_*, profesor_*, comun_*
    ├── values/                    ← colors_cetis, dimens_cetis, styles_cetis, strings_*.xml
    ├── drawable/                  ← Íconos (ic_), formas (shape_), adornos (img_, bg_)
    ├── drawable-nodpi/            ← Logotipo del CETis 61 (.png)
    └── font/                      ← Fuente Plus Jakarta Sans (.ttf)
```

**Cómo funciona una pantalla.** Cada rol tiene un `*App.kt` con:

1. Un `enum` con sus pantallas (por ejemplo `PrefectoPantalla`).
2. Una **pila**: la última pantalla de la lista es la que se ve; "regresar" quita la última.
3. Una lista `PESTANAS` con sus pantallas principales (las opciones del menú ☰).
4. Un `when` que, para cada pantalla, pide sus datos a `CetisRepositorio` y llama a
   `PantallaXml(layout) { vista -> … }`. Dentro del bloque se llenan los datos (con su
   `*Vistas.kt`), se conectan los botones (`vista.alTocar(R.id.…) { … }`) y, en las
   pantallas principales, el menú (`conectarMenu(vista, pantalla)`).

`PantallaXml` infla el XML, deja abajo el alto de la barra de gestos de Android y vuelve a
llamar al bloque cada vez que cambian los datos.

**Regla para ubicarte:**

| Si quieres cambiar… | Ve a… |
|---|---|
| Cómo se ve **una** pantalla | Su XML en `res/layout/` |
| Algo que se repite en **varias** pantallas (barra de arriba, tarjetas, chips, botones) | `res/values/styles_cetis.xml` |
| Las opciones del menú ☰ de un rol | La lista `PESTANAS` del `*App.kt` del rol |
| Cómo se ve el menú ☰ | `res/layout/comun_menu.xml` y `comun_item_menu.xml` |
| A dónde lleva un botón | El `*App.kt` del rol |
| Qué datos se ponen en una vista | El `*Vistas.kt` del rol (o `VistasComunes.kt`) |
| Qué se consulta o guarda en la base | `datos/CetisRepositorio.kt` |
| Cómo se escribe una fecha u hora | `datos/Formatos.kt` |
| Un nombre, grupo, anuncio o evento | **En Firestore**, no en el código ([BASE_DE_DATOS.md](BASE_DE_DATOS.md)) |
| Un título o etiqueta fija | `res/values/strings*.xml` |
| Un color en **toda** la app | `res/values/colors_cetis.xml` (y `ui/theme/Color.kt` para Bienvenida e Iniciar sesión) |

---

## 5. El estilo

Los valores del diseño están guardados **una sola vez** y con el mismo nombre que en Figma.

### 5.1 Colores — `res/values/colors_cetis.xml`

| Nombre | Variable en Figma | Valor | Dónde se usa |
|---|---|---|---|
| `cetis_brand_800` | color/brand/800 | `#5A1E24` | Guinda principal: barras, botones, círculos de íconos |
| `cetis_brand_900` | color/brand/900 | `#461419` | Texto principal (títulos, nombres) |
| `cetis_brand_accent` | color/brand/accent | `#7B4331` | Café: salidas, enlaces, esquinas decorativas |
| `cetis_surface_background` | color/surface/background | `#F7F5F4` | Fondo de las pantallas |
| `cetis_surface_card` | color/surface/card | `#EDE5E1` | Fondo de las tarjetas |
| `cetis_surface_pill` | color/surface/pill | `#E4D5D2` | Etiquetas, notificaciones sin leer, bordes de campos |
| `cetis_text_on_brand` | color/text/on-brand | `#FFFFFF` | Texto sobre guinda |
| `cetis_text_on_brand_muted` | color/text/on-brand-muted | `#D9BFBA` | Texto secundario sobre guinda; círculo de iniciales |
| `cetis_text_secondary` | color/text/secondary | `#6F5B5B` | Texto secundario (subtítulos, fechas) |
| `cetis_status_success` / `_bg` | color/status/success(-bg) | `#2F8F55` / `#E2F2E7` | Alumno activo; "el grupo ya salió" |
| `cetis_status_danger` / `_bg` | color/status/danger(-bg) | `#C93B3B` / `#FBE3E3` | Alumno inactivo; mensajes de error |

Bienvenida e Iniciar sesión (Compose) usan los mismos colores desde `ui/theme/Color.kt`.

### 5.2 Letra — `res/values/styles_cetis.xml`

| Estilo en Figma | Estilo XML | Tamaño / alto de línea | Grosor |
|---|---|---|---|
| Brand/AppName | `Cetis.Texto.NombreApp` | 32 / 38 | Bold |
| Title/Name | `Cetis.Texto.TituloNombre` | 18 / 24 | SemiBold |
| Heading/Banner | `Cetis.Texto.Banner` | 17 / 22 | SemiBold |
| Heading/Section | `Cetis.Texto.Seccion` | 15 / 20 | SemiBold |
| Body/Default | `Cetis.Texto.Cuerpo` | 14 / 20 | Regular |
| Body/Small | `Cetis.Texto.CuerpoChico` | 13 / 19 | Regular |
| Card/Title | `Cetis.Texto.TituloTarjeta` | 13.5 / 18 | SemiBold |
| Card/Subtitle | `Cetis.Texto.SubtituloTarjeta` | 11.5 / 15 | Regular |

**La fuente** es *Plus Jakarta Sans*, la misma de Figma, en `res/font/` (Regular, Medium,
SemiBold y Bold). Es libre (*SIL Open Font License 1.1*), descargada de
[github.com/tokotype/PlusJakartaSans](https://github.com/tokotype/PlusJakartaSans).

### 5.3 Espacios y esquinas — `res/values/dimens_cetis.xml`

| Nombre | Variable en Figma | Valor |
|---|---|---|
| `cetis_space_4` … `cetis_space_20` | space/4 … space/20 | 4, 8, 12, 16 y 20 dp |
| `cetis_radius_md` | radius/md | 14 dp (tarjetas, botones) |
| `cetis_radius_lg` | radius/lg | 20 dp (barra superior, tarjetas grandes) |
| `cetis_screen_margin` | — | 12 dp a los lados de la pantalla |
| `cetis_menu_width` | — | 288 dp: ancho del menú ☰ |

---

## 6. Piezas compartidas

### 6.1 Estilos que arman las pantallas (`styles_cetis.xml`)

| Estilo | Qué es (nombre en Figma) |
|---|---|
| `Cetis.Pantalla`, `Cetis.Pantalla.Columna`, `Cetis.Pantalla.Scroll`, `Cetis.Pantalla.Lista` | El "esqueleto": fondo, columna con 12 dp a los lados, zona con scroll o lista |
| `Cetis.BarraSuperior` (+ `.Adorno`, `.Fila`, `.Regresar`, `.Menu`, `.Titulo`, `.Campana`) | Barra guinda de arriba (AppBar); `.Menu` es el botón ☰ |
| `Cetis.Seccion` (+ `.Linea`, `.Pildora`) | Título "línea · píldora · línea" (SectionLabel) |
| `Cetis.Acceso` (+ partes) y `Cetis.Circulo` | Tarjeta de acceso rápido de 128 dp (QuickAccessCard) |
| `Cetis.Opcion` (+ partes) | Renglón de opción con círculo y flecha (Opcion) |
| `Cetis.Tarjeta`, `Cetis.Campo.Renglon` | Tarjeta con renglones "Etiqueta ..... valor" (InfoRow) |
| `Cetis.Chip` | Chips de días, turnos y tipos de estatus |
| `Cetis.Campo.*` | Campo de texto (TextField) |
| `Cetis.Boton` | Botón guinda grande (PrimaryButton) |
| `Cetis.Estatus` (+ partes) | Etiqueta de estado con punto de color (StatusChip) |
| `Cetis.Menu.*` | Opciones del menú ☰ (no está en Figma) |

### 6.2 Renglones de lista (`res/layout/*_item_*.xml`)

| Archivo | Qué es | Dónde se usa |
|---|---|---|
| `comun_item_lista.xml` | Ícono en círculo + título + detalle (+ flecha si se puede tocar) | Entradas y salidas, eventos, grupos, salidas de grupo, estatus |
| `comun_item_notificacion.xml` | Aviso (más oscuro y con punto rojo si no se ha leído) | Notificaciones |
| `comun_item_dia.xml` | Casilla del calendario | Calendario |
| `comun_item_encabezado.xml` | Título de un día | Entradas y salidas |
| `profesor_item_alumno.xml` | Iniciales + nombre + No. de control + estado (StudentRow) | Lista de alumnos del profesor |
| `alumno_item_anuncio.xml` | Tarjeta de anuncio (AnnouncementCard) | Boletín del alumno, Anuncios del padre, Boletín del profesor |
| `alumno_item_clase.xml` | Una clase del horario | Todos los horarios |
| `comun_item_menu.xml` | Una opción del menú ☰ (ícono en círculo + texto) | Menú de todos los roles |

### 6.3 Código compartido (`ui/comun/`)

| Pieza | Qué hace |
|---|---|
| `PantallaXml` | Muestra un XML a pantalla completa; el contenido termina arriba de la barra de gestos |
| `VistasComunes.conectarMenu` y `Pestana` | Conectan el botón ☰ con el menú del rol (opciones, la marcada, a dónde lleva cada una) |
| `VistasComunes.mostrarDialogoCerrarSesion` | Muestra "¿Cerrar sesión?" encima de la pantalla |
| `VistasComunes.llenarLista` / `llenarRenglon` | Llenan una lista corta dentro de una pantalla con scroll |
| `VistasComunes.marcarChip` / `llenarEstado` | Chip elegido en guinda; etiqueta de estado verde o roja |
| `CalendarioXml`, `NotificacionesXml`, `HorarioXml`, `PerfilPersonalXml`, `CambiarContrasenaXml` | Pantallas completas que usan varios roles (piden sus datos solas) |
| `cargar { … }` y `Carga` | Hacen una consulta sin que un error (sin internet, sin permiso) cierre la app |
| `vista.texto(id, …)`, `vista.mostrar(id, …)`, `vista.alTocar(id) { … }`, `vista.avisar(…)` | Atajos para poner un texto, mostrar u ocultar, conectar un toque y mostrar un mensaje corto |

---

## 7. Imágenes e íconos (carpeta `res`)

Todos salieron del archivo de Figma. El prefijo del nombre dice qué son:

| Prefijo | Qué es | Ejemplos |
|---|---|---|
| `ic_` | Íconos | `ic_home`, `ic_qr`, `ic_users`, `ic_flag`, `ic_edit`, `ic_scan` |
| `img_` | Ilustraciones y adornos | `img_avatar_64`, `img_watermark_gear_card` |
| `bg_` | Franjas diagonales de fondo | `bg_header_accent` |
| `shape_` | Formas de color (tarjetas, círculos, chips) | `shape_tarjeta`, `shape_estatus_exito` |
| `logo_` | Logotipos | `logo_cetis61` (PNG, en `drawable-nodpi`) |

Para usar uno en un XML: `android:src="@drawable/ic_home"`.

---

## 8. Textos y datos

Hay dos lugares, y la diferencia importa:

- **`res/values/strings*.xml` → textos que nunca cambian.** `strings.xml` (generales y del
  padre), `strings_alumno.xml`, `strings_comun.xml`, `strings_prefecto.xml` y
  `strings_profesor.xml`.
- **Firestore → datos de cada persona.** Nombre, grupo, anuncios, registros, eventos,
  notificaciones, estatus… Se dan de alta en la consola de Firebase (después, en la web de
  Control Escolar). **La app no trae datos de ejemplo.**

---

## 9. Recetas (cómo hago para…)

### Cambiar un texto fijo
Abre el `strings*.xml` que corresponda, busca el texto y cámbialo. No toques el `name="..."`.

### Cambiar un dato (nombre, grupo, anuncio…)
Se cambia **en Firestore**, no en el código ([BASE_DE_DATOS.md §9](BASE_DE_DATOS.md)).

### Mostrar un dato nuevo de la base en una pantalla
1. Agrega el campo a la clase de `datos/Modelos.kt` (con el mismo nombre que en Firestore).
2. Si necesita otra consulta, agrégala en `datos/CetisRepositorio.kt`.
3. En el XML agrega un `TextView` con su `android:id` y un `tools:text="{variable}"`.
4. En el `*Vistas.kt` del rol, ponlo: `vista.texto(R.id.txt_nuevo, valor)`.

### Cambiar a dónde lleva un botón
Abre el `*App.kt` del rol, busca la pantalla dentro del `when` y cambia lo que está entre llaves:
```kotlin
vista.alTocar(R.id.card_horario) { abrir(PadrePantalla.Horario) }
```

### Agregar una pantalla nueva a un rol
1. Copia un XML parecido de `res/layout/` y renómbralo con el prefijo del rol
   (por ejemplo `profesor_asistencia.xml`). Usa los estilos `Cetis.*` para que mida igual.
2. En el `*App.kt` del rol, agrega su nombre al `enum` (por ejemplo `ProfesorPantalla`).
3. En el mismo archivo, dentro del `when`, agrega su caso:
   ```kotlin
   ProfesorPantalla.Asistencia -> PantallaXml(R.layout.profesor_asistencia, modifier) { vista ->
       vista.alTocar(R.id.btn_regresar) { regresar() }
   }
   ```
4. Desde donde quieras abrirla: `abrir(ProfesorPantalla.Asistencia)`.

### Agregar o cambiar una opción del menú ☰
1. En el `*App.kt` del rol agrega un renglón a `PESTANAS`:
   `Pestana(ProfesorPantalla.Asistencia, R.drawable.ic_users, R.string.profesor_nav_asistencia)`.
2. En el XML de esa pantalla pon el botón ☰ en la barra superior:
   `<ImageButton android:id="@+id/btn_menu" style="@style/Cetis.BarraSuperior.Menu" />`.
3. En su caso del `when`, conéctalo: `conectarMenu(vista, pantalla)`.

### Agregar un ícono nuevo desde Figma
1. En Figma selecciona el ícono → *Export* → formato **SVG**.
2. En Android Studio: clic derecho en `res/drawable` → *New* → *Vector Asset* → *Local file*.
3. Ponle nombre en minúsculas con guion bajo y el prefijo `ic_` (ejemplo: `ic_mapa`).

### El inicio de sesión anterior
`MainAppContainer()` en `Main.kt` es el inicio de sesión viejo (contraseñas guardadas en
Firestore). **Ya no se usa** y las reglas de seguridad actuales lo bloquean. No lo reactives.

---

## 10. Cómo funciona la conexión con la base de datos

```
CetisApp ──► CetisRepositorio.iniciarSesion(No. de control o usuario, contraseña)
   │             └─ Authentication + usuarios/{uid}
   ├─ ¿debe cambiar su contraseña? ──► CambiarContrasenaXml ──► cambiarContrasena()
   ├─ rol "alumno"   ──► AlumnoApp   ─┐
   ├─ rol "padre"    ──► PadresApp   ─┤  piden datos a CetisRepositorio
   ├─ rol "prefecto" ──► PrefectoApp ─┤  (alumno, horario, anuncios, eventos, grupos, estatus…)
   ├─ rol "profesor" ──► ProfesorApp ─┘  y el *Vistas.kt del rol los pone en el XML
   └─ otro rol       ──► aviso "se usa desde la web" + Cerrar sesión
```

- **Las vistas no consultan la base.** Reciben los datos ya listos. Así se pueden ver en la
  vista previa (vacías) sin conexión.
- **Cada consulta puede fallar** (sin internet, sin permiso). `cargar { … }` convierte el error
  en una lista vacía o un mensaje, nunca en un cierre de la app.
- **El profesor escribe** dos cosas: estatus (`agregarEstatus`) y salidas de grupo
  (`darSalidaAlGrupo`). Si falla, ve "No se pudo guardar…" y puede intentar otra vez.
- **El QR del alumno** se dibuja con `Formatos.imagenQr(NoControl.contenidoQr(noControl))`
  (librería ZXing): lleva `CETIS61:` + su No. de control de 14 dígitos (ADR-009).
- **El escaneo del prefecto** (`ui/prefecto/EscaneoPrefecto.kt`): la cámara es la vista
  `BarcodeView` de la librería zxing-android-embedded; al leer un QR, `NoControl.desdeQr` saca
  el número, `CetisRepositorio.datosDelEscaneo` lee al alumno, `Escaneo.alerta` decide la
  alerta y `CetisRepositorio.registrarEscaneo` guarda todo de una vez (registro, ubicación y
  aviso a los padres). La app pide el permiso de cámara al abrir el escáner.
- **Avisos (notificaciones dentro de la app, ADR-010):** todos van al buzón de cada cuenta,
  `usuarios/{uid}/notificaciones`. `Avisos.kt` decide a quién (padres del alumno, según
  `configuracion/plantel`) y arma el documento con `Avisos.nuevo(...)`; quien guarda (el
  escaneo o el estatus del profesor) lo mete **en el mismo lote** que lo demás, para que se
  guarde todo o nada. La campana se enciende con `sinLeerEnVivo(uid)` (`ui/comun/Carga.kt`) y
  `VistasComunes.marcarCampana`; la lista se escucha con
  `CetisRepositorio.escucharNotificaciones`.
- **Para agregar un aviso nuevo:** 1) si es un tipo nuevo, agrégalo a `TipoAviso`
  (`Modelos.kt`), a la lista de `avisoValido` en `firebase/firestore.rules` y a su prueba en
  `firebase/pruebas-reglas`; 2) arma el aviso con `Avisos.nuevo(...)` (texto en
  `strings_*.xml`); 3) guárdalo en el mismo lote que la acción, en `CetisRepositorio`.
- **Pruebas sin teléfono:** `app/src/test/.../EscaneoTest.kt` revisa las reglas de las alertas
  y del No. de control; `AvisosTest.kt`, a quién se avisa y cómo se arma el aviso. Se corren
  con `./gradlew.bat :app:testDebugUnitTest`. Las reglas de Firestore tienen sus propias
  pruebas en `firebase/pruebas-reglas` (necesitan Node.js).

---

## 11. Pendientes conocidos

Todos los pendientes del proyecto, también los de la app (escaneo del prefecto, recuperar
contraseña, padre con varios hijos, menú ☰, textos del padre, ajustar prefecto y profesor a
su Figma…), están juntos y sin repetir en [PENDIENTES.md](PENDIENTES.md), con quién tiene
que actuar.

### Qué se revisó

- 5 oct: las 11 pantallas Compose del padre se compararon contra Figma en un teléfono real
  (Samsung, 1080 × 2340, Android 16). Las vistas XML del 19 oct copian ese mismo diseño.
- 19 oct: compila sin advertencias, `lint` sin errores; en el teléfono, con la sesión del
  alumno de prueba, "Cambiar contraseña" abre, valida los campos vacíos y regresa.
  **Falta** probar con cuentas de padre, prefecto y profesor (lo hace el usuario).

No se ha probado en tabletas, en horizontal ni con la letra del sistema agrandada.

---

## 12. Mini glosario

| Palabra | Qué significa |
|---|---|
| **Vista XML / layout** | Un archivo de `res/layout/` que describe una pantalla o un pedazo de ella. |
| **Inflar** | Convertir un layout XML en vistas reales en la pantalla. |
| **`android:id`** | El nombre de una vista para encontrarla desde el código (`R.id.txt_nombre`). |
| **`tools:text`** | Texto que solo se ve en la vista previa de Android Studio, no en el teléfono. |
| **Estilo (`style`)** | Un grupo de atributos con nombre (`Cetis.Boton`) para no repetirlos en cada vista. |
| **AndroidView** | La pieza de Compose que muestra una vista XML (la usa `PantallaXml`). |
| **Composable** | Una función con `@Composable` que dibuja algo; Bienvenida e Iniciar sesión lo son. |
| **dp / sp** | Unidades de medida (sp para letra; respeta el tamaño de letra del usuario). |
| **Drawable** | Imagen, ícono o forma guardada en `res/drawable`. |
| **Frame** | En Figma, el rectángulo que representa una pantalla. |
| **Token / variable** | Un valor del diseño con nombre (un color, un espacio) para no repetir números sueltos. |
