# Logging and Trace Correlation

This project now writes logs to both the console and per-service files, while preserving SkyWalking trace context in each log line.

## Enabled Services

- `scm-auth`
- `scm-gateway`
- `scm-mdm`
- `scm-purchase`
- `scm-inventory`
- `scm-sales`

## Log Pattern

Both console and file logs use this pattern:

```text
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%tid] [%sw_ctx] %logger{36} - %msg%n
```

SkyWalking fields:

- `%tid`
  - trace id
- `%sw_ctx`
  - SkyWalking context
  - format: `[serviceName, instanceName, traceId, traceSegmentId, spanId]`

If a log line is outside the tracing context, SkyWalking prints `N/A`.

## Log Files

Each service writes logs under:

```text
output/logs/<service-name>/
```

Examples:

```text
output/logs/scm-sales/scm-sales.log
output/logs/scm-inventory/scm-inventory.log
output/logs/scm-gateway/scm-gateway.log
```

Archived files go under:

```text
output/logs/<service-name>/archive/
```

Rolling policy:

- rotate by date and size
- `20MB` per file
- keep `14` days
- total archive cap `1GB`

## Prerequisites

To get trace ids in logs:

1. Start the service with SkyWalking `-javaagent`
2. Produce logs inside a traced execution path

If the service is started without `start-service-with-skywalking.ps1`, `tid` and `sw_ctx` will show `N/A`.

## How to Use a Trace ID

After you get a trace id from SkyWalking UI, search the matching service logs directly.

PowerShell examples:

```powershell
Select-String -Path .\output\logs\scm-sales\scm-sales.log -Pattern "f7800d51686a4ebe9d87e86fe4c9673a.174.17781028803410051"
Select-String -Path .\output\logs\scm-inventory\scm-inventory.log -Pattern "f7800d51686a4ebe9d87e86fe4c9673a.174.17781028803410051"
Select-String -Path .\output\logs\scm-gateway\scm-gateway.log -Pattern "f7800d51686a4ebe9d87e86fe4c9673a.174.17781028803410051"
```

This gives you two views of the same request:

1. SkyWalking trace topology and latency
2. Application logs and business details

## Reference

- SkyWalking logback toolkit: https://skywalking.apache.org/docs/skywalking-java/latest/en/setup/service-agent/java-agent/application-toolkit-logback-1.x/
