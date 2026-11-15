# PIM — Sistema de Gestión de Vuelos

Estado: construido y validado con EMF; el usuario abrió y organizó el diagrama en Obeo. Después autorizó continuar con las restricciones OCL. Esta guía conserva las decisiones del PIM; la fase actual se documenta en [restricciones OCL](../../modeling/constraints/README.md).

## Archivos y finalidad

| Archivo | Finalidad |
|---|---|
| [vuelos.ecore](../../modeling/metamodels/vuelos.ecore) | Metamodelo independiente de persistencia y plataforma de ejecución |
| [vuelos-ejemplo.xmi](../../modeling/models/vuelos-ejemplo.xmi) | Instancia representativa con todos los conceptos del negocio |
| [.project](../../modeling/.project) | Proyecto Eclipse importable, sin generación de código |
| [ValidatePim.java](../../infrastructure/validation/ValidatePim.java) | Verificador manual que utiliza EMF real; no reemplaza ATL, Acceleo ni OCL |
| [validate-pim.ps1](../../infrastructure/validation/validate-pim.ps1) | Compila y ejecuta el verificador con los JAR de EMF instalados |

## Decisiones de modelado

- EPackage `vuelos`, prefijo `vuelos`, URI `https://ucuenca.edu.ec/mda/vuelos/1.0`. Es un identificador de namespace; no requiere un servicio web en esa dirección.
- Ocho EClasses del negocio: Aeropuerto, Aerolinea, Avion, Ruta, Vuelo, Pasajero, Reserva y Boleto.
- EClass adicional `SistemaVuelos`: raíz de organización, no entidad comercial. Contiene ocho colecciones `0..*`, una por concepto. Cada objeto de negocio tiene un solo contenedor; las asociaciones entre objetos no son contenciones.
- Todas las asociaciones conceptuales tienen referencias opuestas explícitas. Las multiplicidades conservan el CIM: cada vuelo tiene una ruta y un avión; cada reserva tiene pasajero y vuelo; cada boleto tiene una reserva y cada reserva tiene `0..1` boleto.
- No se eliminan referencias al borrar ni se implementan operaciones de negocio todavía. La contención define propiedad del archivo y no autoriza eliminar datos referenciados desde la aplicación futura.
- Las referencias de negocio son locales a la instancia de SistemaVuelos en el ejemplo. La regla que impide enlazar objetos de otras raíces se deberá concretar con las restricciones; las cardinalidades por sí solas no la garantizan.
- Todos los atributos tienen cardinalidad `1`. Se usa EString para texto, EInt para capacidad/asiento, EDate para instantes y EEnum para estado. No hay tipos Java/Spring, anotaciones JPA ni tablas.
- `EDate` es el tipo estándar EMF elegido para interoperabilidad; su representación interna en la implementación EMF no constituye una decisión sobre el backend. Los valores se guardan con desplazamiento UTC `+0000`. El ejemplo 15:00 UTC corresponde a las 10:00 de Ecuador continental.
- EEnum `EstadoReserva`: CONFIRMADA (0), CANCELADA (1). CONFIRMADA es el valor predeterminado de Ecore; el ejemplo lo expresa explícitamente. La transición y su autorización no se implementan en el enum.
- `numeroAsiento` pertenece a Reserva y se consulta desde Boleto mediante su referencia `reserva`. No se añade EClass Asiento ni se duplica el dato en Boleto.
- Se conservan códigos, documentos, matrícula, localizador y número como identificadores del negocio. No se marcan como atributos EID, porque EID impondría identidad de recurso y no la unicidad por concepto requerida. Los `xmi:id` del ejemplo son identificadores de serialización para enlazar objetos; no son columnas ni nuevos atributos del dominio.
- No se añaden cupos ni aerolínea operadora como datos duplicados. Se obtienen a partir de reservas confirmadas y del avión, respectivamente, cuando se implementen consultas y generadores.

## Atributos

Todos son `1..1`.

| EClass | EAttributes |
|---|---|
| Aeropuerto | codigo, nombre, ciudad: EString |
| Aerolinea | codigo, nombre: EString |
| Avion | matricula: EString; capacidad: EInt |
| Ruta | Sin atributos; se identifica conceptualmente por origen y destino |
| Vuelo | codigo: EString; salida, llegada: EDate |
| Pasajero | documento, nombreCompleto, contacto: EString |
| Reserva | localizador: EString; creadaEn: EDate; estado: EstadoReserva; numeroAsiento: EInt |
| Boleto | numero: EString; emitidoEn: EDate |

Los nombres se escriben sin tildes en Ecore para facilitar futuras transformaciones. La ausencia de datos y las referencias obligatorias se comprueban estructuralmente; texto vacío, capacidad positiva, rango y unicidad del asiento, identidad de negocio y demás reglas del CIM necesitan restricciones adicionales. Un EInt no implica por sí solo un valor positivo.

## Abrir en Obeo

1. **File → Import… → General → Existing Projects into Workspace**.
2. En **Select root directory**, seleccionar la carpeta `modeling` de este repositorio. Importar `ec.edu.ucuenca.mda.vuelos.modeling`. Conviene dejar desmarcado **Copy projects into workspace** para trabajar sobre estos mismos archivos.
3. Abrir `metamodels/vuelos.ecore` con **Open With → Sample Ecore Model Editor**. Expandir el paquete `vuelos`. En algunos menús el nombre se abrevia como Ecore Model Editor.
4. Seleccionar el paquete y usar **Validate** en su menú contextual. Revisar los resultados y la vista Problems.
5. Abrir `models/vuelos-ejemplo.xmi` con **Open With → Sample Reflective Ecore Model Editor**. Mantener el Ecore accesible en su ruta relativa: `xsi:schemaLocation` lo referencia. Expandir SistemaVuelos y sus colecciones; comprobar las propiedades de RES-001 y RES-002. Usar **Validate** sobre la raíz.

No hace falta generar Java ni registrar un plugin del dominio: se usa EMF dinámico. Si el editor no aparece, comprobar **Open With → Other…** y buscar el editor Reflective Ecore.

## Visualización gráfica con Ecore Tools/Sirius

Desde el menú contextual de `vuelos.ecore`, utilizar **Initialize Ecore Diagram…** si está disponible. En el asistente seleccionar el paquete `vuelos`, el viewpoint **Design** y la representación **Entities**. Guardar la representación `.aird` dentro del proyecto `modeling`.

Si se trabaja desde un Modeling Project, habilitar **Design** mediante **Viewpoints Selection** y crear **New Representation → Entities** sobre el EPackage. El diagrama Entities no añade necesariamente todas las clases: usar **Add Related Elements** desde el fondo del diagrama para incorporar las clases del paquete y ordenar sus posiciones.

La [documentación oficial de Ecore Tools](https://eclipse.dev/ecoretools/doc/) describe viewpoints, representaciones y el comportamiento de Entities. Las representaciones se guardan en `.aird`; no se ha fabricado ese archivo manualmente ni comprobado todavía la apertura visual en tu IDE. El `.ecore` es la fuente semántica y se puede abrir sin el diagrama.

## Validación reproducible desde PowerShell

Ejecutar desde la raíz del repositorio, ajustando las rutas si se mueve la instalación:

```powershell
& './infrastructure/validation/validate-pim.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

El verificador copia los JAR de EMF, OCL y sus dependencias a `infrastructure/validation/build/deps`, compila sus clases y carga el metamodelo y la instancia con ResourceSet, EcoreResourceFactory y XMIResourceFactory. La copia evita un problema de permisos observado al cerrar archivos ZIP desde javac sobre la instalación externa. Los artefactos de `build` están ignorados en Git. No se modifica la instalación de Obeo. El comando ahora también ejecuta la suite OCL.

Resultado obtenido el 15 de noviembre de 2026:

| Comprobación ejecutada | Resultado |
|---|---|
| Compilación del verificador con JDK 21 | Correcta |
| Diagnostician sobre EPackage | OK (severidad 0) |
| Carga y resolución de referencias del XMI | Sin errores ni proxies pendientes |
| Diagnostician sobre SistemaVuelos | OK (severidad 0) |
| Relaciones inversas del ejemplo | Comprobadas para vuelo-reservas, reserva-boleto y avión-vuelos |
| Fechas y asientos 1/2 del ejemplo | Comprobados mediante aserciones de infraestructura |
| Copia en memoria con reserva sin pasajero | Rechazada con ERROR (severidad 4), como se esperaba |
| Serialización y recarga XMI en memoria | OK (severidad 0) |

El control negativo no modifica el archivo de instancia. Las comprobaciones de fechas y asientos solo verifican el ejemplo; **no son implementación de restricciones OCL** ni una validación completa de las reglas RN-01 a RN-19.

## Revisión antes de continuar

El diagrama ya fue abierto por el usuario. Las restricciones OCL están comprobadas automáticamente y el usuario confirmó un caso negativo y uno positivo en Obeo. La fase actual es el [PSM](../psm/README.md), todavía sin generadores.
