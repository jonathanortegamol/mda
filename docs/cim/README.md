# CIM — Índice y propósito

**Estado:** análisis revisado; el usuario autorizó construir el PIM tras incorporar los asientos. Consultar la [guía del PIM](../pim/README.md) para la fase actual.

El Computation Independent Model expresa qué necesita el negocio y bajo qué reglas opera. Aquí no se definen clases Ecore, atributos técnicos, anotaciones, tablas, APIs ni generadores. Las entidades conceptuales son vocabulario de análisis y deberán traducirse explícitamente al PIM en la siguiente fase.

| Documento | Finalidad |
|---|---|
| [01 — Alcance y requisitos](01-alcance-y-requisitos.md) | Objetivos, límites, actores y requisitos verificables |
| [02 — Casos de uso y procesos](02-casos-de-uso-y-procesos.md) | Interacciones, condiciones, excepciones y flujo de negocio |
| [03 — Reglas del negocio](03-reglas-del-negocio.md) | Invariantes, estados y escenarios válidos e inválidos |
| [04 — Modelo conceptual](04-modelo-conceptual.md) | Conceptos, significado, relaciones y multiplicidades del negocio |
| [05 — Revisión y trazabilidad](05-revision-y-trazabilidad.md) | Cobertura, decisiones propuestas y condiciones para pasar al PIM |

Los identificadores OBJ, ACT, RF, RNF, CU, RN y D permiten vincular objetivos, actores, requisitos, casos de uso, reglas y decisiones. Las reglas son normativas para el alcance propuesto; las decisiones que las sustentan todavía requieren revisión.

## Objetivos del negocio

- **OBJ-01:** mantener información coherente de aeropuertos, aerolíneas, aviones y rutas para programar vuelos.
- **OBJ-02:** ofrecer información de vuelos con cupos y registrar pasajeros y reservas sin duplicidades ni sobreventa.
- **OBJ-03:** emitir y consultar boletos que acrediten una reserva confirmada.

La reproducibilidad de las transformaciones es un objetivo académico transversal del proyecto, no un proceso del negocio aeronáutico. Se especificará y verificará en las fases posteriores.
