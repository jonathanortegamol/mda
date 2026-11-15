# M2M — Ecore PIM → Spring PSM con ATL

Estado: **transformación ATL compilada y ejecutada**, con salida validada mediante EMF, comparación semántica con la referencia y prueba de propagación de un atributo. Posteriormente se implementó [M2T con Acceleo](../m2t/README.md); la aplicación aún no se ha ejecutado con PostgreSQL.

## Archivos y responsabilidades

| Archivo | Función |
|---|---|
| [src/PimToSpringPsm.atl](src/PimToSpringPsm.atl) | Reglas reales que crean Application, entidades, campos, asociaciones, componentes, operaciones y políticas del PSM |
| [supported-ocl.properties](supported-ocl.properties) | Perfil explícito de invariantes conocidas; permite rechazar cambios de negocio que las reglas todavía no soportan |
| [.project](.project) | Proyecto Eclipse con naturaleza y builder ATL; referencia el proyecto de modelado |
| [run-m2m.ps1](../../infrastructure/m2m/run-m2m.ps1) | Compila infraestructura, invoca ATL, valida salida y registra procedencia |
| [RunAtl.java](../../infrastructure/m2m/RunAtl.java) | Comprobación de entrada y orquestación de las APIs oficiales de compilación, VM e inyección/extracción EMF |
| [VerifyM2m.java](../../infrastructure/m2m/VerifyM2m.java) | Pruebas de equivalencia, propagación y rechazo de OCL no soportado |
| [spring-psm.xmi](../../modeling/models/generated/spring-psm.xmi) | Salida producida por ATL; se puede regenerar |
| [spring-psm.xmi.provenance.json](../../modeling/models/generated/spring-psm.xmi.provenance.json) | Fecha UTC, motor, JAR y hashes SHA-256 de fuentes, metamodelos, ASM y salida |

El script y el lanzador Java **no crean objetos del metamodelo PSM**. Toda la correspondencia está en el `.atl`. Java utiliza el [compilador oficial ATL](https://help.eclipse.org/latest/topic/org.eclipse.m2m.atl.doc/references/javadoc/org/eclipse/m2m/atl/engine/compiler/AtlStandaloneCompiler.html) y EMFVMLauncher. Esto es ejecución standalone de ATL con los JAR instalados en Obeo; no es una sustitución de ATL por otro lenguaje.

La referencia [spring-psm-referencia.xmi](../../modeling/models/spring-psm-referencia.xmi) sigue siendo manual. **No es entrada de la transformación ni se copia para construir la salida.** Solo se lee, después de ejecutar ATL, cuando se solicita la prueba comparativa `-Verify`.

## Modelos y nivel de abstracción

```text
modeling/metamodels/vuelos.ecore                 Modelo PIM, conforme a Ecore
                       │ IN : ECORE
                       ▼
transformations/m2m/src/PimToSpringPsm.atl        Reglas ATL
                       │ OUT : SPRINGPSM
                       ▼
modeling/models/generated/spring-psm.xmi         Modelo PSM, conforme a spring-psm.ecore
```

`vuelos-ejemplo.xmi` contiene datos de pasajeros y vuelos; no define qué clases generar y no se consume aquí. El `.ecore` PIM es un modelo Ecore y puede ser entrada de ATL como tal. La raíz SistemaVuelos organiza las instancias del negocio y se excluye de tablas y componentes Java.

## Reglas implementadas

- EPackage → Application y Database del perfil Java 21 / Spring Boot 3.5.16 / PostgreSQL 17.
- EClass de negocio → Entity, clave tecnológica `id`, Repository, Service y RestController.
- EAttribute → Attribute; se recorre la colección de atributos del modelo, sin lista fija de campos del dominio.
- EReference → Association con destino, opuesta, cardinalidad, propietario, joinColumn, mappedBy y preferencia LAZY.
- EEnum y EEnumLiteral → enum Java con persistencia por nombre y sus literales.
- Helpers y reglas llamadas construyen nombres, rutas REST, restricciones SQL y políticas específicas de las reglas del CIM.

La transformación resuelve automáticamente las referencias de origen a sus objetos destino mediante las trazas de ATL. El PSM resultante incluye 8 entidades, 27 atributos, 16 extremos de asociación, 16 restricciones/índices SQL, 26 operaciones REST y los componentes del diseño de referencia.

## Ejecutar desde PowerShell

Desde la raíz del repositorio:

```powershell
& './infrastructure/m2m/run-m2m.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot' `
  -Verify
```

No se necesita Maven ni descargar dependencias para esta fase. Se usan EMF y ATL 4.12.0 de Obeo y JDK 21. El script copia las dependencias a `transformations/m2m/build/deps`, compila la infraestructura y compila el `.atl` a `build/PimToSpringPsm.asm`. Este ASM es código de la VM ATL, no el backend de la aplicación. `build/` está ignorado en Git.

El compilador seleccionado es **Atl2006Compiler**; su nombre es el identificador histórico de la implementación, no la versión instalada. Los JAR utilizados son de ATL 4.12.0. El parser de esta distribución también requiere EMFTVM, aunque la ejecución del ASM utiliza EMFVM; ambos están presentes en Obeo. No se necesita el adaptador CDO.

Sin `-Verify`, se transforma y valida la salida, pero no se compara con la referencia ni se ejecutan los escenarios adicionales. Con `-InputPim` se puede indicar una copia modificada del Ecore. Para una entrada modificada, ejecutar sin `-Verify`: la comparación de esa suite corresponde al PIM base y su referencia; la propia suite ya incluye una variante de propagación. Con `-OutputPsm` se puede elegir un XMI dentro de `modeling/models/generated/` o `transformations/m2m/build/`; el script impide sobrescribir los modelos manuales.

Solo una ejecución con código de salida 0 es satisfactoria. Los fallos de compilación, perfil o validación interrumpen el script; un archivo generado previamente no acredita que una ejecución posterior haya pasado. El registro de procedencia se actualiza después del éxito y permite comprobar que los hashes corresponden a la salida actual.

## Pruebas realizadas

Ejecutadas el 15 de noviembre de 2026 con el entorno documentado:

| Prueba | Resultado |
|---|---|
| Compilación del módulo con ATL 4.12 | Sin diagnósticos de compilación |
| Ejecución EMFVM y extracción del resultado | XMI producido correctamente |
| Validación estructural y tecnológica | EMF OK; campos, referencias, tipos y rutas coherentes con PIM |
| Comparación con referencia manual | Equivalente en los valores de todos los elementos del PSM |
| Propagación de `Pasajero.telefono` en copia del PIM | Nuevo Attribute STRING y columna telefono en salida ATL; resultado válido |
| Cambio de expresión OCL `AsientoEnRango` a `false` en otra copia | Rechazo explícito por perfil no soportado |
| Regeneración con las mismas entradas | Hash del XMI estable entre ejecuciones comprobadas |

La comparación usa EMF y valores efectivos, incluidos los valores por defecto. Ignora IDs XMI y orden de componentes, porque la referencia usa IDs y la salida ATL usa fragmentos estructurales. Conserva el orden de literales y de columnas, y compara también los destinos de referencias por su identidad semántica.

Los escenarios de prueba se guardan en `build/tests/` y no modifican el PIM, la referencia ni el diagrama del usuario. La prueba de propagación de esta entrega llega **hasta el PSM**; no demuestra todavía regeneración de código ni funcionamiento de la aplicación.

## Abrir resultado y fuente en Obeo

1. En el proyecto de modelado ya importado, pulsar **F5 / Refresh**.
2. Abrir `models/generated/spring-psm.xmi` con **Sample Reflective Ecore Model Editor**.
3. Expandir Application → entities → Reserva y revisar atributos, asociaciones, restricciones y servicio; ejecutar **Validate** sobre Application.
4. Para inspeccionar ATL, importar `transformations/m2m` mediante **File → Import → General → Existing Projects into Workspace**, sin copiar. Se llamará `ec.edu.ucuenca.mda.vuelos.m2m`.
5. Abrir `src/PimToSpringPsm.atl` con el editor ATL. La cabecera declara el metamodelo Ecore por URI y el PSM por ruta del proyecto de modelado. Mantener ambos proyectos importados.

La salida se guarda dentro del proyecto de modelado para que su `xsi:schemaLocation` relativo funcione tanto con rutas de archivo como con URIs de recurso de Eclipse. No se alteró el `.aird` del PIM. La apertura y ejecución en el IDE por el usuario sigue pendiente; la compilación y ejecución standalone ya están comprobadas.

## Límites explícitos

El perfil soporta las ocho clases, el enum de reserva, atributos simples EString/EInt/EDate/EstadoReserva, referencias opuestas y las invariantes actuales. Se rechazan tipos no soportados, herencia, operaciones, nuevos clasificadores, campos calculados, atributos multivaluados, claves EID, defaults explícitos y relaciones sin propietario inequívoco. El nombre `id` está reservado para la clave tecnológica.

`supported-ocl.properties` es un control de cambios, no un compilador OCL a SQL. Si cambia una expresión OCL, hay que revisar su correspondencia tecnológica en ATL y luego actualizar ese perfil conscientemente; cambiar solo el listado no implementa la nueva regla. Las políticas temporales y transaccionales del PSM siguen siendo especificaciones que las plantillas Acceleo deberán implementar y probar en M2T.

## Siguiente fase

Acceleo M2T ya toma como entrada este PSM generado y produce el backend. Consultar la [guía M2T](../m2t/README.md) para generación, procedencia y límites de compilación comprobados. La referencia manual sigue sin ser la entrada habitual del generador.
