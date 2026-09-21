# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

피아노 연습 기록, 악보 읽기, 청음 훈련, 정답률과 연습 시간 확인을 위한 앱입니다.

- [Android APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 이상
- [소개 페이지](https://eirene-bel.github.io/PianoNote/)
- [데모 체험](https://eirene-bel.github.io/PianoNote/demo/)

## 주요 기능

- 연습 내용과 시간을 기록하고, 선택적으로 소감을 남길 수 있습니다.
- 높은음자리표와 낮은음자리표의 무작위 문제로 1–3개 음의 독보와 청음을 연습합니다.
- 답하는 동안만 자동으로 시간을 측정하고, 음표 위에 음이름과 정답 여부를 표시합니다.
- 최근 정답률, 연습 달력, JSON 백업을 제공합니다.

Android 기록은 기기에 저장됩니다. 공개 웹사이트에서는 소개와 데모를 제공하며, 데모에 입력한 내용은 저장되지 않습니다.

## 표시 언어

Android 앱, 소개 페이지, 데모는 위에 나열된 9개 언어를 지원합니다.

기본적으로 기기 또는 브라우저의 언어를 따릅니다. Android 앱 설정이나 웹페이지의 언어 선택 메뉴에서 변경할 수 있습니다. 지원하지 않는 언어는 영어로 표시됩니다.

번역은 오프라인으로 포함되어 번역 API나 추가 비용이 필요하지 않습니다. 직접 입력한 곡명과 소감은 그대로 유지됩니다. 언어를 변경해도 채점, 저장된 기록, JSON 백업 형식은 바뀌지 않습니다.

음이름은 일본어에서 ド–シ, 영어에서 C–B, 독일어에서 C–H, 한국어에서 도–시, 나머지 지원 언어에서 현지화된 Do–Si로 표시됩니다. 영어와 독일어에서 FA를 붙여 입력하면 F와 A 두 음으로 처리하며, 계이름을 사용하는 언어에서 Fa는 한 음입니다.

## 개발

| 디렉터리 | 내용 |
|---|---|
| `android/` | Android 앱 |
| `site/` | 소개 페이지와 데모 |
| `tests/ · contracts/` | 웹 테스트와 공통 테스트 데이터 |
| `tools/` | 빌드 및 배포 스크립트 |

[빌드 안내（일본어）](../BUILD.md) · [서드파티 고지](../../THIRD_PARTY_NOTICES.md)

## 배포 상태

이 문서는 현재 PR 브랜치의 기능을 설명합니다. 공개 웹사이트와 최신 배포 APK에는 아직 변경 사항이 반영되지 않았을 수 있습니다.
