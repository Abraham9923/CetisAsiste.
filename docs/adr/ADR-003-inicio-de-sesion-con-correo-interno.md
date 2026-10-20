# ADR-003: Inicio de sesión con No. de control o usuario convertido en correo interno

**Estado:** Aceptada (aplicada el 19 de octubre de 2026)
**Fecha:** 19 de octubre de 2026
**Quién decide:** el equipo de CetisAsiste

## Contexto

- El inicio de sesión anterior (`Main.kt`, `MainAppContainer`) **guardaba la contraseña tal
  cual** en `usuarios` y la comparaba desde el teléfono. Además permitía **registrarse desde la
  app**.
- El pedido es que la app muestre la información **según quién inicie sesión** y que **las
  cuentas las dé de alta Control Escolar**, no cada persona.
- Los alumnos conocen su **No. de control**, no un correo. El documento "Pantallas por rol" pide
  "No. de control o usuario y contraseña".
- Firebase Authentication con contraseña funciona con **correo + contraseña**.

## Decisión

- La persona escribe su **No. de control** (alumno) o su **usuario** (padre y personal).
- La app arma un **correo interno**: `noControl@cetis61.mx` o `usuario@cetis61.mx`
  (`CetisRepositorio.correoDeAcceso`) y lo usa con Firebase Authentication.
- Después lee `usuarios/{uid}`: **sin ese documento o con `activo = false` no entra**, aunque
  la contraseña sea correcta.
- La pantalla de registro desaparece del flujo; las cuentas las crea Control Escolar.

## Opciones consideradas

### Opción A: correo interno armado con el No. de control (la elegida)

| Aspecto | Evaluación |
|---|---|
| Dificultad | Baja |
| Seguridad | Buena: contraseñas cifradas por Google; nada de contraseñas en Firestore |
| Para el alumno | Escribe lo que ya conoce: su No. de control |
| Límite | "Olvidé mi contraseña" por correo no funciona (el correo no existe) |

### Opción B: correo institucional real de cada persona

| Aspecto | Evaluación |
|---|---|
| Dificultad | Baja |
| Para el alumno | Tiene que recordar un correo además del No. de control |
| Ventaja | Recuperar contraseña por correo sí funcionaría |
| Problema | No todos los padres tienen correo institucional; habría que buscar el correo a partir del No. de control **antes** de iniciar sesión, lo que expone datos |

### Opción C: seguir con el inicio de sesión de `Main.kt`

Descartada: contraseñas visibles para cualquiera que lea la colección y registro libre.

## Consecuencias

- **Más fácil:** un solo inicio de sesión sirve para los seis roles; el rol decide qué se ve.
- **Más difícil:** recuperar la contraseña necesita a Control Escolar (o la web o una
  pantalla futura). La receta temporal está en `BASE_DE_DATOS.md`, sección 9.8.
- **Importante:** el dominio `cetis61.mx` está escrito en `CetisRepositorio.DOMINIO_CORREO` y
  en `configuracion/plantel.dominioCorreo`. Si cambia, hay que cambiarlo en ambos y en las
  cuentas.

## Acciones

1. [x] Activar el método Correo/Contraseña en Authentication.
2. [x] `CetisRepositorio.iniciarSesion` y mensajes de error en español.
3. [x] `MainAppContainer` deja de usarse (se conserva el código como referencia).
4. [x] Pantalla "Cambiar contraseña" (usa `debeCambiarContrasena` y `contrasenaVence`).
   Hecha el 19 de octubre con las vistas comunes (ADR-006).
5. [ ] Decidir cómo recupera la contraseña quien la olvida.
