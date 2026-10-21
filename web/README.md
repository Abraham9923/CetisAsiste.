# Web de CetisAsiste — Control Escolar y Enfermería

La parte web de CetisAsiste (ADR-011). Es **una sola página** hecha con HTML, CSS y
JavaScript, sin herramientas de compilación: se abre y se publica tal cual. Usa la **misma
base de datos y las mismas cuentas** que la app Android.

| Quién entra | Qué puede hacer |
|---|---|
| **Enfermería** | Ver alumnos por grupo o buscarlos · perfil del alumno · **dar pase de salida por salud** · **activar o desactivar por salud** · ver y editar datos de salud · sus notificaciones · cambiar contraseña |
| **Control Escolar** | Lo mismo para ver alumnos · **activar o desactivar por pago** · **nuevo estatus** (cualquier tipo) · datos personales · **entradas y salidas del día** (con alertas, en vivo) · **avisos al padre** (qué se le avisa) · notificaciones · cambiar contraseña |
| Alumno, padre, prefecto, profesor | No entran: ven "Esta cuenta se usa en la app CetisAsiste para Android" |

Cada acción **avisa a los padres** en su app (ADR-010): el pase de salud, los cambios por
pago o salud y los estatus que el plantel elija.

## Cómo está hecha

```
web/
├── index.html              La única página
├── css/estilos.css         Colores y letra de la app (Figma)
├── fuentes/ img/           Plus Jakarta Sans y el logo (copias de la app)
├── js/
│   ├── config.js           ← Datos del proyecto de Firebase (llenar apiKey y appId)
│   ├── firebase.js         Arranca Firebase (o el emulador)
│   ├── datos.js            ÚNICO archivo que habla con Firebase (como CetisRepositorio)
│   ├── avisos.js           A quién se avisa (igual que Avisos.kt de la app)
│   ├── formatos.js         Fechas, horas, nombres (como Formatos.kt)
│   ├── ui.js               Ayudantes para armar pantallas (sin datos)
│   ├── app.js              Sesión, menú según el rol y cambio de pantalla
│   └── pantallas/          Una pantalla por archivo
│       ├── acceso.js         Iniciar sesión y cambiar contraseña
│       ├── alumnos.js        Buscar y alumnos por grupo
│       ├── perfil.js         Perfil del alumno y sus acciones
│       ├── entradas.js       Entradas y salidas del día (Control Escolar)
│       ├── configuracion.js  Avisos al padre (Control Escolar)
│       └── notificaciones.js Avisos de la propia cuenta
└── pruebas/                Pruebas (no se publican)
```

La librería de Firebase se carga desde el sitio de Google (`gstatic.com`) con la versión
fija (12.19.0, en `firebase.js` y `datos.js`). Todo el texto de la base se pone como texto
(nunca como HTML), para que un dato raro no pueda ejecutar código.

## Ponerla a funcionar (una sola vez)

1. ~~**Registrar la web en Firebase**~~ **Hecho** (20 oct): se llama "CetisAsiste web" y su
   `apiKey` y `appId` ya están en [`js/config.js`](js/config.js).
2. **Publicar las reglas** (`firebase/firestore.rules`) y **crear el índice** de alertas, si
   no se ha hecho (PENDIENTES §1, punto 4). La web necesita las reglas nuevas.
3. **Confirmar Enfermería en la web:** en `roles/enfermeria`, `plataformaConfirmada` = `true`
   (ya está así en `firebase/roles.json`).
4. **Publicar** con Firebase Hosting (plan Spark: gratis). Necesitas Node.js:

   ```bash
   cd firebase/pruebas-reglas
   npm install
   npx firebase login
   npx firebase deploy --only hosting --project appcetis-7c79d --config ../firebase.json
   ```

   La dirección será `https://appcetis-7c79d.web.app`. `firebase/firebase.json` ya dice que
   publique la carpeta `web/` (sin `pruebas/`, `package.json` ni este README).

> **Cuentas:** la web **no crea cuentas** (una página no puede crear cuentas de otros sin un
> servidor). Siguen dándose de alta en la consola (BASE_DE_DATOS §9) hasta tener el script
> de administración (ARQUITECTURA.md, punto E1).

## Abrirla en esta computadora sin Node.js (base real)

Windows ya trae lo necesario. En PowerShell, desde la raíz del proyecto:

```bash
powershell -ExecutionPolicy Bypass -File docs\herramientas\servidor_web.ps1
```

Abre <http://localhost:5000> y entra con tu cuenta de Control Escolar o Enfermería. Usa la
**base real**: lo que hagas ahí lo ven los padres en su app. Para apagarlo: `Ctrl+C`.

El script ([`servidor_web.ps1`](../docs/herramientas/servidor_web.ps1)) solo entrega los
archivos de esta carpeta; no guarda nada. Si el puerto 5000 está ocupado, agrega
`-Puerto 5050`.

## Probar en tu computadora (sin tocar la base real)

Con el **emulador de Firebase** (necesitas Node.js y Java; sirve el de Android Studio):

```bash
# Terminal 1: enciende los emuladores (Authentication, Firestore y la web)
cd firebase/pruebas-reglas
npm install
npx firebase emulators:start --project demo-cetisasiste --config ../firebase.json

# Terminal 2: carga los datos de prueba
cd firebase
node emulador/datos-de-prueba.mjs
```

Abre <http://127.0.0.1:5000/?emulador> (como `config.js` ya está lleno, sin `?emulador` la
web usaría la base real). Arriba sale una franja amarilla que dice que es el emulador.

Cuentas de prueba (contraseña `prueba1234`): `control.prueba`, `enfermeria.prueba`,
`cambio.prueba` (debe cambiar su contraseña al entrar) y `padre.prueba` (no puede entrar).

## Pruebas

En la carpeta `web`:

| Comando | Qué revisa | Necesita |
|---|---|---|
| `npm test` | `avisos.js` y `formatos.js` (10 pruebas) | Node.js |
| `npm install` y luego `npm run recorrido` | **Toda la web de punta a punta** (18 pasos): entra con cada cuenta, da un pase de salud, desactiva por salud y por pago, pone un estatus, revisa entradas y salidas, avisos al padre y notificaciones, y comprueba en el emulador lo guardado | Los emuladores encendidos con los datos de prueba |

Las capturas del recorrido quedan en `web/pruebas/capturas/`.

## Lo que falta (siguientes versiones)

Ver [PENDIENTES.md](../docs/PENDIENTES.md) §8: alta de alumnos y cuentas (con el script de
administración), cambio de grupo o turno, altas para prácticas, horarios, publicar anuncios
y eventos, editar datos personales, y App Check.
