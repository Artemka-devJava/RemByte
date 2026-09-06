# FixByte CRM - launcher
#   .\run.ps1          auto: MariaDB if up on :9092, otherwise embedded H2
#   .\run.ps1 -H2      force embedded H2
#   .\run.ps1 -Build   build jar first (mvn package -DskipTests)
param(
    [switch]$H2,
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

function Test-Port {
    param([string]$TargetHost, [int]$Port)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async  = $client.BeginConnect($TargetHost, $Port, $null, $null)
        $ok     = $async.AsyncWaitHandle.WaitOne(600)
        $client.Close()
        return $ok
    } catch {
        return $false
    }
}

$mvn = 'mvn'
if (Test-Path (Join-Path $PSScriptRoot 'mvnw.cmd')) { $mvn = (Join-Path $PSScriptRoot 'mvnw.cmd') }

if ($Build) {
    Write-Host '==> mvn -q package -DskipTests' -ForegroundColor Cyan
    & $mvn -q package -DskipTests
}

$useH2 = [bool]$H2
if (-not $useH2) {
    if (Test-Port -TargetHost '127.0.0.1' -Port 9092) {
        Write-Host '==> MariaDB found on 127.0.0.1:9092 -> profile: dev' -ForegroundColor Green
    } else {
        Write-Host '==> MariaDB on 127.0.0.1:9092 not reachable -> using embedded H2' -ForegroundColor Yellow
        $useH2 = $true
    }
}

Write-Host ''
Write-Host 'FixByte CRM: http://localhost:9087   (login: admin, see .env)' -ForegroundColor Cyan
Write-Host 'Stop: Ctrl+C' -ForegroundColor DarkGray
Write-Host ''

if ($useH2) {
    & $mvn spring-boot:run '-Ph2' '-Dspring-boot.run.profiles=h2'
} else {
    & $mvn spring-boot:run
}
