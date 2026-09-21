# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

Una aplicación para registrar la práctica de piano, leer partituras, entrenar el oído y revisar los aciertos y el tiempo de práctica.

- [APK de Android](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 o posterior
- [Sitio web](https://eirene-bel.github.io/PianoNote/)
- [Probar la demo](https://eirene-bel.github.io/PianoNote/demo/)

## Funciones principales

- Registra el contenido y la duración de la práctica, con reflexiones opcionales.
- Preguntas aleatorias en clave de sol y de fa: lectura y reconocimiento auditivo de 1–3 notas.
- Medición automática del tiempo solo mientras respondes; nombres de notas y resultados sobre las notas.
- Porcentaje de aciertos reciente, calendario de práctica y copias de seguridad JSON.

Los registros de Android se guardan en el dispositivo. El sitio público ofrece una presentación y una demo; los datos introducidos en la demo no se guardan.

## Idiomas de la interfaz

Android, el sitio web y la demo admiten los nueve idiomas indicados arriba.

Por defecto se utiliza el idioma del dispositivo o navegador. Puedes cambiarlo en los ajustes de la aplicación Android o en el selector de idioma del sitio. Si el idioma no está disponible, se utiliza el inglés.

Las traducciones están incluidas y funcionan sin conexión, sin API de traducción ni costes adicionales. Los títulos de las piezas y las reflexiones se mantienen tal como los escribiste. Cambiar de idioma no modifica la evaluación, los registros guardados ni el formato de las copias JSON.

Las notas se muestran como ド–シ en japonés, C–B en inglés, C–H en alemán, 도–시 en coreano y Do–Si adaptado a los demás idiomas. En inglés y alemán, FA representa dos notas, F + A; en los idiomas con solfeo, Fa representa una sola nota.

## Desarrollo

| Directorio | Contenido |
|---|---|
| `android/` | Aplicación Android |
| `site/` | Sitio web y demo |
| `tests/ · contracts/` | Pruebas web y datos de prueba compartidos |
| `tools/` | Scripts de compilación y distribución |

[Instrucciones de compilación (japonés)](../BUILD.md) · [Avisos de terceros](../../THIRD_PARTY_NOTICES.md)

## Estado de publicación

Esta documentación describe la rama de este PR. Es posible que el sitio público y el último APK publicado aún no incluyan estos cambios.
