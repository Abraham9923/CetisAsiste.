# ADR-001: El contenido termina arriba de la barra inferior (vistas de alumno)

> Un **ADR** es el registro de una decisión: qué se decidió, por qué y qué otras
> opciones había. Sirve para que nadie tenga que adivinar después "¿por qué está así?".

**Estado:** Reemplazada por [ADR-007](ADR-007-menu-hamburguesa-en-lugar-de-barra-inferior.md)
(19 de octubre de 2026): ya no hay barra inferior; las pantallas principales se abren desde el
menú ☰. Se conserva como historia.
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste (diseño y Android)

## Contexto

- Cuatro vistas XML del alumno llevan barra inferior: Inicio, Horario, Perfil y
  Boletín. "Confirmar cerrar sesión" reutiliza Perfil, así que también la tiene.
- **En Figma** (frame "Horario - Alumno", capa `BottomNav` 79:1467) la barra mide
  348 × 68, queda a **6 dp** de cada lado y a **32 dp** del borde de abajo. Debajo de
  ella solo hay fondo: la lista de clases termina antes.
- **Cómo estaba:** la lista ocupaba toda la pantalla y la barra flotaba encima. Para
  que el último elemento no quedara tapado, la lista tenía 116 dp de relleno al final.
- **El problema:** al hacer scroll (y en la vista previa de Android Studio) las
  tarjetas pasaban por detrás de la barra y se asomaban en los 32 dp de abajo. Como
  la barra (6 dp de margen) es más ancha que las tarjetas (12 dp), lo que se asomaba
  además se veía desalineado.
- **Límites:** por ahora son solo vistas (sin datos ni navegación), deben ser fieles a
  Figma, no se agregan librerías y el equipo tiene poca experiencia en Android.

## Decisión

El bloque de contenido de cada pantalla deja libres **100 dp abajo** (68 de la barra
+ 32 de margen) con `android:layout_marginBottom="@dimen/cetis_content_bottom_space"`.
La lista o el scroll solo dejan **16 dp** de aire al final para que la última tarjeta
no toque la barra.

Además, la barra quedó a **12 dp** de cada lado (`@dimen/cetis_screen_margin`, el
mismo margen que el resto del contenido) en lugar de los 6 dp de Figma, para que
quede alineada con las tarjetas.

## Opciones consideradas

### Opción A: barra flotante con el contenido pasando por detrás (como estaba)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Baja (ya estaba hecho) |
| Parecido con Figma | Medio: solo coincide cuando la lista es corta |
| Mantenimiento | Un número (116) que hay que adivinar para que nada quede tapado |
| Facilidad para el equipo | Media: "por qué se ve una tarjeta debajo de la barra" confunde |

**A favor:** aprovecha más pantalla; es un estilo moderno.
**En contra:** las tarjetas se asoman debajo de la barra; se nota que la barra y las
tarjetas tienen distinto ancho; la vista previa no se parece a Figma.

### Opción B: el contenido termina arriba de la barra (la elegida)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Baja: un atributo por pantalla |
| Parecido con Figma | Alto: debajo de la barra solo queda fondo, como en el diseño |
| Mantenimiento | Un solo valor compartido (`cetis_content_bottom_space`) |
| Facilidad para el equipo | Alta: lo que ves en la vista previa es lo que sale |

**A favor:** nada se mete debajo de la barra; cambio mínimo (la barra conserva su
lugar en el archivo y sus `id`); las cuatro pantallas siguen teniendo la misma estructura.
**En contra:** la franja de 32 dp bajo la barra queda vacía, así que se ve un poco
menos contenido a la vez. El 100 está escrito a mano: Android no puede sumar medidas
dentro del XML, así que si cambia el alto o el margen de la barra hay que corregirlo.

### Opción C: reacomodar en columna (contenido arriba, barra al final)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Media: reestructurar los 4 archivos y mover la barra |
| Parecido con Figma | Alto |
| Mantenimiento | No hay número que sincronizar: el contenido termina solo donde empieza la barra |
| Facilidad para el equipo | Alta, una vez hecho |

**A favor:** es la forma más "limpia"; ya no depende de ninguna suma.
**En contra:** cambia mucho más código para llegar al mismo resultado visual, y el
margen de 32 dp seguiría siendo un valor aparte.

## Comparación

A y B difieren en **qué se ve debajo de la barra**: A muestra contenido y B muestra
fondo. Figma muestra fondo, y el equipo revisa contra Figma, así que se descartó A.
B y C se ven igual en pantalla; la diferencia es solo de mantenimiento. B toca una
línea por archivo y C reestructura los cuatro. Mientras estas vistas no tengan datos
ni navegación, el cambio pequeño es el más seguro. Si más adelante se modifica
seguido la barra, conviene pasar a C.

## Consecuencias

- **Más fácil:** la vista previa de Android Studio se ve como Figma. Una pantalla
  nueva con barra solo necesita copiar el mismo `layout_marginBottom`.
- **Más difícil:** si cambia el alto (68) o el margen (32) de la barra, hay que
  actualizar a mano `cetis_content_bottom_space` en `dimens_cetis.xml`.
- **Hay que revisar después:**
  - **Barra del sistema.** Cuando estas vistas se usen en una `Activity`, la app
    dibuja hasta el borde (`enableEdgeToEdge` en `Main.kt`). Con la barra de gestos
    caben bien los 32 dp, pero con la navegación de **3 botones** (unos 48 dp) la
    barra quedaría encima de los botones del sistema. Ahí habrá que sumar el alto real
    de la barra del sistema (los *insets*) a la barra y al margen del contenido, como
    ya lo hace la versión de padres en `BottomNav.kt` con `navigationBarsPadding()`.
  - **Ancho de la barra.** El equipo decidió usar 12 dp a los lados para alinear la
    barra con las tarjetas, así que aquí la app ya **no** coincide con Figma (6 dp).
    Conviene corregir la capa `BottomNav` en Figma (x = 12, ancho = 336) para que
    vuelvan a coincidir. La versión de padres (`BottomNav.kt`, en Compose) sigue con
    6 dp; si se quiere igual, se cambia ahí también.

## Acciones

1. [x] Cambiar `cetis_content_bottom_space` a 100 dp y explicar en su comentario de dónde sale.
2. [x] Usarlo como `layout_marginBottom` del bloque de contenido en `alumno_inicio`,
   `alumno_horario`, `alumno_perfil` y `alumno_boletin`; dejar 16 dp de aire al final de la lista.
3. [x] Nota en `VISTAS_ALUMNO.md`.
4. [ ] Revisarlo en la vista previa de Android Studio y en un teléfono.
5. [ ] Al conectar las vistas a una `Activity`: sumar el alto de la barra del sistema.
6. [x] Decidir si la barra va a 6 dp o a 12 dp de los lados: **12 dp**
   (`cetis_screen_margin`), ya aplicado en las cuatro pantallas.
7. [ ] Corregir la capa `BottomNav` en Figma a 12 dp de los lados.
