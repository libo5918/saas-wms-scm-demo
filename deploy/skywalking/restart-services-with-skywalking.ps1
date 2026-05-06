param(
    [string[]]$Modules = @("scm-auth", "scm-gateway", "scm-mdm", "scm-inventory", "scm-sales"),
    [switch]$SkipBuild,
    [int]$StartupDelaySeconds = 3
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$startScript = Join-Path $scriptDir "start-service-with-skywalking.ps1"

$modulePorts = @{
    "scm-gateway"   = 18080
    "scm-mdm"       = 18082
    "scm-purchase"  = 18083
    "scm-inventory" = 18084
    "scm-sales"     = 18085
    "scm-auth"      = 18086
}

function Get-TargetProcessIds {
    param(
        [string]$Module,
        [int]$Port
    )

    $processIds = New-Object System.Collections.Generic.HashSet[int]

    try {
        $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction Stop
        foreach ($connection in $connections) {
            [void]$processIds.Add([int]$connection.OwningProcess)
        }
    } catch {
    }

    $jarPattern = [regex]::Escape("$Module\target\$Module-")
    $javaProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -match $jarPattern }

    foreach ($process in $javaProcesses) {
        [void]$processIds.Add([int]$process.ProcessId)
    }

    return @($processIds)
}

foreach ($module in $Modules) {
    if (-not $modulePorts.ContainsKey($module)) {
        throw "Unsupported module: $module"
    }

    $port = $modulePorts[$module]
    $processIds = Get-TargetProcessIds -Module $module -Port $port

    if ($processIds.Count -gt 0) {
        Write-Output "Stopping $module on port ${port}: $($processIds -join ', ')"
        Stop-Process -Id $processIds -Force
        Start-Sleep -Seconds 2
    } else {
        Write-Output "No running process found for $module on port $port"
    }
}

foreach ($module in $Modules) {
    $skipBuildArg = if ($SkipBuild) { "-SkipBuild" } else { "" }
    $command = "Set-Location '$repoRoot'; & '$startScript' -Module '$module' $skipBuildArg"

    Write-Output "Starting $module in a new PowerShell window"
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command", $command
    ) -WorkingDirectory $repoRoot

    Start-Sleep -Seconds $StartupDelaySeconds
}
