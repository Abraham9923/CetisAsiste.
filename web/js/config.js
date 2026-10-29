// CONFIGURACIÓN DE FIREBASE PARA LA WEB (proyecto appcetis-7c79d).
//
// `apiKey` y `appId` son de la app web "CetisAsiste web", registrada el 20-10-2026 en
//   consola de Firebase → ⚙ Configuración del proyecto → "Tus apps".
// No son secretos: lo que protege los datos son las reglas de Firestore (y, más adelante,
// App Check). Ver web/README.md.
//
// Como ya están llenos, la web usa la BASE REAL también en esta computadora (localhost).
// Para usar el emulador con datos de prueba, abre la dirección con ?emulador.

export const configuracionFirebase = {
  apiKey: 'AIzaSyDNFHwLa09wIR3IFEEczOasA3Zje4p5CjY',
  authDomain: 'appcetis-7c79d.firebaseapp.com',
  projectId: 'appcetis-7c79d',
  storageBucket: 'appcetis-7c79d.firebasestorage.app',
  messagingSenderId: '801213728411',
  appId: '1:801213728411:web:b95f7100603dd7b702e83e',
};

/**
 * Dominio de los correos internos: la persona escribe su usuario y la web arma
 * "usuario@cetis61.mx", igual que la app Android (CetisRepositorio.DOMINIO_CORREO).
 */
export const DOMINIO_CORREO = 'cetis61.mx';

/** Proyecto que usa el emulador (los proyectos "demo-" nunca tocan la nube). */
export const PROYECTO_EMULADOR = 'demo-cetisasiste';
