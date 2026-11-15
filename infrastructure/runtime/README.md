# Completar la compilación y probar la aplicación

Estos archivos son infraestructura manual de ejecución y prueba. No reemplazan código generado ni transformaciones. Los comandos se ejecutan desde la raíz de `trabajo_final`, en PowerShell. Se prepararon contra los DTO y rutas generados; **el flujo PostgreSQL/HTTP todavía no se ha ejecutado desde Codex**.

## 1. Compilar con la versión objetivo

```powershell
& './infrastructure/m2t/build-backend.ps1' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

Esperar `BUILD SUCCESS`, con doce pruebas exitosas. El JAR esperado es `generated/backend/target/flight-management-0.0.1-SNAPSHOT.jar`. Si falla, no continuar al arranque: conservar el error completo. La descarga requiere acceso a Maven Central; la versión objetivo está publicada en [Maven Central](https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-parent/3.5.16). El fallo observado en Codex fue una conexión denegada, no un resultado satisfactorio de Maven.

No cambiar solo el POM generado para resolver problemas de versión: las decisiones de versión proceden de ATL/PSM y deben propagarse por la cadena.

## 2. Iniciar PostgreSQL

Abrir Docker Desktop y esperar a que su motor esté activo. En la misma terminal:

```powershell
$env:DB_PASSWORD = 'vuelos_demo_local'
docker compose -f './infrastructure/runtime/compose.yaml' up -d --wait
```

La configuración usa PostgreSQL 17, base y usuario `vuelos`, puerto local **55432** y un volumen propio. La contraseña del comando es un ejemplo local; se suministra por entorno y no queda en los archivos de configuración.

En la primera creación de ese volumen, la imagen aplica automáticamente el **schema.sql generado por Acceleo**, montado en su directorio de inicialización, siguiendo el mecanismo de la [imagen oficial PostgreSQL](https://hub.docker.com/_/postgres). La comprobación de salud espera también la tabla boletos. No se depende de tener `psql` instalado en Windows.

Si falla:

```powershell
docker compose -f './infrastructure/runtime/compose.yaml' logs postgres
```

En reinicios se conservan los datos y no se vuelve a aplicar el esquema. Esta configuración no borra volúmenes ni intenta migrar un esquema anterior. Si posteriormente cambia el modelo, habrá que preparar una migración o una base de demostración nueva de forma explícita.

## 3. Arrancar el backend

En esa misma terminal, una vez compilado el JAR y PostgreSQL saludable:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:55432/vuelos'
$env:DB_USER = 'vuelos'
& 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe' `
  -jar './generated/backend/target/flight-management-0.0.1-SNAPSHOT.jar'
```

DB_PASSWORD sigue definida del paso anterior. Esperar el mensaje `Started FlightManagementApplication`. La aplicación valida el esquema existente; no lo crea ni elimina al arrancar. Mantener esta terminal abierta.

## 4. Ejecutar el flujo completo

Abrir **otra terminal PowerShell en la raíz del proyecto**:

```powershell
& './infrastructure/runtime/smoke-test.ps1'
```

La prueba crea aeropuertos, aerolínea, avión de dos asientos, ruta, vuelo futuro y tres pasajeros. Luego comprueba reserva, consulta, emisión, duplicidad, sobreventa, cancelación e historial. Usa identificadores nuevos en cada ejecución y no elimina registros.

Resultado esperado: **13 comprobaciones correctas**, más un resumen con los identificadores de vuelo, reserva, asiento y boleto. Es un resultado esperado, no una ejecución ya observada en esta entrega.

Para consultar datos después:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/vuelos'
Invoke-RestMethod 'http://localhost:8080/api/reservas'
Invoke-RestMethod 'http://localhost:8080/api/boletos'
```

No existe todavía una página web de usuario: esta comprobación usa la API REST. Un navegador en `/` puede responder 404 sin que eso signifique que el backend no arrancó.

## 5. Detener y volver a probar persistencia

Usar Ctrl+C en la terminal del backend y arrancar otra vez con el mismo comando Java. Las consultas deben conservar los datos. Para detener PostgreSQL conservando su volumen:

```powershell
docker compose -f './infrastructure/runtime/compose.yaml' stop
```

No se requiere borrar la base para repetir el script de prueba. Los bloqueos y solicitudes simultáneas requieren pruebas adicionales; este script comprueba un flujo secuencial.
