# PSM — Java/Spring Boot y PostgreSQL

Estado: metamodelo e instancia de referencia construidos; validación EMF y comprobaciones de coherencia ejecutadas. **La instancia de referencia fue escrita manualmente para diseñar el destino; no es salida de ATL.** La [transformación M2M](../../transformations/m2m/README.md) produce `modeling/models/generated/spring-psm.xmi`, y [Acceleo M2T](../../transformations/m2t/README.md) genera código desde esa salida. Su integración con PostgreSQL todavía no se ha ejecutado.

## Archivos

| Archivo | Finalidad |
|---|---|
| [spring-psm.ecore](../../modeling/metamodels/spring-psm.ecore) | Metamodelo de configuración tecnológica y componentes generables |
| [spring-psm-referencia.xmi](../../modeling/models/spring-psm-referencia.xmi) | Diseño de referencia de la aplicación: ocho entidades con sus componentes |
| [ValidatePsm.java](../../infrastructure/validation/ValidatePsm.java) | Verifica estructura con EMF y coherencia tecnológica con el PIM; no transforma modelos |
| [validate-psm.ps1](../../infrastructure/validation/validate-psm.ps1) | Compila y ejecuta el verificador usando los JAR EMF de Obeo |
| [Decisiones y mapeo](01-decisiones-y-mapeo.md) | Arquitectura, reglas de correspondencia y contrato de las próximas transformaciones |

## Qué representa el metamodelo

El PSM tiene 13 EClasses y 6 EEnums:

| EClass | Información tecnológica |
|---|---|
| Application | Coordenadas Maven, paquete Java, JDK, versión Spring Boot y namespace del PIM |
| Database | Producto, versión mayor objetivo, esquema, modo DDL y zona horaria |
| Entity | Nombre Java, tabla y clase de origen; contiene campos, asociaciones, restricciones y componentes |
| Attribute | Campo y columna, tipos Java/SQL, nulabilidad, identidad, generación, longitud y validaciones básicas |
| Association | Tipo JPA, destino, cardinalidad, extremo opuesto, propietario, FK, mappedBy, fetch y borrado |
| EnumType / EnumLiteral | Enumeración Java y persistencia STRING |
| Repository | Nombre y tipo base Spring Data JPA |
| Service | Nombre, transacción y políticas de negocio requeridas |
| Policy | Estrategia a implementar, reglas CIM y descripción del comportamiento |
| RestController | Nombre, ruta base y operaciones |
| Operation | Nombre, verbo HTTP, ruta relativa y tipo de operación |
| SqlConstraint | Restricción UNIQUE, CHECK o índice único parcial, columnas, predicado y reglas CIM |

Los EEnums son JavaType, JpaRelation, HttpMethod, OperationKind, ConstraintKind y PolicyKind. Los hijos de Application y Entity usan contención; los destinos de asociaciones y enumType son referencias dentro de la misma aplicación. `opposite` es una referencia explícita entre asociaciones; el verificador exige reciprocidad. No se confunde la composición del metamodelo PSM con la propiedad de una relación JPA.

Policy no contiene código Java y no ejecuta todavía reglas. Su `kind` permite que Acceleo seleccione una plantilla especializada; su descripción permite revisar el comportamiento esperado. El texto descriptivo por sí solo nunca se presentará como implementación funcional.

## Referencia preparada

`spring-psm-referencia.xmi` contiene 8 entidades, 27 atributos (incluidas 8 claves tecnológicas), 16 extremos de asociación, un enum, 8 repositorios, 8 servicios, 8 controladores, 26 operaciones REST y 16 restricciones/índices SQL. SistemaVuelos es organización del PIM y no se convierte en una tabla.

Las dependencias y políticas están modeladas; no se han descargado Spring Boot ni PostgreSQL para ejecutar una aplicación. No se guardan contraseñas ni cadenas de conexión locales en el PSM.

## Revisión en Obeo

1. Guardar el trabajo abierto y pulsar **F5 / Refresh** sobre el proyecto `ec.edu.ucuenca.mda.vuelos.modeling`.
2. Abrir `metamodels/spring-psm.ecore` con **Sample Ecore Model Editor** y validar el paquete `springpsm`.
3. Para visualizar el metamodelo, usar **Initialize Ecore Diagram… → Entities in a Class Diagram** sobre ese Ecore. Guardar un `.aird` diferente del diagrama del PIM; la distribución del PIM no se modifica.
4. Abrir `models/spring-psm-referencia.xmi` con **Sample Reflective Ecore Model Editor**.
5. Expandir Application → entities → Reserva. Revisar `numeroAsiento`, las asociaciones `pasajero` y `vuelo`, los índices parciales y las políticas del servicio.
6. Revisar Boleto → asociación `reserva`: ONE_TO_ONE, propietaria, FK `reserva_id`; Reserva → `boleto`: inversa, `mappedBy=reserva`, `0..1`.
7. Seleccionar la raíz Application y ejecutar **Validate**. Se espera éxito estructural. Las comprobaciones tecnológicas adicionales del verificador Java no están registradas como OCL del PSM en el IDE.

El diagrama representa el **metamodelo tecnológico**, no las ocho clases de negocio. Los objetos Entity dentro del XMI representan las futuras clases Java. No se creó manualmente un `.aird` que aparente haber sido renderizado.

## Verificación reproducible

```powershell
& './infrastructure/validation/validate-psm.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

Se puede pasar `-ModelPath` para validar la futura salida ATL. El verificador corresponde al perfil académico de ocho entidades y un enum; si se amplía ese alcance, también habrá que revisar sus comprobaciones de cantidad. Añadir un atributo al PIM no exige cambiar esas cantidades.

Comprobaciones ejecutadas el 15 de noviembre de 2026:

- Metamodelo PSM y referencia XMI: Diagnostician con severidad OK.
- Resolución de referencias: sin proxies pendientes ni errores de carga.
- Correspondencia de campos, tipos, destinos y cardinalidades con el Ecore PIM.
- Claves IDENTITY, nombres de columnas, propietarios JPA, mappedBy, extremos opuestos, enum por nombre y rutas HTTP coherentes.
- Columnas usadas por restricciones de unicidad existentes; índices de reservas filtrados por CONFIRMADA.
- Serialización y recarga de la referencia: válidas.
- Control negativo estructural, asociación sin destino: rechazado por EMF.
- Control negativo tecnológico, dos atributos sobre columna `id`: rechazado por el verificador.

Los controles negativos modifican copias en memoria. Estas pruebas **no compilan Spring Boot ni ejecutan SQL**; la sintaxis y el comportamiento del esquema se probarán al generar la aplicación.

## Próxima fase

ATL produce un XMI separado de la referencia, validado con el mismo verificador y comparado semánticamente con el diseño. Acceleo ya lo utiliza para generar el backend; consultar las guías [M2M](../../transformations/m2m/README.md) y [M2T](../../transformations/m2t/README.md).
