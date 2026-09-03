[CmdletBinding()]
param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "..\runpod.local.env"),
    [string]$AndroidPropertiesPath = (Join-Path $PSScriptRoot "..\..\..\app\local.properties")
)

$ErrorActionPreference = "Stop"

function Read-PropertiesFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required configuration file is missing: $Path"
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([^#=]+)=(.*)$') {
            $values[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    return $values
}

function Require-HttpsUrl {
    param([Parameter(Mandatory = $true)][string]$Name, [string]$Value)

    $uri = $null
    $isAbsolute = -not [string]::IsNullOrWhiteSpace($Value) `
        -and [Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$uri)
    if (-not $isAbsolute -or $uri.Scheme -ne 'https') {
        throw "$Name must be an absolute HTTPS URL."
    }
    return $Value.TrimEnd('/')
}

function Expect-Unauthorized {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [ValidateSet('GET', 'POST')][string]$Method = 'GET',
        [string]$Body = ''
    )

    $request = @{
        Uri = $Uri
        Method = $Method
        ContentType = 'application/json'
        TimeoutSec = 20
        UseBasicParsing = $true
    }
    if ($Method -eq 'POST') {
        $request.Body = $Body
    }

    try {
        Invoke-WebRequest @request | Out-Null
        throw "Protected endpoint accepted a request without the app token: $Uri"
    }
    catch {
        $statusCode = if ($_.Exception.Response) {
            [int]$_.Exception.Response.StatusCode
        }
        elseif ($_.Exception.Message -match '\((\d{3})\)') {
            [int]$matches[1]
        }
        else {
            0
        }
        if ($statusCode -ne 401) {
            throw "Expected HTTP 401 from $Uri but received $statusCode."
        }
    }
}

$serverConfig = Read-PropertiesFile -Path $ConfigPath
$androidConfig = Read-PropertiesFile -Path $AndroidPropertiesPath
$baseUrl = Require-HttpsUrl -Name 'RUNPOD_PUBLIC_URL' -Value $serverConfig.RUNPOD_PUBLIC_URL
$aiBaseUrl = Require-HttpsUrl -Name 'BITWISE_LLM_BASE_URL' -Value $androidConfig.BITWISE_LLM_BASE_URL
$ragBaseUrl = Require-HttpsUrl -Name 'RETAILER_BACKEND_BASE_URL' -Value $androidConfig.RETAILER_BACKEND_BASE_URL

if ($aiBaseUrl -ne $baseUrl -or $ragBaseUrl -ne $baseUrl) {
    throw "Android AI and RAG base URLs must match the deployed hosted base URL."
}
if ([string]::IsNullOrWhiteSpace([string]$serverConfig.GEMINI_API_KEY)) {
    throw "GEMINI_API_KEY must be configured only in the ignored server environment file."
}
$openFdaApiKey = [string]$serverConfig.OPENFDA_API_KEY
if ([string]::IsNullOrWhiteSpace($openFdaApiKey)) {
    throw "OPENFDA_API_KEY must be configured only in the ignored server environment file."
}
$appToken = [string]$serverConfig.BITWISE_APP_TOKEN
if ([string]::IsNullOrWhiteSpace($appToken)) {
    throw "BITWISE_APP_TOKEN must be configured in the ignored server environment file."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
Push-Location $repoRoot
try {
    foreach ($ignoredPath in @('app/local.properties', 'backend/retailer/runpod.local.env')) {
        & git check-ignore --quiet -- $ignoredPath
        if ($LASTEXITCODE -ne 0) {
            throw "$ignoredPath is not excluded from Git."
        }
    }

    $forbiddenAndroidNames = 'GEMINI_API_KEY|GOOGLE_API_KEY|OPENFDA_API_KEY|GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY|WALMART_PRIVATE_KEY_PEM|AMAZON_SECRET_ACCESS_KEY'
    $trackedProviderSecrets = & git grep -n -I -E $forbiddenAndroidNames -- app ':!app/build'
    if ($LASTEXITCODE -eq 0 -and $trackedProviderSecrets) {
        throw "A server-side provider credential name was found in tracked Android files.`n$trackedProviderSecrets"
    }

    $trackedProviderKey = & git grep -l -F -- ([string]$serverConfig.GEMINI_API_KEY)
    if ($LASTEXITCODE -eq 0 -and $trackedProviderKey) {
        throw "The configured Gemini credential was found in tracked repository content."
    }

    $trackedRecallKey = & git grep -l -F -- $openFdaApiKey
    if ($LASTEXITCODE -eq 0 -and $trackedRecallKey) {
        throw "The configured openFDA credential was found in tracked repository content."
    }

    $apkPath = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (Test-Path -LiteralPath $apkPath) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead($apkPath)
        try {
            $forbiddenApkValues = @(
                [string]$serverConfig.GEMINI_API_KEY,
                $openFdaApiKey,
                'GEMINI_API_KEY',
                'GOOGLE_API_KEY',
                'OPENFDA_API_KEY',
                'GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY',
                'WALMART_PRIVATE_KEY_PEM',
                'AMAZON_SECRET_ACCESS_KEY'
            )
            $binaryEncoding = [Text.Encoding]::GetEncoding(28591)
            foreach ($entry in $archive.Entries) {
                if ($entry.Length -eq 0) { continue }
                $stream = $entry.Open()
                try {
                    $memory = New-Object IO.MemoryStream
                    $stream.CopyTo($memory)
                    $entryText = $binaryEncoding.GetString($memory.ToArray())
                    foreach ($forbiddenValue in $forbiddenApkValues) {
                        $containsForbiddenValue = -not [string]::IsNullOrWhiteSpace($forbiddenValue) `
                            -and $entryText.Contains($forbiddenValue)
                        if ($containsForbiddenValue) {
                            throw "A server-side provider credential was found inside the APK."
                        }
                    }
                }
                finally {
                    $stream.Dispose()
                }
            }
        }
        finally {
            $archive.Dispose()
        }
    }
}
finally {
    Pop-Location
}

$health = Invoke-RestMethod -Uri "$baseUrl/health" -TimeoutSec 20
if (-not $health.ok -or $health.bitwiseProvider -ne 'google-gemini' -or -not $health.foodRecallKeyConfigured) {
    throw "Hosted /health did not confirm the Google Gemini and openFDA providers."
}

$ready = Invoke-RestMethod -Uri "$baseUrl/ready" -TimeoutSec 20
$requiredReadyChecks = @(
    $ready.checks.publicHttpsConfigured,
    $ready.checks.aiProviderCredentialConfigured,
    $ready.checks.appAuthenticationConfigured,
    $ready.checks.ragProviderConfigured,
    $ready.checks.foodRecallCredentialConfigured
)
if (-not $ready.ok -or $requiredReadyChecks -contains $false) {
    throw "Hosted /ready did not confirm every required configuration check."
}

Expect-Unauthorized -Method POST -Uri "$baseUrl/v1/bitwise/analyze" -Body '{"prompt":"configuration check"}'
Expect-Unauthorized -Uri "$baseUrl/api/retail/products/0000000000000/ingredients/rag"
Expect-Unauthorized -Uri "$baseUrl/v1/food-recalls?productName=Oat%20Cereal"

$recallResponse = Invoke-RestMethod `
    -Uri "$baseUrl/v1/food-recalls?productName=Oat%20Cereal&brand=Sample%20Foods" `
    -Headers @{ 'X-APP-TOKEN' = $appToken } `
    -TimeoutSec 25
if ($null -eq $recallResponse.results) {
    throw "Authenticated hosted recall check did not return the expected results collection."
}

$ragResponse = Invoke-RestMethod `
    -Uri "$baseUrl/api/retail/products/051500255162/ingredients/rag" `
    -Headers @{ 'X-APP-TOKEN' = $appToken } `
    -TimeoutSec 25
$ragHasIngredients = -not [string]::IsNullOrWhiteSpace(
    [string]$ragResponse.product.ingredients_text
)
if ($ragResponse.status -ne 1 -or -not $ragHasIngredients) {
    throw "Authenticated hosted RAG recovery did not return usable Jif ingredients."
}

Write-Host "Hosted environment validation passed."
Write-Host "Base URL: $baseUrl"
Write-Host "Health: google-gemini and openFDA"
Write-Host "Readiness: HTTPS, provider credentials, app authentication, RAG, and recalls confirmed"
Write-Host "RAG smoke: recovered ingredients from $($ragResponse.source)"
Write-Host "Recall smoke: protected backend returned a valid results collection"
Write-Host "Security: Gemini and openFDA credentials are absent from tracked files and the debug APK"
