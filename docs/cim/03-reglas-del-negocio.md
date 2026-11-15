# 03 — Reglas del negocio

## Vocabulario de estados y tiempo

Una reserva está **CONFIRMADA** o **CANCELADA**. «Activa» equivale exclusivamente a confirmada. La creación produce una confirmada; la única transición es confirmada → cancelada, permitida sin boleto y antes de la salida. No existe estado pendiente, expiración, reactivación ni reembolso. Una reserva confirmada continúa registrada como tal después del vuelo; las restricciones de ocupación se interpretan para su vuelo.

Los intervalos de vuelo incluyen la salida y excluyen la llegada: `[salida, llegada)`. Dos vuelos que se tocan exactamente en la llegada/salida no se solapan. Los horarios representan instantes comparables, independientemente del lugar de origen. Para simplificar la demostración se propone presentar todos los horarios en una misma zona horaria, documentada en las fases posteriores.

## Catálogo de reglas

| ID | Regla | Operación o contexto |
|---|---|---|
| RN-01 | Una ruta tiene exactamente un origen y un destino existentes y diferentes | Crear ruta |
| RN-02 | La llegada de un vuelo es estrictamente posterior a su salida | Programar vuelo |
| RN-03 | La cantidad de reservas confirmadas de un vuelo no supera la capacidad de su avión | Confirmar/cancelar reserva; integridad global |
| RN-04 | Un pasajero tiene como máximo una reserva confirmada para el mismo vuelo; las canceladas no impiden reservar otra vez | Confirmar reserva |
| RN-05 | Cada vuelo tiene exactamente una ruta y un avión existentes; sus extremos se obtienen de la ruta | Programar vuelo |
| RN-06 | Cada reserva tiene exactamente un pasajero y un vuelo existentes | Crear reserva |
| RN-07 | Cada boleto corresponde a exactamente una reserva confirmada, y cada reserva tiene a lo sumo un boleto | Emitir boleto; integridad global |
| RN-08 | Un avión no tiene vuelos con intervalos superpuestos: para dos vuelos distintos del avión, salida1 < llegada2 y salida2 < llegada1 indica conflicto | Programar vuelo |
| RN-09 | La capacidad del avión es un entero positivo | Registrar avión |
| RN-10 | Código de aeropuerto, código de aerolínea, matrícula, documento de pasajero, código de vuelo, localizador y número de boleto son únicos dentro de su concepto | Altas y emisión |
| RN-11 | No se repite una ruta con el mismo par ordenado origen-destino; la ruta inversa es distinta | Crear ruta |
| RN-12 | Un vuelo se programa con salida futura; reservar, emitir o cancelar exige que el momento de la operación sea anterior a la salida | Operaciones temporales |
| RN-13 | Solo se cancela una reserva confirmada sin boleto; una cancelada no ocupa cupo y no se reactiva | Cancelar reserva |
| RN-14 | Todo avión pertenece a exactamente una aerolínea; la aerolínea operadora del vuelo es la de su avión | Registrar avión; consultar vuelo |
| RN-15 | Los datos conceptuales listados como obligatorios no están vacíos; no se eliminan registros referenciados | Altas e integridad |
| RN-16 | Toda reserva tiene un número de asiento entero entre 1 y la capacidad del avión de su vuelo | Integridad de reservas |
| RN-17 | Dos reservas confirmadas de un mismo vuelo no comparten número de asiento; ese número puede repetirse en vuelos distintos o en reservas canceladas | Confirmar reserva; integridad global |
| RN-18 | Al confirmar se asigna automáticamente el menor asiento libre, considerando ocupados solo los de reservas confirmadas del vuelo; sin asiento libre se rechaza toda la operación | Crear reserva |
| RN-19 | Cancelar libera el asiento pero conserva su número histórico; el boleto muestra el asiento de su reserva, sin nueva asignación ni cambio de asiento | Cancelar, emitir y consultar |

Disponibilidad = capacidad del avión − número de reservas confirmadas del vuelo. Emitir un boleto no altera esta cuenta. La consulta de «vuelos disponibles» exige además una salida futura.

Con RN-16 y RN-17, los cupos disponibles coinciden con los números de asiento libres. El asiento es información de la reserva, no una entidad independiente ni un identificador único global. No se cambia después de asignarlo.

RN-08 comprueba solo solapamiento temporal: no verifica ubicación del avión entre vuelos, tiempo de limpieza, embarque o mantenimiento. La coherencia logística entre aeropuertos queda fuera del alcance.

## Ejemplos para revisar y reutilizar en validación

Son escenarios especificados, **no resultados de validaciones ejecutadas**. En ejemplos temporales se supone un momento de operación anterior a la salida.

| Escenario | Resultado esperado | Reglas |
|---|---|---|
| Ruta Cuenca → Quito | Válido si ambos aeropuertos existen | RN-01 |
| Ruta Cuenca → Cuenca | Rechazo | RN-01 |
| Salida 10:00 y llegada 11:00 del mismo día | Válido | RN-02 |
| Salida 10:00 y llegada 10:00 | Rechazo | RN-02 |
| Avión de capacidad 2 con dos reservas confirmadas | Válido, sin cupos | RN-03 |
| Tercera confirmación en ese vuelo | Rechazo, permanecen dos | RN-03 |
| Dos confirmadas del mismo pasajero para el mismo vuelo | Rechazo de la segunda | RN-04 |
| Una cancelada y una nueva confirmada del mismo pasajero y vuelo | Válido | RN-04, RN-13 |
| Reserva sin pasajero o sin vuelo | Inválido | RN-06 |
| Emitir para una reserva cancelada | Rechazo | RN-07 |
| Repetir emisión de una reserva con boleto | Se devuelve el boleto existente | RN-07 |
| Mismo avión: vuelos 10:00–11:00 y 10:30–12:00 | Rechazo del segundo | RN-08 |
| Mismo avión: vuelos 10:00–11:00 y 11:00–12:00 | Válido bajo la simplificación temporal | RN-08 |
| Dos solicitudes simultáneas por el último cupo | Solo una se confirma | RN-03 |
| Cancelar una reserva sin boleto, antes de salir | Se libera un cupo | RN-13 |
| Reservar exactamente en el instante de salida | Rechazo | RN-12 |
| Primera reserva de un vuelo vacío de capacidad 2 | Se asigna el asiento 1 | RN-16, RN-18 |
| Segunda reserva de otro pasajero en ese vuelo | Se asigna el asiento 2 | RN-17, RN-18 |
| Asiento 0 o 3 en un avión de capacidad 2 | Modelo inválido | RN-16 |
| Dos reservas confirmadas con asiento 1 en el mismo vuelo | Modelo inválido | RN-17 |
| Asiento 1 en dos vuelos diferentes | Válido | RN-17 |
| Se cancela la reserva del asiento 1 y luego reserva otro pasajero | La nueva reserva recibe el asiento 1; la cancelada conserva el mismo número histórico | RN-18, RN-19 |
| Dos solicitudes simultáneas con asientos 1 y 2 libres | Pueden confirmarse ambas con asientos distintos; ninguna comparte asiento | RN-17, RN-18 |
| Emitir y volver a consultar un boleto | Muestra el mismo asiento de su reserva, sin consumir otro | RN-19 |

## Orientación para fases posteriores

RN-01 a RN-11, RN-14 y la integridad estructural de RN-15 son candidatas a restricciones declarativas del modelo, incluidas cardinalidades y futuras restricciones OCL. La comparación de instantes de RN-02 y RN-08 exige definir cómo se representará el tiempo y verificar el soporte de la herramienta elegida. Esta es una clasificación preliminar, no una implementación OCL.

RN-12 depende del momento de operación; RN-13 también depende de una transición y del estado anterior. Requerirán validación de operaciones en la aplicación. RN-03, RN-04, RN-07 y RN-10 deberán protegerse además al confirmar cambios concurrentes: validar un modelo aislado no garantiza integridad ante solicitudes simultáneas. La unicidad y referencias también deberán preservarse en la persistencia. La fase PIM/OCL concretará qué se expresa en cada capa y cómo probarlo.

RN-16 y RN-17 son candidatas a invariantes OCL sobre rango y unicidad por vuelo. RN-18 es una política de asignación durante una operación; RN-19 combina conservación histórica, liberación y consulta. Estas dos últimas requieren validaciones de comportamiento adicionales. La asignación y confirmación deben proteger RN-17 y RN-18 también ante concurrencia; no basta validar el modelo antes de guardar.
