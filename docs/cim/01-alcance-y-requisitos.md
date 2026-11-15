# 01 — Alcance y requisitos

## Alcance propuesto

Una oficina de atención mantiene el catálogo y realiza la programación, reserva y emisión. Cada vuelo es una operación concreta en una fecha, sobre una ruta directa y con un avión. Cada reserva corresponde a un pasajero y un vuelo; no existen reservas grupales ni itinerarios con conexiones. Emitir un boleto confirma documentalmente la reserva y no procesa pagos.

Se incluyen altas y consultas de los ocho conceptos del negocio, consulta de disponibilidad y cancelación de una reserva sin boleto. El mínimo demostrable no necesita edición o eliminación general de catálogos, reprogramación ni cancelación de vuelos. Los registros referenciados se conservan. Esta reducción permite demostrar MDA con relaciones y reglas significativas.

Cada reserva recibe automáticamente el menor número de asiento disponible, entre 1 y la capacidad del avión. El asiento se consulta en la reserva y en su boleto. No se requiere una entidad Asiento.

Se excluyen pagos, tarifas e impuestos, mapas de cabina, clases, elección manual o cambio de asiento, equipaje, check-in, tarjetas de embarque, escalas, tripulaciones, mantenimiento, integración con aeropuertos, autenticación con roles y notificaciones externas. Una reserva no expira automáticamente. No se pretende cubrir regulación ni operación comercial de una aerolínea real.

## Actores

| ID | Actor | Responsabilidad |
|---|---|---|
| ACT-01 | Gestor de operaciones | Registrar y consultar catálogos; programar y consultar vuelos |
| ACT-02 | Agente de atención | Registrar pasajeros, consultar disponibilidad, reservar, cancelar y emitir/consultar boletos |
| ACT-03 | Pasajero | Solicitar una reserva o cancelación y recibir el boleto a través del agente |

ACT-03 es un actor del proceso de negocio, sin acceso directo previsto a la aplicación. ACT-01 y ACT-02 son responsabilidades organizativas y pueden ser ejercidas por una misma persona; no implican un sistema de permisos.

## Requisitos funcionales

| ID | Requisito y criterio de aceptación | Prioridad |
|---|---|---|
| RF-01 | Registrar y consultar aeropuertos con código, nombre y ciudad; rechazar un código repetido | Esencial |
| RF-02 | Registrar y consultar aerolíneas con código y nombre; rechazar un código repetido | Esencial |
| RF-03 | Registrar y consultar aviones con matrícula, capacidad y aerolínea; rechazar capacidad no positiva o matrícula repetida | Esencial |
| RF-04 | Crear y consultar rutas dirigidas entre dos aeropuertos existentes; rechazar extremos iguales y pares duplicados | Esencial |
| RF-05 | Programar y consultar vuelos con código de operación único, ruta, avión, salida y llegada; rechazar intervalos inválidos o incompatibles | Esencial |
| RF-06 | Registrar y consultar pasajeros con documento, nombre y contacto; rechazar documento repetido | Esencial |
| RF-07 | Consultar vuelos futuros con cupos, con filtro opcional por origen y destino; mostrar horario y cupos restantes | Esencial |
| RF-08 | Crear una reserva confirmada de un pasajero existente para un vuelo disponible; asignar un localizador único y el menor asiento libre entre 1 y la capacidad, respetando capacidad y no duplicidad | Esencial |
| RF-09 | Consultar reservas por localizador, pasajero o vuelo, mostrando su estado y número de asiento | Esencial |
| RF-10 | Emitir y consultar un boleto con número único para una reserva confirmada de un vuelo futuro, mostrando el asiento de la reserva; una segunda emisión devuelve el boleto existente | Esencial |
| RF-11 | Cancelar una reserva confirmada sin boleto antes de la salida; conservarla como cancelada con su asiento histórico y liberar su cupo y asiento para otra reserva | Apoyo a la demostración |

## Requisitos no funcionales

Estos criterios describen la calidad observable esperada; su comprobación corresponde a fases posteriores.

| ID | Requisito | Criterio de comprobación previsto |
|---|---|---|
| RNF-01 | Integridad | Una operación rechazada no deja registros parciales ni consume cupos o asientos; dos solicitudes simultáneas por el último cupo permiten como máximo una confirmación y nunca asignan el mismo asiento a dos reservas confirmadas del mismo vuelo |
| RNF-02 | Persistencia | Tras reiniciar la aplicación, los registros creados y sus relaciones siguen disponibles |
| RNF-03 | Comprensibilidad | Formularios, etiquetas y errores en español; un rechazo identifica la regla incumplida |
| RNF-04 | Rendimiento de demostración | En el equipo documentado para la presentación, consultas y altas habituales completan en hasta 2 segundos para 100 vuelos y 1.000 reservas, sin contar arranque; medir antes de aceptar |
| RNF-05 | Coherencia temporal | Los horarios representan instantes inequívocos; una misma comparación temporal produce el mismo resultado para todos los registros |
| RNF-06 | Uso académico de datos | Emplear pasajeros ficticios y evitar publicar documentos o contactos reales en modelos y evidencias |

La evaluación final deberá distinguir los criterios satisfechos de los pendientes; en esta entrega no se afirma ninguna medición de la aplicación.
