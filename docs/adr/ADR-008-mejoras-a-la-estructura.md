# ADR-008: Mejoras a la estructura del proyecto (propuesta)

**Estado:** Propuesta (19 de octubre de 2026). Nada de esto está hecho todavía: el usuario
decide qué se aplica y en qué orden. **Revisada el 20 de octubre** junto con todo el sistema
en [ARQUITECTURA.md](../ARQUITECTURA.md) §3.4 (qué avanzó y qué es ahora más urgente).
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste

> Revisión de cómo está organizado hoy el código de la app y qué conviene cambiar antes de que
> crezca (escaneo, web, miles de alumnos). Los pendientes que salen de aquí están en
> [PENDIENTES.md](../PENDIENTES.md) (sección 12).

## Contexto: cómo está hoy

```
app/src/main/java/com/example/cetisasiste/
├── Main.kt                  MainActivity (45 líneas) + ~530 líneas viejas, sin uso
├── AlumnoVistas.kt          llena las vistas del alumno   ← fuera de ui/alumno
├── AlumnoVariables.kt       datos que reciben esas vistas ← fuera de ui/alumno
├── datos/                   CetisRepositorio (único que habla con Firebase), Modelos, Formatos
└── ui/
    ├── CetisApp.kt          bienvenida → inicio de sesión → rol
    ├── comun/               PantallaXml, menú ☰, pantallas y variables comunes
    ├── alumno/ padres/ prefecto/ profesor/   un *App.kt (navegación + datos) y un *Vistas.kt por rol
    ├── padres/screens/      Bienvenida e Iniciar sesión (Compose) ← las usan TODOS los roles
    ├── padres/components/   botón, ícono y logo (Compose)
    └── theme/               tema de Compose (solo lo usan esas dos pantallas)
```

### Lo que está bien (conviene conservarlo)

| Qué | Por qué es bueno |
|---|---|
| **Un solo lugar habla con Firebase** (`CetisRepositorio`) | Si cambia la base o se pasa a Cloud Functions, se toca un archivo |
| **Las vistas no deciden nada** (`*Vistas.kt` solo ponen textos) | Las pantallas se pueden cambiar sin miedo a romper datos |
| **Un archivo por pantalla, con el prefijo del rol** | Fácil encontrar cualquier cosa |
| **Lo que se repite se escribe una vez** (`comun_*`, estilos, menú ☰) | Un cambio llega a todas las pantallas |
| **Reglas de seguridad por rol y datos por semestre** | La base ya aguanta a todo el plantel (ADR-002, ADR-005) |
| **Documentación y ADR** | Cualquiera entiende el porqué sin preguntar |

### Lo que conviene mejorar

| # | Problema | Qué pasa si no se arregla |
|---|---|---|
| 1 | Los `*App.kt` hacen **tres trabajos**: navegar, pedir datos y guardar (ProfesorApp tiene ~300 líneas). No hay `ViewModel` | Al girar el teléfono o volver a la app se pierden datos y se vuelven a pedir; la lógica no se puede probar sin teléfono; con el escaneo (mucha lógica) se volverá difícil de leer |
| 2 | La **navegación está copiada 4 veces** (pila, abrir, regresar, pestaña, botón atrás) | Un arreglo hay que hacerlo en 4 lugares |
| 3 | **Código muerto:** ~530 líneas del inicio de sesión viejo en `Main.kt`, `alumno_iniciar_sesion.xml` sin uso y 18 recursos sin uso (lint) | Confunde a quien llega; hace pensar que hay dos inicios de sesión |
| 4 | **Cosas en la carpeta equivocada:** Bienvenida e Iniciar sesión viven en `ui/padres` aunque son de todos; `AlumnoVistas.kt` y `AlumnoVariables.kt` están en la raíz | Cuesta encontrarlas; parece que son solo del padre |
| 5 | **Dos tecnologías de pantalla:** casi todo es XML, pero Bienvenida, Iniciar sesión y los mensajes de "Cargando…" son Compose (con su propio tema y colores repetidos en `ui/theme`) | Hay que saber las dos cosas; los colores están dos veces (XML y Kotlin) |
| 6 | **Casi no hay pruebas** (solo los ejemplos de Android Studio) | Un error en fechas, No. de control o contraseñas solo se ve en el teléfono |
| 7 | El paquete es **`com.example.cetisasiste`** | Google Play **no acepta** apps con `com.example`; cambiarlo después obliga a registrar la app otra vez en Firebase |
| 8 | **Versiones atrasadas** de librerías (lint: `activity-compose` 1.8 → 1.13, `lifecycle` 2.6 → 2.11, Firebase BoM 34 → 35) | Se acumulan los cambios y actualizar después cuesta más |
| 9 | `minSdk` 24, pero las esquinas redondeadas (`clipToOutline`) solo funcionan desde Android 12 | En teléfonos con Android 7 a 11 las fotos y tarjetas se ven con esquinas rectas |

## Decisión propuesta (en orden: primero lo barato y seguro)

1. **Borrar el código muerto** (punto 3): el inicio de sesión viejo de `Main.kt` (ya
   documentado en DECISIONES H1 y ADR-003), `alumno_iniciar_sesion.xml` y los recursos que
   lint marca sin uso. *Una tarde. Sin riesgo: nada los usa.*
2. **Ordenar carpetas** (punto 4): `ui/acceso/` para Bienvenida, Iniciar sesión y sus piezas;
   `AlumnoVistas.kt` y `AlumnoVariables.kt` a `ui/alumno/`. *Una tarde. Solo mueve archivos.*
3. **Una sola navegación** (punto 2): una clase pequeña `Navegacion<T>` en `ui/comun`
   (pila, abrir, regresar, ir a pantalla principal y botón atrás) que usen los 4 roles.
   *Medio día.*
4. **Pruebas de lo que no depende del teléfono** (punto 6): `Formatos` (fechas, horas, día
   escolar, nombres), la conversión No. de control → correo interno y las reglas de contraseña.
   *Un día. Atrapa errores antes de instalar.*
5. **Un `ViewModel` por rol** (punto 1), empezando por el profesor (el que guarda datos) y
   **antes de hacer el escaneo**: el `ViewModel` pide y guarda; el `*App.kt` solo dibuja y
   navega. Necesita la librería `lifecycle-viewmodel-compose` (sería la primera librería
   nueva desde DECISIONES H3). *Dos a tres días.*
6. **Cambiar el paquete** a uno propio, por ejemplo `mx.cetis61.cetisasiste` (punto 7),
   **antes de publicar**. Hay que registrar la app nueva en Firebase y bajar otro
   `google-services.json`. *Medio día, coordinado con la consola.*
7. **Actualizar librerías** (punto 8) una vez por semestre, probando en el teléfono.
8. **Decidir `minSdk`** (punto 9): subirlo a 31 (Android 12) si el plantel acepta dejar fuera
   teléfonos viejos, o aceptar las esquinas rectas en ellos. *Es una decisión del plantel:
   hay que saber qué teléfonos tienen los alumnos.*

**No se propone** (por ahora): partir la app en módulos, inyección de dependencias (Hilt) ni
pasar todo a Compose. Para el tamaño del proyecto y la experiencia del equipo, agregan más
complejidad de la que quitan.

## Opciones consideradas (para el punto 1, el más grande)

### Opción A: dejar los `*App.kt` como están

| Aspecto | Evaluación |
|---|---|
| Trabajo | Ninguno |
| Pruebas | Solo en el teléfono |
| Escaneo (mucha lógica nueva) | Haría al `PrefectoApp.kt` muy largo |

### Opción B: un `ViewModel` por rol (la recomendada)

| Aspecto | Evaluación |
|---|---|
| Trabajo | Medio (2 a 3 días) |
| Pruebas | La lógica se prueba sin teléfono |
| Girar el teléfono / volver a la app | No se pierden los datos |
| Lo que hay que aprender | `ViewModel` y `StateFlow` (lo estándar de Android) |

### Opción C: además, capas completas (casos de uso, interfaces, Hilt)

| Aspecto | Evaluación |
|---|---|
| Trabajo | Alto |
| Lo que hay que aprender | Mucho |
| Beneficio para este tamaño de app | Poco |

## Consecuencias

- **Más fácil:** encontrar cada cosa, agregar el escaneo sin que un archivo crezca sin
  control, probar sin teléfono y publicar en Google Play.
- **Más difícil:** el equipo tiene que aprender `ViewModel` (punto 5).
- **Hay que revisar después:** si la web de Control Escolar comparte reglas con la app, quizá
  convenga sacar `Formatos` y los modelos a un lugar común.

## Acciones

1. [ ] Que el usuario elija qué puntos se aplican.
2. [ ] Puntos 1 y 2 (limpieza y carpetas).
3. [ ] Punto 3 (una sola navegación).
4. [ ] Punto 4 (pruebas).
5. [ ] Punto 5 (`ViewModel`), antes del escaneo.
6. [ ] Punto 6 (paquete propio), antes de publicar.
7. [ ] Puntos 7 y 8 (librerías y `minSdk`).
