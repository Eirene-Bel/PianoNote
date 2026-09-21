# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

Une application pour consigner la pratique du piano, lire les partitions, entraîner l’oreille et suivre le taux de réussite et le temps de pratique.

- [APK Android](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 ou version ultérieure
- [Site web](https://eirene-bel.github.io/PianoNote/)
- [Essayer la démo](https://eirene-bel.github.io/PianoNote/demo/)

## Fonctionnalités

- Consignez le contenu et la durée de vos séances, avec des réflexions facultatives.
- Questions aléatoires en clé de sol et de fa : lecture et reconnaissance à l’oreille de 1 à 3 notes.
- Chronométrage automatique uniquement pendant la réponse ; noms des notes et résultats affichés au-dessus des notes.
- Taux de réussite récent, calendrier de pratique et sauvegardes JSON.

Les données Android sont enregistrées sur l’appareil. Le site public propose une présentation et une démo ; les données saisies dans la démo ne sont pas enregistrées.

## Langues d’affichage

L’application Android, le site et la démo prennent en charge les neuf langues indiquées ci-dessus.

Par défaut, la langue de l’appareil ou du navigateur est utilisée. Vous pouvez la modifier dans les paramètres de l’application Android ou dans le sélecteur du site. L’anglais est utilisé si la langue n’est pas prise en charge.

Les traductions sont intégrées et disponibles hors ligne, sans API de traduction ni frais supplémentaires. Les titres et réflexions saisis restent inchangés. Changer de langue ne modifie ni l’évaluation, ni les données enregistrées, ni le format des sauvegardes JSON.

Les notes sont affichées sous la forme ド–シ en japonais, C–B en anglais, C–H en allemand, 도–시 en coréen et Do–Si adapté aux autres langues. En anglais et en allemand, FA désigne deux notes, F + A ; dans les langues utilisant le solfège, Fa désigne une seule note.

## Développement

| Répertoire | Contenu |
|---|---|
| `android/` | Application Android |
| `site/` | Site et démo |
| `tests/ · contracts/` | Tests web et données de test communes |
| `tools/` | Scripts de compilation et de distribution |

[Instructions de compilation (japonais)](../BUILD.md) · [Mentions relatives aux composants tiers](../../THIRD_PARTY_NOTICES.md)

## État de publication

Cette documentation décrit la branche de cette PR. Le site public et le dernier APK publié peuvent ne pas encore inclure ces modifications.
