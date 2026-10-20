# El porqué de todo — Decisiones de las pantallas de padres

**Proyecto:** CetisAsiste (Android, Kotlin + Jetpack Compose)
**Fecha:** 5 de octubre de 2026

Este documento explica **por qué** cada cosa se hizo como se hizo.
Para saber *dónde está* cada cosa y *cómo cambiarla*, lee [MANUAL_TECNICO.md](MANUAL_TECNICO.md).

> **Ojo (19 de octubre de 2026):** las secciones A a H describen la primera versión de las
> pantallas del padre, hecha en **Compose** el 5 de octubre. Ese mismo diseño ahora está en
> **vistas XML** (sección K y [ADR-006](adr/ADR-006-vistas-xml-para-todos-los-roles.md)).
> Las razones de diseño (Figma manda, nombres de Figma, lo que se repite se escribe una vez,
> las pantallas solo dibujan) siguen valiendo; lo que cambió es dónde vive el código.

Cada decisión tiene el mismo formato:

- **Qué se hizo**
- **Por qué**
- **Qué otra opción había** y por qué no se eligió

---

## Resumen en 30 segundos

1. **Figma manda.** Colores, tamaños, textos e íconos salen del archivo de Figma, no se inventó nada.
2. **Los nombres del código son los nombres de Figma.** Así diseño y código "hablan el mismo idioma".
3. **Lo que se repite se escribe una sola vez.** Colores, estilos de letra y piezas compartidas.
4. **Las pantallas solo dibujan.** No saben de bases de datos ni deciden a dónde navegar.
5. **Se eligió siempre lo más simple** que funcionara, sin agregar librerías nuevas.
6. **No se borró nada de lo que ya existía.**

---

## A. Organización del proyecto

### A1. Todo lo nuevo vive en la carpeta `ui/padres`

- **Qué se hizo:** las pantallas, piezas y navegación están en un paquete propio, `ui/padres`.
- **Por qué:** el pedido fue construir el apartado de padres. Al tenerlo separado, se
  puede trabajar en él sin miedo de romper otra parte, y si después se hace el apartado
  de otro tipo de usuario, tendrá su propia carpeta junto a esta.
- **Otra opción:** seguir escribiendo todo dentro de `Main.kt`, como estaba. Ese archivo
  ya tenía más de 500 líneas con una sola pantalla; con 11 sería imposible de leer.

### A2. Una pantalla = un archivo, con el nombre del frame de Figma

- **Qué se hizo:** el frame "Calendario" es `CalendarioScreen.kt`, "Perfil" es `PerfilScreen.kt`, etc.
- **Por qué:** para encontrar una pantalla no hay que buscar; basta ver cómo se llama en Figma.
- **Otra opción:** nombres en inglés (`HomeScreen`, `ProfileScreen`). Se descartó porque
  obligaría a "traducir" mentalmente cada vez que alguien compara con Figma.

### A3. Las piezas tienen el nombre de su capa en Figma

- **Qué se hizo:** `AppBar`, `BottomNav`, `QuickAccessCard`, `AnnouncementCard`,
  `ImportantBanner`, `SectionLabel`… son los nombres de las capas y componentes de Figma.
- **Por qué:** misma razón que A2. Además, el código que ya existía en `Main.kt` nombraba
  sus piezas en inglés (`ProfileCard`, `QuickAccessCard`), así que se respetó esa costumbre.
- **Nota:** los *datos* sí están en español (`Anuncio`, `Evento`, `Notificacion`) porque
  son palabras del plantel, igual que los campos que ya se usan en la base de datos
  (`noControl`, `usuarios`).

### A4. Carpeta `components` solo para lo que se repite

- **Qué se hizo:** una pieza va a `components/` solo si la usan **dos o más** pantallas.
  Si la usa una sola, queda dentro del archivo de esa pantalla.
- **Por qué:** al abrir `InicioScreen.kt` ves todo lo que forma Inicio sin saltar entre
  archivos, y `components/` no se llena de piezas que nadie más usa.
- **Otra opción:** un archivo por cada pieza, aunque se use una vez. Serían más de 20
  archivos pequeños y sería más difícil seguir una pantalla completa.

---

## B. El estilo: colores, letra y medidas

### B1. Los colores son constantes con el nombre de Figma

- **Qué se hizo:** en `Color.kt` hay 9 colores (`Brand800`, `SurfaceCard`, `TextSecondary`…)
  y las pantallas los usan directamente por su nombre.
- **Por qué:** si mañana el guinda cambia, se cambia **una línea** y se actualiza toda la
  app. Y como el nombre es el mismo que en Figma, no hay duda de cuál color es cuál.
- **Otra opción:** escribir el código de color (`#5A1E24`) en cada lugar. Se descartó:
  ese color aparece decenas de veces y habría que cambiarlas una por una.
- **Otra opción:** usar solo los "roles" de Material (`primary`, `surfaceVariant`…).
  Material no tiene un rol que corresponda a `SurfacePill` o `TextOnBrandMuted`, así que
  habría que forzarlos en nombres que no significan eso. Aun así, los colores principales
  **también** se registraron en el tema de Material (`Theme.kt`) para que lo que Material
  dibuja por su cuenta (botones, efecto al tocar) salga con los colores correctos.

### B2. Los estilos de letra van en los "casilleros" de Material

- **Qué se hizo:** los 9 estilos de texto de Figma se guardaron en los casilleros que
  Material ya trae (`titleLarge`, `bodyMedium`, `labelSmall`…). La tabla de equivalencias
  está en el manual técnico.
- **Por qué:** aquí sí hubo correspondencia limpia, uno a uno, y es la forma en que el
  proyecto ya declaraba su tipografía. Varios coincidían casi exacto con los valores de fábrica.
- **Otra opción:** crear una lista propia de estilos. Funcionaría, pero sería una segunda
  forma de hacer lo mismo que Material ya resuelve.

### B3. El alto de línea se reparte como en Figma

- **Qué se hizo:** en `Type.kt` todos los estilos indican que el alto de línea se
  reparte parejo arriba y abajo del texto.
- **Por qué:** por defecto Android recorta ese espacio en el primer y último renglón, y
  cada texto quedaría unos puntos más bajo que en Figma. Sumado en una pantalla entera,
  todo se vería más apretado que el diseño.

### B4. La fuente va dentro de la app, en cuatro archivos

- **Qué se hizo:** se agregaron a `res/font/` los cuatro grosores de *Plus Jakarta Sans*
  que usa Figma (Regular, Medium, SemiBold y Bold), y todos los estilos leen la fuente
  de una sola línea (`AppFontFamily` en `Type.kt`).
- **Por qué dentro de la app:** así la letra es idéntica a Figma en cualquier teléfono
  y funciona sin internet. Pesa alrededor de medio megabyte en total.
- **Otra opción:** las "fuentes descargables" de Google, que el teléfono baja la
  primera vez. Ahorra ese medio megabyte, pero depende de internet y de los servicios de
  Google, y mientras descarga la app se ve con otra letra.
- **Otra opción:** un solo archivo "variable" con todos los grosores. Los grosores de
  ese tipo de archivo no funcionan en Android 7 (el proyecto lo soporta: `minSdk 24`);
  ahí todo se vería con el mismo grosor.
- **Licencia:** es una fuente libre (*SIL Open Font License*); se puede incluir en la
  app sin costo. El aviso de derechos va dentro de los propios archivos.

### B5. Se quitaron el modo oscuro y el "color dinámico"

- **Qué se hizo:** el tema ahora usa siempre los colores del diseño.
- **Por qué:** el proyecto venía con la plantilla de Android Studio, que en Android 12 o
  superior **pinta la app con los colores del fondo de pantalla del usuario** y cambia a
  morado oscuro en modo noche. Con eso la app nunca se vería guinda. Figma solo define
  un modo (claro), así que se dejó ese.
- **Otra opción:** inventar un modo oscuro. Se descartó porque no está diseñado.

### B6. Espacios y esquinas con nombre

- **Qué se hizo:** `Dimens.kt` guarda los espacios (`Space4`…`Space20`) y radios
  (`RadiusMd`, `RadiusLg`) que Figma tiene como variables.
- **Por qué:** misma idea que los colores. Los valores que en Figma **no** son variable
  (por ejemplo un margen de 6 o 22) se dejaron como número, para ser fieles al diseño.

---

## C. Imágenes e íconos

### C1. Los íconos son vectores exportados de Figma

- **Qué se hizo:** cada ícono se exportó de Figma como SVG y se convirtió a *Vector
  Drawable* (`.xml` en `res/drawable`), el formato de vectores de Android.
- **Por qué:** un vector se ve nítido en cualquier pantalla y pesa muy poco (los 37
  archivos juntos pesan menos que una foto). Y al venir de Figma, son **exactamente** los
  del diseño.
- **Otra opción:** una librería de íconos (Material Icons). Los íconos se parecerían
  pero no serían los mismos, y agrega una dependencia.
- **Otra opción:** PNG. Se necesitarían 5 tamaños de cada uno y se verían borrosos al agrandar.

### C2. Algunos íconos están "repetidos" en otro color o tamaño

- **Qué se hizo:** hay, por ejemplo, `ic_chevron_right_dark` y `ic_chevron_right_light`,
  o `img_avatar_64`, `img_avatar_72` e `img_avatar_96`.
- **Por qué:** así están en Figma, y cada uno se usa en el lugar exacto donde el diseño
  lo pone. El avatar no es el mismo dibujo agrandado: la cabeza y los hombros tienen
  proporciones distintas en cada tamaño.
- **Otra opción:** un solo ícono y pintarlo por código. Es válido y ahorra archivos,
  pero se priorizó que el resultado sea idéntico al diseño. Se puede simplificar después.

### C3. El logotipo es PNG y se recorta

- **Qué se hizo:** `logo_cetis61.png` está en `drawable-nodpi` y la pieza `SchoolLogo` lo recorta.
- **Por qué:** en Figma el logotipo es una imagen (no un vector), así que se usó tal
  cual. La imagen trae margen en blanco arriba y abajo que el diseño recorta; `SchoolLogo`
  hace ese mismo recorte en un solo lugar. `nodpi` le dice a Android que no la reescale.

### C4. No se dibujó la hora "9:41" ni la barrita de abajo

- **Qué se hizo:** la hora, la batería, la señal y la barra de gestos que aparecen en
  Figma **no** están en el código.
- **Por qué:** eso lo dibuja Android. En Figma están solo para que el diseño parezca un
  teléfono. Si se dibujaran, saldrían dos relojes.

---

## D. Estructura de las pantallas

### D1. Todas las pantallas usan el mismo "esqueleto"

- **Qué se hizo:** `PadresScaffold` pone el fondo, la barra de arriba, la barra de abajo
  (si lleva) y acomoda el contenido en columna con 12 dp a los lados y 16 dp entre bloques.
- **Por qué:** esas medidas son iguales en los 9 frames. Escribirlas una vez garantiza
  que todas las pantallas queden alineadas igual, y una pantalla nueva sale ya bien acomodada.

### D2. El contenido tiene scroll aunque en Figma "cabe"

- **Qué se hizo:** el contenido de cada pantalla se puede desplazar hacia abajo.
- **Por qué:** Figma dibuja un teléfono de 360 × 800. En un teléfono más chico, o si el
  usuario agranda la letra, el contenido ya no cabe y sin scroll quedaría cortado.

### D3. El color llega hasta arriba, detrás de la barra de estado

- **Qué se hizo:** la barra guinda se dibuja también detrás de la hora y la batería, y
  se le reserva a esa zona la altura real que reporta el teléfono (mínimo 28 dp).
- **Por qué:** así está en el diseño (el guinda llega al borde superior). La altura de
  esa zona cambia según el teléfono (con cámara en "isla" es más alta); por eso se le
  pregunta al sistema en vez de fijar un número. El mínimo de 28 dp es para que la vista
  previa de Android Studio, donde no hay barra de estado, mida lo mismo que en Figma.
- **Además:** en `Main.kt` se indicó que los íconos de arriba sean claros (van sobre
  guinda) y los de abajo oscuros (van sobre fondo claro).

### D4. Los adornos se anclan a la derecha

- **Qué se hizo:** el engrane de marca de agua, el "61", la franja diagonal y las
  esquinas café se colocan midiendo desde el borde **derecho**.
- **Por qué:** en Figma están a "tantos píxeles desde la izquierda" porque el frame mide
  siempre 360. En un teléfono más ancho, esa misma medida los dejaría flotando a media
  tarjeta. Anclados a la derecha quedan en su esquina en cualquier ancho.

### D5. Algunas tarjetas tienen alto fijo

- **Qué se hizo:** la tarjeta de perfil (100 dp) y las de accesos rápidos (128 dp)
  tienen el alto del diseño; el resto crece según su contenido.
- **Por qué:** en Figma esas tienen alto fijo, y las cuatro de accesos rápidos deben
  medir igual para formar la cuadrícula.
- **Ojo:** con la letra del sistema muy grande, un texto largo podría cortarse ahí.

---

## E. Navegación

### E1. Una "pila" sencilla en vez de la librería Navigation

- **Qué se hizo:** `PadresApp.kt` guarda una lista con las pantallas abiertas. Abrir una
  la agrega al final; regresar quita la última; se muestra siempre la última.
- **Por qué:** son unas 20 líneas que se pueden leer completas, sin instalar nada. Para
  11 pantallas sin datos que pasar entre ellas, es suficiente.
- **Otra opción:** *Navigation Compose*, la librería oficial. Es la opción correcta
  cuando la app crezca (enlaces directos, pasar datos, animaciones), pero trae conceptos
  nuevos (`NavHost`, rutas) y una dependencia más. Las pantallas ya están hechas de forma
  que el cambio solo tocaría `PadresApp.kt` (ver E2).

### E2. Las pantallas no navegan: solo avisan

- **Qué se hizo:** una pantalla no dice "abre Notificaciones". Recibe una función
  (`onNotificationsClick`, `onBack`, `onTabSelected`) y la llama cuando tocan algo.
  Quien decide a dónde ir es `PadresApp`.
- **Por qué:**
  1. Todo el "mapa" de la app está en un solo archivo.
  2. Las pantallas se pueden ver en *Preview* sin armar la app completa.
  3. El día que se cambie la forma de navegar, las pantallas no se tocan.

### E3. Las pestañas de abajo no se apilan

- **Qué se hizo:** al tocar Anuncios, Calendario o Perfil, debajo siempre queda solo Inicio.
- **Por qué:** es lo que la gente espera en Android: si brincas entre pestañas diez
  veces, "atrás" te lleva a Inicio y luego sale de la app, no te regresa por las diez.

### E4. La bienvenida avanza sola

- **Qué se hizo:** después de 1.5 segundos pasa a Iniciar sesión, y ya no se puede
  volver a ella con "atrás".
- **Por qué:** es una pantalla de presentación. Más adelante, ese tiempo es el lugar
  natural para revisar si el usuario ya tenía sesión iniciada.

---

## F. Textos y datos

### F1. Textos fijos en `strings.xml`

- **Qué se hizo:** títulos, etiquetas y botones están en `strings.xml`, no escritos
  dentro de las pantallas.
- **Por qué:** es la forma estándar en Android y la que el proyecto ya usaba. Permite
  corregir un texto sin abrir código, y los que se repiten ("Credencial digital" aparece
  en tres lugares) se escriben una vez.

### F2. Datos de ejemplo en `SampleData.kt` — *reemplazada el 19 de octubre (ver J1)*

- **Qué se hizo:** todo lo que es "de mentira" (nombre, grupo, anuncios, registros,
  eventos, notificaciones) está junto en un archivo.
- **Por qué:** deja clarísimo **qué es interfaz y qué es dato**. Cuando se conecte la
  base de datos, la lista de lo que hay que reemplazar es exactamente ese archivo.
- **Otra opción:** escribir "Juan Perez Rodriguez" dentro de cada pantalla. Aparece en
  cinco pantallas; habría que cazarlo una por una.

### F3. Se corrigieron acentos

- **Qué se hizo:** donde Figma dice "Codigo", "Matricula", "mas recientes" y "de el",
  la app dice "Código", "No. de control", "más recientes" y "del".
- **Por qué:** son erratas del diseño (en otras partes del mismo archivo sí llevan
  acento). Conviene corregirlas también en Figma para que coincidan.

### F4. Los textos dicen "Estudiante"

- **Qué se hizo:** se respetaron los textos de Figma: "Estudiante", "Escanea tu código",
  "Consulta tus horarios de clase", "Registraste tu salida".
- **Por qué:** el pedido fue construir estas pantallas para el apartado de padres, pero
  el diseño está redactado como si lo leyera el alumno. Cambiar la redacción es una
  decisión del equipo, no del código. Por F1 y F2, hacerlo después solo requiere editar
  `strings.xml` y `SampleData.kt`.

---

## G. Detalles de pantallas específicas

### G1. Los campos de Iniciar sesión se construyeron "a mano"

- **Qué se hizo:** se usa `BasicTextField` (el campo de texto sin decoración) y se le
  dibujó encima el diseño: fondo blanco, borde, ícono, etiqueta arriba.
- **Por qué:** el campo de Material trae su propio aspecto (etiqueta flotante, otra
  altura, otros márgenes) y dejarlo igual a Figma requiere pelear contra él.
- **En cambio,** el botón **sí** es el `Button` de Material, porque con cambiarle color,
  forma y alto queda idéntico al diseño y conserva el efecto al tocarlo y la accesibilidad.

### G2. Sí se puede escribir en los campos, aunque "no haya funcionalidad"

- **Qué se hizo:** los campos aceptan texto y el ojo muestra/oculta la contraseña, pero
  lo escrito no se envía ni se valida.
- **Por qué:** escribir en un campo es comportamiento de la *interfaz*. Un campo donde
  no se puede teclear parecería descompuesto al probar las pantallas.

### G3. El logotipo de Iniciar sesión se va al fondo

- **Qué se hizo:** el logotipo queda pegado abajo si sobra espacio, y si no sobra (o
  sale el teclado) se recorre y se puede hacer scroll.
- **Por qué:** en Figma está en una posición fija cerca del borde inferior. Fijo de
  verdad se encimaría con el formulario en teléfonos chicos o al abrir el teclado.

### G4. La sombra de la credencial

- **Qué se hizo:** la tarjeta usa una sombra con los valores exactos de Figma
  (desplazada 6 hacia abajo, difuminado 18, guinda al 12 %).
- **Por qué:** la sombra normal de Android (por "elevación") no permite elegir color ni
  difuminado; se vería gris y más dura que en el diseño.

### G5. El calendario se arma con una cuenta, no día por día

- **Qué se hizo:** se arma una lista con 3 casillas vacías + los días 1 al 31 y se
  parte en renglones de 7. Sábado y domingo van en gris; el día de hoy, en círculo guinda;
  los días con evento llevan un punto.
- **Por qué:** en Figma son 35 casillas dibujadas una por una. Copiarlas así serían
  cientos de líneas repetidas. Con la cuenta, mostrar otro mes será cambiar tres números.
- **Ojo:** el mes es fijo (octubre 2026, como en Figma). Calcularlo con la fecha real ya
  es funcionalidad.

### G6. Horario está vacía

- **Qué se hizo:** solo tiene la barra superior con su flecha de regresar.
- **Por qué:** así está el frame en Figma. No se inventó un contenido que no está diseñado.

### G7. Hay cosas que se ven "tocables" y no hacen nada

- *(19 oct: el menú ☰ y "Cambiar contraseña" ya funcionan; ver L1 y K7.)*
- **Qué se hizo:** el menú ☰, "Cambiar contraseña", "¿Olvidaste tu contraseña?",
  "Marcar todas como leídas" y las flechas del calendario están dibujados pero no llevan a ningún lado.
- **Por qué:** están en el diseño, pero su destino no existe en Figma o es funcionalidad.
  Se dejaron dibujados para que la pantalla esté completa.

---

## H. Lo que ya existía

### H1. No se borró el inicio de sesión con Firebase

- **Qué se hizo:** en `Main.kt` solo cambiaron dos cosas: el color de los íconos de la
  barra de estado y que ahora se muestra `PadresApp()` en lugar de `MainAppContainer()`.
  Todo el código anterior sigue ahí, intacto.
- **Por qué:** ese código tiene la lógica de registro e inicio de sesión que servirá
  cuando se conecte la funcionalidad. Las pantallas nuevas son la versión diseñada de lo
  que ese archivo ya esbozaba (tarjeta de perfil, información importante, accesos
  rápidos, barra inferior), así que tenía sentido que la app arranque con ellas.
- **Para regresar:** cambiar `PadresApp()` por `MainAppContainer()`.

### H2. El tema sí cambió para toda la app

- **Qué se hizo:** `Color.kt`, `Type.kt` y `Theme.kt` se reescribieron con los valores de Figma.
- **Por qué:** el tema es de toda la app, no de un apartado. Las pantallas anteriores
  también toman de ahí sus colores, así que ahora también se ven guindas en vez de moradas.

### H3. No se agregó ninguna librería

- **Qué se hizo:** `build.gradle.kts` y `libs.versions.toml` no se tocaron. Lo único
  que se agregó de fuera fueron los archivos de la fuente (B4) y las imágenes de Figma,
  que son recursos, no librerías.
- **Por qué:** todo se pudo resolver con lo que el proyecto ya tenía. Menos
  dependencias significa menos cosas que actualizar y menos que explicar.

---

## I. Aviso aparte: seguridad del inicio de sesión anterior — *resuelto el 19 de octubre*

Al revisar `Main.kt` se vio que el inicio de sesión de entonces **guardaba las contraseñas
tal cual en la base de datos** y las comparaba desde el teléfono. Eso permitía que
cualquiera que pudiera leer esa colección viera las contraseñas de todos.

**Ya se resolvió** (ver J2 y J3): la app usa *Firebase Authentication* y las reglas de
seguridad cierran la colección vieja. Queda una tarea para el equipo: **borrar desde la
consola** los documentos viejos de `usuarios` y la colección `CetisAsiste`, que todavía
tienen el campo `contrasenia`.

---

## J. Base de datos e inicio de sesión real (19 de octubre de 2026)

El detalle de cada una está en los ADR de [`adr/`](adr/) y en
[BASE_DE_DATOS.md](BASE_DE_DATOS.md).

### J1. No hay datos de ejemplo, ni en la app ni en la base

- **Qué se hizo:** se borró `SampleData.kt`. Las pantallas reciben sus datos como
  parámetros y, si no hay nada, lo dicen ("Todavía no hay anuncios."). La base empezó vacía.
- **Por qué:** lo pidió el usuario. Los datos los da de alta Control Escolar, y un dato de
  ejemplo en la base lo vería cualquier persona real (por ejemplo, un anuncio falso).
- **Otra opción:** dejar los ejemplos para probar. Se cargaron 32 documentos de prueba y se
  retiraron el mismo día.

### J2. Inicio de sesión con el No. de control convertida en correo interno

- **Qué se hizo:** la persona escribe su No. de control o usuario; la app arma
  `noControl@cetis61.mx` y usa Firebase Authentication. Luego lee `usuarios/{uid}` y su rol.
- **Por qué:** el alumno no tiene que recordar un correo y las contraseñas quedan cifradas
  por Google. Detalle: [ADR-003](adr/ADR-003-inicio-de-sesion-con-correo-interno.md).
- **Otra opción:** el correo institucional real. Obligaría a recordar otro dato y no todos
  los padres tienen uno.

### J3. Reglas de seguridad por rol, sin Cloud Functions por ahora

- **Qué se hizo:** reglas que dicen qué lee y escribe cada rol; la app escribe directo.
- **Por qué:** Cloud Functions requiere el plan de pago Blaze y la base estaba abierta a
  cualquiera. Detalle: [ADR-002](adr/ADR-002-reglas-sin-cloud-functions.md).

### J4. Un solo lugar habla con Firebase

- **Qué se hizo:** `datos/CetisRepositorio.kt` hace todas las consultas; las pantallas no
  saben que existe Firestore.
- **Por qué:** es la misma idea que E2 (las pantallas solo dibujan). Si cambia la base, se
  toca un archivo.

### J5. Las vistas de alumno se muestran dentro de Compose

- **Qué se hizo:** `ui/alumno/AlumnoApp.kt` muestra los `alumno_*.xml` con `AndroidView`.
- **Por qué:** la app es una sola actividad en Compose; así no hace falta una segunda
  actividad y las vistas XML se usan tal como se diseñaron.
- **Otra opción:** rehacer las vistas de alumno en Compose. Sería duplicar trabajo ya hecho.

### J6. El QR lleva un texto al azar, no el No. de control — *reemplazada el 19 de octubre (ver M2)*

- **Qué se hizo:** el QR se dibuja con `alumnos/{m}.codigoQr` usando la librería ZXing
  (la primera dependencia nueva desde H3; es pequeña y no necesita internet).
- **Por qué:** con el No. de control, cualquiera podría fabricar el QR de otro alumno.

---

## K. Vistas XML para todos los roles (19 de octubre de 2026)

Detalle y opciones: [ADR-006](adr/ADR-006-vistas-xml-para-todos-los-roles.md). Cada vista:
[VISTAS_POR_ROL.md](VISTAS_POR_ROL.md).

### K1. El padre pasó de Compose a vistas XML

- **Qué se hizo:** sus 9 pantallas son ahora `padre_*.xml` (y algunas comunes o del alumno).
  Se borraron las pantallas y piezas Compose que ya no se usaban.
- **Por qué:** lo pidió el usuario. Con todo en XML el equipo aprende y mantiene una sola
  forma de hacer pantallas, y padre, prefecto y profesor comparten las mismas piezas.
- **Otra opción:** dejar al padre en Compose. Habría dos versiones de lo mismo (calendario,
  notificaciones) escritas de dos formas distintas.

### K2. Lo que usan dos o más roles se escribe una vez

- **Qué se hizo:** `comun_*.xml` para calendario, notificaciones, horario de un grupo, perfil
  del personal y cambiar contraseña; el padre usa el Código QR y la Credencial del alumno.
- **Por qué:** es la regla de siempre (resumen, punto 3). Un cambio en el calendario se hace
  en un solo archivo y se ve en todos los roles.

### K3. La barra inferior la pone el código — *reemplazada el 19 de octubre (ver L1)*

- **Qué se hizo:** una barra por rol (`<rol>_barra_inferior.xml`); la app la pone abajo y
  marca la pestaña activa.
- **Por qué:** así una pantalla común sirve con la barra de cualquier rol.
- **Costo:** en la vista previa de Android Studio las pestañas se ven sin barra.

### K4. Estilos para las piezas repetidas

- **Qué se hizo:** `styles_cetis.xml` ganó estilos como `Cetis.BarraSuperior`,
  `Cetis.Acceso`, `Cetis.Opcion`, `Cetis.Chip` y `Cetis.Boton`.
- **Por qué:** es lo mismo que B1 y B6 (un valor con nombre se cambia en un lugar), pero para
  piezas completas. Cada pantalla XML queda corta y todas miden igual.

### K5. Prefecto y profesor sin Figma propio

- **Qué se hizo:** se diseñaron con el estilo de las vistas del alumno y las piezas de la
  página "Componentes" (`StudentRow`, `StatusChip`, `AlertBanner`, íconos).
- **Por qué:** al revisar el archivo de Figma solo se vieron las páginas "Alumno" y
  "Componentes". No se inventaron colores ni formas: todo sale del diseño que ya existe.
- **Corrección (19 oct):** **sí hay páginas "Prefecto" y "Profesor"** en Figma; la
  herramienta no las mostraba en la lista de páginas. Las diferencias con las vistas actuales
  están en [referencias/FIGMA.md](referencias/FIGMA.md). Ajustarlas queda pendiente; casi todo
  se cambia en los XML y en su `*App.kt`.

### K6. El escaneo se ve, pero no funciona todavía — *reemplazada el 19 de octubre (ver M3)*

- **Qué se hizo:** en el Inicio del prefecto, "Escanear QR" y "Capturar No. de control" se ven más
  claras, dicen "Próximamente" y al tocarlas avisan que todavía no están.
- **Por qué:** el usuario pidió dejar el escaneo pendiente; así el prefecto ve dónde estará.

### K7. Cambiar contraseña pide la actual y puede ser obligatorio

- **Qué se hizo:** una sola pantalla para todos. Firebase confirma la contraseña actual antes
  de cambiarla. Si `debeCambiarContrasena` es true o la contraseña venció, la app la muestra
  al entrar, sin flecha de regresar.
- **Por qué:** Firebase exige haber confirmado la contraseña hace poco para cambiarla; y el
  documento de roles pide que el prefecto cambie la contraseña que le da Control Escolar.
- **Otra opción:** mandar un correo para cambiarla. Los correos internos `@cetis61.mx` no
  existen (ADR-003).

---

## L. Menú ☰ en lugar de la barra inferior (19 de octubre de 2026)

Detalle y opciones: [ADR-007](adr/ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md).

### L1. Se quitó la barra inferior; el ☰ abre un menú con las mismas pantallas

- **Qué se hizo:** ningún rol tiene barra inferior. Sus pantallas principales llevan el botón
  ☰ en la barra superior, y el menú (`comun_menu.xml`) muestra el nombre y el rol de la
  persona, una opción por pantalla principal y, hasta abajo, "Cerrar sesión".
- **Por qué:** lo pidió el usuario: el ☰ estaba dibujado y no hacía nada (G7). Además se ganan
  100 dp en cada pantalla y ya no hay que reservar ese espacio a mano.
- **Otra opción:** dejar la barra y que el menú repitiera sus opciones. Habría dos caminos para
  lo mismo.

### L2. Un solo menú para los cuatro roles

- **Qué se hizo:** cada `*App.kt` tiene una lista `PESTANAS` (pantalla, ícono, texto) y llama
  a `VistasComunes.conectarMenu`. El menú, su animación y el botón "atrás" son el mismo código.
- **Por qué:** es la regla de siempre (resumen, punto 3): agregar una opción es agregar un
  renglón.

### L3. "Cerrar sesión" del menú pregunta antes

- **Qué se hizo:** la opción de hasta abajo abre el mismo diálogo "¿Cerrar sesión?" que Perfil.
- **Por qué:** está junto a las demás opciones y es fácil tocarla sin querer.

### L4. Notificaciones en dos lugares: la campana y el menú

- **Qué se hizo:** el menú tiene la opción "Notificaciones", debajo de las pantallas
  principales. La campana de Inicio se queda donde estaba; las dos abren la misma pantalla
  (`comun_notificaciones.xml`), que se apila: "atrás" regresa a donde estabas. El alumno, que
  tenía la campana pero sin pantalla, ahora también tiene sus notificaciones.
- **Por qué:** lo pidió el usuario: la campana solo está en Inicio, y desde el menú se llega a
  las notificaciones desde cualquier pantalla principal.
- **Otra opción:** poner la campana en la barra superior de todas las pantallas. Repetiría el
  botón en 12 archivos.

---

## M. No. de control y escaneo del prefecto (19 de octubre de 2026)

Detalle y opciones: [ADR-009](adr/ADR-009-no-de-control-y-escaneo.md).

### M1. "No. de control" de 14 dígitos en lugar de "matrícula"

- **Qué se hizo:** todo lo que decía "matrícula" dice "No. de control": textos, documentos,
  código y base de datos (el campo `matricula` pasó a `noControl`). La app exige 14 dígitos al
  iniciar sesión y en la captura del prefecto.
- **Por qué:** es el número que usa el plantel y que trae la credencial (14 dígitos).
- **Otra opción:** cambiar solo los textos y dejar `matricula` en la base. El usuario prefirió
  que todo dijera lo mismo.

### M2. El QR lleva el No. de control

- **Qué se hizo:** el QR es `CETIS61:` + el No. de control; la app lo dibuja sola. El campo
  `codigoQr` ya no se usa.
- **Por qué:** generar y guardar un texto al azar para 2,500 alumnos no era práctico, y no se
  puede imprimir en una credencial. No es menos seguro: el prefecto ya podía teclear cualquier
  número, y al escanear ve el nombre del alumno para compararlo.
- **Otra opción:** firmar el número con una clave. La clave tendría que ir dentro de la app.

### M3. El escaneo funciona en el teléfono del prefecto

- **Qué se hizo:** escáner con la cámara (librería zxing-android-embedded), captura por No. de
  control con teclado propio y resultado verde o rojo, como en Figma. "Escanear" es una opción
  del menú ☰ del prefecto.
- **Por qué:** era el pendiente más importante de la app. La librería da la cámara como vista
  XML, igual que el resto de las pantallas.
- **Otra opción:** CameraX + ML Kit: más moderno, pero dos librerías más y mucho más código.

### M4. Las alertas se deciden en un archivo sin Firebase

- **Qué se hizo:** `datos/Escaneo.kt` decide la alerta con los datos ya leídos, y
  `EscaneoTest` la prueba (20 pruebas, sin teléfono).
- **Por qué:** es la regla más delicada de la app; así se revisa en segundos cada vez que cambie.

### M5. El registro se guarda siempre; la ubicación, solo sin alerta

- **Qué se hizo:** cada escaneo crea su registro aunque haya alerta. Si no hubo alerta, también
  cambia la ubicación del alumno y se avisa a sus padres dentro de la app; todo en un solo lote.
- **Por qué:** el registro es la bitácora de la puerta. Con alerta, el alumno puede no haber
  pasado, así que su ubicación no se toca.
