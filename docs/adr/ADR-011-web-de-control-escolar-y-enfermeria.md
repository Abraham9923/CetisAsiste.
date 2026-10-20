# ADR-011: Web de Control Escolar y Enfermería (primera versión)

**Estado:** Aceptada; primera versión hecha y probada con el emulador (20 de octubre de 2026).
**Falta** registrar la web en Firebase, publicarla y probarla con datos reales (ver
"Acciones").
**Fecha:** 20 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste (el usuario pidió "empieza a crear el apartado
web, son 2 usuarios")

Cómo usarla y probarla: [web/README.md](../../web/README.md). Lo que se hizo:
[BITACORA.md](../BITACORA.md) (entrada 20-03).

## Contexto

- El documento de roles pone a **Control Escolar** solo en web y a **Enfermería** "web, por
  confirmar". El usuario confirmó que la web es para esos **2 usuarios**.
- Mientras no había web, todo se daba de alta en la consola de Firebase (BASE_DE_DATOS §9) y
  nadie podía dar pases de salud ni activar o desactivar por pago o salud sin la consola.
- El equipo tiene **poca experiencia** y en la computadora de trabajo **no hay Node.js**
  (TRASPASO §7). El plan es **Spark** (sin Cloud Functions), pero **Hosting sí es gratis**.
- La base, las reglas por rol y los avisos (ADR-010) ya están listos: la web solo tiene que
  usarlos.

## Decisión

1. **Una página web sin herramientas de compilación:** HTML, CSS y JavaScript con módulos
   (`import`), y la librería oficial de Firebase cargada desde `gstatic.com` con la versión
   fija. Se abre y se publica tal cual; no hay `npm run build`.
2. **La misma organización que la app**, para que quien conoce una entienda la otra:
   - `js/datos.js` es el **único** archivo que habla con Firebase (como `CetisRepositorio`);
   - `js/avisos.js` repite las reglas de `Avisos.kt` (con pruebas en los dos lados);
   - `js/formatos.js` repite lo necesario de `Formatos.kt`;
   - una pantalla por archivo en `js/pantallas/`; `js/app.js` hace lo de `CetisApp.kt`.
3. **Misma sesión que la app:** usuario → `usuario@cetis61.mx` (o el correo completo); se lee
   `usuarios/{uid}`; solo entran `control_escolar` y `enfermeria` con `activo = true`; el
   cambio de contraseña es obligatorio si `debeCambiarContrasena` o si venció; la cuenta se
   escucha en vivo (si la desactivan, se cierra la sesión).
4. **Primera versión (lo que más se necesita en el día a día):**

   | Pantalla | Enfermería | Control Escolar |
   |---|---|---|
   | Alumnos: buscar (No. de control o apellidos) y por turno y grupo | ✔ | ✔ |
   | Perfil del alumno (ficha en vivo, estatus, historial de activaciones) | ✔ | ✔ |
   | Pase de salida por salud (+ aviso al padre) | ✔ | — |
   | Activar o desactivar por salud (+ historial + aviso) | ✔ | — |
   | Datos de salud (ver y editar) | ✔ | ✔ |
   | Activar o desactivar por pago (+ historial + aviso) | — | ✔ |
   | Nuevo estatus de cualquier tipo, con "vigente hasta" (+ aviso si el plantel lo pide) | — | ✔ |
   | Datos personales (solo ver) y entradas y salidas del semestre del alumno | — | ✔ |
   | Entradas y salidas del día, en vivo, con "solo alertas" | — | ✔ |
   | Avisos al padre (`configuracion/plantel.avisarAlPadre…`) | — | ✔ |
   | Notificaciones propias y cambiar contraseña | ✔ | ✔ |

5. **Todo lo que guarda va en un solo lote con sus avisos** (todo o nada) y **firmado** con
   el uid de la sesión (`asignadoPor`, `por`, `creadaPor`), como piden las reglas.
6. **Se publica con Firebase Hosting** desde la carpeta `web/`. `firebase/firebase.json` ya lo
   configura (y agrega encabezados de seguridad: sin incrustar en otras páginas, sin
   adivinar tipos de archivo).
7. **Se prueba con el emulador:** `firebase/emulador/datos-de-prueba.mjs` carga cuentas y
   datos "Prueba"; `web/pruebas/recorrido.mjs` recorre la web en Chromium y revisa lo guardado.
   La web usa el emulador sola si se abre en `localhost` sin configuración, o con `?emulador`.

## Opciones consideradas

### Cómo hacer la web

| Opción | A favor | En contra |
|---|---|---|
| **A. HTML + JavaScript sin compilar (la elegida)** | Nada que instalar para usarla o publicarla; se lee como texto; misma estructura que la app | Sin componentes ni tipos: hay que ser ordenados (un archivo por pantalla, un solo archivo de datos) |
| B. React o Vue con Vite | Lo más común en la industria; componentes | Hay que instalar Node y compilar en cada cambio; otra cosa que aprender |
| C. Kotlin en la web (Compose Multiplatform) | Se podría compartir código con la app | Muy nuevo, pesado y difícil para el equipo |
| D. Seguir con la consola de Firebase | Nada que programar | Peligroso (se puede borrar o romper cualquier cosa), lento y sin avisos a los padres |

### Dónde vive el código común (avisos, nombres de alertas)

| Opción | A favor | En contra |
|---|---|---|
| **A. Repetirlo en Kotlin y JavaScript, con pruebas en los dos (la elegida)** | Simple; cada lado se entiende solo | Si cambia una regla hay que cambiarla dos veces (los comentarios lo recuerdan) |
| B. Moverlo a Cloud Functions | Una sola versión, en el servidor | Necesita Blaze; es el camino a largo plazo (ARQUITECTURA.md B5) |

## Consecuencias

- **Más fácil:** Enfermería y Control Escolar trabajan sin la consola; cada acción avisa a los
  padres; Control Escolar ve las alertas del día y decide qué se avisa.
- **Más difícil:** hay dos lugares con la lógica de avisos (Kotlin y JavaScript).
- **Lo que la web todavía no puede hacer:** **crear cuentas** de Authentication (una página
  no puede crear cuentas de otras personas sin un servidor) ni importar alumnos en masa. Se
  propone un script de administración con el Admin SDK (ARQUITECTURA.md, E1), que funciona
  con Spark.
- **Seguridad:** igual que la app, la protección real son las reglas. Falta App Check
  (ARQUITECTURA.md B3) para que solo la web y la app oficiales puedan usar la base.

## Acciones

1. ☑ Web con las pantallas de la tabla, 10 pruebas de `avisos.js`/`formatos.js` y el
   recorrido de punta a punta (18 pasos) con el emulador.
2. [ ] **Registrar la web en Firebase** y llenar `apiKey` y `appId` en `web/js/config.js`
   (el usuario; web/README.md, paso 1).
3. [ ] Publicar las reglas y el índice (si no se ha hecho; PENDIENTES §1) y poner
   `roles/enfermeria.plataformaConfirmada = true` en la consola.
4. [ ] **Publicar** con `firebase deploy --only hosting` (web/README.md, paso 4).
5. [ ] Probar con las cuentas reales: la de Control Escolar del usuario y una de Enfermería
   (que hay que crear en la consola).
6. [ ] Siguientes versiones (PENDIENTES §8): alta de alumnos y cuentas (script E1), cambio
   de grupo o turno, prácticas, horarios, anuncios y eventos, editar datos personales,
   App Check.
