param(
    [Parameter(Mandatory = $true)][string]$ObeoHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$ModelPath
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $ModelPath) { $ModelPath = Join-Path $projectRoot 'modeling/models/spring-psm-referencia.xmi' }
$pluginsPath = Join-Path $ObeoHome 'plugins'
$buildDir = Join-Path $projectRoot 'infrastructure/validation/build'
$depsDir = Join-Path $buildDir 'deps'
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
$dependencies = @('org.eclipse.emf.common_', 'org.eclipse.emf.ecore_', 'org.eclipse.emf.ecore.xmi_')
$jars = foreach ($dependency in $dependencies) {
    $matches = @(Get-ChildItem -LiteralPath $pluginsPath -File | Where-Object { $_.Name.StartsWith($dependency) -and $_.Extension -eq '.jar' })
    if ($matches.Count -ne 1) { throw "Se esperaba un unico JAR para $dependency" }
    $localJar = Join-Path $depsDir $matches[0].Name
    Copy-Item -LiteralPath $matches[0].FullName -Destination $localJar -Force
    $localJar
}
$classpath = $jars -join [IO.Path]::PathSeparator
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $buildDir (Join-Path $PSScriptRoot 'ValidatePsm.java')
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del verificador PSM' }
& (Join-Path $JdkHome 'bin/java.exe') -cp ($buildDir + [IO.Path]::PathSeparator + $classpath) ValidatePsm (Join-Path $projectRoot 'modeling/metamodels/spring-psm.ecore') $ModelPath (Join-Path $projectRoot 'modeling/metamodels/vuelos.ecore')
if ($LASTEXITCODE -ne 0) { throw 'Fallo la validacion del PSM' }
