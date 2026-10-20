# Vistas XML — Alumno

**Diseño de origen:** [Figma — CetisAsiste, página "Alumno"](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=75-507)

Las vistas no traen datos de ejemplo. Cada dato de la persona es un campo vacío con su
`id`, y se llena con **variables** (`AlumnoVariables.kt`) por medio de `AlumnoVistas.kt`.

**Desde el 19 de octubre de 2026 ya se usan en la app:** cuando entra una cuenta con rol
`alumno`, `ui/alumno/AlumnoApp.kt` muestra estas vistas, conecta sus botones (tarjetas,
menú ☰, chips de días, opciones de Perfil, cerrar sesión) y las llena con los datos
de Firestore. Ver [BASE_DE_DATOS.md](BASE_DE_DATOS.md).

En la vista previa de Android Studio (pestaña *Design* o *Split*) los campos muestran
el nombre de la variable entre llaves, por ejemplo `{alumno.nombre}`. Eso es solo
para la vista previa (`tools:text`); en el teléfono se ven vacíos hasta que se llenen.

## Cómo están separadas por usuario

Cada archivo empieza con el tipo de usuario:

| Prefijo | Qué es |
|---|---|
| `alumno_*.xml` | Vistas del alumno (esta entrega) |
| `padre_*.xml`, `prefecto_*.xml`, `profesor_*.xml`, `comun_*.xml` | Vistas de los otros roles (19 oct): ver [VISTAS_POR_ROL.md](VISTAS_POR_ROL.md) |
| `colors_cetis.xml`, `dimens_cetis.xml`, `styles_cetis.xml`, `shape_*.xml` | Estilo compartido por todos los usuarios |

Los otros roles reutilizan los mismos colores, estilos y formas, y algunas vistas del alumno:
el padre ve el Código QR y la Credencial de su hijo con `alumno_codigo_qr.xml` y
`alumno_credencial_digital.xml`, y las tarjetas `alumno_item_anuncio.xml` y
`alumno_item_clase.xml` se usan en los boletines y horarios de todos.

## Pantallas

| Frame de Figma | Layout | Variables que recibe |
|---|---|---|
| Iniciar sesion - Alumno | `alumno_iniciar_sesion.xml` | — (la persona escribe No. de control y contraseña en `edit_no_control`, `edit_contrasena`) |
| Inicio - Alumno | `alumno_inicio.xml` | `Alumno`: nombre, rol, grupo |
| Horario - Alumno | `alumno_horario.xml` + `alumno_item_clase.xml` | `HorarioDelDia`: grupo, día, lista de `Clase` |
| Perfil - Alumno | `alumno_perfil.xml` | `Alumno`: nombre, rol, grupo, No. de control, correo |
| Codigo QR - Alumno | `alumno_codigo_qr.xml` | `Alumno` + imagen del QR |
| Credencial digital - Alumno | `alumno_credencial_digital.xml` | `Alumno`: nombre, rol, grupo, No. de control, vigencia |
| Boletin - Alumno | `alumno_boletin.xml` + `alumno_item_anuncio.xml` | lista de `Anuncio` |
| Confirmar cerrar sesion - Alumno | `alumno_confirmar_cerrar_sesion.xml` (Perfil + `alumno_dialogo_cerrar_sesion.xml`) | igual que Perfil |
| (sin frame) Cambiar contraseña | `comun_cambiar_contrasena.xml` (común a todos los roles) | — (la persona escribe las contraseñas) |
| (sin frame) Notificaciones | `comun_notificaciones.xml` (común a todos los roles) | avisos de `usuarios/{uid}/notificaciones`. Se abre con la campana de Inicio o desde el menú ☰ |

## Cómo se llenan

`AlumnoApp.kt` escucha `alumnos/{No. de control}` en Firestore (se actualiza solo), lo convierte
con `Formatos.alumno(...)` y llama a las funciones de `AlumnoVistas`:

```kotlin
val doc = alumnoDeLaSesionEnVivo(sesion)            // alumnos/{No. de control}, en tiempo real
AlumnoVistas.llenarPerfil(vista, Formatos.alumno(doc, nombreGrupo))
```

Funciones disponibles: `llenarInicio`, `llenarPerfil`, `llenarCodigoQr`,
`llenarCredencial`, `llenarHorario`, `llenarBoletin`.

## Notas

- La hora, batería y barra de gestos de Figma no se dibujan: las pone Android.
- **Ya no hay barra inferior** (19 oct, [ADR-007](adr/ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md)).
  Inicio, Boletín, Horario y Perfil llevan el botón ☰ en la barra superior; abre el menú con
  esas cuatro pantallas, "Notificaciones" y "Cerrar sesión" (`comun_menu.xml`, común a todos
  los roles). La campana de Inicio abre las mismas notificaciones (antes no hacía nada). El contenido termina
  arriba de la barra de gestos de Android (lo calcula `PantallaXml`).
- Los recortes de esquinas redondeadas (`clipToOutline`) se ven completos en Android 12 o superior.
- **Desde el 19 de octubre las pantallas del padre también son vistas XML** (antes eran
  Compose), igual que las nuevas del prefecto y del profesor: ver
  [VISTAS_POR_ROL.md](VISTAS_POR_ROL.md) y [ADR-006](adr/ADR-006-vistas-xml-para-todos-los-roles.md).
- En Perfil, **"Cambiar contraseña"** ya abre su pantalla (antes no hacía nada).
- `AlumnoApp.kt` muestra estas vistas con `PantallaXml`, la misma función que usan los otros
  roles.
- **El código QR** lleva `CETIS61:` + el No. de control de 14 dígitos; la app lo dibuja sola
  (ADR-009). Es lo que lee el escáner del prefecto.
