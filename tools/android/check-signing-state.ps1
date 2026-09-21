param([Parameter(Mandatory)][string]$KeyPath,[Parameter(Mandatory)][string]$CredentialPath)
$ErrorActionPreference='Stop'
$hasKey=Test-Path -LiteralPath $KeyPath
$hasCredentials=Test-Path -LiteralPath $CredentialPath
if($hasKey -ne $hasCredentials){throw 'Signing material is incomplete. Restore the existing key and credentials; never generate a replacement identity.'}
if($hasKey){'existing'}else{'new'}
