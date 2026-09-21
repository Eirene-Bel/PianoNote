# PianoNote

[日本語](README.md) · [English](docs/readme/README.en.md) · [简体中文](docs/readme/README.zh-Hans.md) · [繁體中文](docs/readme/README.zh-Hant.md) · [한국어](docs/readme/README.ko.md) · [Español](docs/readme/README.es.md) · [Français](docs/readme/README.fr.md) · [Deutsch](docs/readme/README.de.md) · [Português](docs/readme/README.pt.md)

ピアノの練習記録、読譜・聴き取り、正答率と練習時間の振り返りができるアプリです。

- [Android APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16以降
- [紹介ページ](https://eirene-bel.github.io/PianoNote/)
- [サンプル体験](https://eirene-bel.github.io/PianoNote/demo/)

## 主な機能

- 練習内容・時間と、任意の振り返りを記録
- ト音記号・ヘ音記号のランダム出題、1〜3音の読譜と聴き取り
- 回答中だけの自動計測、音符上の音名と正誤表示
- 最近の正答率、練習カレンダー、JSONバックアップ

Android版の記録は端末内に保存します。公開サイトでは紹介とサンプル体験ができ、サンプルへの入力は保存されません。

## 表示言語

Android版・紹介ページ・サンプルは、次の9言語に対応しています。

| 言語 | 選択肢 |
|---|---|
| 日本語 | 日本語 |
| 英語 | English |
| 中国語（簡体字） | 简体中文 |
| 中国語（繁体字） | 繁體中文 |
| 韓国語 | 한국어 |
| スペイン語 | Español |
| フランス語 | Français |
| ドイツ語 | Deutsch |
| ポルトガル語 | Português |

初期設定では端末・ブラウザーの言語に合わせます。Androidは「設定」の言語選択、Webはページの言語選択から変更できます。未対応の言語では英語を表示します。

翻訳はアプリに同梱し、翻訳APIや追加料金は不要です。自分で入力した曲名・振り返りは翻訳せず、そのまま保持します。表示言語を変えても採点・保存済みの記録・JSONバックアップの形式は変わりません。

音名の表示は、日本語のドレミ、英語のC–B、ドイツ語のC–H、韓国語の도–시、その他の対応言語のDo–Siに切り替わります。

英語・ドイツ語では「FA」を F と A の2音として入力できます。ドレミ式の言語では「Fa」は1音です。

## 開発

| ディレクトリ | 内容 |
|---|---|
| `android/` | Androidアプリ |
| `site/` | 紹介ページとサンプル |
| `tests/`・`contracts/` | Webテストと共通テストデータ |
| `tools/` | ビルド・配布用スクリプト |

[ビルド手順](docs/BUILD.md) · [同梱ライセンス](THIRD_PARTY_NOTICES.md)

## 公開状況

このREADMEは本PRブランチの機能を説明しています。公開サイト・最新配布APKには、まだ変更が反映されていない場合があります。
