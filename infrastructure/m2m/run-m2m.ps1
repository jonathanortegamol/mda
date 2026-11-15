param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$InputPim,
    [string]$OutputPsm,
    [switch]$Verify
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $InputPim) { $InputPim = Join-Path $projectRoot 'modeling/metamodels/vuelos.ecore' }
if (-not $OutputPsm) { $OutputPsm = Join-Path $projectRoot 'modeling/models/generated/spring-psm.xmi' }
$OutputPsm = [IO.Path]::GetFullPath($OutputPsm)
$outputRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'modeling/models/generated')) + [IO.Path]::DirectorySeparatorChar
$testOutputRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'transformations/m2m/build')) + [IO.Path]::DirectorySeparatorChar
if ((-not $OutputPsm.StartsWith($outputRoot, [StringComparison]::OrdinalIgnoreCase) -and -not $OutputPsm.StartsWith($testOutputRoot, [StringComparison]::OrdinalIgnoreCase)) -or [IO.Path]::GetExtension($OutputPsm) -ne '.xmi') { throw 'La salida debe estar en modeling/models/generated o transformations/m2m/build; no se sobrescriben modelos manuales' }
$buildDir = Join-Path $projectRoot 'transformations/m2m/build'
$depsDir = Join-Path $buildDir 'deps'
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
$prefixes = @('org.eclipse.emf.common_', 'org.eclipse.emf.ecore_', 'org.eclipse.emf.ecore.xmi_',
    'org.eclipse.m2m.atl.common_', 'org.eclipse.m2m.atl.core_', 'org.eclipse.m2m.atl.core.emf_',
    'org.eclipse.m2m.atl.engine_', 'org.eclipse.m2m.atl.engine.emfvm_', 'org.eclipse.m2m.atl.dsls_',
    'org.eclipse.m2m.atl.drivers.emf4atl_', 'org.eclipse.core.runtime_', 'org.eclipse.equinox.common_',
    'org.eclipse.osgi_', 'org.antlr.runtime_', 'org.eclipse.m2m.atl.emftvm_',
    'org.eclipse.m2m.atl.emftvm.trace_', 'org.eclipse.emf.validation_', 'org.objectweb.asm_')
$jars = foreach ($prefix in $prefixes) {
    $matches = @(Get-ChildItem -LiteralPath (Join-Path $ObeoHome 'plugins') -File | Where-Object { $_.Name.StartsWith($prefix) -and $_.Extension -eq '.jar' })
    if ($matches.Count -ne 1) { throw "Se esperaba un JAR $prefix; encontrados $($matches.Count)" }
    $localJar = Join-Path $depsDir $matches[0].Name
    Copy-Item -LiteralPath $matches[0].FullName -Destination $localJar -Force
    $localJar
}
$classpath = $jars -join [IO.Path]::PathSeparator
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $buildDir (Join-Path $PSScriptRoot 'RunAtl.java') (Join-Path $PSScriptRoot 'VerifyM2m.java') (Join-Path $projectRoot 'infrastructure/validation/ValidatePsm.java')
if ($LASTEXITCODE -ne 0) { throw 'Fallo compilacion de infraestructura ATL' }
$runtimeClasspath = $buildDir + [IO.Path]::PathSeparator + $classpath
$atlSource = Join-Path $projectRoot 'transformations/m2m/src/PimToSpringPsm.atl'
$psmMetamodel = Join-Path $projectRoot 'modeling/metamodels/spring-psm.ecore'
$profile = Join-Path $projectRoot 'transformations/m2m/supported-ocl.properties'
& (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath RunAtl $atlSource $InputPim $psmMetamodel (Join-Path $buildDir 'PimToSpringPsm.asm') $OutputPsm $profile
if ($LASTEXITCODE -ne 0) { throw 'Fallo compilacion o ejecucion ATL' }
& (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath ValidatePsm $psmMetamodel $OutputPsm $InputPim
if ($LASTEXITCODE -ne 0) { throw 'La salida ATL no paso la validacion EMF/PSM' }
if ($Verify) {
    & (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath VerifyM2m $projectRoot $OutputPsm
    if ($LASTEXITCODE -ne 0) { throw 'Fallo verificacion de equivalencia o propagacion M2M' }
}
$evidence = [ordered]@{ engine = 'ATL EMFVM'; compiler = 'Atl2006Compiler'; verificationSuiteExecuted = $Verify.IsPresent; runtimeJars = @($jars | ForEach-Object { [IO.Path]::GetFileName($_) }); generatedAtUtc = [DateTime]::UtcNow.ToString('o'); files = @() }
foreach ($file in @($atlSource, $profile, $InputPim, $psmMetamodel, (Join-Path $buildDir 'PimToSpringPsm.asm'), $OutputPsm)) {
    $absoluteFile = [IO.Path]::GetFullPath($file)
    $evidencePath = if ($absoluteFile.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { $absoluteFile.Substring($projectRoot.Length + 1) } else { $absoluteFile }
    $evidence.files += [ordered]@{ path = $evidencePath; sha256 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
}
$evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($OutputPsm + '.provenance.json') -Encoding UTF8
