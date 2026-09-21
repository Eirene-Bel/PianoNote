# v0.4.0の署名・公開

既存の公開APKと同じ署名鍵があるWindows PCで、リポジトリのルートからPowerShellで実行します。Android SDK API 36・Build Tools 36.0.0・JDK 21・GitHub CLI（`gh`）が必要です。

署名鍵と認証情報の保存先は `%LOCALAPPDATA%\PianoPracticePlannerDev\signing` です。既存の `piano-personal.p12` と `credentials.json` を使用します。鍵やパスワードはGitHubやチャットに貼り付けないでください。署名の一致は既存のpackage.ps1で確認します。

```powershell
git switch main
if ($LASTEXITCODE -ne 0) { throw 'Cannot switch to main' }
git pull --ff-only
if ($LASTEXITCODE -ne 0) { throw 'Cannot update main' }
if (git status --porcelain) { throw 'Commit or stash local changes first' }
$releaseCommit = git rev-parse HEAD

# 必要に応じて設定
# $env:ANDROID_JAVA_HOME = 'C:\path\to\jdk-21'
# $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

& .\tools\android\build.ps1 -Tasks @(':app:testReleaseUnitTest', ':app:lintRelease')
if ($LASTEXITCODE -ne 0) { throw 'Release checks failed' }
& .\tools\android\package.ps1 -VersionCode 13
if (-not $?) { throw 'Signed packaging failed' }

$apk = '.\artifacts\android\PianoNote-0.4.0-v13.apk'
if (-not (Test-Path -LiteralPath $apk)) { throw 'Signed APK missing' }
$sum = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
"$sum  PianoNote-0.4.0-v13.apk" | Set-Content -Encoding ascii .\artifacts\android\SHA256SUMS.txt

gh release create v0.4.0 $apk .\artifacts\android\SHA256SUMS.txt --repo Eirene-Bel/PianoNote --target $releaseCommit --title 'PianoNote v0.4.0' --notes-file .\docs\releases\v0.4.0.md
if ($LASTEXITCODE -ne 0) { throw 'Release publication failed' }
```

GitHub CLIに未ログインの場合は、先に `gh auth login` を実行します。
