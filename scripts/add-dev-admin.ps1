[CmdletBinding()]
param(
    [string]$Email,
    [string]$Password,
    [string]$FullName,
    [string]$BackendUrl,
    [string]$ComposeProjectDir,
    [string]$PostgresService,
    [string]$DbName,
    [string]$DbUser,
    [switch]$AdminOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-Setting {
    param(
        [AllowNull()][string]$ExplicitValue,
        [AllowNull()][string]$EnvironmentValue,
        [Parameter(Mandatory = $true)][string]$DefaultValue
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitValue)) {
        return $ExplicitValue.Trim()
    }

    if (-not [string]::IsNullOrWhiteSpace($EnvironmentValue)) {
        return $EnvironmentValue.Trim()
    }

    return $DefaultValue
}

function ConvertTo-SqlLiteral {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) {
        return 'NULL'
    }

    return "'{0}'" -f ($Value -replace "'", "''")
}

function Write-Step {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ("==> {0}" -f $Message) -ForegroundColor Cyan
}

function Write-Detail {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ("    {0}" -f $Message) -ForegroundColor DarkGray
}

function Fail {
    param([Parameter(Mandatory = $true)][string]$Message)
    throw $Message
}

function Get-StatusCode {
    param([Parameter(Mandatory = $true)]$ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return $null
    }

    if ($response.StatusCode) {
        return [int]$response.StatusCode
    }

    if ($response.BaseResponse -and $response.BaseResponse.StatusCode) {
        return [int]$response.BaseResponse.StatusCode
    }

    return $null
}

function Get-ResponseBody {
    param([Parameter(Mandatory = $true)]$ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return $null
    }

    try {
        $stream = $response.GetResponseStream()
        if ($null -eq $stream) {
            return $null
        }

        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    } catch {
        return $null
    }
}

function Invoke-ComposePsql {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [hashtable]$Variables = @{},
        [switch]$Raw
    )

    $arguments = @(
        'compose',
        '--project-directory', $script:ComposeProjectDir,
        'exec',
        '-T',
        $script:PostgresService,
        'psql',
        '-P', 'pager=off',
        '-v', 'ON_ERROR_STOP=1',
        '-U', $script:DbUser,
        '-d', $script:DbName
    )

    foreach ($entry in $Variables.GetEnumerator()) {
        $arguments += @('-v', ("{0}={1}" -f $entry.Key, $entry.Value))
    }

    if ($Raw.IsPresent) {
        $arguments += @('-t', '-A')
    }

    $arguments += @('-c', $Sql)

    $output = & docker @arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        $combinedOutput = ($output | Out-String).Trim()
        if ($combinedOutput) {
            Fail $combinedOutput
        }

        Fail 'The PostgreSQL command failed.'
    }

    if ($Raw.IsPresent) {
        return (($output | Out-String) -replace "`r", '').Trim()
    }

    if ($output) {
        Write-Host ($output | Out-String).Trim()
    }

    return $null
}

function Ensure-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$CommandName)

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        Fail ("Required command not found: {0}" -f $CommandName)
    }
}

function Test-AuthSchemaReady {
    try {
        $tableCount = Invoke-ComposePsql -Raw -Sql @"
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('roles', 'users', 'user_roles');
"@

        return ($tableCount -eq '3')
    } catch {
        return $false
    }
}

function Wait-ForAuthSchema {
    param(
        [int]$TimeoutSeconds = 120,
        [int]$PollIntervalSeconds = 3
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $hasWaited = $false

    while ((Get-Date) -lt $deadline) {
        if (Test-AuthSchemaReady) {
            if ($hasWaited) {
                Write-Detail 'Detected roles/users/user_roles after waiting for Liquibase to finish.'
            }

            return
        }

        if (-not $hasWaited) {
            Write-Detail 'Auth tables are not available yet; waiting for the backend to finish applying Liquibase migrations...'
            $hasWaited = $true
        }

        Start-Sleep -Seconds $PollIntervalSeconds
    }

    Fail @"
The PostgreSQL container is reachable, but the auth schema is still missing after waiting $TimeoutSeconds seconds.

Start the dev stack and let the backend finish booting so Liquibase can create:
  - roles
  - users
  - user_roles

Recommended command from the repository root:
  docker compose up -d postgres backend

If those services are already running, check the backend logs for Liquibase/database errors:
  docker compose logs backend --tail=200

If the logs show repeated startup failures or the schema is unexpectedly incomplete, you may be using a stale PostgreSQL dev volume.
"@
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Email = Resolve-Setting -ExplicitValue $Email -EnvironmentValue $env:EDUSECURE_ADMIN_EMAIL -DefaultValue 'admin@example.com'
$Password = Resolve-Setting -ExplicitValue $Password -EnvironmentValue $env:EDUSECURE_ADMIN_PASSWORD -DefaultValue 'AdminPass123!'
$FullName = Resolve-Setting -ExplicitValue $FullName -EnvironmentValue $env:EDUSECURE_ADMIN_FULL_NAME -DefaultValue 'System Administrator'
$BackendUrl = Resolve-Setting -ExplicitValue $BackendUrl -EnvironmentValue $env:EDUSECURE_BACKEND_URL -DefaultValue 'http://localhost:8080'
$ComposeProjectDir = Resolve-Setting -ExplicitValue $ComposeProjectDir -EnvironmentValue $env:EDUSECURE_COMPOSE_PROJECT_DIR -DefaultValue $projectRoot
$PostgresService = Resolve-Setting -ExplicitValue $PostgresService -EnvironmentValue $env:EDUSECURE_POSTGRES_SERVICE -DefaultValue 'postgres'
$DbName = Resolve-Setting -ExplicitValue $DbName -EnvironmentValue $env:POSTGRES_DB -DefaultValue 'edusecure'
$DbUser = Resolve-Setting -ExplicitValue $DbUser -EnvironmentValue $env:POSTGRES_USER -DefaultValue 'postgres'
$Email = $Email.ToLowerInvariant()
$EmailSqlLiteral = ConvertTo-SqlLiteral -Value $Email
$BackendUrl = $BackendUrl.TrimEnd('/')

Ensure-CommandAvailable -CommandName 'docker'

Write-Step 'Waiting for the auth schema managed by Liquibase'
Wait-ForAuthSchema

Write-Step 'Ensuring auth roles exist in PostgreSQL'
try {
    Invoke-ComposePsql -Sql @"
INSERT INTO roles (name)
VALUES ('STUDENT'), ('LECTURER'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;
"@
} catch {
    Fail ("Unable to insert or verify auth roles after the schema readiness check. Details: {0}" -f $_.Exception.Message)
}

Write-Step ("Checking whether {0} already exists" -f $Email)
$userExists = Invoke-ComposePsql -Raw -Sql (@"
SELECT 1
FROM users
WHERE email = {0}
LIMIT 1;
"@ -f $EmailSqlLiteral)

if (-not $userExists) {
    Write-Step 'User not found; creating it through the backend register endpoint'
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $csrfUri = [Uri]::new("$BackendUrl/api/auth/csrf")
    $registerUri = "$BackendUrl/api/auth/register"

    try {
        Invoke-WebRequest -Method Get -Uri $csrfUri -WebSession $session -UseBasicParsing | Out-Null
    } catch {
        Fail ("Could not reach {0}. Start the backend before running this script for a new user. Details: {1}" -f $csrfUri, $_.Exception.Message)
    }

    $csrfCookie = $session.Cookies.GetCookies($csrfUri) | Where-Object { $_.Name -eq 'XSRF-TOKEN' } | Select-Object -First 1
    if ($null -eq $csrfCookie -or [string]::IsNullOrWhiteSpace($csrfCookie.Value)) {
        Fail 'Failed to obtain the XSRF-TOKEN cookie from the backend.'
    }

    $headers = @{ 'X-XSRF-TOKEN' = $csrfCookie.Value }
    $body = @{
        email = $Email
        password = $Password
        fullName = $FullName
    } | ConvertTo-Json

    try {
        Invoke-WebRequest -Method Post -Uri $registerUri -WebSession $session -Headers $headers -ContentType 'application/json' -Body $body -UseBasicParsing | Out-Null
        Write-Host ("Created user {0}" -f $Email) -ForegroundColor Green
    } catch {
        $statusCode = Get-StatusCode -ErrorRecord $_
        $responseBody = Get-ResponseBody -ErrorRecord $_

        if ($statusCode -eq 409) {
            Write-Warning ("User {0} already exists. Continuing with admin role assignment." -f $Email)
        } else {
            $detailSuffix = if ([string]::IsNullOrWhiteSpace($responseBody)) { '' } else { " Response: $responseBody" }
            $displayStatusCode = if ($null -eq $statusCode) { 'unknown' } else { [string]$statusCode }
            Fail ("Failed to register {0} via the backend (HTTP {1}).{2}" -f $Email, $displayStatusCode, $detailSuffix)
        }
    }
} else {
    Write-Host ("User {0} already exists; skipping registration." -f $Email) -ForegroundColor Yellow
}

Write-Step 'Assigning the ADMIN role'
Invoke-ComposePsql -Sql (@"
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = {0}
ON CONFLICT DO NOTHING;
"@ -f $EmailSqlLiteral)

if ($AdminOnly.IsPresent) {
    Write-Step 'Removing the STUDENT role because -AdminOnly was specified'
    Invoke-ComposePsql -Sql (@"
DELETE FROM user_roles
WHERE user_id = (
    SELECT id
    FROM users
    WHERE email = {0}
)
AND role_id = (
    SELECT id
    FROM roles
    WHERE name = 'STUDENT'
);
"@ -f $EmailSqlLiteral)
}

Write-Step 'Reading back the assigned roles'
$storedFullName = Invoke-ComposePsql -Raw -Sql (@"
SELECT full_name
FROM users
WHERE email = {0};
"@ -f $EmailSqlLiteral)

$rolesOutput = Invoke-ComposePsql -Raw -Sql (@"
SELECT r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = {0}
ORDER BY r.name;
"@ -f $EmailSqlLiteral)

if (-not $rolesOutput) {
    Fail ("No roles were found for {0}. The admin bootstrap did not complete successfully." -f $Email)
}

$roles = $rolesOutput -split "`n" |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ }

Write-Host ''
Write-Host 'Admin bootstrap complete.' -ForegroundColor Green
Write-Host ("Email:      {0}" -f $Email)
$displayFullName = if ($null -eq $storedFullName) { '' } else { $storedFullName.Trim() }
Write-Host ("Full name:  {0}" -f $displayFullName)
Write-Host ("Roles:      {0}" -f ($roles -join ', '))
Write-Host ("DB:         {0} ({1})" -f $DbName, $PostgresService)
Write-Host ("Backend:    {0}" -f $BackendUrl)

if (-not $userExists) {
    Write-Host ("Password:   {0}" -f $Password) -ForegroundColor Yellow
} else {
    Write-Host 'Password:   unchanged (existing user)' -ForegroundColor Yellow
}



