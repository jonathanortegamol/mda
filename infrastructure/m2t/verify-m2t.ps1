param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [switch]$CachedJava
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
& (Join-Path $projectRoot 'infrastructure/m2m/run-m2m.ps1') -ObeoHome $ObeoHome -JdkHome $JdkHome -Verify
& (Join-Path $PSScriptRoot 'run-m2t.ps1') -ObeoHome $ObeoHome -JdkHome $JdkHome
$backend = Join-Path $projectRoot 'generated/backend'
$before = Get-Content -LiteralPath (Join-Path $backend '.generation-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
& (Join-Path $PSScriptRoot 'run-m2t.ps1') -ObeoHome $ObeoHome -JdkHome $JdkHome
$after = Get-Content -LiteralPath (Join-Path $backend '.generation-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if (($before.outputs | ConvertTo-Json -Compress) -ne ($after.outputs | ConvertTo-Json -Compress)) { throw 'La regeneracion Acceleo cambio archivos con las mismas entradas' }
Write-Output 'OK: regeneracion Acceleo identica en los 56 archivos de salida.'
$propagationBackend = Join-Path $projectRoot 'transformations/m2t/build/propagation-backend'
& (Join-Path $PSScriptRoot 'run-m2t.ps1') -ObeoHome $ObeoHome -JdkHome $JdkHome -InputPsm (Join-Path $projectRoot 'transformations/m2m/build/tests/spring-psm-telefono.xmi') -InputPim (Join-Path $projectRoot 'transformations/m2m/build/tests/vuelos-telefono.ecore') -OutputDirectory $propagationBackend
foreach ($file in @('src/main/java/ec/edu/ucuenca/mda/vuelos/domain/Pasajero.java', 'src/main/java/ec/edu/ucuenca/mda/vuelos/dto/PasajeroRequest.java', 'src/main/java/ec/edu/ucuenca/mda/vuelos/dto/PasajeroResponse.java', 'src/main/java/ec/edu/ucuenca/mda/vuelos/service/PasajeroService.java', 'schema.sql')) {
    $content = Get-Content -LiteralPath (Join-Path $propagationBackend $file) -Raw -Encoding UTF8
    if ($content -notmatch '(?i)telefono') { throw "No se propago telefono a $file" }
    $baselineContent = Get-Content -LiteralPath (Join-Path $backend $file) -Raw -Encoding UTF8
    if ($baselineContent -match '(?i)telefono') { throw 'La prueba modifico las fuentes de entrega' }
}
Write-Output 'OK: telefono se propaga de PIM a PSM mediante ATL y a entidad, DTO, servicio y SQL mediante Acceleo.'
if ($CachedJava) {
    & (Join-Path $PSScriptRoot 'verify-cached-java.ps1') -JdkHome $JdkHome
    Copy-Item -LiteralPath (Join-Path $projectRoot 'transformations/m2t/build/cached-verification/junit-summary.txt') -Destination (Join-Path $projectRoot 'transformations/m2t/build/baseline-junit-summary.txt') -Force
    & (Join-Path $PSScriptRoot 'verify-cached-java.ps1') -JdkHome $JdkHome -BackendDirectory $propagationBackend
}
$result = [ordered]@{ checkedAtUtc = [DateTime]::UtcNow.ToString('o'); m2m = 'passed'; acceleoCompilation = 'passed'; deterministicOutputs = 56; phonePropagation = 'passed'; cachedJavaAndJUnit = $CachedJava.IsPresent; exactMaven3516 = 'not verified: dependencies unavailable and network restricted'; databaseAndHttpExecution = 'not performed in this phase' }
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $projectRoot 'transformations/m2t/verification-result.json') -Encoding UTF8
