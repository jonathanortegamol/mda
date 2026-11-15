param(
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$BackendDirectory,
    [string]$InstalledMavenCache = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $BackendDirectory) { $BackendDirectory = Join-Path $projectRoot 'generated/backend' }
$localRepository = Join-Path $projectRoot '.cache/maven'
$cacheUri = ([Uri]($InstalledMavenCache + '/')).AbsoluteUri
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JdkHome
    & mvn -B -ntp -U -s (Join-Path $PSScriptRoot 'maven-settings.xml') "-Dmda.cache.url=$cacheUri" "-Dmaven.repo.local=$localRepository" '-Dmaven.wagon.http.retryHandler.count=0' '-Dmaven.wagon.rto=5000' -f (Join-Path $BackendDirectory 'pom.xml') package
    if ($LASTEXITCODE -ne 0) { throw 'No se completo la compilacion/pruebas del backend generado' }
} finally { $env:JAVA_HOME = $previousJavaHome }
