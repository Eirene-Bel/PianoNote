# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

A piano practice app for logging practice, reading music, ear training, and reviewing accuracy and practice time.

- [Android APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 or later
- [Website](https://eirene-bel.github.io/PianoNote/)
- [Try the demo](https://eirene-bel.github.io/PianoNote/demo/)

## Features

- Record practice content and duration, with optional reflections.
- Random treble/bass clef questions: read or identify 1–3 notes by ear.
- Automatic timing only while answering; note names and correctness displayed above the notes.
- Recent accuracy, a practice calendar, and JSON backups.

Android records are stored on your device. The public website provides an introduction and demo; demo input is not saved.

## Display languages

Android, the website, and the demo support the nine languages listed above.

By default, the app follows the device or browser language. Change it in the Android app’s settings or the website language selector. Unsupported languages fall back to English.

Translations are bundled offline: no translation API or additional fees. Your piece titles and reflections stay as entered. Changing languages does not alter scoring, saved records, or the JSON backup format.

Note labels use Japanese solfège in Japanese, C–B in English, C–H in German, 도–시 in Korean, and localized Do–Si in the other supported languages. In English and German, type adjacent letter names such as FA for F + A; in solfège languages, Fa means one note.

## Development

| Directory | Contents |
|---|---|
| `android/` | Android app |
| `site/` | Website and demo |
| `tests/ · contracts/` | Web tests and shared fixtures |
| `tools/` | Build and distribution scripts |

[Build instructions (Japanese)](../BUILD.md) · [Third-party notices](../../THIRD_PARTY_NOTICES.md)

## Release status

These documents describe this PR branch. The public website and latest release APK may not yet include these changes.
