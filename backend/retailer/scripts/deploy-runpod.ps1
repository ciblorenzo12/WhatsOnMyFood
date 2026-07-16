[CmdletBinding()]
param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "..\\runpod.local.env")
)

$ErrorActionPreference = "Stop"
# PowerShell 5 defaults external-command pipeline output to UTF-16LE. The RunPod
# transfer uses Base64 text, so force UTF-8 before piping it to SSH.
$OutputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = $OutputEncoding

function Read-ConfigFile {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            throw "Invalid configuration line in ${Path}: $line"
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $values[$name] = $trimmed.Substring($separator + 1).Trim()
    }

    return $values
}

function Require-Value {
    param([hashtable]$Values, [string]$Name)

    $value = [string]$Values[$Name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match "YOUR-|your-") {
        throw "Set $Name in $ConfigPath before deploying. See runpod.local.env.example."
    }

    return $value
}

function ConvertFrom-SecureString {
    param([System.Security.SecureString]$Value)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function New-PortableBackendArchive {
    param([string]$Root, [string]$DestinationPath)

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $stream = [IO.File]::Open($DestinationPath, [IO.FileMode]::Create)
    $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        $files = @(
            Get-Item -LiteralPath (Join-Path $Root "package.json")
            Get-ChildItem -LiteralPath (Join-Path $Root "src") -File -Recurse
            Get-ChildItem -LiteralPath (Join-Path $Root "scripts") -File -Recurse
        )
        foreach ($file in $files) {
            $relativePath = $file.FullName.Substring($Root.Length).TrimStart([char[]]'\\/') -replace '\\', '/'
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive,
                $file.FullName,
                $relativePath,
                [IO.Compression.CompressionLevel]::Optimal
            ) | Out-Null
        }
    }
    finally {
        $archive.Dispose()
        $stream.Dispose()
    }
}

$config = Read-ConfigFile -Path $ConfigPath
$sshTarget = Require-Value -Values $config -Name "RUNPOD_SSH_TARGET"
$sshKeyPath = Require-Value -Values $config -Name "RUNPOD_SSH_KEY_PATH"
$publicUrl = (Require-Value -Values $config -Name "RUNPOD_PUBLIC_URL").TrimEnd("/")
$sshPortText = if ($config.ContainsKey("RUNPOD_SSH_PORT")) { [string]$config.RUNPOD_SSH_PORT } else { "" }
$portText = if ($config.ContainsKey("PORT")) { [string]$config.PORT } else { "8000" }
$port = 0
$sshPort = 0

if (-not [int]::TryParse($portText, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
    throw "PORT must be a number between 1 and 65535."
}
if (-not [string]::IsNullOrWhiteSpace($sshPortText) -and (-not [int]::TryParse($sshPortText, [ref]$sshPort) -or $sshPort -lt 1 -or $sshPort -gt 65535)) {
    throw "RUNPOD_SSH_PORT must be a number between 1 and 65535 when it is set."
}
if (-not (Test-Path -LiteralPath $sshKeyPath)) {
    throw "SSH key not found: $sshKeyPath"
}
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    throw "OpenSSH is required. Install the Windows OpenSSH Client, then rerun this command."
}

$geminiApiKey = [string]$config.GEMINI_API_KEY
if ([string]::IsNullOrWhiteSpace($geminiApiKey)) {
    $enteredKey = Read-Host "Gemini API key (kept only in memory for this deployment)" -AsSecureString
    $geminiApiKey = ConvertFrom-SecureString -Value $enteredKey
}
if ([string]::IsNullOrWhiteSpace($geminiApiKey)) {
    throw "A Gemini API key is required to enable the AI service."
}

$geminiModel = if ([string]::IsNullOrWhiteSpace([string]$config.GEMINI_MODEL)) { "gemini-2.5-flash" } else { [string]$config.GEMINI_MODEL }
$runtimeValues = [ordered]@{
    PORT = "$port"
    GEMINI_API_KEY = $geminiApiKey
    GEMINI_MODEL = $geminiModel
}
if (-not [string]::IsNullOrWhiteSpace([string]$config.BITWISE_APP_TOKEN)) {
    $runtimeValues.BITWISE_APP_TOKEN = [string]$config.BITWISE_APP_TOKEN
}

$backendRoot = Split-Path -Parent $PSScriptRoot
$archivePath = Join-Path ([IO.Path]::GetTempPath()) ("whats-on-my-food-backend-" + [Guid]::NewGuid().ToString("N") + ".zip")
$runtimePath = Join-Path ([IO.Path]::GetTempPath()) ("whats-on-my-food-runtime-" + [Guid]::NewGuid().ToString("N") + ".json")
$remoteRoot = "/workspace/whats-on-my-food-backend"
$remoteArchive = "/tmp/whats-on-my-food-backend.zip"

try {
    New-PortableBackendArchive -Root $backendRoot -DestinationPath $archivePath
    [IO.File]::WriteAllText($runtimePath, ($runtimeValues | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))

    $sshArguments = @("-i", $sshKeyPath)
    if ($sshPort -gt 0) {
        $sshArguments += @("-p", "$sshPort")
    }
    $sshArguments += $sshTarget
    Write-Host "Checking the RunPod connection..."
    & ssh @sshArguments "printf 'connected\\n'"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not connect to RunPod over SSH."
    }

    Write-Host "Uploading the backend package..."
    if ($sshPort -gt 0 -and (Get-Command scp -ErrorAction SilentlyContinue)) {
        # RunPod's direct TCP endpoint supports SCP, which preserves binary files on
        # Windows without relying on PowerShell's text pipeline encoding.
        $scpArguments = @("-i", $sshKeyPath, "-P", "$sshPort")
        $archiveTarget = "${sshTarget}:$remoteArchive"
        & scp @scpArguments $archivePath $archiveTarget
        if ($LASTEXITCODE -ne 0) {
            throw "Could not upload the backend package."
        }

        & ssh @sshArguments "mkdir -p '$remoteRoot'"
        if ($LASTEXITCODE -ne 0) {
            throw "Could not prepare the RunPod application directory."
        }

        $runtimeTarget = "${sshTarget}:$remoteRoot/runpod.runtime.json"
        & scp @scpArguments $runtimePath $runtimeTarget
        if ($LASTEXITCODE -ne 0) {
            throw "Could not upload the server configuration."
        }
        & ssh @sshArguments "chmod 600 '$remoteRoot/runpod.runtime.json'"
        if ($LASTEXITCODE -ne 0) {
            throw "Could not protect the server configuration."
        }
    }
    else {
        [Convert]::ToBase64String([IO.File]::ReadAllBytes($archivePath)) |
            & ssh @sshArguments "base64 -d > '$remoteArchive'"
        if ($LASTEXITCODE -ne 0) {
            throw "Could not upload the backend package."
        }

        [Convert]::ToBase64String([IO.File]::ReadAllBytes($runtimePath)) |
            & ssh @sshArguments "mkdir -p '$remoteRoot'; base64 -d > '$remoteRoot/runpod.runtime.json'; chmod 600 '$remoteRoot/runpod.runtime.json'"
        if ($LASTEXITCODE -ne 0) {
            throw "Could not upload the server configuration."
        }
    }

    $remoteSetup = @'
set -euo pipefail
APP_DIR="$1"
ARCHIVE="$2"
PORT="$3"
STAGING="$(mktemp -d)"
cleanup() { rm -rf "$STAGING" "$ARCHIVE"; }
trap cleanup EXIT

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js 18 or newer is required on this RunPod image." >&2
  exit 80
fi

NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
if [ "$NODE_MAJOR" -lt 18 ]; then
  echo "Node.js 18 or newer is required; found $(node --version)." >&2
  exit 80
fi

if command -v unzip >/dev/null 2>&1; then
  unzip -q "$ARCHIVE" -d "$STAGING"
else
  python3 -m zipfile -e "$ARCHIVE" "$STAGING"
fi

mkdir -p "$APP_DIR"
rm -rf "$APP_DIR/src" "$APP_DIR/scripts"
cp "$STAGING/package.json" "$APP_DIR/package.json"
cp -R "$STAGING/src" "$APP_DIR/src"
cp -R "$STAGING/scripts" "$APP_DIR/scripts"

cat > "$APP_DIR/run-server.js" <<'EOF'
const fs = require("fs");
const path = require("path");
const appDir = __dirname;
Object.assign(process.env, JSON.parse(fs.readFileSync(path.join(appDir, "runpod.runtime.json"), "utf8")));
require(path.join(appDir, "src", "server.js"));
EOF

if [ -s "$APP_DIR/server.pid" ]; then
  OLD_PID="$(cat "$APP_DIR/server.pid")"
  if kill -0 "$OLD_PID" 2>/dev/null; then
    kill "$OLD_PID" || true
    for _ in 1 2 3 4 5; do
      kill -0 "$OLD_PID" 2>/dev/null || break
      sleep 1
    done
  fi
fi

nohup node "$APP_DIR/run-server.js" > "$APP_DIR/server.log" 2>&1 < /dev/null &
echo $! > "$APP_DIR/server.pid"
sleep 2
if ! kill -0 "$(cat "$APP_DIR/server.pid")" 2>/dev/null; then
  cat "$APP_DIR/server.log" >&2 || true
  exit 1
fi

curl -fsS "http://127.0.0.1:${PORT}/health"
echo
'@

    Write-Host "Starting the Gemini backend on port $port..."
    # Windows PowerShell writes CRLF to external-command stdin. Strip the carriage
    # returns on the pod before Bash evaluates the setup script.
    $remoteSetup | & ssh @sshArguments "tr -d '\r' | bash -s -- '$remoteRoot' '$remoteArchive' '$port'"
    if ($LASTEXITCODE -ne 0) {
        throw "RunPod could not start the backend. Check the error above."
    }

    Write-Host "Checking the public RunPod address..."
    $health = $null
    for ($attempt = 1; $attempt -le 6; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$publicUrl/health" -TimeoutSec 15
            break
        }
        catch {
            if ($attempt -eq 6) {
                throw
            }
            Start-Sleep -Seconds 3
        }
    }
    if (-not $health.ok -or $health.bitwiseProvider -ne "google-gemini") {
        throw "The public health check did not confirm the Gemini provider."
    }

    Write-Host "Deployment complete. Gemini is active at $publicUrl"
}
finally {
    Remove-Item -LiteralPath $archivePath, $runtimePath -Force -ErrorAction SilentlyContinue
    $geminiApiKey = $null
}
