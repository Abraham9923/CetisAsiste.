// CONFIGURACIÓN DE FIREBASE PARA LA WEB (proyecto appcetis-7c79d).
//
// Falta llenar `apiKey` y `appId` (una sola vez):
//   consola de Firebase → ⚙ Configuración del proyecto → "Tus apps" → Agregar app → Web (</>)
//   → nombre "CetisAsiste web" → copia aquí los valores de `apiKey` y `appId`.
// No son secretos: lo que protege los datos son las reglas de Firestore (y, más adelante,
// App Check). Ver web/README.md.
//
// Mientras `apiKey` esté vacío y la web se abra en esta computadora (localhost), se usa el
// emulador de Firebase con datos de prueba, sin tocar la base real.

export const configuracionFirebase = {
  apiKey: '',
  authDomain: 'appcetis-7c79d.firebaseapp.com',
  projectId: 'appcetis-7c79d',
  storageBucket: 'appcetis-7c79d.firebasestorage.app',
  messagingSenderId: '801213728411',
  appId: '',
};

/**
 * Dominio de los correos internos: la persona escribe su usuario y la web arma
 * "usuario@cetis61.mx", igual que la app Android (CetisRepositorio.DOMINIO_CORREO).
 */
export const DOMINIO_CORREO = 'cetis61.mx';

/** Proyecto que usa el emulador (los proyectos "demo-" nunca tocan la nube). */
export const PROYECTO_EMULADOR = 'demo-cetisasiste';
