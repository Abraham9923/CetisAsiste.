# Vistas XML — Padre, prefecto, profesor y comunes

**Fecha:** 19 de octubre de 2026
**Por qué así:** [ADR-006](adr/ADR-006-vistas-xml-para-todos-los-roles.md). Las vistas del
alumno tienen su propio documento: [VISTAS_ALUMNO.md](VISTAS_ALUMNO.md).

**Diseño de origen:**

- **Padre:** el mismo diseño que tenían sus pantallas en Compose (Figma "Pantallas", 5 oct).
- **Prefecto y profesor:** se armaron con el estilo de las vistas del alumno (Figma, página
  "Alumno") y las piezas de la página "Componentes": `StudentRow`, `StatusChip`,
  `AlertBanner` y los íconos *scan*, *users*, *edit*, *flag* y *check-circle*.
  **Después se encontró que sí tienen su página en Figma** ("Prefecto" y "Profesor"), con
  otro diseño. Las diferencias y los enlaces están en
  [referencias/FIGMA.md](referencias/FIGMA.md); ajustar las vistas queda pendiente.

Igual que las del alumno: **las vistas no traen datos de ejemplo.** Cada dato de la persona
es un campo vacío con su `id`, y lo llena el código con lo que viene de Firestore. En la
vista previa de Android Studio los campos muestran el nombre de la variable entre llaves, por
ejemplo `{alumno.nombre}` (es `tools:text`; en el teléfono no se ve).

---

## 1. Cómo están separadas

| Prefijo | Qué es |
|---|---|
| `padre_*.xml` | Vistas del padre de familia |
| `prefecto_*.xml` | Vistas del prefecto |
| `profesor_*.xml` | Vistas del profesor |
| `comun_*.xml` | Vistas que usan dos o más roles |
| `alumno_*.xml` | Vistas del alumno ([VISTAS_ALUMNO.md](VISTAS_ALUMNO.md)). El padre reutiliza algunas |
| `comun_menu.xml` y `comun_item_menu.xml` | El menú ☰ de todos los roles y cada una de sus opciones |
| `*_item_*.xml` | Un renglón de una lista |

Estilo compartido (todos los roles): `colors_cetis.xml`, `dimens_cetis.xml`,
`styles_cetis.xml`, `shape_*.xml`. Textos fijos: `strings.xml`, `strings_alumno.xml`,
`strings_comun.xml`, `strings_prefecto.xml` y `strings_profesor.xml`.

### Menú ☰ (ya no hay barra inferior)

Desde el 19 de octubre **ninguna pantalla tiene barra inferior** ([ADR-007](adr/ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md)).
Las pantallas principales de cada rol llevan el botón ☰ a la izquierda de la barra superior
(estilo `Cetis.BarraSuperior.Menu`). Al tocarlo se abre `comun_menu.xml`: entra desde la
izquierda sobre una capa oscura, muestra el nombre y el rol de quien inició sesión, una
opción por pantalla principal (la de la pantalla abierta va marcada), **"Notificaciones"**
(la misma pantalla que abre la campana de Inicio, que se queda en su lugar) y, hasta abajo,
**"Cerrar sesión"**, que pregunta antes con el mismo diálogo de Perfil. Se cierra tocando
fuera, eligiendo una opción o con "atrás".

| Rol | Opciones del menú (y pantallas que llevan el ☰) |
|---|---|
| Alumno | Inicio · Boletín · Horario · Perfil |
| Padre | Inicio · Anuncios · Calendario · Perfil |
| Prefecto | Inicio · Escanear · Horarios · Calendario · Perfil |
| Profesor | Inicio · Boletín · Perfil |

Las opciones de cada rol son la lista `PESTANAS` de su `*App.kt` (pantalla, ícono y texto); el
menú lo abre y lo llena `VistasComunes.conectarMenu`, el mismo para todos.

### Estilos nuevos (para que cada vista quede corta)

| Estilo | Qué es |
|---|---|
| `Cetis.Pantalla`, `Cetis.Pantalla.Columna`, `Cetis.Pantalla.Scroll` | Fondo, columna con 12 dp a los lados y zona con scroll |
| `Cetis.BarraSuperior` (+ `.Adorno`, `.Fila`, `.Regresar`, `.Titulo`, `.Campana`) | Barra guinda de arriba (AppBar de Figma) |
| `Cetis.Seccion` (+ `.Linea`, `.Pildora`) | "línea · píldora · línea" (SectionLabel) |
| `Cetis.Acceso` (+ `.Contenido`, `.Flecha`, `.Titulo`, `.Subtitulo`, `.Esquina`) y `Cetis.Circulo` | Tarjetas de Accesos Rápidos de 128 dp |
| `Cetis.Opcion` (+ `.Circulo`, `.Icono`, `.Texto`, `.Flecha`) | Renglón de opción (Perfil) |
| `Cetis.Tarjeta`, `Cetis.Campo.Renglon` | Tarjeta con renglones "Etiqueta ..... valor" |
| `Cetis.Chip` | Chips (días, turnos, tipos de estatus) |
| `Cetis.Campo.Titulo`, `.Caja`, `.Icono`, `.Edicion` | Campo de texto (TextField de Figma) |
| `Cetis.Boton` | Botón guinda grande |
| `Cetis.Estatus` (+ `.Punto`, `.Texto`) | Etiqueta de estado con punto de color (StatusChip) |
| `Cetis.Texto.Vacio`, `Cetis.Texto.Enlace`, `Cetis.Subtitulo` | Texto de "no hay nada", enlace café y subtítulo |

Colores nuevos (de "Componentes"): `cetis_status_success` (#2F8F55) y su fondo (#E2F2E7),
`cetis_status_danger` (#C93B3B) y su fondo (#FBE3E3).

---

## 2. Padre

Código: `ui/padres/PadresApp.kt` (navegación y datos) y `ui/padres/PadresVistas.kt` (llena las
vistas). El alumno que se muestra es el **primer hijo** de `usuarios/{uid}.hijos`.

| Pantalla | Layout | Variables | Cómo se llega |
|---|---|---|---|
| Inicio | `padre_inicio.xml` | hijo (nombre, "Estudiante", grupo) y el anuncio destacado | Al entrar · pestaña |
| Anuncios | `padre_anuncios.xml` + `alumno_item_anuncio.xml` | anuncios dirigidos a `padre` | Pestaña · recuadro guinda de Inicio |
| Calendario | `comun_calendario.xml` | mes y eventos | Pestaña |
| Perfil | `padre_perfil.xml` | hijo (nombre, No. de control, grupo, correo) | Pestaña |
| Código QR | `alumno_codigo_qr.xml` | hijo + imagen del QR | Inicio |
| Credencial digital | `alumno_credencial_digital.xml` | hijo | Inicio · Perfil |
| Entradas y salidas | `padre_entradas_salidas.xml` (+ `comun_item_encabezado`, `comun_item_lista`) | días con sus registros; "Ver más" | Inicio · Perfil |
| Horario | `comun_horario.xml` + `alumno_item_clase.xml` | grupo del hijo, día, clases | Inicio |
| Notificaciones | `comun_notificaciones.xml` | avisos de hoy y anteriores | Campana de Inicio |
| Cambiar contraseña | `comun_cambiar_contrasena.xml` | — | Perfil |

**Novedades respecto a la versión Compose:** el Horario del padre ya muestra las clases del
hijo (antes estaba vacío porque su frame de Figma solo tenía la barra), las flechas del
calendario ya cambian de mes, "Cambiar contraseña" ya abre su pantalla, y "Cerrar sesión"
pregunta antes con el mismo diálogo del alumno.

---

## 3. Prefecto

Código: `ui/prefecto/PrefectoApp.kt`, `ui/prefecto/PrefectoVistas.kt` y, para el escaneo,
`ui/prefecto/EscaneoPrefecto.kt`.

| Pantalla | Layout | Variables | Cómo se llega |
|---|---|---|---|
| Inicio | `prefecto_inicio.xml` | nombre, fecha de hoy, grupos que ya salieron hoy (**en tiempo real**) | Al entrar · pestaña |
| Escanear (Figma 77:500) | `prefecto_escaner.xml` | Entrada o Salida, mensaje ("Buscando al alumno…", "Ese código no es de CetisAsiste."), permiso de cámara | Menú ☰ · tarjeta "Escanear QR" del Inicio |
| Capturar No. de control (Figma 89:859) | `prefecto_captura.xml` | Entrada o Salida, dígitos tecleados (14), error | "Escribir No. de control" del escáner · tarjeta del Inicio |
| Resultado (Figma 77:501 y 77:502) | `prefecto_resultado.xml` | entrada o salida, alerta (o ninguna), hora, alumno (nombre, grupo, No. de control), si se avisó al padre | Después de escanear o capturar · "Escanear siguiente" regresa |
| Horarios de grupos | `prefecto_horarios.xml` | turno elegido y grupos del semestre | Pestaña · Inicio |
| Horario de un grupo | `comun_horario.xml` | grupo elegido, día, clases | Tocar un grupo |
| Calendario | `comun_calendario.xml` | mes y eventos | Pestaña · Inicio |
| Perfil | `comun_perfil_personal.xml` | nombre, usuario, correo | Pestaña |
| Notificaciones | `comun_notificaciones.xml` | avisos | Campana de Inicio |
| Cambiar contraseña | `comun_cambiar_contrasena.xml` | — | Perfil · sola al entrar si es obligatoria |

- **Escaneo** (ADR-009): el QR del alumno lleva `CETIS61:` + su No. de control. Al leerlo (o al
  teclear los 14 dígitos) la app busca al alumno, decide si hay alerta (`datos/Escaneo.kt`) y
  guarda el registro; si no hubo alerta, cambia su ubicación y avisa a sus padres dentro de la
  app. El resultado se ve en verde ("Entrada registrada") o en rojo con el título de la
  alerta. El mismo QR no se registra dos veces seguidas (20 segundos de espera).
- **Alertas:** no dado de alta, inactivo (pago o salud), ya ingresó, no ha ingresado, castigo
  y salida fuera de horario. Cuándo sale cada una: ADR-009 y BASE_DE_DATOS §5.3.
- **Salidas de hoy:** cuando un profesor da salida a su grupo, aparece al instante en el
  Inicio del prefecto ("Grupo 102 puede salir · 11:40 am · materia · profesor").
- **Turno:** antes de la 1 pm se muestran primero los grupos del matutino; después, los del
  vespertino. Los chips cambian de turno.

---

## 4. Profesor

Código: `ui/profesor/ProfesorApp.kt` y `ui/profesor/ProfesorVistas.kt`.

| Pantalla | Layout | Variables | Cómo se llega |
|---|---|---|---|
| Inicio | `profesor_inicio.xml` | nombre, cuántos grupos, sus grupos | Al entrar · pestaña |
| Grupo (lista de alumnos) | `profesor_grupo.xml` + `profesor_item_alumno.xml` | grupo, si ya salió hoy, alumnos inscritos con su estado | Tocar un grupo |
| Horario del grupo | `comun_horario.xml` | grupo, día, clases | Grupo |
| Perfil del alumno | `profesor_alumno.xml` | datos del alumno, su estado y sus estatus | Tocar un alumno |
| Cambiar estatus | `profesor_cambiar_estatus.xml` | alumno; tipo elegido; descripción | Perfil del alumno |
| Dar salida al grupo | `profesor_dar_salida.xml` | grupo, fecha, última clase de hoy | Grupo |
| Boletín de noticias | `profesor_boletin.xml` + `alumno_item_anuncio.xml` | anuncios dirigidos a `profesor` | Pestaña |
| Perfil | `comun_perfil_personal.xml` | nombre, usuario, correo | Pestaña |
| Notificaciones | `comun_notificaciones.xml` | avisos | Campana de Inicio |
| Cambiar contraseña | `comun_cambiar_contrasena.xml` | — | Perfil |

**Lo que guarda el profesor en Firestore** (las reglas ya lo permitían, ver
[BASE_DE_DATOS.md](BASE_DE_DATOS.md) §6):

| Acción | Dónde se guarda | Qué revisa la app antes |
|---|---|---|
| Cambiar estatus | `alumnos/{No. de control}/estatus/{id}` con `tipo` (falta, reporte, llamada_atencion o castigo), `descripcion`, `fecha`, `asignadoPor` y `asignadoPorNombre` | Que haya tipo y descripción |
| Dar salida al grupo | `periodos/{semestre}/salidasDeGrupo/{id}` con `grupoId`, `grupo`, `fecha`, `fechaHora`, `autorizadaPor`, `autorizadaPorNombre` y `materia` (la última clase de hoy) | Que el grupo no haya salido ya hoy (si ya salió, el botón se apaga y se ve el aviso verde) |

Estado del alumno (etiqueta de color): **Inactivo por pago** (rojo) si `activoPorPago` es
false; si no, **Inactivo por salud** (rojo) si `activoPorSalud` es false; si no, **En
prácticas** (verde) si `enPracticas` es true; si no, **Activo** (verde).

---

## 5. Vistas comunes

Código: `ui/comun/PantallasComunes.kt` (cada pantalla pide sus datos) y
`ui/comun/VistasComunes.kt` (las llena).

| Vista | Quién la usa | Qué hace |
|---|---|---|
| `comun_calendario.xml` | Padre, prefecto | Mes con los días con evento y la lista de eventos; las flechas cambian de mes |
| `comun_notificaciones.xml` | Padre, prefecto, profesor | Avisos de hoy y anteriores; "Marcar todas como leídas" |
| `comun_horario.xml` | Padre, prefecto, profesor | Horario de un grupo con chips de lunes a viernes (mismos ids que `alumno_horario.xml`) |
| `comun_perfil_personal.xml` | Prefecto, profesor | Nombre, rol, usuario y correo; Cambiar contraseña; Cerrar sesión |
| `comun_cambiar_contrasena.xml` | Todos (también el alumno) | Contraseña actual, nueva y repetida |
| `comun_item_lista.xml` | Varias | Renglón con ícono en círculo, título, detalle y flecha opcional |
| `comun_item_notificacion.xml` | Notificaciones | Aviso (más oscuro y con punto rojo si no se ha leído) |
| `comun_item_dia.xml` | Calendario | Casilla de un día |
| `comun_item_encabezado.xml` | Entradas y salidas | Título de un día |

### Cambiar contraseña

1. La persona escribe su contraseña actual, la nueva (mínimo 8 caracteres) y la repite.
2. La app revisa que estén los tres campos, que la nueva tenga 8 o más, que las dos nuevas
   sean iguales y que sea distinta de la actual.
3. Firebase confirma la contraseña actual y cambia la nueva.
4. La app anota en `usuarios/{uid}`: `debeCambiarContrasena = false` y `contrasenaVence` =
   hoy + `configuracion/plantel.diasVenceContrasena` (90). Si ese valor es 0, no vence.

**Cambio obligatorio:** si al entrar la cuenta tiene `debeCambiarContrasena = true` o su
`contrasenaVence` ya pasó, la app muestra primero esta pantalla, sin flecha de regresar y con
"Cerrar sesión" abajo. Después de cambiarla, entra normal.

---

## 6. Cómo se llenan (ejemplo)

Cada `*App.kt` pide los datos y los pasa a su `*Vistas.kt`:

```kotlin
PantallaXml(R.layout.prefecto_horarios, modifier) { vista ->
    PrefectoVistas.llenarHorarios(vista, turno, grupos) { id, grupo -> /* abrir su horario */ }
    vista.alTocar(R.id.chip_matutino) { turno = Turno.MATUTINO }
    conectarMenu(vista, pantalla)
}
```

`PantallaXml` (en `ui/comun/VistasComunes.kt`) infla el XML y vuelve a llamar al bloque cada
vez que cambian los datos. `conectarMenu` conecta el botón ☰ con el menú del rol.

## 7. Notas

- La hora, la batería y la barra de gestos no se dibujan: las pone Android. `PantallaXml`
  deja abajo de cada pantalla el alto exacto de la barra de gestos, para que nada quede
  debajo de ella.
- Mientras una lista carga no se muestra el texto de "no hay nada", para que no aparezca
  antes de tiempo.
- **Lista de alumnos del profesor:** Firestore deja fuera a los alumnos que no tengan el campo
  `apellidoPaterno` (la lista se ordena por ese campo). Control Escolar debe llenarlo siempre.
- Los recortes de esquinas redondeadas (`clipToOutline`) se ven completos en Android 12 o
  superior.
- **Lint** de Android avisa "Missing contentDescription" en muchos íconos: es un falso aviso,
  porque la descripción (o "no leer este ícono") viene del estilo y lint no la ve.
