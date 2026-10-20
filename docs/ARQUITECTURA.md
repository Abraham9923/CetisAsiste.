# Arquitectura de CetisAsiste: cómo está y qué le mejoraría

**Fecha:** 20 de octubre de 2026
**Para quién:** el equipo y quien continúe el proyecto.
**Qué es:** una revisión de **todo el sistema** (app, base de datos, reglas, web y forma de
trabajar), hecha después de terminar los avisos (ADR-010) y antes de empezar la web
(ADR-011). Complementa a [ADR-008](adr/ADR-008-mejoras-a-la-estructura.md), que revisó solo
el código de la app; lo que dice ADR-008 sigue valiendo y aquí se marca qué cambió.

---

## 1. Cómo está hoy

```
                      ┌──────────────────────────── Firebase (proyecto appcetis-7c79d, plan Spark) ──┐
 App Android ──────►  │  Authentication (correo interno usuario@cetis61.mx)                           │
 (alumno, padre,      │  Cloud Firestore  ── reglas por rol (firestore.rules) deciden quién ve qué   │
  prefecto, profesor) │     usuarios · alumnos · periodos/{semestre}/registros · grupos · avisos…    │
                      │  (sin Cloud Functions: no hay servidor propio)                                │
 Web (nueva) ──────►  │  Hosting (para publicar la web)                                               │
 (Control Escolar,    └──────────────────────────────────────────────────────────────────────────────┘
  Enfermería)
```

- **No hay servidor propio.** Cada app escribe directo en Firestore y **las reglas de
  seguridad son la única defensa** (ADR-002). Lo que "decide" (qué alerta toca, a quién se
  avisa) corre en el teléfono o en el navegador de quien hace la acción.
- **App Android:** una sola actividad; cada rol tiene su `*App.kt` (navegación y datos) que
  muestra vistas XML dentro de Compose. `CetisRepositorio` es el único que habla con
  Firebase. La lógica que se puede probar está aparte y tiene pruebas (`Escaneo.kt`,
  `Avisos.kt`).
- **Datos por semestre** (ADR-005) y **un buzón de avisos por cuenta** (ADR-010).

## 2. Lo que está bien (conviene conservarlo)

| Qué | Por qué es bueno |
|---|---|
| Un solo lugar habla con Firebase (`CetisRepositorio`) | Si mañana se pasa a Cloud Functions, se toca un archivo |
| Lógica sin Firebase con pruebas (`Escaneo`, `Avisos`) | 29 pruebas que corren en segundos, sin teléfono |
| Reglas por rol, firmadas y **con pruebas** (`firebase/pruebas-reglas`) | Un error en las reglas se ve antes de publicarlas |
| Datos por semestre; registros de 20 en 20; consultas con índice | La base aguanta a todo el plantel sin leer de más |
| Un buzón de avisos por cuenta | El push futuro es **una** Cloud Function, sin tocar las apps |
| Documentación en español, ADR y bitácora | Cualquiera entiende el porqué sin preguntar |

## 3. Qué le mejoraría

Ordenado por **prioridad**: primero lo que puede fallar en la puerta del plantel o con datos
de los alumnos; al final, lo que solo hace el código más cómodo.

### 3.1 Antes de usarlo en la puerta (prioridad alta)

| # | Problema | Qué pasa | Qué haría | Esfuerzo |
|---|---|---|---|---|
| A1 | **El escaneo espera al servidor.** `registrarEscaneo` hace `lote.commit().await()`, y Firestore no termina esa tarea mientras no haya internet | Si la señal de la puerta falla, el escáner se queda en "Guardando…" y no deja escanear al siguiente alumno | Esperar como máximo unos segundos: si no hay respuesta, dar el registro por guardado "en el teléfono" (Firestore lo manda solo al volver la señal) y avisarlo en el resultado. Probar con el teléfono en modo avión | Medio día |
| A2 | **La hora la pone el teléfono.** `fechaHora` y `fecha` salen del reloj del teléfono del prefecto o del profesor | Un teléfono con la hora mal guarda entradas con hora falsa (y cambia "salida fuera de horario") | Guardar `fechaHora` con la hora del servidor (`FieldValue.serverTimestamp()`) y que las reglas exijan que sea la hora real (`request.time`). Dejar `fecha` como está, pero revisar la zona horaria (Cd. Juárez) | Medio día |
| A3 | **Un solo proyecto de Firebase** para pruebas y para el plantel | Las cuentas "Prueba" viven junto a los datos reales; un error al probar toca datos reales | Crear un segundo proyecto, `appcetis-pruebas` (gratis), o probar con el emulador (ya está configurado en `firebase/firebase.json`) | Medio día |
| A4 | **El plan Spark no alcanza** para 2,500 alumnos (lecturas, escrituras y espacio; ADR-005 y ADR-010) | A media mañana Firestore dejaría de aceptar escaneos ese día | Decidir el plan antes de cargar al plantel (PENDIENTES §1). Mientras, las reglas usan `get()`: **cada `get()` de las reglas también cuenta como lectura** (ver B2) | Decisión |

### 3.2 Seguridad y datos de los alumnos (prioridad alta)

| # | Problema | Qué haría | Esfuerzo |
|---|---|---|---|
| B1 | Se guardan datos de **menores** (CURP, domicilio, salud) y la app incluye **Google Analytics** (`firebase-analytics`), que junta datos del teléfono | Hacer un **aviso de privacidad** (LFPDPPP) y preguntar al plantel; quitar Analytics si no se usa, o desactivar la recolección de datos personales | Decisión + 1 hora |
| B2 | Las reglas leen `usuarios/{uid}` en **cada** consulta para saber el rol (cuesta lecturas y hace más lentas las reglas) | Guardar el rol en la cuenta de Authentication (**custom claims**) y que las reglas usen `request.auth.token.rol`. Se pone con el Admin SDK (ver E1), sin Blaze | 1 día |
| B3 | Cualquiera que saque la clave de `google-services.json` puede llamar a Firebase desde otro programa (las reglas lo frenan, pero puede gastar la cuota) | Activar **App Check** (gratis; Play Integrity en Android, reCAPTCHA en la web) y **restringir la clave** de la API a la app y al dominio de la web en Google Cloud | Medio día |
| B4 | **No hay respaldos.** Spark no tiene respaldos automáticos | Exportar la base cada semana (con Blaze: respaldos programados; sin Blaze: un script con el Admin SDK) | Medio día |
| B5 | La validación del escaneo es en el teléfono (ADR-002) | Cuando haya Blaze: Cloud Function `validarEscaneo` (PENDIENTES §7) | 2 a 3 días |

### 3.3 Cómo se cargan los datos (prioridad media-alta)

| # | Problema | Qué haría | Esfuerzo |
|---|---|---|---|
| E1 | Todo se da de alta **a mano en la consola** (cuentas, alumnos, grupos), y una página web **no puede crear cuentas de otros** sin un servidor | Un **script de administración** con el Admin SDK de Firebase (Node.js) que corre en la computadora de Control Escolar con una llave de servicio: importa alumnos desde Excel/CSV, crea sus cuentas, pone los custom claims (B2) y hace el cambio de semestre. **Funciona con Spark** | 2 a 3 días |
| E2 | Los nombres están copiados (`alumnoNombre`, `asignadoPorNombre`, `grupo`) y no se sincronizan | Aceptarlo para la bitácora (es lo que pasó ese día), pero que la web y el script cambien siempre **las dos copias** de la ficha (`alumnos` y `usuarios`) | Incluido en E1 |

### 3.4 Código de la app (prioridad media; ADR-008 sigue vigente)

| ADR-008 | Estado al 20 oct | Comentario |
|---|---|---|
| 1. Borrar código muerto (`Main.kt`, ~530 líneas) | **Pendiente** | Sigue confundiendo; es lo más barato |
| 2. Ordenar carpetas | Pendiente | |
| 3. Una sola navegación | Pendiente | Ahora hay 5 copias de la pila contando la web |
| 4. Pruebas | **Avanzó**: `EscaneoTest`, `AvisosTest` y pruebas de reglas | Faltan `Formatos` y No. de control → correo |
| 5. `ViewModel` por rol | **Pendiente y más urgente**: `PrefectoApp` ya escucha 4 cosas en vivo y guarda el estado del escaneo | Hacerlo antes de la siguiente pantalla grande del prefecto |
| 6. Paquete propio (`com.example` no se acepta en Google Play) | Pendiente | Antes de publicar |
| 7. Actualizar librerías / 8. `minSdk` | Pendientes | |

Nuevo:

| # | Problema | Qué haría |
|---|---|---|
| C1 | "Hoy" se calcula al abrir la pantalla: si la app queda abierta después de la medianoche, las listas de "hoy" siguen mostrando ayer | Volver a calcular la fecha al regresar a la app (`ON_RESUME`) |
| C2 | Cada cambio en vivo vuelve a dibujar las listas completas (`llenarLista` borra y crea los renglones) | Con 8 a 50 renglones está bien; si una lista crece (alertas de un día con muchas), pasar esa a `RecyclerView` |
| C3 | La lógica de avisos está **dos veces** (Kotlin y JavaScript) | Mantenerla mínima y con pruebas en los dos lados; a la larga, moverla a Cloud Functions |

### 3.5 Forma de trabajar (prioridad media, barato)

| # | Problema | Qué haría | Esfuerzo |
|---|---|---|---|
| D1 | Nada revisa los cambios antes de subirlos | **GitHub Actions**: en cada push, compilar, correr las pruebas de la app y las de las reglas. Así un error se ve en GitHub y no en el teléfono | 2 horas |
| D2 | En git están `.idea/` y `app/.idea/caches/` (18 archivos de Android Studio, uno de 2,458 líneas) | Sacarlos de git (`git rm --cached`) y agregarlos al `.gitignore` | 15 minutos |
| D3 | Los 20 `.docx` (1.7 MB) se guardan en git junto a sus `.md`; cada cambio guarda otra copia completa | Seguir generándolos, pero guardarlos solo al cerrar una versión (o publicarlos como *release*) | Decisión |
| D4 | El proyecto ya está en GitHub, pero los documentos dicen "no es un repositorio git" | Trabajar en ramas y con *pull requests*; ya se puede deshacer cualquier cambio | Hecho en el traspaso |

## 4. Plan que propongo

1. **Esta semana (barato y evita sustos):** A1 (escaneo sin señal), D2, D1, B3 y quitar
   Analytics si no se usa (B1).
2. **Antes de cargar al plantel:** decidir el plan (A4), proyecto de pruebas (A3), script de
   administración (E1) con custom claims (B2), respaldos (B4) y aviso de privacidad (B1).
3. **Antes de la siguiente pantalla grande del prefecto:** `ViewModel` (ADR-008 punto 5), con
   A2 y C1.
4. **Con Blaze:** validación en el servidor (B5) y avisos push (ADR-010, acción 8).
5. **Antes de publicar en Google Play:** paquete propio (ADR-008 punto 6).

**No propongo** (por ahora): reescribir la app en Compose, separar en módulos, inyección de
dependencias ni un framework para la web (ADR-011 explica por qué la web va sin
herramientas). Para el tamaño del proyecto y la experiencia del equipo, agregan más trabajo
del que ahorran.
