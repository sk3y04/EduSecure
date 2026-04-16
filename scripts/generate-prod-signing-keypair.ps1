param(
    [string]$OutputDir = $(if ($env:EDUSECURE_CRYPTO_DIR) { $env:EDUSECURE_CRYPTO_DIR } else { (Join-Path (Split-Path -Parent $PSScriptRoot) 'deploy\home-server\crypto') }),
    [string]$PrivateKeyPath = $env:EDUSECURE_SIGNING_PRIVATE_KEY_PATH,
    [string]$PublicKeyPath = $env:EDUSECURE_SIGNING_PUBLIC_KEY_PATH,
    [string]$Curve = $(if ($env:EDUSECURE_SIGNING_CURVE) { $env:EDUSECURE_SIGNING_CURVE } else { 'prime256v1' }),
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message"
}

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

if (-not $PrivateKeyPath) {
    $PrivateKeyPath = Join-Path $OutputDir 'signing-private.pem'
}

if (-not $PublicKeyPath) {
    $PublicKeyPath = Join-Path $OutputDir 'signing-public.pem'
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $PrivateKeyPath) | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $PublicKeyPath) | Out-Null

if ((Test-Path $PrivateKeyPath) -and -not $Force) {
    throw "Refusing to overwrite existing file: $PrivateKeyPath`nRe-run with -Force if you intend to replace it."
}

if ((Test-Path $PublicKeyPath) -and -not $Force) {
    throw "Refusing to overwrite existing file: $PublicKeyPath`nRe-run with -Force if you intend to replace it."
}

Require-Command openssl

Write-Step "Generating EC private key ($Curve)"
& openssl genpkey -algorithm EC -pkeyopt "ec_paramgen_curve:$Curve" -out $PrivateKeyPath
if ($LASTEXITCODE -ne 0) {
    throw 'OpenSSL failed while generating the private key.'
}

Write-Step 'Deriving public key'
& openssl pkey -in $PrivateKeyPath -pubout -out $PublicKeyPath
if ($LASTEXITCODE -ne 0) {
    throw 'OpenSSL failed while deriving the public key.'
}

Write-Host ''
Write-Host 'Generated signing key pair:'
Write-Host "  Private: $PrivateKeyPath"
Write-Host "  Public:  $PublicKeyPath"
Write-Host ''
Write-Host 'Copy them to /srv/edusecure/crypto/ for the production Compose stack.'

