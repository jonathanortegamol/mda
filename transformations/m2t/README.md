# M2T — PSM generado → backend con Acceleo 3

Se añadió un segundo destino **frontend**, ahora prioritario: [guía de pantallas](../../docs/frontend/README.md) y [frontend.mtl](src/frontend.mtl). Para generarlo se usa `run-m2t.ps1 -Target frontend`. El resto de este documento conserva el detalle del destino backend, que sigue siendo el valor predeterminado del script.

Estado: plantilla Acceleo compilada y ejecutada con el PSM producido por ATL. Generación reproducible y propagación hasta código comprobadas. Las fuentes Java compilan y sus 12 pruebas unitarias pasan con dependencias instaladas de Boot 3.5.3 / Spring 6.2.8. **El empaquetado Maven con la versión objetivo 3.5.16 no se completó: sus dependencias no están en la caché y el entorno denegó la conexión a Maven Central.** No se afirma ejecución HTTP ni integración con PostgreSQL en esta fase.

## Archivos y responsabilidades

| Archivo o carpeta | Finalidad |
|---|---|
| [src/backend.mtl](src/backend.mtl) | Plantilla real Acceleo 3: genera fuentes Java, pruebas, Maven, configuración y SQL |
| [.project](.project), [.classpath](.classpath), [MANIFEST.MF](META-INF/MANIFEST.MF), [build.properties](build.properties) | Proyecto Eclipse/PDE para editar las plantillas con Acceleo; no son código del backend |
| [RunAcceleo.java](../../infrastructure/m2t/RunAcceleo.java) | Carga PSM, invoca AcceleoParser y AcceleoService; registra los archivos escritos por el motor |
| [run-m2t.ps1](../../infrastructure/m2t/run-m2t.ps1) | Valida entrada, verifica procedencia ATL, compila infraestructura y ejecuta Acceleo |
| [verify-m2t.ps1](../../infrastructure/m2t/verify-m2t.ps1) | Ejecuta la cadena y prueba regeneración y propagación de un atributo |
| [build-backend.ps1](../../infrastructure/m2t/build-backend.ps1), [maven-settings.xml](../../infrastructure/m2t/maven-settings.xml) | Intento de compilación Maven exacta; usa JDK 21, lee caché instalada y escribe repositorio dentro del workspace |
| [verify-cached-java.ps1](../../infrastructure/m2t/verify-cached-java.ps1), [CachedTestLauncher.java](../../infrastructure/m2t/CachedTestLauncher.java) | Compilación javac y ejecución JUnit con JAR disponibles; verificación parcial independiente del POM objetivo |
| [generated/backend](../../generated/backend/pom.xml) | Proyecto backend producido íntegramente por Acceleo |
| [verification-result.json](verification-result.json) | Resultado resumido de la suite de esta fase y límites de la verificación |

El lanzador Java **no genera texto de aplicación, no sustituye Acceleo ni copia una aplicación prefabricada**. Compila la plantilla a `build/backend.emtl` y utiliza AcceleoService con la estrategia de escritura del motor. La especialización TrackedStrategy registra rutas al abrir archivos; el contenido sigue siendo producido por las instrucciones MTL. El `.emtl` es el módulo compilado del generador, no una clase de la aplicación.

## Entrada y salida

```text
vuelos.ecore → PimToSpringPsm.atl → models/generated/spring-psm.xmi
                                                 │
                                                 ▼
                                             backend.mtl
                                                 │ Acceleo 3
                                                 ▼
                                            generated/backend/
```

La entrada habitual es el PSM generado por ATL. El script comprueba sus hashes de procedencia y rechaza entradas obsoletas respecto de las fuentes de M2M. No se usa la referencia manual como entrada de la entrega. Los modelos temporales de prueba se pasan explícitamente como tales y quedan identificados en sus manifiestos.

Acceleo 3 utiliza expresiones OCL para consultar el PSM. Esta plantilla no es intercambiable con Acceleo 4/AQL. El lanzador resuelve localmente `mtlstdlib.ecore` y `mtlnonstdlib.ecore` desde los JAR instalados, evitando intentos de descarga de las bibliotecas del lenguaje. La cabecera MTL usa la URI de recurso del proyecto de modelado; en ejecución standalone se mapea al mismo metamodelo local y se registra como alias del EPackage.

## Qué se genera

**56 archivos de aplicación/prueba/configuración**, más dos registros de infraestructura:

- 51 archivos Java de producción: aplicación Spring Boot; ocho entidades, ocho repositorios, ocho servicios, ocho controladores, ocho DTO de respuesta y siete de entrada; enum; excepción de negocio y manejador HTTP.
- Una clase de pruebas con doce casos de comportamiento del negocio.
- Configuración Mockito para pruebas sin un agente JVM dinámico.
- `pom.xml`, `src/main/resources/application.properties` y `schema.sql`.
- `.acceleo-files.txt`: lista de rutas abiertas por el motor de generación.
- `.generation-manifest.json`: versiones de JAR, hashes de fuentes/modelos/módulo compilado y de cada salida. Estos dos registros los escribe la infraestructura, no las plantillas.

Todos los `.java`, SQL, propiedades de la aplicación y POM proceden de bloques `[file]` en `backend.mtl`. No se editan para corregir fallos: los cambios se hacen en la plantilla y se regenera. No hay áreas protegidas de código manual. Las plantillas sobrescriben sus propios archivos; no editarlas sobre una copia manual de una aplicación existente. El perfil actual conserva ocho entidades y no implementa eliminación automática de archivos obsoletos si se cambia el alcance del generador.

El frontend también se genera desde el PSM con frontend.mtl. Sus formularios HTML y metadatos de listado dependen de los campos del modelo; un adaptador local permite recorrer la interfaz sin PostgreSQL. La conexión con la API real se conserva como modo separado y todavía no se ha probado de forma integrada.

## Código generado y reglas

| Componente | Comportamiento generado |
|---|---|
| Entidades | Columnas, clave IDENTITY, enum STRING, relaciones JPA y sus inversas; sin Lombok ni borrado en cascada |
| DTO de entrada | Campos del modelo y referencias mediante IDs; Bean Validation y mensajes en español |
| DTO de respuesta | Datos y referencias por ID; evita ciclos JSON y serialización directa de entidades |
| Catálogos y pasajeros | Alta y consulta; normalización de texto y mayúsculas en identificadores; sin edición ni borrado general |
| Rutas | Origen/destino existentes y diferentes; unicidad respaldada por SQL |
| Vuelos | Avión bloqueado al programar, salida futura, llegada posterior y exclusión de intervalos solapados |
| Disponibilidad | Vuelos futuros con cupos y filtro opcional por origen/destino; cupos derivados de confirmadas |
| Reservas | Vuelo bloqueado, control de cupo y pasajero duplicado; asignación del menor asiento libre; localizador y fecha del servidor |
| Cancelación | Orden de bloqueo vuelo → reserva; solo antes de salida y sin boleto; conserva asiento histórico; repetición idempotente |
| Boletos | Mismo orden de bloqueo; reserva confirmada y salida futura; emisión repetida devuelve el existente y no ocupa otro asiento |
| Errores | 400 por datos inválidos, 404 por referencias ausentes y 409 por reglas o integridad |
| Persistencia | DDL PostgreSQL con FK RESTRICT, UNIQUE, CHECK y dos índices únicos parciales para reservas confirmadas |

Las políticas del PSM identifican el perfil de comportamiento; los campos y operaciones se recorren desde el modelo. Las secciones especializadas para vuelos, reservas y boletos están escritas en MTL. No se ha construido un traductor universal de cualquier expresión OCL a Java/SQL.

Los bloqueos se generan con Spring Data JPA `@Lock(PESSIMISTIC_WRITE)`. Su [documentación oficial](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html) describe la aplicación de modos de bloqueo en repositorios. Aquí se comprobaron las llamadas y su orden mediante pruebas unitarias; su efecto real bajo concurrencia se verificará con PostgreSQL en la fase de aplicación.

Las consultas JPQL se generan pero todavía no se han validado durante el arranque de un contexto Spring con la base real. Las restricciones SQL tampoco se han ejecutado en PostgreSQL en esta entrega. Estas comprobaciones son parte explícita de la siguiente fase.

## Regeneración desde PowerShell

Desde la raíz del repositorio:

```powershell
& './infrastructure/m2m/run-m2m.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'

& './infrastructure/m2t/run-m2t.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

Se copian los JAR de EMF, OCL, Acceleo, Guava y las dependencias necesarias desde Obeo a `transformations/m2t/build/deps`. No se modifica la instalación. AcceleoParser comprueba la sintaxis y los tipos de las expresiones antes de ejecutar el módulo. Un fallo interrumpe el proceso: una salida antigua no prueba que una nueva ejecución haya terminado bien.

Para repetir también las pruebas de propagación y compilación con caché disponible:

```powershell
& './infrastructure/m2t/verify-m2t.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot' `
  -CachedJava
```

Sin `-CachedJava`, se ejecutan generación, equivalencia M2M, repetibilidad y propagación, pero no javac/JUnit. Las copias con telefono quedan en `transformations/m2t/build/propagation-backend`, no en la entrega principal. `-InputPsm`, `-InputPim` y `-OutputDirectory` permiten indicar esa variante de prueba de forma explícita. El verificador PSM conserva el perfil de ocho entidades acordado.

## Resultado real de las comprobaciones

Ejecutadas el 15 de noviembre de 2026:

| Comprobación | Resultado |
|---|---|
| Compilar `backend.mtl` con Acceleo 3.7.15 | Correcto |
| Generar desde salida real de ATL | 56 archivos escritos |
| Regenerar con mismas entradas | Mismos hashes de los 56 archivos |
| Añadir telefono a copia PIM y ejecutar ATL/Acceleo | Aparece en entidad, DTO de entrada/salida, servicio y SQL |
| Compilar fuentes base con JDK 21 y JAR instalados | Correcto: Boot 3.5.3, Spring 6.2.8 y dependencias de caché |
| Ejecutar pruebas base con JUnit | 12 exitosas, 0 fallidas |
| Compilar y probar variante con telefono usando caché | 12 exitosas, 0 fallidas |
| Ejecutar Maven sobre POM objetivo Boot 3.5.16 | No completado: dependencia ausente y conexión denegada |
| Ejecutar esquema PostgreSQL y API HTTP | Pendiente, sin ejecución en esta fase |

Los casos unitarios cubren primer asiento, hueco libre, vuelo lleno, pasajero duplicado, salida alcanzada, cancelación con conservación de asiento, rechazo de cancelación con boleto, cancelación repetida, boleto existente, reserva cancelada, nueva emisión sin alterar asiento y vuelo solapado. No sustituyen las pruebas de persistencia ni de concurrencia.

El verificador de caché compila los **mismos archivos Java entregados**, sin cambiar su contenido ni el POM objetivo. Su lista concreta de JAR queda en `build/cached-verification/classpath.txt`; los resúmenes JUnit quedan en esa carpeta y en `build/baseline-junit-summary.txt`. Es una comprobación de fuentes y reglas con APIs disponibles, **no certifica el empaquetado con Spring Boot 3.5.16**.

## Completar la compilación Maven

En una terminal con acceso a Maven Central, ejecutar:

```powershell
& './infrastructure/m2t/build-backend.ps1' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

El script selecciona JDK 21 solo durante la ejecución, restaura JAVA_HOME, reintenta artefactos con `-U`, usa la caché instalada como repositorio de lectura y escribe descargas dentro de `.cache/maven`. El bloqueo encontrado fue `Permission denied: getsockopt` al resolver `spring-boot-starter-parent:3.5.16`. No se cambió la versión en ATL, el PSM ni el backend para ocultar ese resultado.

También puede utilizarse Maven directamente con JDK 21 y `mvn -f generated/backend/pom.xml package`. No se necesita una base de datos para ejecutar estas doce pruebas unitarias.

## Abrir en Eclipse/Obeo

Importar `transformations/m2t` como **Existing Projects into Workspace**, sin copiar, para revisar `src/backend.mtl`. El proyecto tiene naturaleza Acceleo/PDE y referencia el proyecto de modelado. Su importación y compilación mediante el builder del IDE quedan por comprobar visualmente; la compilación standalone ya se ejecutó.

Para el backend usar **File → Import → Maven → Existing Maven Projects** y seleccionar `generated/backend`. Requiere JDK 21 y resolución de dependencias. Si esa distribución no tiene m2e, abrir los fuentes como archivos para revisión y compilar desde la terminal; no es necesario modificar Obeo para inspeccionar el código.

## Configuración y siguiente fase

El backend usa DB_URL (predeterminado `jdbc:postgresql://localhost:5432/vuelos`), DB_USER (predeterminado `vuelos`) y DB_PASSWORD (obligatoria, sin contraseña en archivos). PORT permite cambiar el puerto 8080. `spring.jpa.open-in-view=false` exige construir los DTO dentro del servicio transaccional, como hacen las plantillas.

`schema.sql` es una instantánea del esquema para una base vacía, no una migración incremental. Se aplica una vez antes de arrancar. La aplicación usa ddl-auto=validate y no inicializa SQL automáticamente. Por tanto, no basta con lanzar Spring Boot sin preparar PostgreSQL.

La siguiente fase será resolver la compilación Maven exacta, preparar PostgreSQL, aplicar este SQL y verificar el flujo HTTP completo y la concurrencia. La prueba actual de propagación termina en fuentes y pruebas unitarias; la propagación hasta una aplicación ejecutada continúa pendiente.
