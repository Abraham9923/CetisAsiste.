// Arranca Firebase (Authentication y Firestore) para la web.
//
// La librería se carga directo desde el sitio de Google (gstatic), con la versión fija:
// no hay que instalar nada. Para actualizarla, cambia VERSION en los tres archivos de abajo
// (datos.js también la importa).
//
// Si se abre en esta computadora (localhost) y la configuración no está llena, o si la
// dirección trae ?emulador, se usa el emulador de Firebase (datos de prueba, ver web/README.md).

import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js';
import { connectAuthEmulator, getAuth } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import { connectFirestoreEmulator, getFirestore } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { PROYECTO_EMULADOR, configuracionFirebase } from './config.js';

const enEstaComputadora = ['localhost', '127.0.0.1'].includes(location.hostname);
const configurada = configuracionFirebase.apiKey !== '';

/** true si la web habla con el emulador (datos de prueba) y no con la base real. */
export const usaEmulador =
  new URLSearchParams(location.search).has('emulador') || (enEstaComputadora && !configurada);

/** false si falta llenar web/js/config.js y no se puede usar el emulador. */
export const listaParaUsarse = configurada || usaEmulador;

const app = initializeApp(
  usaEmulador ? { apiKey: 'emulador', projectId: PROYECTO_EMULADOR, authDomain: `${PROYECTO_EMULADOR}.firebaseapp.com` } : configuracionFirebase,
);

export const auth = getAuth(app);
export const db = getFirestore(app);

if (usaEmulador) {
  // Los mismos puertos que firebase/firebase.json.
  connectAuthEmulator(auth, `http://${location.hostname}:9099`, { disableWarnings: true });
  connectFirestoreEmulator(db, location.hostname, 8080);
}
