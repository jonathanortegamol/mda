param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$InputPsm,
    [string]$InputPim,
    [string]$OutputDirectory,
    [ValidateSet('backend','frontend')][string]$Target = 'backend'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $InputPsm) { $InputPsm = Join-Path $projectRoot 'modeling/models/generated/spring-psm.xmi' }
if (-not $InputPim) { $InputPim = Join-Path $projectRoot 'modeling/metamodels/vuelos.ecore' }
$canonicalPsm = [IO.Path]::GetFullPath((Join-Path $projectRoot 'modeling/models/generated/spring-psm.xmi'))
if ([IO.Path]::GetFullPath($InputPsm) -eq $canonicalPsm) {
    $provenance = Get-Content -LiteralPath ($canonicalPsm + '.provenance.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $provenance.files) {
        $sourcePath = if ([IO.Path]::IsPathRooted($entry.path)) { $entry.path } else { Join-Path $projectRoot $entry.path }
        if (-not (Test-Path -LiteralPath $sourcePath) -or (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash -ne $entry.sha256) { throw 'La evidencia ATL no coincide con las entradas actuales; ejecute run-m2m.ps1 antes de M2T' }
    }
}
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot ('generated/' + $Target) }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
$allowedOutput = [IO.Path]::GetFullPath((Join-Path $projectRoot ('generated/' + $Target)))
$testRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'transformations/m2t/build')) + [IO.Path]::DirectorySeparatorChar
if ($OutputDirectory -ne $allowedOutput -and -not $OutputDirectory.StartsWith($testRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'La salida M2T debe corresponder al destino generado o una carpeta de pruebas dentro de transformations/m2t/build' }
$buildDir = Join-Path $projectRoot 'transformations/m2t/build'
$depsDir = Join-Path $buildDir 'deps'
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
$prefixes = @('org.eclipse.emf.common_', 'org.eclipse.emf.ecore_', 'org.eclipse.emf.ecore.xmi_',
    'org.eclipse.acceleo.common_', 'org.eclipse.acceleo.model_', 'org.eclipse.acceleo.parser_',
    'org.eclipse.acceleo.engine_', 'org.eclipse.acceleo.profiler_', 'org.eclipse.acceleo.traceability_',
    'org.eclipse.acceleo.traceability.model_', 'org.eclipse.ocl_', 'org.eclipse.ocl.ecore_',
    'org.eclipse.ocl.common_', 'org.eclipse.core.runtime_', 'org.eclipse.equinox.common_',
    'org.eclipse.osgi_', 'lpg.runtime.java_', 'com.google.guava_', 'com.google.guava.failureaccess_', 'org.eclipse.text_', 'org.eclipse.core.contenttype_')
$jars = foreach ($prefix in $prefixes) {
    $matches = @(Get-ChildItem -LiteralPath (Join-Path $ObeoHome 'plugins') -File | Where-Object { $_.Name.StartsWith($prefix) -and $_.Extension -eq '.jar' })
    if ($matches.Count -ne 1) { throw "Se esperaba un JAR $prefix; encontrados $($matches.Count)" }
    $localJar = Join-Path $depsDir $matches[0].Name
    Copy-Item -LiteralPath $matches[0].FullName -Destination $localJar -Force
    $localJar
}
$classpath = $jars -join [IO.Path]::PathSeparator
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $buildDir (Join-Path $PSScriptRoot 'RunAcceleo.java') (Join-Path $projectRoot 'infrastructure/validation/ValidatePsm.java')
if ($LASTEXITCODE -ne 0) { throw 'Fallo compilacion de infraestructura Acceleo' }
$runtimeClasspath = $buildDir + [IO.Path]::PathSeparator + $classpath
& (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath ValidatePsm (Join-Path $projectRoot 'modeling/metamodels/spring-psm.ecore') $InputPsm $InputPim
if ($LASTEXITCODE -ne 0) { throw 'PSM de entrada invalido' }
$templatePath = Join-Path $projectRoot ('transformations/m2t/src/' + $Target + '.mtl')
$compiledPath = Join-Path $buildDir ($Target + '.emtl')
& (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath RunAcceleo $templatePath (Join-Path $projectRoot 'modeling/metamodels/spring-psm.ecore') $InputPsm $compiledPath $OutputDirectory
if ($LASTEXITCODE -ne 0) { throw 'Fallo compilacion o generacion Acceleo' }
$manifest = [ordered]@{
    generator = 'Acceleo 3'; target = $Target; generatedAtUtc = [DateTime]::UtcNow.ToString('o')
    inputMode = $(if ([IO.Path]::GetFullPath($InputPsm) -eq $canonicalPsm) { 'ATL-produced PSM (provenance checked)' } else { 'Explicit test/custom PSM' })
    runtimeJars = @($jars | ForEach-Object { [IO.Path]::GetFileName($_) }); sources = @(); outputs = @()
}
foreach ($file in @($templatePath, $InputPsm, $InputPim, (Join-Path $projectRoot 'modeling/metamodels/spring-psm.ecore'), $compiledPath)) {
    $absoluteFile = [IO.Path]::GetFullPath($file)
    $recordPath = if ($absoluteFile.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { $absoluteFile.Substring($projectRoot.Length+1) } else { $absoluteFile }
    $manifest.sources += [ordered]@{ path = $recordPath; sha256 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
}
foreach ($relativeFile in [IO.File]::ReadAllLines((Join-Path $OutputDirectory '.acceleo-files.txt'), [Text.Encoding]::UTF8)) {
    $generatedFile = Join-Path $OutputDirectory $relativeFile
    $manifest.outputs += [ordered]@{ path = $relativeFile; sha256 = (Get-FileHash -LiteralPath $generatedFile -Algorithm SHA256).Hash }
}
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $OutputDirectory '.generation-manifest.json') -Encoding UTF8
