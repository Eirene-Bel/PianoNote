# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

用于记录钢琴练习、识谱、听音训练以及回顾正确率和练习时间的应用。

- [Android APK](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 及以上
- [介绍页面](https://eirene-bel.github.io/PianoNote/)
- [体验示例](https://eirene-bel.github.io/PianoNote/demo/)

## 主要功能

- 记录练习内容、时长和可选的练习心得。
- 随机生成高音谱号或低音谱号题目，进行 1–3 个音的识谱和听音训练。
- 仅在作答时自动计时，并在音符上方显示音名及正误。
- 查看近期正确率、练习日历，并使用 JSON 备份。

Android 版的记录保存在设备中。公开网站提供介绍和示例体验；示例中输入的内容不会保存。

## 显示语言

Android 版、介绍页面和示例支持上方列出的九种语言。

默认跟随设备或浏览器语言。可在 Android 应用的设置中或通过网页语言选择器更改。不支持的语言会回退为英语。

翻译随应用离线提供，无需翻译 API 或额外费用。自行输入的曲名和心得保持原样。切换语言不会更改评分、已保存的记录或 JSON 备份格式。

日语显示ド–シ，英语显示 C–B，德语显示 C–H，韩语显示 도–시，其他支持的语言显示本地化的 Do–Si。使用英语或德语时，连续输入 FA 表示 F 和 A 两个音；使用唱名的语言中，Fa 表示一个音。

## 开发

| 目录 | 内容 |
|---|---|
| `android/` | Android 应用 |
| `site/` | 介绍页面和示例 |
| `tests/ · contracts/` | Web 测试和共享测试数据 |
| `tools/` | 构建及发布脚本 |

[构建说明（日语）](../BUILD.md) · [第三方声明](../../THIRD_PARTY_NOTICES.md)

## 发布状态

本文档描述本 PR 分支的功能。公开网站及最新发布的 APK 可能尚未包含这些更改。
