[CmdletBinding()]
param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "..\\runpod.local.env")
)

$ErrorActionPreference = "Stop"

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

$config = Read-ConfigFile -Path $ConfigPath
$sshTarget = Require-Value -Values $config -Name "RUNPOD_SSH_TARGET"
$sshKeyPath = Require-Value -Values $config -Name "RUNPOD_SSH_KEY_PATH"
$publicUrl = (Require-Value -Values $config -Name "RUNPOD_PUBLIC_URL").TrimEnd("/")
$portText = if ($config.ContainsKey("PORT")) { [string]$config.PORT } else { "8000" }
$port = 0

if (-not [int]::TryParse($portText, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
    throw "PORT must be a number between 1 and 65535."
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
    $archiveItems = @(
        (Join-Path $backendRoot "package.json"),
        (Join-Path $backendRoot "src"),
        (Join-Path $backendRoot "scripts")
    )
    Compress-Archive -LiteralPath $archiveItems -DestinationPath $archivePath -Force
    [IO.File]::WriteAllText($runtimePath, ($runtimeValues | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))

    $sshArguments = @("-i", $sshKeyPath, $sshTarget)
    Write-Host "Checking the RunPod connection..."
    & ssh @sshArguments "printf 'connected\\n'"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not connect to RunPod over SSH."
    }

    Write-Host "Uploading the backend package..."
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
    $remoteSetup | & ssh @sshArguments "bash -s -- '$remoteRoot' '$remoteArchive' '$port'"
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
