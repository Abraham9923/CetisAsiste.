# ADR-010: Notificaciones, alertas y avisos (padres y prefectos)

**Estado:** Aceptada y aplicada en la app (20 de octubre de 2026). **Falta** publicar las
reglas y crear un índice en la consola (ver "Acciones").
**Fecha:** 20 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste (el usuario pidió "encárgate de la gestión del
sistema de notificaciones, alertas y avisos para los padres y las alertas para los prefectos")
**Cambia a:** ADR-009, punto 6 (al padre solo se le avisaba si no había alerta)

Lo que se hizo, paso por paso: [BITACORA.md](../BITACORA.md) (entrada 20-01). Base de datos:
[BASE_DE_DATOS.md](../BASE_DE_DATOS.md) §4.1, §4.8, §6 y §7.

## Contexto

Hasta el 19 de octubre:

- El padre recibía un aviso **dentro de la app** solo cuando el prefecto registraba una
  entrada o salida **sin alerta**. No se enteraba de las faltas, los reportes o los castigos
  que ponía el profesor, ni de que su hijo quiso salir sin permiso o no pudo entrar.
- La pantalla de Notificaciones **se leía una vez**: un aviso nuevo no aparecía hasta salir y
  volver a entrar. La campana de Inicio **siempre** tenía el punto rojo, hubiera o no avisos.
- El prefecto veía cada alerta solo en el momento del escaneo. No había forma de ver las
  alertas del día ni los últimos escaneos (el Figma del prefecto sí tiene "Últimos escaneos").
- Las reglas dejaban que **cualquier** prefecto, profesor o enfermera escribiera avisos en el
  buzón de **cualquier** cuenta, con cualquier contenido.
- No hay Cloud Functions (plan Spark, ADR-002): los avisos los escribe la app de quien hace
  la acción, y no hay avisos **push** (con la app cerrada).

## Decisión

### 1. Un solo buzón por cuenta

Todos los avisos van a `usuarios/{uid}/notificaciones`, sin importar quién los mande (app del
prefecto, app del profesor o la web de Enfermería y Control Escolar). Así:

- la app solo lee un lugar, y
- cuando haya plan Blaze, **una sola** Cloud Function que escuche ese buzón mandará cada
  aviso también como push, sin cambiar las apps.

La lógica de **a quién** se avisa vive en `datos/Avisos.kt` (sin Firebase, con pruebas:
`AvisosTest`, 9 pruebas). La web repite las mismas reglas en `web/js/avisos.js`.

### 2. Qué se le avisa al padre

| Evento | Quién lo manda | Tipo (`tipo`) | ¿Siempre? |
|---|---|---|---|
| Entrada o salida sin alerta | Prefecto (escaneo) | `entrada` / `salida` | Sí |
| Alerta al escanear | Prefecto (escaneo) | `alerta` | Solo las de `avisarAlPadreAlertas` |
| Falta, reporte, llamada de atención, castigo | Profesor | `estatus` | Solo los de `avisarAlPadreEstatus` |
| Pase de salida por salud | Enfermería (web) | `estatus` | Sí |
| Activado o desactivado por salud | Enfermería (web) | `estado` | Sí |
| Activado o desactivado por pago | Control Escolar (web) | `estado` | Sí |

**El plantel decide qué alertas y qué estatus se avisan** con dos campos de
`configuracion/plantel` (los puede cambiar Control Escolar en la consola, y más adelante en
la web). Si los campos **no existen**, se usa lo de por omisión:

- `avisarAlPadreEstatus` → `["falta", "reporte", "llamada_atencion", "castigo"]` (todo lo que
  pone el profesor).
- `avisarAlPadreAlertas` → `["inactivo", "salida_fuera_de_horario"]`: lo que el padre
  necesita saber (no pudo entrar por pago o salud; quiso salir sin permiso). **No** se
  avisan "ya ingresó" ni "no ha ingresado", porque casi siempre son errores al escanear, ni
  "no dado de alta" (no hay a quién avisar).

Una lista vacía (`[]`) apaga esos avisos. El profesor **ve en su pantalla** si el estatus que
eligió se le avisará al padre ("Su padre o tutor recibirá un aviso…"), porque la descripción
que escribe llega tal cual.

### 3. La campana y la lista, en tiempo real

- La **campana** de Inicio (los cuatro roles de Android) tiene el punto rojo **solo** si hay
  avisos sin leer; el menú ☰ muestra cuántos ("3", "9+"). Se escucha una consulta pequeña:
  `leida == false`, hasta 10 documentos (`Avisos.MAXIMO_SIN_LEER`). Así cada cambio cuesta
  pocas lecturas.
- La pantalla **Notificaciones** escucha el buzón mientras está abierta: un aviso nuevo
  aparece solo, y al tocar "Marcar todas como leídas" la campana se apaga sola. El enlace solo
  sale si hay algo sin leer.
- Íconos nuevos por tipo: alerta (triángulo en rojo), estatus (bandera), estado (credencial),
  grupo que salió (bandera café).

### 4. Alertas para el prefecto

- **Inicio:** nueva sección **"Últimos escaneos"** (los 8 más recientes de hoy, en vivo, en
  rojo los que tuvieron alerta) y arriba un renglón **"Alertas de hoy"** con cuántas van.
- **Alertas de hoy:** pantalla nueva (también en el menú ☰) con todas las alertas del día, en
  vivo. Necesita un índice nuevo: `registros` (`fecha`, `alerta`, `fechaHora`).
- **Resultado del escaneo:** además del título, dice **por qué** saltó la alerta ("Control
  Escolar lo desactivó por pago", "Castigo: … (lo registró …)", "Su entrada ya está registrada
  (7:05 am)…") y si se le avisó al padre.
- **Notificaciones del prefecto:** arriba de sus avisos salen los grupos que ya salieron hoy
  ("Grupo 102 ya salió de clases"), como en el Figma. No se copian a su buzón: se leen de
  `salidasDeGrupo` (el profesor no puede saber quiénes son los prefectos).

### 5. Reglas más estrictas para el buzón

`usuarios/{uid}/notificaciones` (función `avisoValido` de `firebase/firestore.rules`):

- Un aviso nuevo debe traer **exactamente** los campos `tipo`, `titulo`, `mensaje`, `fecha`,
  `leida`, `alumnoId`, `creadaPor` y `venceEn`; `tipo` debe ser uno de los conocidos; el
  título, de 1 a 120 letras y el mensaje hasta 500; `leida` = false, y `creadaPor` = el uid
  de quien lo manda.
- **Prefecto y Enfermería** solo pueden avisar a los **padres del alumno** del aviso; el
  **profesor**, solo a los padres de un alumno **de sus grupos**; **Control Escolar**, a
  cualquier cuenta.
- De paso, los **estatus** (`asignadoPor`) y los **cambios de estado** (`por`) deben ir
  firmados con el uid de quien los pone.

Hay **21 pruebas automáticas** de las reglas con el emulador
([firebase/pruebas-reglas](../../firebase/pruebas-reglas/README.md)); 6 de ellas fallan con las
reglas anteriores y pasan con las nuevas.

### 6. Los avisos vencen

Cada aviso lleva `venceEn` = su fecha + 60 días (`Avisos.DIAS_DE_VIGENCIA`). No se borra solo
todavía: sirve para activar una política **TTL** de Firestore (borrado automático por fecha) o
para que la web o una Cloud Function borre los vencidos.

## Opciones consideradas

### Dónde guardar los avisos

| Opción | A favor | En contra |
|---|---|---|
| **A. Un buzón por cuenta, `usuarios/{uid}/notificaciones` (la elegida, ya existía)** | Cada quien marca lo suyo como leído; las reglas son simples; una Cloud Function lo convierte en push | Un aviso por padre: dos padres = dos documentos |
| B. Un buzón por alumno (`alumnos/{n}/avisos`) que leen sus padres | Un solo documento por evento | "Leído" sería el mismo para los dos padres; el alumno no tendría avisos propios |

### Qué se avisa al padre

| Opción | A favor | En contra |
|---|---|---|
| A. Fijo en el código | Simple | Cada cambio del plantel obliga a sacar otra versión de la app |
| **B. Lista en `configuracion/plantel` con valores por omisión (la elegida)** | El plantel lo cambia sin programar; funciona aunque el campo no exista | Los valores por omisión están dos veces (app y web) |

### Avisos push ahora

| Opción | A favor | En contra |
|---|---|---|
| A. Push desde el teléfono del prefecto | Sin Blaze | Necesita la clave del servidor dentro de la app: cualquiera podría sacarla y mandar avisos a todos. **Inseguro** |
| **B. Esperar a Cloud Functions (la elegida)** | Seguro; no cambia las apps | Mientras, el padre solo ve el aviso al abrir la app |

## Consecuencias

- **Más fácil:** el padre se entera de todo lo importante de su hijo; el prefecto ve las
  alertas del día; agregar un aviso nuevo es llamar a `Avisos.nuevo()` y escribirlo en el lote.
- **Más difícil:** el aviso al padre del profesor sale con la **descripción que escribe**: hay
  que pedirle al personal que la escriba pensando en que la leerá el padre.
- **Volumen (importante para el plan):** con 2,500 alumnos, cada día hay unas 5,000 entradas y
  salidas. Con un padre y medio por alumno, son unos **7,500 avisos al día**, que se suman a
  los 5,000 registros y a los cambios de ubicación: unas **17,500 escrituras diarias**, cerca
  del límite gratis de Spark (20,000). Y en un semestre se juntan cerca de un millón de
  avisos: sin borrar los vencidos, en menos de un año se llena el espacio gratis (1 GiB). **El
  plan Blaze ya era necesario para todo el plantel (ADR-005); esto lo confirma.**
- **Seguridad:** la validación sigue en los teléfonos (ADR-002). Las reglas nuevas impiden lo
  peor (avisos a quien no corresponde o con otro nombre), pero un profesor podría mandar un
  texto inapropiado a los padres de sus alumnos. Queda anotado quién lo mandó (`creadaPor`).
- **Hay que revisar después:** qué hace el prefecto ante cada alerta (PENDIENTES §3) y si el
  alumno también debe recibir los avisos de sus estatus.

## Acciones

1. ☑ Buzón único, `Avisos.kt`, campana y lista en vivo, avisos del profesor, alertas del
   prefecto, motivo de cada alerta (app Android).
2. ☑ Reglas del buzón más estrictas y 21 pruebas con el emulador.
3. [ ] **Publicar `firebase/firestore.rules` en la consola** (el usuario). **Importante:**
   instalar la app nueva en los teléfonos al mismo tiempo; la app anterior manda avisos sin
   `creadaPor` y las reglas nuevas los rechazan (el escaneo no se guardaría).
4. [ ] **Crear el índice** `registros`: `fecha` ↑, `alerta` ↑, `fechaHora` ↓ (consola →
   *Firestore* → *Indexes*, o con el enlace que da el error la primera vez). Sin él, la
   pantalla "Alertas de hoy" se ve vacía.
5. [ ] (Opcional) Agregar a `configuracion/plantel` los campos `avisarAlPadreEstatus` y
   `avisarAlPadreAlertas` si el plantel quiere algo distinto de lo de por omisión.
6. [ ] (Opcional) Activar una política TTL sobre `venceEn` del grupo de colecciones
   `notificaciones`, si la consola lo permite en el plan actual; y excluir `titulo` y
   `mensaje` de los índices automáticos para que ocupen menos.
7. [ ] Probar en los teléfonos: escanear con alerta y sin alerta, poner un estatus como
   profesor y revisar la campana y la lista del padre.
8. [ ] Cuando haya Blaze: Cloud Function que mande push con cada aviso nuevo del buzón
   (`tokensFcm` ya existe en `usuarios`).
