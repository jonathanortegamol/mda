# Decisiones del PSM y contrato de transformación

## Plataforma objetivo

Se propone Java 21, Spring Boot **3.5.16**, Spring MVC, Spring Data JPA y Bean Validation con namespace `jakarta.*`, Maven y PostgreSQL 17. La versión exacta de Spring se fija en la referencia para una generación posterior reproducible. Sus [requisitos oficiales de la rama 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html) admiten Java 21 y exigen Maven 3.6.3 o posterior. Esto verifica requisitos publicados, no una compilación de esta aplicación; las dependencias se resolverán y fijarán en M2T. PostgreSQL 17 es un objetivo de diseño y no una afirmación sobre una instalación local encontrada.

La versión del backend no tiene que coincidir con la versión del IDE: el generador produce un proyecto Maven separado. Se conserva Acceleo 3.7.15 y ATL 4.12 para las transformaciones. No se utiliza Lombok para evitar una dependencia adicional en el código generado.

## Decisiones concretas

| Decisión | Representación o consecuencia |
|---|---|
| Una entidad persistente por concepto de negocio | Ocho Entity; SistemaVuelos no se persiste |
| Identidad tecnológica independiente de códigos | `id: Long`, columna bigint, PK no nula y estrategia IDENTITY; códigos del negocio siguen siendo únicos |
| Texto | String → varchar(255), no nulo y validación de texto no vacío; longitud modelada |
| Enteros | Integer → integer; capacidad y asiento positivos |
| Tiempo | EDate → OffsetDateTime → timestamp with time zone; normalización de intercambios a UTC |
| Estado de reserva | Enum EstadoReserva con `@Enumerated(STRING)`, varchar(32) y CHECK de literales |
| Referencias obligatorias | FK no nula, según lower=1; borrado RESTRICT |
| Relaciones inversas | ONE_TO_MANY con mappedBy; Reserva.boleto es ONE_TO_ONE inversa |
| Boleto-reserva | Boleto posee reserva_id no nulo y único; Reserva tiene boleto opcional |
| Acceso a datos | JpaRepository por entidad; métodos especializados requeridos por políticas |
| Servicios | Transaccionales; validaciones antes de confirmar cambios |
| API | Controladores REST sobre servicios; sin edición o eliminación general |
| Persistencia inicial | Generar esquema SQL y configuración ddl-auto=validate; aplicar esquema antes del arranque mediante instrucciones reproducibles de la fase M2T |

La elección temporal sigue los [tipos de PostgreSQL 17](https://www.postgresql.org/docs/17/datatype-datetime.html): `timestamp with time zone` representa un instante y no conserva la zona original. OffsetDateTime evita trasladar el horario del vuelo a un LocalDateTime sin contexto. El comportamiento de Hibernate/JDBC se verificará con el esquema generado.

JPA distingue el [propietario y el mappedBy de la asociación inversa](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/ManyToOne.html). El almacenamiento del enum se declara STRING expresamente: no se utiliza el ordinal predeterminado de [Enumerated](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/enumerated).

Se modela LAZY como preferencia para las asociaciones. No se presupone que todas las asociaciones uno a uno inversas puedan diferirse sin configuración adicional del proveedor. La API futura utilizará DTO con identificadores de relaciones para evitar ciclos de serialización y exposición directa de entidades; los DTO podrán derivarse de campos y asociaciones, sin otro metamodelo en esta fase.

## Relaciones con propietario

| Entidad/campo propietario | Tipo | FK | Extremo inverso |
|---|---|---|---|
| Avion.aerolinea | MANY_TO_ONE | aerolinea_id | Aerolinea.aviones |
| Ruta.origen | MANY_TO_ONE | origen_id | Aeropuerto.rutasOrigen |
| Ruta.destino | MANY_TO_ONE | destino_id | Aeropuerto.rutasDestino |
| Vuelo.ruta | MANY_TO_ONE | ruta_id | Ruta.vuelos |
| Vuelo.avion | MANY_TO_ONE | avion_id | Avion.vuelos |
| Reserva.pasajero | MANY_TO_ONE | pasajero_id | Pasajero.reservas |
| Reserva.vuelo | MANY_TO_ONE | vuelo_id | Vuelo.reservas |
| Boleto.reserva | ONE_TO_ONE | reserva_id UNIQUE | Reserva.boleto |

No se añaden cascadas REMOVE ni orphanRemoval para borrar registros históricos. Las operaciones de alta reciben identificadores de objetos existentes y los servicios resuelven sus referencias; no se persisten grafos arbitrarios enviados por el cliente.

## Reglas en PostgreSQL y en servicios

El PSM incluye unicidad de identificadores, del par dirigido de ruta y de reserva_id en Boleto; CHECK para capacidad positiva, extremos distintos, llegada posterior, asiento positivo y literales de estado.

Las reservas necesitan **índices únicos parciales**, con predicado `estado = 'CONFIRMADA'`:

- `(vuelo_id, numero_asiento)` para no duplicar asientos activos.
- `(vuelo_id, pasajero_id)` para no duplicar pasajeros activos.

No se deben sustituir por UNIQUE sobre todas las filas ni por UNIQUE incluyendo estado: eso impediría reutilizar asientos o conservar múltiples cancelaciones históricas. Esta decisión se apoya en los [índices parciales de PostgreSQL](https://www.postgresql.org/docs/17/indexes-partial.html). Los índices se generarán en SQL; una anotación JPA de unicidad general no expresa este filtro.

Capacidad total, rango superior del asiento y validez de la reserva de un boleto cruzan tablas; no se presentan como CHECK locales. Requieren validaciones de servicio. La generación futura deberá implementar las reglas concretas: no basta con copiar los nombres de OCL a comentarios Java.

| Operación | Política transaccional prevista |
|---|---|
| Programar vuelo | Bloquear fila del avión; comprobar salida futura, llegada posterior y ausencia de solapamientos antes de insertar |
| Reservar | Bloquear fila del vuelo; verificar salida, pasajero, duplicidad y cupo; asignar menor asiento libre y guardar en una sola transacción |
| Cancelar | Bloquear vuelo y luego reserva; comprobar plazo y ausencia de boleto; cambiar estado conservando asiento histórico |
| Emitir | Bloquear vuelo y luego reserva, igual que cancelación; devolver boleto existente o emitir para confirmada antes de salida |

Los bloqueos se concretarán con Spring Data JPA en M2T y se probarán con solicitudes simultáneas. El orden compartido vuelo → reserva evita que emisión y cancelación implementen secuencias incompatibles. PostgreSQL también debe rechazar duplicados por los índices, aunque la aplicación haya comprobado antes. Los fallos se traducirán a errores de negocio, sin resultados parciales.

## API prevista

Cada entidad tiene GET de colección y GET `/{id}`. Las altas de catálogos, pasajero y vuelo usan POST sobre su colección. Se conservan los nombres de entidad del PIM para formar clases, componentes y rutas; por ello Avion usa `/api/aviones` y Boleto `/api/boletos`.

| Operación de negocio | Endpoint modelado |
|---|---|
| Consultar vuelos disponibles | GET /api/vuelos/disponibles |
| Reservar | POST /api/reservas |
| Cancelar | POST /api/reservas/{id}/cancelacion |
| Emitir boleto de una reserva | POST /api/boletos/reserva/{reservaId} |

La búsqueda por identificadores de negocio y los filtros por pasajero, vuelo, origen y destino se implementarán como parámetros de las operaciones LIST en M2T. La instancia describe las rutas y tipos de operación; aún no es una especificación OpenAPI ni define todos los DTO y códigos HTTP.

## Contrato para ATL: qué es el modelo de entrada

El **esquema del dominio `vuelos.ecore` es el modelo PIM que se transforma**. Ese archivo es a su vez un modelo conforme al metametamodelo Ecore. La transformación ATL se planteará con `IN : ECORE` y `OUT : SPRINGPSM`. El archivo `vuelos-ejemplo.xmi` contiene datos del negocio y sirve para validación; no es la entrada para inferir qué clases generar.

| Elemento del PIM | Elemento esperado del PSM |
|---|---|
| EPackage vuelos | Application y configuración tecnológica explícita |
| EClass de negocio | Entity + tabla + Repository + Service + RestController |
| EClass SistemaVuelos y referencias de contención | Organización del modelo; se excluyen de tablas y relaciones JPA |
| EAttribute EString/EInt/EDate | Attribute STRING/INTEGER/OFFSET_DATE_TIME y columna SQL correspondiente |
| EAttribute de EEnum | Attribute ENUM con referencia a EnumType |
| EEnum y literales | EnumType y EnumLiteral, almacenamiento STRING |
| EReference no contenida con opuesta | Association con destino, opuesta, cardinalidad y propietario JPA |
| Identidad técnica | Attribute id generado por política de plataforma, sin modificar PIM |
| Restricciones conocidas y reglas CIM | SqlConstraint y Policy según reglas explícitas de correspondencia |

Convenciones: nombres de columnas en snake_case, tablas y rutas de colección mediante una tabla explícita de plurales del perfil del proyecto; propietario en el extremo singular obligatorio, con Boleto.reserva como caso uno a uno; FK basada en nombre del campo. Estas reglas se deben escribir en ATL, no dejar implícitas en scripts externos.

La transformación no será un traductor universal de OCL a Java o SQL. Reconocerá las restricciones del perfil académico y rechazará o señalará las no soportadas antes de generar una aplicación. Para datos nuevos y cambios estructurales se deberá comprobar su propagación, no mantener una lista fija de atributos en las plantillas. El ejemplo de propagación previsto es añadir `telefono` a Pasajero: un Attribute STRING nuevo debe aparecer en el PSM y posteriormente en código, esquema y entrada/salida de datos.

`spring-psm-referencia.xmi` es una referencia de diseño escrita manualmente, no una prueba de M2M. La salida ATL futura se guardará aparte, con ejecución reproducible y validación del resultado. No se ha creado un script que sustituya o simule esa transformación.
