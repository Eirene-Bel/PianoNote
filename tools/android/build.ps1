param([string[]]$Tasks=@(':app:testDebugUnitTest',':app:lintDebug',':app:assembleDebug'),[string]$SdkRoot,[string]$JavaHome)
$ErrorActionPreference='Stop'
$environment=& (Join-Path $PSScriptRoot 'resolve-environment.ps1') -SdkRoot $SdkRoot -JavaHome $JavaHome
$root=Split-Path (Split-Path $PSScriptRoot)
$android=Join-Path $root 'android'
$env:JAVA_HOME=$environment.JavaHome;$env:ANDROID_HOME=$environment.SdkRoot
if($Tasks -match 'connected.*AndroidTest'){
    if(-not $env:ANDROID_SERIAL){throw 'Set ANDROID_SERIAL to the explicit test device serial before connected tests'}
    $devices=& $environment.Adb devices
    if(-not($devices -match ('^'+[regex]::Escape($env:ANDROID_SERIAL)+'\s+device(?:\s|$)'))){throw 'Selected device is not ready'}
}
$sdkValue=$environment.SdkRoot.Replace('\','/').Replace(':','\:')
[IO.File]::WriteAllText((Join-Path $android 'local.properties'),"sdk.dir=$sdkValue`n")
$config=Join-Path $android '.gradle';New-Item -ItemType Directory -Path $config -Force | Out-Null
$jdkValue=$environment.JavaHome.Replace('\','/').Replace(':','\:')
[IO.File]::WriteAllText((Join-Path $config 'config.properties'),"java.home=$jdkValue`n")
Push-Location -LiteralPath $android
try {& .\gradlew.bat @Tasks;if($LASTEXITCODE -ne 0){throw "Gradle failed: $LASTEXITCODE"}}finally{Pop-Location}
