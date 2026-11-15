param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$tag = [Guid]::NewGuid().ToString('N').Substring(0,8).ToUpperInvariant()
$checks = [Collections.Generic.List[string]]::new()
function Send-Json([string]$Path, [hashtable]$Body) {
    $json = $Body | ConvertTo-Json -Depth 8
    Invoke-RestMethod -UseBasicParsing -Method Post -Uri ($BaseUrl + $Path) -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 20
}
function Assert-Result([bool]$Condition, [string]$Description) {
    if (-not $Condition) { throw "FALLO: $Description" }
    $checks.Add($Description)
    Write-Host "OK: $Description"
}
function Assert-Conflict([string]$Path, [hashtable]$Body, [string]$Description) {
    $observedStatus = 0
    try { Send-Json $Path $Body | Out-Null }
    catch {
        if ($null -eq $_.Exception.Response) { throw }
        $observedStatus = [int]$_.Exception.Response.StatusCode
    }
    Assert-Result ($observedStatus -eq 409) "$Description (HTTP 409)"
}
Write-Host "Prueba API $BaseUrl; identificadores de ejemplo $tag"
Write-Host 'Se crearan registros ficticios nuevos. No se eliminan registros existentes.'
$origin = Send-Json '/api/aeropuertos' @{ codigo="O-$tag"; nombre='Aeropuerto Origen Demo'; ciudad='Cuenca' }
$destination = Send-Json '/api/aeropuertos' @{ codigo="D-$tag"; nombre='Aeropuerto Destino Demo'; ciudad='Quito' }
$airline = Send-Json '/api/aerolineas' @{ codigo="AL-$tag"; nombre='Aerolinea Demo' }
$plane = Send-Json '/api/aviones' @{ matricula="AV-$tag"; capacidad=2; aerolineaId=$airline.id }
$route = Send-Json '/api/rutas' @{ origenId=$origin.id; destinoId=$destination.id }
$departure = [DateTimeOffset]::UtcNow.AddDays(7)
$flight = Send-Json '/api/vuelos' @{ codigo="TEST-$tag"; salida=$departure.ToString('o'); llegada=$departure.AddHours(2).ToString('o'); rutaId=$route.id; avionId=$plane.id }
Assert-Result ($flight.cuposDisponibles -eq 2) 'Vuelo registrado con dos cupos'
$available = @(Invoke-RestMethod -UseBasicParsing -Uri "$BaseUrl/api/vuelos/disponibles?origenId=$($origin.id)&destinoId=$($destination.id)" -TimeoutSec 20)
Assert-Result (@($available | Where-Object { $_.id -eq $flight.id }).Count -eq 1) 'Vuelo visible en consulta de disponibilidad'
$p1 = Send-Json '/api/pasajeros' @{ documento="P1-$tag"; nombreCompleto='Ana Demo'; contacto='ana@example.invalid' }
$p2 = Send-Json '/api/pasajeros' @{ documento="P2-$tag"; nombreCompleto='Luis Demo'; contacto='luis@example.invalid' }
$p3 = Send-Json '/api/pasajeros' @{ documento="P3-$tag"; nombreCompleto='Maria Demo'; contacto='maria@example.invalid' }
$r1 = Send-Json '/api/reservas' @{ pasajeroId=$p1.id; vueloId=$flight.id }
Assert-Result ($r1.numeroAsiento -eq 1 -and $r1.estado -eq 'CONFIRMADA') 'Primera reserva confirmada con asiento 1'
Assert-Conflict '/api/reservas' @{ pasajeroId=$p1.id; vueloId=$flight.id } 'Rechazo de pasajero con reserva duplicada'
$r2 = Send-Json '/api/reservas' @{ pasajeroId=$p2.id; vueloId=$flight.id }
Assert-Result ($r2.numeroAsiento -eq 2) 'Segunda reserva con asiento 2'
Assert-Conflict '/api/reservas' @{ pasajeroId=$p3.id; vueloId=$flight.id } 'Rechazo de sobreventa'
$ticket = Send-Json "/api/boletos/reserva/$($r1.id)" @{}
Assert-Result ($ticket.reservaId -eq $r1.id -and $ticket.numeroAsiento -eq 1) 'Boleto emitido para la primera reserva y su asiento'
$sameTicket = Send-Json "/api/boletos/reserva/$($r1.id)" @{}
Assert-Result ($sameTicket.id -eq $ticket.id -and $sameTicket.numero -eq $ticket.numero) 'Emision repetida devuelve el mismo boleto'
Assert-Conflict "/api/reservas/$($r1.id)/cancelacion" @{} 'Rechazo de cancelacion con boleto'
$cancelled = Send-Json "/api/reservas/$($r2.id)/cancelacion" @{}
Assert-Result ($cancelled.estado -eq 'CANCELADA' -and $cancelled.numeroAsiento -eq 2) 'Cancelacion conserva asiento historico'
$r3 = Send-Json '/api/reservas' @{ pasajeroId=$p3.id; vueloId=$flight.id }
Assert-Result ($r3.numeroAsiento -eq 2 -and $r3.estado -eq 'CONFIRMADA') 'Asiento 2 liberado y reutilizado'
$history = @(Invoke-RestMethod -UseBasicParsing -Uri "$BaseUrl/api/reservas?vueloId=$($flight.id)" -TimeoutSec 20)
Assert-Result ($history.Count -eq 3 -and @($history | Where-Object { $_.estado -eq 'CONFIRMADA' }).Count -eq 2) 'Historial conserva tres reservas y solo dos confirmadas'
$finalFlight = Invoke-RestMethod -UseBasicParsing -Uri "$BaseUrl/api/vuelos/$($flight.id)" -TimeoutSec 20
Assert-Result ($finalFlight.cuposDisponibles -eq 0) 'Cupos finales coherentes con la capacidad'
Write-Host "Finalizado: $($checks.Count) comprobaciones correctas."
[pscustomobject]@{ vuelo=$flight.codigo; vueloId=$flight.id; reserva=$r1.localizador; reservaId=$r1.id; asiento=$r1.numeroAsiento; boleto=$ticket.numero; boletoId=$ticket.id; comprobaciones=$checks.Count }
