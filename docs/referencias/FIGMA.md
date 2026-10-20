# Figma de CetisAsiste

**Archivo:** <https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste>
(es el que el documento de roles llama "CETis 61 - Inicio (Android)").
Clave del archivo para las herramientas de Figma: `EHHsVy5Ag5tQZKaeKv7oCW`.

> **Corrección (19 de octubre de 2026):** antes este documento y otros decían que el archivo
> solo tenía las páginas "Alumno" y "Componentes". **Era un error:** al pedir la lista de
> páginas, la herramienta de Figma solo devolvió esas dos. Las páginas de padre, prefecto y
> profesor **sí existen**; se abren con los enlaces de abajo (los dio el usuario).
> **Consejo:** para revisar una página, ábrela por su `node-id`, no por la lista de páginas.

## Páginas

| Página | Enlace | `node-id` | Pantallas |
|---|---|---|---|
| **Padre** | [abrir](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=0-1) | `0:1` | 12 |
| **Alumno** | [abrir](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=75-507) | `75:507` | 8 |
| **Prefecto** | [abrir](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=75-508) | `75:508` | 11 + tablero de alertas |
| **Profesor** | [abrir](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=75-509) | `75:509` | 8 |
| **Componentes** | [abrir](https://www.figma.com/design/EHHsVy5Ag5tQZKaeKv7oCW/CetisAsiste?node-id=2-21) | `2:21` | Íconos y piezas |

Las pantallas web (Control Escolar y Enfermería) que menciona el documento de roles no se
revisaron todavía.

## Pantallas de cada página

### Padre (`0:1`)

| Pantalla | `node-id` |
|---|---|
| Bienvenida - CetisAsiste | `20:94` |
| Iniciar sesion - Android | `32:435` |
| Inicio - Android | `3:33` |
| Codigo QR - Android | `20:95` |
| Credencial digital - Android | `20:96` |
| Entradas y salidas - Android | `20:97` |
| Horario - Android (solo encabezado) | `20:98` |
| Anuncios - Android | `22:240` |
| Calendario - Android | `22:241` |
| Perfil - Android | `22:242` |
| Notificaciones - Android | `32:436` |
| Confirmar cerrar sesion - Padre | `124:1765` |

### Alumno (`75:507`)

Iniciar sesión (`124:1876`), Inicio (`79:1146`), Horario (`79:1147`), Perfil (`79:1148`),
Código QR (`79:1149`), Credencial digital (`79:1176`), Boletín (`79:1218`) y Confirmar cerrar
sesión (`124:1923`). Detalle: [VISTAS_ALUMNO.md](../VISTAS_ALUMNO.md).

### Prefecto (`75:508`)

| Pantalla | `node-id` | Qué tiene |
|---|---|---|
| Iniciar sesion - Prefecto | `125:1090` | Igual que el alumno, con "Usuario" |
| Inicio - Prefecto | `77:499` | Saludo ("Prefectura · CETis 61"), botón guinda grande "Escanear código QR", accesos a Horarios de grupos y Calendario, "Últimos escaneos" |
| Escaner - Prefecto | `77:500` | Selector Entrada / Salida, cámara, botón "Escribir No. de control" |
| Captura por noControl - Prefecto | `89:859` | Selector Entrada / Salida, No. de control tecleado, teclado numérico, "Registrar" |
| Resultado OK - Prefecto | `77:501` | "Entrada registrada", alumno y aviso al padre |
| Resultado alerta - Prefecto | `77:502` | Título de la alerta, alumno con su estatus |
| Alertas del escaner (variantes) | `78:775` | Las 6 alertas |
| Notificaciones - Prefecto | `77:503` | "Grupo 102 ya salió de clases" (hoy y anteriores) |
| Horarios de grupos - Prefecto | `77:504` | Chips de grupo (101, 102…), chips de día y la lista de clases, en una sola pantalla |
| Cambiar contrasena obligatorio | `77:505` | Aviso amarillo "Tu contraseña venció", 3 campos y "Guardar" |
| Perfil - Prefecto | `125:964` | Usuario, **Área**, Correo; "Tus datos los administra Control Escolar." |
| Confirmar cerrar sesion - Prefecto | `125:1136` | Perfil con el diálogo |

Barra inferior: **Inicio · Escanear · Horarios · Perfil**.

### Profesor (`75:509`)

| Pantalla | `node-id` | Qué tiene |
|---|---|---|
| Iniciar sesion - Profesor | `125:1613` | Igual que el alumno, con "Usuario" |
| Inicio - Profesor | `78:1083` | Saludo ("Docente · CETis 61"), botón guinda grande "Dar salida al grupo · Grupo 102 · última clase del turno", "Mis grupos", tarjeta Boletín |
| Lista de alumnos - Profesor | `78:1084` | Título "Grupo 102", buscador "Nombre o No. de control", renglones de alumno con su último estatus |
| Perfil del alumno - Profesor | `78:1085` | Iniciales, nombre, "Grupo · No. de control", estatus; 3 acciones (Notificar una falta, Levantar un reporte, Llamada de atención) e Historial |
| Cambiar estatus - Profesor | `78:1086` | Alumno, chips **Falta / Reporte / Llamada de atención**, "Motivo", "Guardar estatus" |
| Dar salida al grupo - Profesor | `78:1087` | Tarjeta del grupo con la última clase, nota, "Dar salida a todo el grupo", "Solo está disponible en la última clase del turno." |
| Perfil - Profesor | `125:1486` | Usuario, Área ("Docente"), Correo |
| Confirmar cerrar sesion - Profesor | `125:1659` | Perfil con el diálogo |

Barra inferior: **Inicio · Grupos · Boletín · Perfil**.

## Diferencias entre este Figma y las vistas XML de hoy (pendiente de ajustar)

Las vistas de prefecto y profesor se hicieron el 19 de octubre **antes de encontrar estas
páginas**, con el estilo del alumno ([ADR-006](../adr/ADR-006-vistas-xml-para-todos-los-roles.md)).
El usuario pidió por ahora **solo guardar esta información**; los cambios quedan para después.

**Prefecto:**

| Tema | Figma | App hoy |
|---|---|---|
| Pestañas | Inicio · Escanear · Horarios · Perfil | Inicio · Horarios · Calendario · Perfil |
| Inicio | Saludo, botón guinda "Escanear código QR", Horarios y Calendario, "Últimos escaneos" | Tarjeta de perfil, 4 accesos, "Salidas de hoy" |
| Salidas de grupo | En Notificaciones ("Grupo 102 ya salió de clases") | En el Inicio, en tiempo real |
| Horarios | Una pantalla: chips de grupo + chips de día + clases | Lista de grupos por turno → horario del grupo |
| Calendario | Se abre desde Inicio | Pestaña |
| Perfil | Agrega "Área" y dice "Tus datos los administra Control Escolar." | Usuario y Correo |
| Contraseña obligatoria | Aviso amarillo con candado "Tu contraseña venció"; campos "Nueva contraseña" y "Confirmar nueva contraseña"; botón "Guardar" | Aviso rojo de texto; "Contraseña nueva", "Repite la contraseña nueva"; "Guardar contraseña" |
| Escáner, captura, resultados y alertas | Diseñados | Pendientes a propósito |

**Profesor:**

| Tema | Figma | App hoy |
|---|---|---|
| Pestañas | Inicio · Grupos · Boletín · Perfil | Inicio · Boletín · Perfil |
| Inicio | Saludo, botón guinda "Dar salida al grupo" (grupo de la última clase de hoy), "Mis grupos" con el nombre del grupo en el círculo, tarjeta Boletín | Tarjeta de perfil y lista de grupos |
| Grupo | Buscador y lista de alumnos | Dar salida, Horario del grupo y lista de alumnos (sin buscador) |
| Estatus en la lista | Último estatus del alumno ("Sin incidencias", "Castigo", "Falta notificada", "Reporte"), con color amarillo para faltas y reportes | Estado de pago/salud ("Activo", "Inactivo por pago"…) |
| Perfil del alumno | 3 botones de acción e "Historial" con círculos amarillos | Datos, un botón "Cambiar estatus" y "Estatus registrados" |
| Cambiar estatus | **3 tipos: Falta, Reporte, Llamada de atención** (sin castigo); campo "Motivo" | 4 tipos (también castigo); campo "Descripción" |
| Dar salida | Tarjeta del grupo con la última clase; botón "Dar salida a todo el grupo"; solo en la última clase | Tarjeta de datos; botón "Dar salida"; disponible a cualquier hora |
| Perfil | Agrega "Área" ("Docente") | Usuario y Correo |

**Colores nuevos que usa este Figma:** advertencia `#B7740C` y su fondo `#FCF0D9` (estatus
amarillos, aviso de contraseña vencida). Todavía no están en `colors_cetis.xml`.

## Dónde se usa en el proyecto

- Vistas del alumno: [VISTAS_ALUMNO.md](../VISTAS_ALUMNO.md) (cada XML dice su nodo de Figma).
- Vistas de padre, prefecto y profesor: [VISTAS_POR_ROL.md](../VISTAS_POR_ROL.md).
- Colores, letras y espacios: [MANUAL_TECNICO.md](../MANUAL_TECNICO.md) §5.
