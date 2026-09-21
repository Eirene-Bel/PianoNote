# ビルド

## 紹介ページ・サンプル

Node.js 22以上を使用します。追加のnpmパッケージは不要です。

```sh
node --test tests/*.test.mjs
node tools/web/build.mjs
```

配布ファイルは `artifacts/web-site/` に生成されます。ローカル確認は `site/` をHTTPサーバーで配信してください。Pythonがある場合:

```sh
python -m http.server 8768 --bind 127.0.0.1 --directory site
```

`http://127.0.0.1:8768/` を開きます。`main`へpushするとGitHub Actionsがテスト・ビルド後、生成物だけをこのリポジトリのGitHub Pagesへ配信します。

## Android

Android Studioで `android/` を開きます。Gradle JDKは21、Android SDKはAPI 36、Build Toolsは36.0.0を使用します。

```sh
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windowsでは `gradlew.bat` を使用します。生成先は `android/app/build/outputs/apk/debug/` です。

PowerShellの補助スクリプトを使う場合は、`ANDROID_HOME` にSDK、`ANDROID_JAVA_HOME` にJDK 21を指定して `tools/android/build.ps1` を実行します。

署名鍵はリポジトリに含めていません。公開APKと同じ署名で更新するには、既存の署名鍵が必要です。
