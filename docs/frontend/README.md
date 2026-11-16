# Interfaz gráfica generada — prioridad actual

La interfaz se puede recorrer **sin Maven, Java, Docker ni PostgreSQL**. Abrir [generated/frontend/index.html](../../generated/frontend/index.html) con Chrome o Edge, manteniendo los otros cuatro archivos de la carpeta a su lado.

La salida fue producida por Acceleo 3 desde el PSM generado por ATL; no es un sitio externo ni una interfaz presentada como generada después de escribir directamente sus archivos finales.

## Pantallas disponibles

- **Resumen:** indicadores, próximos vuelos, próxima salida y últimas reservas.
- **Vuelos:** programación, consulta, filtros por origen/destino y acceso a reserva.
- **Pasajeros:** registro y listado, con campos derivados del modelo.
- **Reservas:** creación con asignación automática de asiento, filtros por estado, cancelación y emisión de boleto.
- **Boletos:** emisión y consulta de una constancia visual, con asiento e impresión desde el navegador.
- **Catálogos:** aeropuertos, aerolíneas, aviones y rutas, con formularios para sus relaciones.

## Recorrido recomendado

1. Abrir `generated/frontend/index.html`. Se cargan datos ficticios de ejemplo y la etiqueta **Demostración local**.
2. En **Vuelos**, elegir **Programar vuelo**. Seleccionar una ruta y un avión existentes y un horario futuro sin solapamientos.
3. En **Pasajeros**, registrar una persona ficticia.
4. En **Reservas**, elegir **Nueva reserva**, seleccionar ese vuelo y pasajero y confirmar. El sistema asigna el menor número de asiento libre.
5. Elegir **Emitir boleto** en la reserva confirmada y revisar la constancia gráfica.
6. Para comprobar cancelación, crear otra reserva sin boleto, cancelarla y verificar que su asiento queda libre y su historial permanece.

Los ejemplos iniciales incluyen una reserva con boleto y otra sin él. Permiten revisar las acciones desde la primera apertura. Los registros propios se conservan en localStorage cuando el navegador lo permite; si lo bloquea, la interfaz indica que los datos son temporales en la sesión. No se borran bases de datos ni se envían los datos de demostración a la API.

**Restablecer datos de demostración**, dentro de **Origen de los datos**, reemplaza únicamente el estado local después de confirmarlo en la propia pantalla. No modifica PostgreSQL.

## Qué depende realmente del modelo

| Salida | Origen |
|---|---|
| HTML de los ocho formularios, nombres de campos y relaciones | Bucles Acceleo sobre Entity, Attribute y Association del PSM |
| Tipos de entrada, obligatoriedad, longitud y mínimo positivo | JavaType, nullable, length y positive del PSM |
| Opciones de enum y selectores de referencias | EnumType y destinos de Association |
| Definiciones de columnas de listados y campos nuevos | model.js generado desde el PSM, utilizado por el renderizador genérico |
| Rutas base y catálogo de operaciones | RestController y Operation del PSM |
| Diseño, navegación y renderizador | Andamiaje de interfaz escrito en la plantilla frontend.mtl y emitido por Acceleo |
| Adaptador de demostración y datos de ejemplo | Implementación especializada del perfil de vuelos, escrita en la plantilla y emitida por Acceleo |

El diseño visual y el adaptador no se deducen automáticamente de cualquier modelo arbitrario. Son parte del generador, igual que las secciones especializadas de los servicios del backend. Los campos y formularios sí se obtienen del PSM sin una lista fija de atributos de Pasajero en el código de interfaz.

La demostración en navegador **no evalúa OCL con Eclipse ni ejecuta Spring Boot/PostgreSQL**. Implementa las reglas necesarias para recorrer las pantallas con datos locales; OCL conserva su validación real en la fase de modelado. No se sustituyeron ATL o Acceleo por este adaptador. El backend PostgreSQL sigue en el proyecto para la integración posterior.

## Cinco archivos producidos por Acceleo

| Archivo | Función |
|---|---|
| [index.html](../../generated/frontend/index.html) | Estructura de pantallas y plantillas HTML de formularios por entidad |
| [styles.css](../../generated/frontend/styles.css) | Diseño, distribución adaptable y presentación del boleto |
| [model.js](../../generated/frontend/model.js) | Metadatos derivados del modelo para campos, columnas, relaciones y operaciones |
| [demo-store.js](../../generated/frontend/demo-store.js) | Adaptador local, ejemplos y almacenamiento del navegador |
| [app.js](../../generated/frontend/app.js) | Navegación, formularios, listados y elección entre demostración y API |

Los dos archivos adicionales `.acceleo-files.txt` y `.generation-manifest.json` registran qué escribió el motor y los hashes de procedencia. No editar los archivos finales para cambiar un campo; editar el modelo o la plantilla y regenerar.

La fuente única del generador es [frontend.mtl](../../transformations/m2t/src/frontend.mtl). Los archivos temporales usados al preparar el texto de la plantilla están dentro de `build/`, ignorados en Git y no se leen durante la generación.

## Regenerar las pantallas

Desde la raíz del proyecto:

```powershell
& './infrastructure/m2m/run-m2m.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'

& './infrastructure/m2t/run-m2t.ps1' -Target frontend `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

Recargar el navegador después de regenerar. Esta ruta solo genera las pantallas: no compila Maven ni arranca una base de datos. El valor predeterminado de `run-m2t.ps1` sigue siendo backend; se usa `-Target frontend` explícitamente para priorizar esta entrega.

## Prueba de propagación visual

```powershell
& './infrastructure/validation/verify-frontend.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

La prueba añade telefono a **una copia del PIM**, ejecuta ATL y genera una interfaz variante en `transformations/m2t/build/frontend-telefono/`. Abrir su `index.html` y entrar en **Pasajeros → Registrar pasajero** para ver el campo adicional. El listado también usa esa definición nueva. El PIM de entrega y la interfaz base no reciben telefono de forma permanente.

Las pruebas necesitan un runtime JavaScript. El script busca Node.js y, si no está disponible, utiliza el runtime de VS Code ya instalado en modo CLI, sin abrir o controlar su interfaz. No se necesita ese runtime para usar las pantallas.

## Comprobaciones realizadas y límites

El 15 de noviembre de 2026 se ejecutaron:

- Compilación real de frontend.mtl con Acceleo 3.7.15 y extracción de cinco archivos.
- Regeneración con entradas iguales: mismos hashes de los cinco archivos.
- Validación de sintaxis de model.js, demo-store.js y app.js.
- 27 comprobaciones sobre los ocho formularios emitidos, cupos, asientos, duplicidad, horarios, boleto, cancelación, reutilización e historial/almacenamiento local.
- 29 comprobaciones en la variante con telefono, incluyendo el campo en el HTML generado y en los metadatos del listado.

Evidencia resumida en [frontend-verification.json](../../transformations/m2t/frontend-verification.json). Estas pruebas son de generación y comportamiento del adaptador; no constituyen una prueba automatizada de DOM ni una revisión de apariencia en navegador.

La revisión visual automatizada no pudo completarse: Computer Use no pudo identificar con suficiente confianza la URL de Chrome y detuvo el acceso. No se intentó eludir ese bloqueo. La navegación visual y los formularios deben revisarse al abrir la interfaz; no se afirma una revisión visual exitosa ni ejecución de la integración API.

## Vista previa HTTP y conexión futura

Opcionalmente:

```powershell
python './infrastructure/preview/serve-frontend.py'
```

Abrir `http://127.0.0.1:8137`. El servidor manual solo sirve archivos generados y reenvía `/api/` al backend de `127.0.0.1:8080`; no implementa reglas de negocio ni una base de datos. Ctrl+C lo detiene. No se publica nada fuera del equipo.

Cuando el backend esté funcionando, abrir **Origen de los datos → Conectar al backend** y dejar `/api`. La interfaz carga los registros reales; si falla la conexión, muestra un error y no presenta datos de ejemplo como resultados de la API. Una URL de otro origen necesita una configuración CORS adecuada; la vista previa con proxy evita esa diferencia de origen.

La conexión API sigue pendiente de pruebas integradas. Por ahora, el recorrido visual de demostración es independiente de la compilación Maven y de PostgreSQL.
