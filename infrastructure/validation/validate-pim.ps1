param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$pluginsPath = Join-Path $ObeoHome 'plugins'
$buildDir = Join-Path $projectRoot 'infrastructure/validation/build'
$depsDir = Join-Path $buildDir 'deps'
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
$dependencies = @('org.eclipse.emf.common_', 'org.eclipse.emf.ecore_', 'org.eclipse.emf.ecore.xmi_',
    'org.eclipse.ocl_', 'org.eclipse.ocl.ecore_', 'org.eclipse.ocl.common_', 'lpg.runtime.java_',
    'org.eclipse.core.runtime_', 'org.eclipse.equinox.common_', 'org.eclipse.osgi_')
$jars = foreach ($dependency in $dependencies) {
    $matches = @(Get-ChildItem -LiteralPath $pluginsPath -File | Where-Object { $_.Name.StartsWith($dependency) -and $_.Extension -eq '.jar' })
    if ($matches.Count -ne 1) { throw "Se esperaba un unico JAR para $dependency; encontrados: $($matches.Count)" }
    $localJar = Join-Path $depsDir $matches[0].Name
    Copy-Item -LiteralPath $matches[0].FullName -Destination $localJar -Force
    $localJar
}
$classpath = $jars -join [IO.Path]::PathSeparator
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $buildDir (Join-Path $PSScriptRoot 'ValidatePim.java') (Join-Path $PSScriptRoot 'ValidateOcl.java')
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del verificador EMF' }
& (Join-Path $JdkHome 'bin/java.exe') -cp ($buildDir + [IO.Path]::PathSeparator + $classpath) ValidatePim (Join-Path $projectRoot 'modeling/metamodels/vuelos.ecore') (Join-Path $projectRoot 'modeling/models/vuelos-ejemplo.xmi')
if ($LASTEXITCODE -ne 0) { throw 'Fallo la validacion del PIM' }
$modelPath = Join-Path $projectRoot 'modeling/models'
$ecorePath = Join-Path $projectRoot 'modeling/metamodels/vuelos.ecore'
$runtimeClasspath = $buildDir + [IO.Path]::PathSeparator + $classpath
& (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath ValidateOcl $ecorePath (Join-Path $modelPath 'vuelos-ejemplo.xmi') ''
if ($LASTEXITCODE -ne 0) { throw 'Fallo la validacion OCL del ejemplo base' }
$cases = Get-Content -LiteralPath (Join-Path $modelPath 'validation/cases.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($case in $cases) {
    & (Join-Path $JdkHome 'bin/java.exe') -cp $runtimeClasspath ValidateOcl $ecorePath (Join-Path $modelPath ('validation/' + $case.file)) $case.expectedConstraint
    if ($LASTEXITCODE -ne 0) { throw "Fallo el escenario OCL $($case.file)" }
}
