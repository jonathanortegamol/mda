param([string]$RemoteUrl = 'https://github.com/jonathanortegamol/mda.git')
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
function Assert-GitSuccess([string]$Operation) {
    if ($LASTEXITCODE -ne 0) { throw "Git no pudo completar: $Operation. No se uso force push." }
}
& git -C $projectRoot rev-parse --is-inside-work-tree
Assert-GitSuccess 'comprobar repositorio'
$currentBranch = & git -C $projectRoot branch --show-current
Assert-GitSuccess 'consultar rama'
if ($currentBranch -ne 'main') { throw 'La rama actual no es main; revisar antes de publicar.' }
$remoteNames = @(& git -C $projectRoot remote)
Assert-GitSuccess 'consultar remotos'
if ($remoteNames -contains 'origin') {
    $currentUrl = & git -C $projectRoot remote get-url origin
    Assert-GitSuccess 'consultar origin'
    if ($currentUrl -ne $RemoteUrl) { throw 'origin apunta a otro repositorio; no se modifico.' }
} else {
    & git -C $projectRoot remote add origin $RemoteUrl
    Assert-GitSuccess 'configurar origin'
}
& git -C $projectRoot ls-remote --symref origin HEAD
Assert-GitSuccess 'conectar con el remoto'
& git -C $projectRoot add -- .gitignore .gitattributes README.md docs modeling transformations infrastructure generated
Assert-GitSuccess 'preparar archivos'
& git -C $projectRoot diff --cached --stat
Assert-GitSuccess 'mostrar cambios'
& git -C $projectRoot diff --cached --quiet
$stagedChanges = $LASTEXITCODE
if ($stagedChanges -eq 1) {
    & git -C $projectRoot commit -m 'Implementar modelos MDA, transformaciones ATL y generacion Acceleo con interfaz grafica'
    Assert-GitSuccess 'crear commit'
} elseif ($stagedChanges -ne 0) { throw 'No se pudieron revisar los cambios preparados.' }
& git -C $projectRoot push -u origin main
Assert-GitSuccess 'subir main'
Write-Output "Proyecto subido a $RemoteUrl"
