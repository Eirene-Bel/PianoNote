# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

用於記錄鋼琴練習、讀譜、聽音訓練，以及回顧答對率和練習時間的應用程式。

- [Android APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 以上
- [介紹頁面](https://eirene-bel.github.io/PianoNote/)
- [體驗範例](https://eirene-bel.github.io/PianoNote/demo/)

## 主要功能

- 記錄練習內容、時間及選填的練習心得。
- 隨機產生高音譜號或低音譜號題目，進行 1–3 個音的讀譜及聽音訓練。
- 僅在作答時自動計時，並在音符上方顯示音名及對錯。
- 查看近期答對率、練習日曆，並使用 JSON 備份。

Android 版的記錄儲存在裝置內。公開網站提供介紹與範例體驗；範例中輸入的內容不會儲存。

## 顯示語言

Android 版、介紹頁面和範例支援上方列出的九種語言。

預設依照裝置或瀏覽器的語言。可在 Android 應用程式的設定中或透過網頁語言選單變更。不支援的語言會改用英語。

翻譯隨應用程式離線提供，不需翻譯 API 或額外費用。自行輸入的曲名與心得維持原樣。切換語言不會改變評分、已儲存的記錄或 JSON 備份格式。

日語顯示ド–シ，英語顯示 C–B，德語顯示 C–H，韓語顯示 도–시，其他支援的語言顯示在地化的 Do–Si。使用英語或德語時，連續輸入 FA 表示 F 和 A 兩個音；使用唱名的語言中，Fa 表示一個音。

## 開發

| 目錄 | 內容 |
|---|---|
| `android/` | Android 應用程式 |
| `site/` | 介紹頁面與範例 |
| `tests/ · contracts/` | Web 測試與共用測試資料 |
| `tools/` | 建置及發布指令碼 |

[建置說明（日語）](../BUILD.md) · [第三方聲明](../../THIRD_PARTY_NOTICES.md)

## 發布狀態

本文件描述此 PR 分支的功能。公開網站及最新發布的 APK 可能尚未包含這些變更。
