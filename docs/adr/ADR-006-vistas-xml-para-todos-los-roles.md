# ADR-006: Vistas XML para padre, prefecto y profesor

**Estado:** Aceptada y aplicada (19 de octubre de 2026). El punto 4 (barra inferior) lo
reemplazó [ADR-007](ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md).
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste (pedido del usuario)

> Explica por qué las pantallas del padre pasaron de Compose a vistas XML, cómo se hicieron
> las del prefecto y del profesor sin tener su diseño en Figma, y qué se comparte entre roles.
> Lo que se hizo, paso por paso: [BITACORA.md](../BITACORA.md) (entrada 19-23). Cada vista:
> [VISTAS_POR_ROL.md](../VISTAS_POR_ROL.md).

## Contexto

Hasta hoy la app tenía dos formas de dibujar pantallas:

| Rol | Cómo | Dónde |
|---|---|---|
| Alumno | Vistas XML (`res/layout/alumno_*.xml`) | `ui/alumno/AlumnoApp.kt` |
| Padre | Jetpack Compose (11 pantallas en código Kotlin) | `ui/padres/screens/` |
| Prefecto y profesor | Nada: solo un aviso | — |

El usuario pidió **seguir con el apartado XML** para padre, prefecto y profesor, y dejar
pendientes el escaneo, las alertas, los avisos push y la web.

Respuestas del usuario (19 de octubre):

| Pregunta | Respuesta |
|---|---|
| ¿Qué hacer con las pantallas Compose del padre? | Pasarlas a XML; la app usa las XML y se borran las Compose que ya no se usen |
| ¿Los botones del profesor guardan en Firestore? | Sí: cambiar estatus y dar salida al grupo |
| ¿De dónde sale el diseño de prefecto y profesor? | Del Figma del proyecto (ver abajo) |
| ¿Hacer la pantalla de cambiar contraseña? | Sí, para todos los roles |

> **Corrección (19 oct, más tarde):** el archivo **sí tiene** páginas "Padre", "Prefecto" y
> "Profesor"; la herramienta de Figma no las mostraba en la lista de páginas. Las diferencias
> entre ese diseño y lo que se hizo están en [referencias/FIGMA.md](../referencias/FIGMA.md).
> El punto 6 de la decisión queda como algo temporal hasta ajustar las vistas.

**Lo que se vio en Figma al tomar esta decisión:** solo la página **"Alumno"** y la página
**"Componentes"**, sin pantallas de prefecto ni de profesor. "Componentes" sí trae
piezas pensadas para ellos: `StudentRow` (renglón de alumno), `StatusChip` (etiqueta de
estado), `AlertBanner` (aviso), `KeypadKey` (teclado de No. de control) y los íconos *scan*,
*users*, *edit*, *flag*, *check-circle*, *x-circle* y *search*.

## Decisión

1. **Todas las pantallas después de iniciar sesión son vistas XML**, mostradas dentro de
   Compose con `AndroidView` (como el alumno; ver DECISIONES J5). Bienvenida e Iniciar sesión
   se quedan en Compose porque son de todos los roles.
2. **Un archivo por pantalla con el prefijo del rol:** `padre_*.xml`, `prefecto_*.xml`,
   `profesor_*.xml`. Lo que usan dos o más roles lleva `comun_*.xml` (calendario,
   notificaciones, horario de un grupo, perfil del personal, cambiar contraseña).
3. **Se reutiliza lo del alumno** cuando el diseño es el mismo: el padre ve el Código QR y la
   Credencial de su hijo con `alumno_codigo_qr.xml` y `alumno_credencial_digital.xml`; las
   tarjetas de anuncios y de clases son `alumno_item_anuncio.xml` y `alumno_item_clase.xml`.
4. *(Reemplazado por [ADR-007](ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md): ya
   no hay barra inferior; el menú ☰ la reemplaza.)* **La barra inferior es un archivo por rol** (`padre_barra_inferior.xml`,
   `prefecto_barra_inferior.xml`, `profesor_barra_inferior.xml`) y **la pone el código**
   abajo de las pantallas que la llevan, marcando la pestaña activa. Así una pantalla común
   (por ejemplo el calendario) sirve para el padre y para el prefecto con su propia barra.
5. **Las medidas repetidas son estilos** (`styles_cetis.xml`: `Cetis.BarraSuperior`,
   `Cetis.Acceso`, `Cetis.Opcion`, `Cetis.Chip`, `Cetis.Campo.*`, `Cetis.Boton`…). Cada
   pantalla queda corta y todas miden igual.
6. **Prefecto y profesor se diseñaron con el estilo del alumno** (encabezado, tarjeta de
   perfil, "Accesos Rápidos", renglones de opción) **más las piezas de "Componentes"**.
   Cuando se ajusten a su Figma (ya existe: ver la corrección arriba), se cambian sobre todo los XML.
7. **Cambiar contraseña** es una sola pantalla para todos. Firebase pide la contraseña actual
   antes de cambiarla. Si la cuenta tiene `debeCambiarContrasena = true` o su
   `contrasenaVence` ya pasó, la app la muestra sola al entrar (sin flecha de regresar).

## Opciones consideradas

### Opción A: dejar al padre en Compose y hacer solo prefecto y profesor en XML

| Aspecto | Evaluación |
|---|---|
| Trabajo | Menor |
| Consistencia | Mala: dos formas de dibujar para cosas iguales (calendario, notificaciones) |
| Lo que hay que aprender | Compose **y** XML |

**En contra:** el equipo tiene poca experiencia en Android; mantener dos tecnologías para las
mismas piezas es más difícil. El usuario eligió pasar todo a XML.

### Opción B: todo en XML, cada pantalla con su barra inferior escrita adentro (como el alumno)

| Aspecto | Evaluación |
|---|---|
| Vista previa en Android Studio | Completa (se ve la barra) |
| Repetición | Alta: la barra (~90 líneas) repetida en cada pestaña; las pantallas comunes tendrían que duplicarse por rol |

**En contra:** el calendario, las notificaciones y el perfil del personal tendrían que
copiarse una vez por rol solo para cambiar la barra.

### Opción C: todo en XML, barra por rol puesta por el código (la elegida)

| Aspecto | Evaluación |
|---|---|
| Repetición | Baja: una barra por rol y una pantalla común por tema |
| Vista previa | Las pestañas se ven sin barra (queda el espacio vacío abajo) |
| Cambiar una pestaña | Se edita un solo archivo por rol |

**A favor:** lo que se repite se escribe una vez (DECISIONES, resumen punto 3). **En contra:**
la vista previa de las pestañas no muestra la barra.

## Consecuencias

- **Más fácil:** agregar una pantalla a un rol (copiar una parecida, agregarla al `enum` del
  `*App.kt`), cambiar una pieza en todas las pantallas (un estilo) y reutilizar pantallas
  entre roles.
- **Más difícil:** la vista previa de las pestañas no trae la barra inferior.
- **Las vistas del alumno no se tocaron** (siguen con su barra adentro); solo ganaron la
  opción "Cambiar contraseña". Se pueden pasar al esquema nuevo después.
- **Se borraron** 9 pantallas y 7 piezas Compose del padre, y `PadresModelos.kt` (sus clases
  pasaron a `ui/comun/VariablesComunes.kt`). Bienvenida, Iniciar sesión, `AppIcon`,
  `PrimaryButton` y `SchoolLogo` se quedan.
- **El profesor escribe en Firestore** (estatus y salidas de grupo) con las reglas que ya
  existían (ADR-002); no hizo falta cambiarlas.
- **La contraseña vence:** al cambiarla, `contrasenaVence` = hoy + `diasVenceContrasena`
  (90 en `configuracion/plantel`). Para que no venza, poner `diasVenceContrasena = 0`. El
  plantel todavía no ha dicho cada cuánto debe vencer (pregunta abierta).
- **Hay que revisar después:** ajustar prefecto y profesor a su Figma ([referencias/FIGMA.md](../referencias/FIGMA.md)).

## Acciones

1. [x] Vistas XML del padre (las 9 pantallas) y conectarlas en `PadresApp.kt`.
2. [x] Vistas XML del prefecto sin escaneo: Inicio, Horarios de grupos, Calendario, Perfil.
3. [x] Vistas XML del profesor: Inicio, Grupo (lista de alumnos), Perfil del alumno, Cambiar
   estatus, Dar salida al grupo, Boletín, Perfil.
4. [x] Vistas comunes: calendario, notificaciones, horario de un grupo, perfil del personal y
   cambiar contraseña (también para el alumno).
5. [ ] Probar en el teléfono con las cuentas `padre.prueba`, `profesor.prueba` y
   `prefecto.prueba` (creada el 19 oct; las contraseñas las tiene el usuario).
6. [ ] Ajustar prefecto y profesor a sus páginas de Figma (diferencias en [referencias/FIGMA.md](../referencias/FIGMA.md)).
7. [ ] Pendientes a propósito: escáner QR, captura por No. de control, resultado del escaneo y
   alertas; avisos push; web de Control Escolar y Enfermería.
