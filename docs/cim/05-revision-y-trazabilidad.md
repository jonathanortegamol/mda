# 05 — Revisión y trazabilidad

## Cobertura del análisis

| Objetivo | Requisitos funcionales | Casos de uso | Reglas principales | Conceptos |
|---|---|---|---|---|
| OBJ-01 | RF-01 a RF-05 | CU-01, CU-02 | RN-01, RN-02, RN-05, RN-08 a RN-12, RN-14, RN-15 | Aeropuerto, Aerolínea, Avión, Ruta, Vuelo |
| OBJ-02 | RF-06 a RF-09, RF-11 | CU-03, CU-04, CU-05, CU-07, CU-08 | RN-03, RN-04, RN-06, RN-09, RN-10, RN-12, RN-13, RN-15 a RN-19 | Pasajero, Vuelo, Reserva, Avión |
| OBJ-03 | RF-10 | CU-06, CU-07 | RN-07, RN-10, RN-12, RN-13, RN-15, RN-19 | Boleto, Reserva, Vuelo |

RNF-01 protege especialmente CU-05, CU-06 y CU-08; RNF-02 y RNF-03 aplican a todas las operaciones. RNF-04 cubre consultas y altas habituales, RNF-05 cubre CU-02, CU-04, CU-05, CU-06 y CU-08, y RNF-06 aplica a los datos de pasajeros en toda la demostración.

## Decisiones propuestas para revisión

La propuesta inicial recibió conformidad general del usuario. La incorporación de asientos simples (D-12) fue autorizada explícitamente mediante «hazlo». Se mantiene la revisión del CIM actualizado antes de comenzar el PIM.

| ID | Propuesta y justificación | Consecuencia que debe revisarse |
|---|---|---|
| D-01 | Reserva individual de un solo vuelo directo | Se excluyen grupos, conexiones y un concepto adicional de itinerario |
| D-02 | Confirmación inmediata sin pagos ni estado pendiente | Reservar ocupa cupo de inmediato y no hay vencimiento automático |
| D-03 | Un boleto por reserva, sin precio, que muestra el asiento de la reserva | La emisión acredita la reserva y no modifica cupos ni asigna otro asiento |
| D-04 | Cancelación solo sin boleto y antes de la salida | Permite demostrar liberación de cupos sin reembolsos ni anulación de boletos |
| D-05 | Sin edición/eliminación general ni reprogramación | Evita invalidar reservas y horarios después de registrarlos |
| D-06 | Aerolínea derivada del avión, aeropuertos derivados de la ruta | No se modelan vuelos compartidos ni aerolínea comercial distinta de la operadora |
| D-07 | Intervalos sin solapamiento y margen de rotación cero | Vuelos consecutivos son aceptables; no se comprueba continuidad geográfica |
| D-08 | Horarios inequívocos con presentación en una zona común | Simplifica comparación y demostración; representación concreta se decide en PIM |
| D-09 | Identificadores de negocio únicos, incluido código de operación de vuelo | No se reutiliza el mismo código en vuelos de diferentes días |
| D-10 | Atención mediada por agente sin autenticación de roles | El pasajero es actor del negocio; los roles no requieren entidades adicionales |
| D-11 | Conservar los ocho conceptos originales | Todos aportan relaciones o reglas útiles a la demostración MDA |
| D-12 | Asignar automáticamente el menor asiento libre del 1 a la capacidad, como dato de Reserva | Autorizado por el usuario; sin entidad Asiento, mapa, clases, elección manual ni cambio de asiento; al cancelar se libera y se conserva el número histórico |

## Revisión previa al PIM

1. Revisar las decisiones D-01 a D-12 en el CIM actualizado, especialmente cancelación, asientos, horarios y exclusiones logísticas.
2. Revisar que las ocho entidades y sus multiplicidades reflejan el negocio acordado.
3. Revisar los criterios de aceptación y los escenarios válidos e inválidos, incluidos duplicidad, último cupo y emisión repetida.
4. Acordar si los criterios RNF-04 y los campos obligatorios son adecuados para la presentación.
5. Registrar la aprobación o los ajustes solicitados en este documento antes de construir Ecore.

**Registro de revisión:** conformidad general del usuario con el análisis inicial e incorporación de asientos simples autorizada. Posteriormente el usuario solicitó «valida nuevamente y empieza con la construccion», autorizando la fase PIM. Evidencia y decisiones en la [guía del PIM](../pim/README.md).

## Preparación técnica de la siguiente fase

Una vez aprobado el CIM, verificar primero versiones y requisitos oficiales de Eclipse/Obeo, EMF, OCL, Acceleo, ATL y JDK disponibles. Registrar una matriz de compatibilidad y una prueba mínima de apertura/validación y ejecución con la combinación elegida. Esta comprobación no se ha realizado en el CIM y debe preceder a la implementación.

Después se definirán el metamodelo PIM, su organización y contención, tipos temporales, enumeración de estados y modelos de ejemplo. También se concretará la distribución de reglas entre estructura, OCL y validaciones de operaciones. El PSM y las transformaciones requieren sus propias revisiones posteriores.

## Evidencia de esta entrega

Se han creado documentos Markdown y fuentes Mermaid del CIM. La revisión automática de archivos comprueba enlaces locales, identificadores y delimitadores de diagramas; no evalúa reglas OCL ni certifica conformidad UML. Los diagramas no se han renderizado en Eclipse/Obeo. Los escenarios son criterios para pruebas futuras, no ejecuciones de una aplicación.
