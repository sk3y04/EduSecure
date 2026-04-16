$ErrorActionPreference = 'Stop'

function New-Secret {
    param(
        [string]$Name,
        [ValidateSet('hex', 'base64')]
        [string]$Mode,
        [int]$Length
    )

    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    switch ($Mode) {
        'hex' {
            $value = ([System.BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
        }
        'base64' {
            $value = [Convert]::ToBase64String($bytes)
        }
    }

    "${Name}=$value"
}

Write-Output '# Paste these values into deploy/home-server/.env.prod after reviewing them.'
Write-Output '# AES-backed settings are generated as 32 random bytes encoded in Base64.'
Write-Output (New-Secret -Name 'POSTGRES_PASSWORD' -Mode 'hex' -Length 24)
Write-Output (New-Secret -Name 'MONGODB_ROOT_PASSWORD' -Mode 'hex' -Length 24)
Write-Output (New-Secret -Name 'MONGODB_APP_PASSWORD' -Mode 'hex' -Length 24)
Write-Output (New-Secret -Name 'JWT_SECRET' -Mode 'base64' -Length 48)
Write-Output (New-Secret -Name 'MFA_SECRET_ENCRYPTION_KEY' -Mode 'base64' -Length 32)
Write-Output 'MFA_SECRET_KEY_VERSION=v1'
Write-Output 'MFA_ISSUER=EduSecure'
Write-Output (New-Secret -Name 'AUDIT_HMAC_SECRET' -Mode 'base64' -Length 48)
Write-Output (New-Secret -Name 'SUBMISSION_STORAGE_MASTER_KEY' -Mode 'base64' -Length 32)
Write-Output 'SUBMISSION_STORAGE_MASTER_KEY_VERSION=v1'




