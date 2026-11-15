# MDA — Sistema de Gestión de Vuelos

Proyecto académico de Nuevos Paradigmas de Programación, Ingeniería en Ciencias de la Computación, Universidad de Cuenca.

El objetivo es demostrar una cadena reproducible **CIM → PIM → PSM → M2M → M2T → código generado → aplicación funcional**, incluida la propagación de un cambio del modelo hasta la aplicación. La aplicación final gestionará vuelos, pasajeros, reservas individuales y boletos.

## Estado actual

**Prioridad actual: la interfaz gráfica y la demostración visual de MDA.** Abre [generated/frontend/index.html](generated/frontend/index.html) en Chrome o Edge. Incluye datos ficticios y permite programar vuelos, registrar pasajeros, reservar con asiento y emitir un boleto sin arrancar el backend. La [guía de pantallas](docs/frontend/README.md) explica el recorrido y la propagación de un campo del modelo.

El CIM/PIM y las restricciones OCL están construidos y revisados. ATL produce el PSM y Acceleo genera el backend: 56 archivos, con entidades, DTO, repositorios, servicios, controladores, pruebas, configuración y SQL. La propagación de un atributo del PIM hasta fuentes y esquema está comprobada. Las fuentes compilan y pasan 12 pruebas unitarias con dependencias instaladas de Boot 3.5.3; el empaquetado Maven objetivo 3.5.16 está pendiente por dependencias ausentes y conexión a Maven Central denegada. No se ha ejecutado todavía la aplicación con PostgreSQL.

Acceleo también genera cinco archivos del frontend, incluyendo los ocho formularios HTML y las definiciones de campos y listados derivadas del PSM. Se comprobaron 27 casos base y 29 en una variante con telefono, además de la repetibilidad de la generación. La revisión visual automatizada quedó bloqueada por Computer Use; la integración API tampoco se ha ejecutado todavía.

Comenzar por el [índice del CIM](docs/cim/README.md).

Para revisar la nueva fase y abrir los archivos en Obeo, consultar la [guía del PIM](docs/pim/README.md).

Para ejecutar y demostrar las reglas, consultar las [restricciones OCL](modeling/constraints/README.md).

El diseño tecnológico está en la [guía del PSM](docs/psm/README.md). La generación se reproduce con las guías [M2M/ATL](transformations/m2m/README.md) y [M2T/Acceleo](transformations/m2t/README.md).

## Etapas previstas

| Etapa | Propósito y evidencia esperada | Estado |
|---|---|---|
| CIM | Alcance, actores, requisitos, reglas, procesos y conceptos del negocio | Revisado; construcción del PIM autorizada |
| PIM | Metamodelo Ecore y modelo de instancia representativo, sin decisiones de persistencia | Validado con EMF; diagrama abierto por el usuario |
| Restricciones | OCL evaluable, ejemplos válidos e inválidos e instrucciones de validación | 23 invariantes compiladas; 12 modelos evaluados; casos positivo/negativo comprobados en IDE |
| PSM | Representación explícita de decisiones Java/Spring Boot y PostgreSQL | Metamodelo y referencia manual validados; pendiente de revisión visual |
| M2M | Transformación ejecutable PIM → PSM y salida inspeccionable | ATL compilado y ejecutado; equivalencia, propagación y reproducibilidad comprobadas |
| M2T | Plantillas Acceleo y generación reproducible desde el modelo | Backend y frontend generados; campos propagados hasta formularios HTML y código |
| Aplicación | Flujo funcional utilizando los componentes generados | Interfaz con adaptador local disponible; integración backend/PostgreSQL pendiente |
| Demostración | Cambio del modelo, validación, regeneración y comprobación funcional | Pendiente |

Se ha registrado la [inspección del entorno local](docs/architecture/01-entorno-modelado.md): Obeo 12.0, Eclipse 4.37, EMF, Sirius, OCL clásico, Acceleo 3.7.15 y ATL 4.12. EMF, OCL, ATL y Acceleo ya se ejecutaron en la cadena. Se usa JDK 21 explícitamente para compilar; Maven está instalado. La integración PostgreSQL/HTTP queda para la fase de aplicación.

## Organización

```text
docs/cim/                 Análisis del negocio revisado
docs/pim/                 Decisiones del PIM, validación y guía de Obeo
docs/psm/                 Metamodelo tecnológico, referencia y contrato M2M
docs/frontend/            Recorrido visual y prueba de propagación a pantallas
docs/architecture/        Inventario y compatibilidad del entorno
modeling/                Proyecto Eclipse con metamodelo e instancia
transformations/m2m/     Fuente ATL, perfil soportado y guía de ejecución
transformations/m2t/     Plantilla Acceleo, proyecto Eclipse y guía de ejecución
generated/backend/      Backend producido por Acceleo, sin código manual
generated/frontend/     Pantallas generadas por Acceleo; demostración local y adaptador API
infrastructure/         Verificador manual EMF; infraestructura futura
```

La salida ATL está en `modeling/models/generated/`, separada de la referencia manual. El código de aplicación está en `generated/backend/` y `generated/frontend/`, con manifiestos de hashes y generado por Acceleo. Los lanzadores, servidores de vista previa y verificadores manuales están en `infrastructure/`. El adaptador local de la interfaz se identifica como demostración, sin atribuirle ejecución de Spring Boot o PostgreSQL.

## Cómo revisar esta entrega

Leer los documentos del CIM en el orden del índice y resolver las [decisiones pendientes](docs/cim/05-revision-y-trazabilidad.md). Los diagramas están escritos en Mermaid dentro de Markdown; requieren un visor compatible para su representación gráfica. Su fuente textual permanece legible sin ese visor. Esta fase no requiere Eclipse ni servicios de base de datos.

Para revisar ahora las pantallas, seguir la [guía de la interfaz](docs/frontend/README.md). Las fuentes están en las plantillas backend.mtl y frontend.mtl del proyecto Acceleo.

Para completar la compilación, preparar PostgreSQL y probar el flujo REST, seguir la [guía de ejecución](infrastructure/runtime/README.md). Incluye configuración Docker y un script de comprobación; el flujo integrado aún está pendiente de ejecución.
