# Android translations

These complete, bundled catalogs cover Japanese, English, Simplified and Traditional Chinese, Korean, Spanish, French, German and Portuguese. No translation service or network access is used.

Edit the JSON catalog for each language, preserving message keys and numbered `{0}` placeholders. Then run `python3 android/tools/generate_catalog.py` from the repository root. Run the same command with `--check` to verify key coverage, placeholder parity and generated Kotlin freshness. Generated Kotlin is committed so normal Android builds require no Python.

`I18n.language` is resolved from the activity configuration. Android's native per-app language selection stores a manual choice; an empty locale list follows the device. Unsupported languages use English. Traditional Chinese includes `zh-Hant`, Taiwan, Hong Kong and Macao, with explicit script taking precedence over region.

Only presentation is translated. Database values, backup format, settings identifiers, user-entered practice notes and canonical Japanese musical note tokens retain their original representation. Note button labels, score feedback and accessibility labels use local note naming; the input parser accepts the displayed aliases and converts them to canonical Japanese tokens before grading. French/Spanish/Portuguese/Chinese use solfège (with French and Portuguese accents), English uses C–B, German C–H and Korean 도–시. ASCII and accented solfège input are both accepted.

Transient ViewModel messages retain translation provenance through weak identity references. Locale recreation re-renders these messages and preserves retry state; raw user strings are never matched against the catalog or translated. Restored quiz feedback is derived from its grading state.
