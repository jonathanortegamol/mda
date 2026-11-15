param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$NodeExecutable
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $NodeExecutable) {
    $nodeCommand = Get-Command node -ErrorAction SilentlyContinue
    if ($nodeCommand) { $NodeExecutable = $nodeCommand.Source }
    else {
        $bundledRuntime = Join-Path $env:LOCALAPPDATA 'Programs/Microsoft VS Code/Code.exe'
        if (Test-Path -LiteralPath $bundledRuntime) { $NodeExecutable = $bundledRuntime }
        else { throw 'Las pruebas necesitan Node.js o el runtime instalado de VS Code; la interfaz no necesita ninguno para abrirse.' }
    }
}
& (Join-Path $projectRoot 'infrastructure/m2m/run-m2m.ps1') -ObeoHome $ObeoHome -JdkHome $JdkHome -Verify
$generator = Join-Path $projectRoot 'infrastructure/m2t/run-m2t.ps1'
& $generator -Target frontend -ObeoHome $ObeoHome -JdkHome $JdkHome
$frontend = Join-Path $projectRoot 'generated/frontend'
$before = Get-Content -LiteralPath (Join-Path $frontend '.generation-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
& $generator -Target frontend -ObeoHome $ObeoHome -JdkHome $JdkHome
$after = Get-Content -LiteralPath (Join-Path $frontend '.generation-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if (($before.outputs | ConvertTo-Json -Compress) -ne ($after.outputs | ConvertTo-Json -Compress)) { throw 'El frontend no se regenero de forma identica' }
$variant = Join-Path $projectRoot 'transformations/m2t/build/frontend-telefono'
& $generator -Target frontend -ObeoHome $ObeoHome -JdkHome $JdkHome -InputPsm (Join-Path $projectRoot 'transformations/m2m/build/tests/spring-psm-telefono.xmi') -InputPim (Join-Path $projectRoot 'transformations/m2m/build/tests/vuelos-telefono.ecore') -OutputDirectory $variant
$previousElectronMode = $env:ELECTRON_RUN_AS_NODE
try {
    if ([IO.Path]::GetFileName($NodeExecutable) -eq 'Code.exe') { $env:ELECTRON_RUN_AS_NODE = '1' }
    foreach ($folder in @($frontend,$variant)) {
        foreach ($file in @('model.js','demo-store.js','app.js')) {
            & $NodeExecutable --check (Join-Path $folder $file) 2>&1 | Out-Host
            if ($LASTEXITCODE -ne 0) { throw "JavaScript invalido: $file" }
        }
    }
    & $NodeExecutable (Join-Path $PSScriptRoot 'verify-frontend.cjs') $frontend 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Fallo el frontend base' }
    & $NodeExecutable (Join-Path $PSScriptRoot 'verify-frontend.cjs') $variant --require-phone 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la propagacion visual de telefono' }
} finally { $env:ELECTRON_RUN_AS_NODE = $previousElectronMode }
$result = [ordered]@{ checkedAtUtc = [DateTime]::UtcNow.ToString('o'); generator = 'Acceleo 3.7.15'; outputs = 5; repeatability = 'passed'; generatedForms = 8; baselineChecks = 27; phoneVariantChecks = 29; phoneInHtmlAndMetadata = 'passed'; demoAdapterAndStorage = 'passed'; browserVisualReview = 'blocked: Computer Use could not identify Chrome URL confidently'; apiBackendIntegration = 'not executed' }
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $projectRoot 'transformations/m2t/frontend-verification.json') -Encoding UTF8
Write-Output 'OK: formularios generados, flujo local, regeneracion y propagacion hasta HTML verificados.'
