param(
    [string]$KafkaContainer = "kafka-1",
    [string]$BootstrapServer = "kafka-1:29092",
    [int]$Partitions = 3,
    [int]$ReplicationFactor = 3,
    [int]$MinInSyncReplicas = 2,
    [string]$ServiceHost = "host.docker.internal"
)

$ErrorActionPreference = "Stop"

$scriptPath = "docs/operations/scripts/preflight-check.sh"
if (-not (Test-Path $scriptPath)) {
    throw "Script not found: $scriptPath. Please run this command from repo root."
}

$cmd = @(
    "bash",
    $scriptPath,
    "--docker",
    "--container", $KafkaContainer,
    "--host", $ServiceHost,
    $BootstrapServer,
    "$Partitions",
    "$ReplicationFactor",
    "$MinInSyncReplicas"
)

Write-Host "Running: $($cmd -join ' ')" -ForegroundColor Cyan
& $cmd[0] $cmd[1..($cmd.Length - 1)]
