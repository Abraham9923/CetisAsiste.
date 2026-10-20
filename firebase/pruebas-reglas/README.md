# Pruebas de las reglas de seguridad

Revisan, **sin tocar la base real**, que `firebase/firestore.rules` deja hacer lo que debe y
niega lo que no: por ejemplo, que el prefecto pueda avisar al padre de un alumno pero no a
cualquier cuenta, o que Enfermería pueda desactivar por salud pero no por pago.

Todo pasa en el **emulador de Firestore** (una copia de Firestore que corre en tu computadora
y empieza vacía cada vez). Las pruebas están en [`reglas.test.js`](reglas.test.js): cada una
dice en español qué se espera ("puede…" o "no puede…").

## Qué necesitas (una sola vez)

- **Node.js** 20 o más nuevo (<https://nodejs.org>, versión LTS).
- **Java** 11 o más nuevo (el emulador está hecho en Java). Sirve el que trae Android Studio:
  `C:/Program Files/Android/Android Studio/jbr`.

En esta carpeta:

```bash
npm install
```

## Cómo correrlas

```bash
npm test
```

La primera vez descarga el emulador (unos 60 MB). Al final debe decir `# fail 0`. Si una
prueba falla, el mensaje dice cuál y qué regla la negó.

**Cada vez que cambies `firestore.rules`, corre las pruebas antes de publicar las reglas en la
consola.** Si agregas una regla nueva, agrega también su prueba.
