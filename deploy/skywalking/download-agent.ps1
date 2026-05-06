param(
    [string]$Version = "9.6.0",
    [string]$InstallRoot = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

if ([string]::IsNullOrWhiteSpace($InstallRoot)) {
    $InstallRoot = Join-Path $repoRoot "tools\skywalking\java-agent\$Version"
}

$agentJar = Join-Path $InstallRoot "agent\skywalking-agent.jar"
if (Test-Path $agentJar) {
    Write-Output "SkyWalking agent already exists: $agentJar"
    return
}

$downloadUrl = "https://dlcdn.apache.org/skywalking/java-agent/$Version/apache-skywalking-java-agent-$Version.tgz"
$tempArchive = Join-Path ([System.IO.Path]::GetTempPath()) "apache-skywalking-java-agent-$Version.tgz"
$extractRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("skywalking-java-agent-" + [System.Guid]::NewGuid().ToString("N"))

New-Item -ItemType Directory -Path $extractRoot -Force | Out-Null
New-Item -ItemType Directory -Path $InstallRoot -Force | Out-Null

Write-Output "Downloading SkyWalking Java Agent $Version ..."
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    & curl.exe -L $downloadUrl -o $tempArchive
    if ($LASTEXITCODE -ne 0) {
        throw "curl.exe download failed: $downloadUrl"
    }
} else {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempArchive
}

Write-Output "Extracting package ..."
tar -xzf $tempArchive -C $extractRoot

$extractedAgentDir = Get-ChildItem -Path $extractRoot -Recurse -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName "skywalking-agent.jar") } |
    Select-Object -First 1

if ($null -eq $extractedAgentDir) {
    throw "Cannot find extracted SkyWalking agent directory in $extractRoot"
}

$targetAgentDir = Join-Path $InstallRoot "agent"
if (Test-Path $targetAgentDir) {
    Remove-Item -Path $targetAgentDir -Recurse -Force
}

Copy-Item -Path $extractedAgentDir.FullName -Destination $targetAgentDir -Recurse

Remove-Item -Path $tempArchive -Force
Remove-Item -Path $extractRoot -Recurse -Force

Write-Output "SkyWalking agent installed: $agentJar"
