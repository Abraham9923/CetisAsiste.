# ADR-002: Las apps escriben directo en Firestore, protegidas por reglas (sin Cloud Functions por ahora)

**Estado:** Aceptada (aplicada el 19 de octubre de 2026)
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste

## Contexto

- El documento "Pantallas por rol" propone que **todo lo que cambia un registro o un
  estatus pase por Cloud Functions**: la app del prefecto manda el No. de control y una función en
  el servidor revisa estatus, guarda el registro y avisa al padre.
- El proyecto de Firebase (`appcetis-7c79d`) está en el plan **Spark** (gratuito).
  **Cloud Functions solo funciona en el plan Blaze**, que pide una tarjeta y cobra por uso.
- Antes de este cambio, la base estaba en "modo prueba": **cualquiera** podía leer y escribir
  todo hasta el 4 de noviembre de 2026, incluidas contraseñas guardadas tal cual.
- Hacía falta proteger la base ya, sin esperar a la decisión de pago.

## Decisión

Publicar reglas de seguridad por **rol** (`firebase/firestore.rules`) que dejan a cada app
escribir **solo lo que a ese rol le toca**, y diseñar los datos para que el cambio a Cloud
Functions sea después sin tocar las colecciones.

## Opciones consideradas

### Opción A: dejar la base en modo prueba hasta tener Cloud Functions

| Aspecto | Evaluación |
|---|---|
| Dificultad | Ninguna |
| Costo | Gratis |
| Seguridad | **Nula**: cualquiera con el id del proyecto lee y cambia todo |
| Duración | Se cerraba sola el 4 de noviembre y la app dejaba de funcionar |

### Opción B: reglas por rol, apps escriben directo (la elegida)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Media: un archivo de reglas de unas 170 líneas |
| Costo | Gratis (plan Spark) |
| Seguridad | Buena: sin sesión no se ve nada; cada rol solo escribe lo suyo; la bitácora no se edita |
| Límite | Las reglas no pueden **calcular** alertas; si alguien modifica la app del prefecto podría guardar un registro sin alerta |

### Opción C: pasar ya a Blaze y hacer Cloud Functions

| Aspecto | Evaluación |
|---|---|
| Dificultad | Alta: además de las reglas, programar y publicar funciones (Node.js) |
| Costo | Requiere tarjeta; con el uso de un plantel probablemente quede en la capa gratuita, pero hay riesgo de cobro |
| Seguridad | La mejor: el servidor decide la alerta |
| Bloqueo | Es una decisión de dinero del plantel, no técnica |

## Comparación

A quedaba descartada por seguridad. Entre B y C, B protege lo importante (quién ve qué) hoy y
sin costo; C agrega la validación del escaneo en el servidor (el escaneo se hizo después en el teléfono, ADR-009), porque las
pantallas del prefecto no están programadas. Cuando se programen, se decide si pagar Blaze.

## Consecuencias

- **Más fácil:** la base ya está protegida; la app funciona sin servidor propio.
- **Más difícil:** las reglas son largas y hay que mantener sincronizada la copia de
  `firebase/firestore.rules` con la consola.
- **Riesgo aceptado:** mientras no haya Cloud Functions, el cálculo de alertas lo haría la
  app del prefecto. Las reglas sí obligan a que el registro diga quién lo hizo
  (`registradoPor` = su uid) y nadie lo puede borrar.
- **Hay que revisar después:** al pasar a Blaze, quitar el permiso `create` de `registros`
  al prefecto y dárselo solo a la función; igual con `notificaciones`.

## Acciones

1. [x] Escribir `firebase/firestore.rules` y publicarlas en la consola.
2. [x] Comprobar que sin sesión se recibe "403 Permission denied".
3. [ ] Decidir con el plantel si se pasa al plan Blaze.
4. [ ] Si se pasa: función `validarEscaneo` y avisos con Cloud Messaging.
