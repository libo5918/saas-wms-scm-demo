param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("scm-auth", "scm-gateway", "scm-mdm", "scm-purchase", "scm-inventory", "scm-sales")]
    [string]$Module,

    [string]$ServiceName = "",
    [string]$OapAddress = "127.0.0.1:11800",
    [string]$AgentVersion = "9.6.0",
    [switch]$SkipBuild,
    [switch]$DryRun,
    [string[]]$AdditionalJvmArgs = @(),
    [string[]]$ApplicationArgs = @()
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

if ([string]::IsNullOrWhiteSpace($ServiceName)) {
    $ServiceName = $Module
}

$downloadScript = Join-Path $scriptDir "download-agent.ps1"
& $downloadScript -Version $AgentVersion

$agentJar = Join-Path $repoRoot "tools\skywalking\java-agent\$AgentVersion\agent\skywalking-agent.jar"
if (-not (Test-Path $agentJar)) {
    throw "SkyWalking agent jar not found: $agentJar"
}

$agentHome = Split-Path -Parent $agentJar
$pluginMountFolders = @("plugins", "activations")

if ($Module -eq "scm-gateway") {
    $gatewayPluginDir = Join-Path $agentHome "mount-scm-gateway"
    $gatewayPluginJar = Get-ChildItem -Path (Join-Path $agentHome "optional-plugins") -Filter "apm-spring-cloud-gateway-4.x-plugin-*.jar" -File |
        Select-Object -First 1

    if ($null -eq $gatewayPluginJar) {
        throw "Cannot find Spring Cloud Gateway optional plugin under $agentHome\\optional-plugins"
    }

    New-Item -ItemType Directory -Path $gatewayPluginDir -Force | Out-Null
    Copy-Item -Path $gatewayPluginJar.FullName -Destination (Join-Path $gatewayPluginDir $gatewayPluginJar.Name) -Force
    $pluginMountFolders += "mount-scm-gateway"
}

if (-not $SkipBuild) {
    Write-Output "Building module $Module ..."
    & mvn -pl $Module -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed for module $Module"
    }
}

$moduleTarget = Join-Path $repoRoot "$Module\target"
$bootJar = Get-ChildItem -Path $moduleTarget -Filter "*.jar" -File |
    Where-Object {
        $_.Name -notlike "*sources.jar" -and
        $_.Name -notlike "*javadoc.jar" -and
        $_.Name -notlike "*.original" -and
        $_.Name -notlike "*plain*.jar"
    } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $bootJar) {
    throw "Cannot find runnable Spring Boot jar under $moduleTarget"
}

$skywalkingLogDir = Join-Path $repoRoot "output\skywalking\$ServiceName"
New-Item -ItemType Directory -Path $skywalkingLogDir -Force | Out-Null

$javaArgs = @(
    "-javaagent:$agentJar",
    "-Dskywalking.agent.service_name=$ServiceName",
    "-Dskywalking.collector.backend_service=$OapAddress",
    "-Dskywalking.logging.dir=$skywalkingLogDir",
    "-Dskywalking.plugin.mount=$($pluginMountFolders -join ',')"
) + $AdditionalJvmArgs + @(
    "-jar",
    $bootJar.FullName
) + $ApplicationArgs

Write-Output "Starting $ServiceName"
Write-Output "  Module: $Module"
Write-Output "  Jar: $($bootJar.FullName)"
Write-Output "  OAP: $OapAddress"
Write-Output "  Agent: $agentJar"

if ($DryRun) {
    $renderedArgs = $javaArgs | ForEach-Object {
        if ($_ -match "\s") {
            '"' + $_ + '"'
        } else {
            $_
        }
    }
    Write-Output "  Command: java $($renderedArgs -join ' ')"
    return
}

& java $javaArgs
