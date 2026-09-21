param([int]$VersionCode=13,[string]$SdkRoot,[string]$JavaHome)
$ErrorActionPreference='Stop'
$environment=& (Join-Path $PSScriptRoot 'resolve-environment.ps1') -SdkRoot $SdkRoot -JavaHome $JavaHome
$root=Split-Path (Split-Path $PSScriptRoot)
$signing=Join-Path $env:LOCALAPPDATA 'PianoPracticePlannerDev/signing'
New-Item -ItemType Directory -Path $signing -Force | Out-Null
$key=Join-Path $signing 'piano-personal.p12';$credentialFile=Join-Path $signing 'credentials.json'
$state=& (Join-Path $PSScriptRoot 'check-signing-state.ps1') -KeyPath $key -CredentialPath $credentialFile
$identityPath=Join-Path $root 'docs/android/signing-identity.json'
if($state -eq 'new' -and (Test-Path -LiteralPath $identityPath)){throw 'A release identity is already recorded. Restore that signing key instead of generating a new key.'}
if($state -eq 'new'){
    $password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    @{storePassword=$password;keyAlias='piano-personal'} | ConvertTo-Json | Set-Content -Encoding utf8 $credentialFile
}
$credential=Get-Content -LiteralPath $credentialFile | ConvertFrom-Json
if($state -eq 'new'){
    $env:PIANO_STORE_PASS=$credential.storePassword
    try{& (Join-Path $environment.JavaHome 'bin/keytool.exe') -genkeypair -alias $credential.keyAlias -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -keystore $key -storepass:env PIANO_STORE_PASS -keypass:env PIANO_STORE_PASS -dname 'CN=PianoPracticePlanner' -noprompt
        if($LASTEXITCODE -ne 0){throw 'Signing key creation failed'}
    }finally{Remove-Item Env:PIANO_STORE_PASS -ErrorAction SilentlyContinue}
}
$env:PIANO_STORE_PASS=$credential.storePassword
$cert=Join-Path $signing 'certificate.der'
try{& (Join-Path $environment.JavaHome 'bin/keytool.exe') -exportcert -alias $credential.keyAlias -keystore $key -storepass:env PIANO_STORE_PASS -file $cert
    if($LASTEXITCODE -ne 0){throw 'Cannot verify signing identity'}
}finally{Remove-Item Env:PIANO_STORE_PASS -ErrorAction SilentlyContinue}
$fingerprint=(Get-FileHash -LiteralPath $cert -Algorithm SHA256).Hash.ToLowerInvariant()
if(Test-Path -LiteralPath $identityPath){$expected=(Get-Content -LiteralPath $identityPath | ConvertFrom-Json).certificateSha256;if($fingerprint -ne $expected){throw 'Signing certificate differs from the original release'}}
else{@{applicationId='local.pianopracticeplanner';certificateSha256=$fingerprint} | ConvertTo-Json | Set-Content -Encoding utf8 $identityPath}
$keyValue=$key.Replace('\','/').Replace(':','\:')
$properties="storeFile=$keyValue`nstorePassword=$($credential.storePassword)`nkeyAlias=$($credential.keyAlias)`nkeyPassword=$($credential.storePassword)`n"
[IO.File]::WriteAllText((Join-Path $root 'android/keystore.properties'),$properties)
& (Join-Path $PSScriptRoot 'build.ps1') -Tasks @(':app:assembleRelease',"-PappVersionCode=$VersionCode") -SdkRoot $environment.SdkRoot -JavaHome $environment.JavaHome
$apk=Join-Path $root 'android/app/build/outputs/apk/release/app-release.apk'
$env:JAVA_HOME=$environment.JavaHome
$badging=(& (Join-Path $environment.BuildTools 'aapt.exe') dump badging $apk | Out-String)
if($LASTEXITCODE -ne 0 -or $badging -notmatch "package: name='local.pianopracticeplanner' versionCode='([^']+)'"){throw 'APK application ID is unexpected'}
if([int]$Matches[1] -ne $VersionCode){throw 'APK versionCode does not match the requested version'}
if($badging -notmatch "versionName='([0-9]+\.[0-9]+\.[0-9]+)'" ){throw 'APK versionName is missing or unsupported'}
$versionName=$Matches[1]
& (Join-Path $environment.BuildTools 'apksigner.bat') verify --verbose --print-certs $apk
if($LASTEXITCODE -ne 0){throw 'APK signature verification failed'}
& (Join-Path $environment.BuildTools 'zipalign.exe') -c -P 16 4 $apk
if($LASTEXITCODE -ne 0){throw 'APK alignment verification failed'}
$out=Join-Path $root 'artifacts/android';New-Item -ItemType Directory -Path $out -Force | Out-Null
$target=Join-Path $out "PianoNote-$versionName-v$VersionCode.apk"
Copy-Item -LiteralPath $apk -Destination $target
@{applicationId='local.pianopracticeplanner';versionCode=$VersionCode;versionName=$versionName;minSdk=36;targetSdk=36;apk=(Split-Path $target -Leaf);sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant();bytes=(Get-Item -LiteralPath $target).Length;builtAt=[DateTimeOffset]::Now.ToString('o')} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $out "release-v$VersionCode.json")
Write-Output "Signed APK: $target"
