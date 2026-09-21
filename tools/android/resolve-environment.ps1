param([string]$SdkRoot=$env:ANDROID_HOME,[string]$JavaHome=$env:ANDROID_JAVA_HOME)
$ErrorActionPreference='Stop'
if(-not $SdkRoot){$SdkRoot=Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
if(-not $JavaHome){
    $base=Join-Path $env:LOCALAPPDATA 'PianoPracticePlannerDev/jdk'
    if(Test-Path -LiteralPath $base){$JavaHome=(Get-ChildItem -LiteralPath $base -Directory | Where-Object {Test-Path (Join-Path $_.FullName 'bin/java.exe')} | Select-Object -First 1).FullName}
}
if(-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))){throw 'Set ANDROID_JAVA_HOME or pass -JavaHome with the JDK21 folder.'}
if(-not (Test-Path (Join-Path $SdkRoot 'platform-tools/adb.exe'))){throw 'Android SDK not ready; pass -SdkRoot or set ANDROID_HOME.'}
$version=(& (Join-Path $JavaHome 'bin/java.exe') -version 2>&1 | Out-String)
if($version -notmatch 'version "21\.'){throw 'This locked toolchain uses JDK21. The selected Java does not match.'}
[pscustomobject]@{SdkRoot=[IO.Path]::GetFullPath($SdkRoot);JavaHome=[IO.Path]::GetFullPath($JavaHome);JavaVersion=$version.Trim();Adb=Join-Path $SdkRoot 'platform-tools/adb.exe';BuildTools=Join-Path $SdkRoot 'build-tools/36.0.0'}
