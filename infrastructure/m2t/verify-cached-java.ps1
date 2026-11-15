param(
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$BackendDirectory,
    [string]$InstalledMavenCache = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
if (-not $BackendDirectory) { $BackendDirectory = Join-Path $projectRoot 'generated/backend' }
$buildDir = Join-Path $projectRoot 'transformations/m2t/build/cached-verification'
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
$depsDir = Join-Path $buildDir 'deps'
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
# API compatible de la rama 3.5 presente en la cache: no se cambia el POM objetivo 3.5.16.
$artifacts = @(
    'org/springframework/boot/spring-boot/3.5.3', 'org/springframework/boot/spring-boot-autoconfigure/3.5.3',
    'org/springframework/spring-core/6.2.8', 'org/springframework/spring-jcl/6.2.8',
    'org/springframework/spring-beans/6.2.8', 'org/springframework/spring-context/6.2.8',
    'org/springframework/spring-expression/6.2.8', 'org/springframework/spring-aop/6.2.8',
    'org/springframework/spring-tx/6.2.8', 'org/springframework/spring-orm/6.2.8',
    'org/springframework/spring-web/6.2.8', 'org/springframework/spring-webmvc/6.2.8',
    'org/springframework/data/spring-data-jpa', 'org/springframework/data/spring-data-commons',
    'jakarta/persistence/jakarta.persistence-api', 'jakarta/validation/jakarta.validation-api/3.0.2',
    'jakarta/transaction/jakarta.transaction-api', 'org/apache/tomcat/embed/tomcat-embed-core',
    'org/junit/jupiter/junit-jupiter-api/5.12.2', 'org/junit/jupiter/junit-jupiter-engine/5.12.2',
    'org/junit/platform/junit-platform-commons/1.12.2', 'org/junit/platform/junit-platform-engine/1.12.2',
    'org/junit/platform/junit-platform-launcher/1.12.2', 'org/opentest4j/opentest4j', 'org/apiguardian/apiguardian-api',
    'org/mockito/mockito-core/5.17.0', 'net/bytebuddy/byte-buddy', 'org/objenesis/objenesis'
)
$jars = foreach ($artifact in $artifacts) {
    $artifactPath = Join-Path $InstalledMavenCache $artifact
    $candidates = @(Get-ChildItem -LiteralPath $artifactPath -Recurse -File -Filter '*.jar' -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch '-(sources|javadoc|tests)\.jar$' } | Sort-Object -Property @{Expression={ ([regex]::Matches($_.FullName,'\d+') | ForEach-Object { $_.Value.PadLeft(8,'0') }) -join '.' }} -Descending)
    if ($candidates.Count -eq 0) { throw "Falta dependencia en la cache: $artifact" }
    $localJar = Join-Path $depsDir $candidates[0].Name
    Copy-Item -LiteralPath $candidates[0].FullName -Destination $localJar -Force
    $localJar
}
$classpath = $jars -join [IO.Path]::PathSeparator
$sources = @(Get-ChildItem -LiteralPath (Join-Path $BackendDirectory 'src/main/java'),(Join-Path $BackendDirectory 'src/test/java') -Recurse -File -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -parameters -cp $classpath -d $buildDir @sources (Join-Path $PSScriptRoot 'CachedTestLauncher.java')
if ($LASTEXITCODE -ne 0) { throw 'Fallo compilacion Java contra la cache instalada' }
$testResources = Join-Path $BackendDirectory 'src/test/resources'
& (Join-Path $JdkHome 'bin/java.exe') -cp ($buildDir + [IO.Path]::PathSeparator + $testResources + [IO.Path]::PathSeparator + $classpath) CachedTestLauncher 'ec.edu.ucuenca.mda.vuelos.BusinessRulesTest' (Join-Path $buildDir 'junit-summary.txt')
if ($LASTEXITCODE -ne 0) { throw 'Fallaron pruebas JUnit con dependencias de cache' }
$jars | Set-Content -LiteralPath (Join-Path $buildDir 'classpath.txt') -Encoding UTF8
Write-Output 'Fuentes Java compiladas y pruebas ejecutadas con cache Boot 3.5.3; la compilacion Maven exacta 3.5.16 sigue pendiente.'
