$ErrorActionPreference='Stop'
$check=Join-Path (Split-Path $PSScriptRoot) 'check-signing-state.ps1'
if(-not(Test-Path -LiteralPath $check)){throw 'Signing state checker is not implemented'}
$temp=Join-Path ([IO.Path]::GetTempPath()) ('android-signing-test-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null
$key=Join-Path $temp 'key.p12';$credentials=Join-Path $temp 'credentials.json'
if((& $check -KeyPath $key -CredentialPath $credentials) -ne 'new'){throw 'Fresh signing state should be new'}
'{}' | Set-Content $credentials
$failed=$false;try {& $check -KeyPath $key -CredentialPath $credentials | Out-Null}catch{$failed=$true}
if(-not $failed){throw 'Lost signing key must stop packaging before generation'}
'test fixture only' | Set-Content $key
if((& $check -KeyPath $key -CredentialPath $credentials) -ne 'existing'){throw 'Existing state should be reused'}
Remove-Item -LiteralPath $credentials
$failed=$false;try {& $check -KeyPath $key -CredentialPath $credentials | Out-Null}catch{$failed=$true}
if(-not $failed){throw 'Missing credentials must stop packaging'}
Remove-Item -LiteralPath $key
Remove-Item -LiteralPath $temp
Write-Output 'Signing state: 4 checks passed.'
