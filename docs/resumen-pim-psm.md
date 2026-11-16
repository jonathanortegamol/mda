# Resumen del PIM y el PSM

Guía rápida para responder preguntas sobre los dos modelos de la cadena MDA.

```
PIM  vuelos.ecore ──(ATL: PimToSpringPsm.atl)──► PSM  spring-psm.xmi ──(Acceleo)──► código
     negocio, sin tecnología                         decisiones Java / Spring / PostgreSQL
```

---

## 1. PIM: `modeling/metamodels/vuelos.ecore`

**Qué es:** el modelo del negocio, independiente de la plataforma. Es un paquete Ecore (`nsURI = https://ucuenca.edu.ec/mda/vuelos/1.0`) con clases, atributos, referencias, un enumerado y restricciones OCL. **No contiene** tablas, claves técnicas, tipos Java, rutas REST ni nada de Spring.

**Dónde se ve en Obeo:** el diagrama `vuelos.aird` (Sirius) o `vuelos.ecore` con el editor Ecore.

### Clases

| Clase | Atributos (todos obligatorios `[1]`) | Referencias |
|---|---|---|
| **SistemaVuelos** | — | Raíz: contiene `[0..*]` aeropuertos, aerolíneas, aviones, rutas, vuelos, pasajeros, reservas y boletos |
| **Aeropuerto** | `codigo`, `nombre`, `ciudad` : EString | `rutasOrigen [0..*]`, `rutasDestino [0..*]` |
| **Aerolinea** | `codigo`, `nombre` : EString | `aviones [0..*]` |
| **Avion** | `matricula` : EString, `capacidad` : EInt | `aerolinea [1]`, `vuelos [0..*]` |
| **Ruta** | — | `origen [1]`, `destino [1]` (Aeropuerto), `vuelos [0..*]` |
| **Vuelo** | `codigo` : EString, `salida`, `llegada` : EDate | `ruta [1]`, `avion [1]`, `reservas [0..*]` |
| **Pasajero** | `documento`, `nombreCompleto`, `contacto` : EString | `reservas [0..*]` |
| **Reserva** | `localizador` : EString, `creadaEn` : EDate, `estado` : EstadoReserva, `numeroAsiento` : EInt | `pasajero [1]`, `vuelo [1]`, `boleto [0..1]` |
| **Boleto** | `numero` : EString, `emitidoEn` : EDate | `reserva [1]` |

**Enumerado** `EstadoReserva`: `CONFIRMADA`, `CANCELADA`.

**Puntos clave:**
- Las referencias son **bidireccionales** (`eOpposite`). Por ejemplo, `Vuelo.ruta` ↔ `Ruta.vuelos`.
- `SistemaVuelos` es solo el contenedor de las instancias. ATL lo **excluye**: no se convierte en tabla.
- `modeling/models/vuelos-ejemplo.xmi` es una **instancia** del PIM con datos de ejemplo. Se usa para validar OCL, no como entrada de ATL.

### Restricciones OCL (23 invariantes)

Están dentro del `.ecore`, como anotaciones `http://www.eclipse.org/emf/2002/Ecore/OCL`, y se evalúan con **Validate** en Obeo.

| Clase | Invariantes |
|---|---|
| SistemaVuelos (9) | Identidad única de aeropuerto, aerolínea, avión, vuelo, pasajero, reserva y boleto; `RutasUnicas`; `ReferenciasDentroDelSistema` |
| Aeropuerto, Aerolínea, Pasajero (1 cada una) | `TextosNoVacios` |
| Avion (2) | `TextosNoVacios`, `CapacidadPositiva` |
| Ruta (1) | `OrigenDistintoDestino` |
| Vuelo (4) | `TextosNoVacios`, `CapacidadRespetada`, `AsientosConfirmadosUnicos`, `PasajerosConfirmadosUnicos` |
| Reserva (2) | `TextosNoVacios`, `AsientoEnRango` |
| Boleto (2) | `TextosNoVacios`, `ReservaConfirmada` |

Ejemplos de expresiones:

```ocl
-- Ruta: OrigenDistintoDestino
self.origen <> self.destino

-- Avion: CapacidadPositiva
self.capacidad > 0

-- Vuelo: CapacidadRespetada (no hay sobrecupo)
self.reservas->select(r | r.estado = EstadoReserva::CONFIRMADA)->size() <= self.avion.capacidad

-- Vuelo: AsientosConfirmadosUnicos
self.reservas->select(r | r.estado = EstadoReserva::CONFIRMADA)->isUnique(r | r.numeroAsiento)

-- Reserva: AsientoEnRango
self.numeroAsiento >= 1 and self.numeroAsiento <= self.vuelo.avion.capacidad

-- Boleto: ReservaConfirmada
self.reserva.estado = EstadoReserva::CONFIRMADA
```

**Cómo se prueban:** `modeling/models/validation/` contiene 1 modelo válido y 10 inválidos, cada uno con un error concreto (sobrecupo, asiento repetido, origen = destino…). Al validarlos en Obeo aparece la X roja con la restricción violada, por ejemplo *«The 'AsientosConfirmadosUnicos' constraint is violated»*.

---

## 2. PSM: metamodelo `spring-psm.ecore` + modelo `spring-psm.xmi`

**Qué es:** el modelo específico de la plataforma **Java 21 + Spring Boot 3.5.16 + PostgreSQL 17**. Tiene dos partes:
- `modeling/metamodels/spring-psm.ecore`: el **metamodelo**, que define qué conceptos puede tener un PSM. Lo diseñamos nosotros.
- `modeling/models/generated/spring-psm.xmi`: el **modelo** concreto, **generado por ATL** a partir del PIM. Nunca se escribe a mano.

**Dónde se ve en Obeo:** `spring-psm.xmi` con *Open With → Sample Reflective Ecore Model Editor*.

### Metaclases del PSM

| Metaclase | Propiedades | Representa |
|---|---|---|
| **Application** | `name`, `groupId`, `artifactId`, `basePackage`, `javaVersion`, `springBootVersion`, `sourcePimNsURI`, `database`, `entities`, `enums` | El proyecto Spring Boot completo (raíz) |
| **Database** | `product`, `majorVersion`, `schemaName`, `ddlMode`, `timeZone` | PostgreSQL 17, esquema `public`, `ddl-auto=validate`, UTC |
| **Entity** | `name`, `tableName`, `sourceClass`, `attributes`, `associations`, `constraints`, `repository`, `service`, `controller` | Una entidad JPA y su tabla |
| **Attribute** | `name`, `columnName`, `sqlType`, `javaType`, `nullable`, `primaryKey`, `generated`, `idStrategy`, `notBlank`, `positive`, `length`, `enumType` | Un campo Java y su columna SQL, con sus validaciones |
| **Association** | `name`, `kind`, `lower`, `upper`, `owning`, `joinColumn`, `mappedBy`, `target`, `opposite`, `fetch`, `onDelete` | Una relación JPA y su clave foránea |
| **EnumType / EnumLiteral** | `name`, `storage`, `literals` | Enum Java guardado como texto |
| **Repository** | `name`, `baseInterface` | Interfaz Spring Data `JpaRepository` |
| **Service** | `name`, `transactional`, `policies` | Servicio transaccional |
| **Policy** | `name`, `kind`, `cimRules`, `description` | Regla de negocio que el servicio debe aplicar, con su origen RN-xx del CIM |
| **RestController** | `name`, `basePath`, `operations` | Controlador REST (`/api/...`) |
| **Operation** | `name`, `path`, `httpMethod`, `kind` | Un endpoint |
| **SqlConstraint** | `name`, `kind`, `columns`, `expression`, `cimRules` | Restricción UNIQUE / CHECK / índice único parcial |

**Enumerados del metamodelo:**

| Enum | Valores |
|---|---|
| `JavaType` | STRING, INTEGER, LONG, OFFSET_DATE_TIME, ENUM |
| `JpaRelation` | MANY_TO_ONE, ONE_TO_MANY, ONE_TO_ONE |
| `HttpMethod` | GET, POST |
| `OperationKind` | LIST, DETAIL, CREATE, AVAILABLE, CANCEL, ISSUE |
| `ConstraintKind` | UNIQUE, CHECK, PARTIAL_UNIQUE |
| `PolicyKind` | VALIDATE, TEMPORAL, LOCK, ALLOCATE_SEAT, CANCEL_RESERVATION, ISSUE_TICKET |

### Contenido del PSM generado

`Application FlightManagement` → `Database PostgreSQL` + **8 entidades** + enum `EstadoReserva`.
En total: 27 atributos, 16 extremos de asociación, 16 restricciones SQL y 26 operaciones REST.

Ejemplo, **Entity Pasajero**:

```xml
<entities name="Pasajero" tableName="pasajeros" sourceClass="Pasajero">
  <attributes name="id" columnName="id" sqlType="bigint" javaType="LONG"
              primaryKey="true" generated="true" idStrategy="IDENTITY"/>
  <attributes name="documento" columnName="documento" sqlType="varchar(255)" notBlank="true" length="255"/>
  <attributes name="nombreCompleto" columnName="nombre_completo" sqlType="varchar(255)" notBlank="true" length="255"/>
  <attributes name="contacto" columnName="contacto" sqlType="varchar(255)" notBlank="true" length="255"/>
  <associations name="reservas" kind="ONE_TO_MANY" upper="-1" mappedBy="pasajero" fetch="LAZY"/>
  <constraints name="uk_pasajero_documento" cimRules="RN-10"><columns>documento</columns></constraints>
  <repository name="PasajeroRepository" baseInterface="org.springframework.data.jpa.repository.JpaRepository"/>
  <service name="PasajeroService" transactional="true">
    <policies name="validarDatos" cimRules="RN-10 RN-15" .../>
  </service>
  <controller name="PasajeroController" basePath="/api/pasajeros"> listar · consultar · crear </controller>
</entities>
```

---

## 3. Qué cambia de PIM a PSM (lo que decide ATL)

| PIM | PSM | Decisión tecnológica |
|---|---|---|
| EPackage `vuelos` | `Application` + `Database` | Proyecto Spring Boot, Java 21, PostgreSQL 17 |
| EClass `Pasajero` | `Entity` tabla `pasajeros` + Repository + Service + RestController | Arquitectura en capas de Spring |
| (no existe) | `Attribute id`, `bigint`, `IDENTITY` | Clave técnica autogenerada |
| `nombreCompleto : EString [1]` | `columnName=nombre_completo`, `varchar(255)`, `notBlank`, `nullable=false` | Nombres snake_case, longitud y validación |
| `capacidad : EInt` | `INTEGER`, `integer`, `positive` | Tipos Java/SQL |
| `salida : EDate` | `OFFSET_DATE_TIME`, `timestamp with time zone` | Fechas con zona horaria |
| `estado : EstadoReserva` | `ENUM`, `varchar(32)`, storage `STRING` | Enum guardado como texto |
| `Vuelo.ruta [1]` ↔ `Ruta.vuelos [*]` | `MANY_TO_ONE`, `joinColumn=ruta_id`, `LAZY`, `RESTRICT` / `ONE_TO_MANY mappedBy=ruta` | Lado propietario y clave foránea |
| OCL `OrigenDistintoDestino` (RN-01) | `SqlConstraint CHECK origen_id <> destino_id` | La regla se garantiza en la base de datos |
| OCL `AsientosConfirmadosUnicos` (RN-17) | `PARTIAL_UNIQUE (vuelo_id, numero_asiento) WHERE estado='CONFIRMADA'` | Índice único parcial |
| (no existe) | `Operation` `GET /api/vuelos/disponibles`, `POST /api/reservas/{id}/cancelacion`… | API REST |
| Reglas de proceso del CIM | `Policy` `bloquearVuelo` (LOCK), `asignarAsiento` (ALLOCATE_SEAT)… | Comportamiento que implementan las plantillas |

**Trazabilidad:** las `SqlConstraint` y las `Policy` llevan `cimRules="RN-xx"`, así que cada decisión técnica se puede seguir hasta la regla del negocio del CIM que la origina.

---

## 4. Respuestas cortas

**¿Por qué el PIM es independiente de la plataforma?**
Solo describe conceptos del negocio y sus reglas. No hay `id`, tablas, tipos Java ni URL. El mismo PIM serviría para generar otra plataforma con otra transformación.

**¿Quién crea el PSM?**
La transformación ATL `PimToSpringPsm.atl`. El archivo `spring-psm-referencia.xmi` es una referencia hecha a mano que solo se usa para comprobar que la salida de ATL es equivalente; no es la entrada de nada.

**¿Por qué un metamodelo PSM propio y no UML?**
Para tener exactamente los conceptos de la plataforma elegida (Entity, Repository, RestController, SqlConstraint…) con propiedades que las plantillas Acceleo leen directamente. Así el PSM se puede validar con EMF y las plantillas quedan simples.

**¿Las restricciones OCL llegan al código?**
No se traducen automáticamente. OCL valida el modelo en Obeo. ATL convierte su intención en `SqlConstraint` y `Policy` del PSM, y las plantillas las implementan en SQL y en los servicios Java. La relación queda documentada con los códigos RN-xx.

**¿Qué está fijo para este dominio en la transformación?**
Qué atributo identifica cada clase (`identity()`: Avión → `matricula`, Pasajero → `documento`…), las restricciones SQL y las políticas por clase. Lo genérico son los atributos, tipos, relaciones, tablas y endpoints básicos. La mejora sería mover esa información al PIM como **marcas** (anotaciones) que ATL lea.
