# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

Eine App zum Protokollieren des Klavierübens, Notenlesen, Gehörtraining und Auswerten von Trefferquote und Übungszeit.

- [Android-APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 oder neuer
- [Website](https://eirene-bel.github.io/PianoNote/)
- [Demo ausprobieren](https://eirene-bel.github.io/PianoNote/demo/)

## Funktionen

- Übungsinhalt und Dauer mit optionalen Reflexionen festhalten.
- Zufällige Aufgaben im Violin- und Bassschlüssel: 1–3 Noten lesen oder nach Gehör erkennen.
- Automatische Zeitmessung nur während der Beantwortung; Notennamen und Bewertung über den Noten.
- Aktuelle Trefferquote, Übungskalender und JSON-Sicherungen.

Android speichert die Aufzeichnungen auf dem Gerät. Die öffentliche Website bietet eine Einführung und eine Demo; Eingaben in der Demo werden nicht gespeichert.

## Anzeigesprachen

Die Android-App, die Website und die Demo unterstützen die oben aufgeführten neun Sprachen.

Standardmäßig wird die Geräte- oder Browsersprache verwendet. Die Sprache lässt sich in den Einstellungen der Android-App oder über die Sprachauswahl der Website ändern. Bei nicht unterstützten Sprachen wird Englisch verwendet.

Die Übersetzungen sind offline enthalten; eine Übersetzungs-API oder zusätzliche Gebühren sind nicht erforderlich. Selbst eingegebene Stücktitel und Reflexionen bleiben unverändert. Ein Sprachwechsel verändert weder die Bewertung noch gespeicherte Aufzeichnungen oder das JSON-Sicherungsformat.

Notennamen werden auf Japanisch als ド–シ, auf Englisch als C–B, auf Deutsch als C–H, auf Koreanisch als 도–시 und in den übrigen Sprachen als lokalisierte Do–Si-Namen angezeigt. Auf Englisch und Deutsch steht die Eingabe FA für zwei Noten, F + A; in Sprachen mit Solfège steht Fa für eine einzelne Note.

## Entwicklung

| Verzeichnis | Inhalt |
|---|---|
| `android/` | Android-App |
| `site/` | Website und Demo |
| `tests/ · contracts/` | Webtests und gemeinsame Testdaten |
| `tools/` | Skripte für Build und Verteilung |

[Build-Anleitung (Japanisch)](../BUILD.md) · [Hinweise zu Drittanbietern](../../THIRD_PARTY_NOTICES.md)

## Veröffentlichungsstand

Diese Dokumentation beschreibt den Branch dieses PRs. Die öffentliche Website und die zuletzt veröffentlichte APK enthalten diese Änderungen möglicherweise noch nicht.
