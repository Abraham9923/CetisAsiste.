# ADR-007: Menú ☰ en lugar de la barra inferior

**Estado:** Aceptada y aplicada (19 de octubre de 2026)
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste (pedido del usuario)
**Reemplaza a:** ADR-001 (completo) y el punto 4 de ADR-006

> Explica por qué las pantallas principales de cada rol ya no tienen barra inferior y ahora
> se abren desde el botón ☰ de la barra superior. Lo que se hizo, paso por paso:
> [BITACORA.md](../BITACORA.md) (entrada 19-31). Cada vista:
> [VISTAS_POR_ROL.md](../VISTAS_POR_ROL.md).

## Contexto

- El Inicio del **alumno** y del **padre** traía, desde el diseño de Figma, un botón ☰ (menú
  "hamburguesa") que **no hacía nada**: el menú no existe en Figma (manual técnico, pendientes).
  Prefecto y profesor ni siquiera tenían el botón.
- Cada rol se movía entre sus pantallas principales con una **barra inferior**:

| Rol | Pantallas de la barra |
|---|---|
| Alumno | Inicio · Boletín · Horario · Perfil (barra escrita dentro de cada XML) |
| Padre | Inicio · Anuncios · Calendario · Perfil |
| Prefecto | Inicio · Horarios · Calendario · Perfil |
| Profesor | Inicio · Boletín · Perfil |

- La barra ocupaba **100 dp** abajo (68 de alto + 32 de margen), y había que reservar ese
  espacio a mano en cada pantalla (`cetis_content_bottom_space`, ADR-001).
- El usuario pidió **darle uso al ☰**: que todas las opciones de la barra inferior se agrupen
  en un menú desplegable.

Respuestas del usuario (19 de octubre):

| Pregunta | Respuesta |
|---|---|
| ¿Qué pasa con la barra inferior? | **Se quita**; el menú la reemplaza |
| ¿Dónde va el ☰? | En **todas las pantallas que tenían barra**, en los 4 roles |

## Decisión

1. **Se quita la barra inferior** de los cuatro roles. Se borraron `padre_barra_inferior.xml`,
   `prefecto_barra_inferior.xml`, `profesor_barra_inferior.xml`, la barra escrita dentro de
   `alumno_inicio`, `alumno_boletin`, `alumno_horario` y `alumno_perfil`, sus estilos
   (`Cetis.Nav.*`), sus medidas y sus formas (`shape_nav`, `shape_nav_indicador`).
2. **Cada pantalla principal lleva el botón ☰** a la izquierda de su barra superior (estilo
   `Cetis.BarraSuperior.Menu`). Son 12 pantallas: las 4 del alumno, 3 del padre (Inicio,
   Anuncios, Perfil), 2 del prefecto (Inicio, Horarios), 2 del profesor (Inicio, Boletín) y 2
   comunes (Calendario y Perfil del personal).
3. **El menú es una vista XML común** (`comun_menu.xml` + una fila `comun_item_menu.xml` por
   opción). Arriba lleva el nombre y el rol de quien inició sesión; en medio, una opción por
   pantalla principal, con su ícono (la de la pantalla abierta va marcada); después,
   **"Notificaciones"** (pedido del usuario: la campana de Inicio se queda y las dos abren la
   misma pantalla); **hasta abajo, "Cerrar sesión"** (pedido del usuario), que pregunta antes
   con el mismo diálogo de Perfil.
4. **Lo pone el código encima de la pantalla**, igual que el diálogo de cerrar sesión: entra
   deslizándose desde la izquierda sobre una capa oscura. Se cierra al tocar fuera, al elegir
   una opción o con el botón "atrás" de Android.
5. **Las opciones de cada rol son una lista en su `*App.kt`** (`PESTANAS`: pantalla, ícono y
   texto). `VistasComunes.conectarMenu` hace el resto: es el mismo código para los 4 roles.
6. **Las pantallas del menú no se apilan** (igual que las pestañas): debajo de cualquiera
   queda solo Inicio, así que "atrás" siempre regresa a Inicio.
7. **El contenido termina arriba de la barra de gestos de Android.** Antes lo resolvía el
   espacio de la barra inferior; ahora `PantallaXml` le da a la columna de cada pantalla el
   alto exacto de la barra de gestos (la app dibuja de borde a borde).

## Opciones consideradas

### Opción A: dejar la barra y que el ☰ abra un menú con las mismas opciones

| Aspecto | Evaluación |
|---|---|
| Parecido con Figma | Alto (Figma tiene barra) |
| Repetición | Las mismas opciones en dos lugares |
| Espacio en pantalla | Se siguen perdiendo 100 dp abajo |

**En contra:** dos caminos para lo mismo confunden y hay que mantener los dos. El usuario
eligió quitar la barra.

### Opción B: quitar la barra; el ☰ abre el menú (la elegida)

| Aspecto | Evaluación |
|---|---|
| Espacio en pantalla | Se ganan 100 dp en cada pantalla principal |
| Repetición | Ninguna: un solo menú común, una lista de opciones por rol |
| Parecido con Figma | Menor: Figma tiene barra y no tiene menú |
| Facilidad de uso | Cambiar de pantalla pide un toque más (abrir el menú) |

**A favor:** lo que se repite se escribe una vez; agregar una opción es agregar un renglón a
`PESTANAS`. **En contra:** se aleja del Figma y cambiar de pantalla tarda un toque más.

### Opción C: menú con el componente de Compose (`ModalNavigationDrawer`)

| Aspecto | Evaluación |
|---|---|
| Trabajo | Parecido |
| Deslizar el dedo desde la orilla para abrirlo | Viene incluido |
| Consistencia | Mala: sería la única pieza de las pantallas principales hecha en Compose (ADR-006 pasó todo a XML) |

**En contra:** el equipo mantiene las pantallas en XML; una pieza en Compose sería una
excepción. El menú XML se abre con el ☰ (no deslizando desde la orilla).

## Consecuencias

- **Más fácil:** agregar o quitar una pantalla principal (un renglón en `PESTANAS`); las
  pantallas ya no reservan espacio a mano para la barra; la vista previa de Android Studio
  muestra la pantalla completa (ya no queda el hueco de la barra, que se veía en ADR-006).
- **Más difícil:** cambiar de pantalla principal pide dos toques (☰ y la opción).
- **Diferencia con Figma:** el diseño tiene barra inferior y no tiene menú. Si el diseño se
  actualiza, conviene dibujar ahí el menú (`comun_menu.xml` usa los colores, letras e íconos
  que ya existen en Figma).
- **ADR-001 queda reemplazado:** ya no hay barra inferior ni espacio reservado para ella.
- **Revisado en el teléfono con el alumno** (bitácora 19-33): el menú abre y cierra bien y el
  contenido termina arriba de la barra de gestos. Falta revisar los otros tres roles.

## Acciones

1. [x] Quitar las barras inferiores y sus estilos, medidas y formas.
2. [x] `comun_menu.xml`, `comun_item_menu.xml`, `shape_menu.xml` y el estilo
   `Cetis.BarraSuperior.Menu`.
3. [x] `VistasComunes.conectarMenu` y la lista `PESTANAS` en los 4 `*App.kt`; el alumno pasó a
   usar `PantallaXml` como los demás.
4. [x] Espacio de la barra de gestos en `PantallaXml`.
5. [x] "Cerrar sesión" hasta abajo del menú (con el diálogo de confirmación).
6. [x] "Notificaciones" en el menú, sin quitar la campana de Inicio; la campana del alumno
   ya abre sus notificaciones. El usuario lo probó en su teléfono (bitácora 19-34).
7. [x] Compila y `lint` sin errores; instalada en el teléfono.
8. [ ] Probar en el teléfono con las cuentas de los 4 roles (abrir, elegir opción, cerrar
   tocando fuera y con "atrás", cerrar sesión). **Alumno: probado y funciona** (bitácora
   19-33; ahí se corrigió el encabezado del menú). Faltan padre, prefecto y profesor.
9. [ ] Si el diseño se actualiza: agregar el menú a Figma y quitar ahí la barra inferior.
