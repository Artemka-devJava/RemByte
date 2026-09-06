# FixByte CRM - Docker launch (Windows)
#   .\up.ps1     normal mode (bridge, port from .env, default 9087)
#
# macvlan mode is NOT supported on Docker Desktop for Windows
# (Docker runs in a NAT VM). For "own IP on the LAN" use a Linux host,
# see deploy/macvlan/README.md.
param()

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

if (-not (Test-Path app\app.jar) -or (Get-Item app\app.jar).Length -eq 0) {
    Write-Host "No app\app.jar. Build it first:  .\build-jar.ps1" -ForegroundColor Yellow
    Write-Host "(or manually: Copy-Item <your>.jar app\app.jar)"
    exit 1
}

if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
    Write-Host "Created deploy\.env - edit passwords, then run again." -ForegroundColor Yellow
    exit 1
}

Write-Host "==> docker compose -f docker-compose.yml up -d --build" -ForegroundColor Cyan
docker compose -f docker-compose.yml up -d --build
docker compose -f docker-compose.yml ps

$m = Select-String -Path .env -Pattern '^HOST_HTTP_PORT=(.+)$'
$port = if ($m) { $m.Matches[0].Groups[1].Value.Trim() } else { '9087' }
Write-Host ''
Write-Host "CRM: http://localhost:$port" -ForegroundColor Cyan
