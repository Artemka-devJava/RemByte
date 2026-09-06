# Builds the app jar from sources into deploy\app\app.jar (Windows).
# Run once on a machine with Maven + JDK 17, then copy deploy\ to a Docker host.
#   .\build-jar.ps1            sources in ..\  (repo root)
#   .\build-jar.ps1 C:\path    other project path with pom.xml
param([string]$Src = "..")

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

if (-not (Test-Path (Join-Path $Src 'pom.xml'))) { throw "pom.xml not found in $Src" }

Write-Host "==> mvn -f $Src\pom.xml clean package -DskipTests" -ForegroundColor Cyan
& mvn -f (Join-Path $Src 'pom.xml') -q clean package -DskipTests

$jar = Get-ChildItem (Join-Path $Src 'target') -Filter 'rembyte-crm-*.jar' |
       Where-Object { $_.Name -notmatch 'sources|javadoc' } |
       Select-Object -First 1
if (-not $jar) { throw "jar not found in $Src\target" }

New-Item -ItemType Directory -Force -Path app | Out-Null
Copy-Item $jar.FullName app\app.jar -Force
$mb = [math]::Round((Get-Item app\app.jar).Length / 1MB, 1)
Write-Host "OK: app\app.jar ($mb MB) <- $($jar.Name)" -ForegroundColor Green
Write-Host "Next:  Copy-Item .env.example .env ; .\up.ps1"
