# Entorno local de modelado

Inspección realizada el 15 de noviembre de 2026. No se han instalado complementos, cambiado variables de entorno ni modificado la instalación del usuario.

## Instalación encontrada

Obeo Designer está en ejecución desde:

```text
C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community\obeodesigner.exe
```

Se inspeccionaron `.eclipseproduct`, `obeodesigner.ini`, el inventario `configuration/org.eclipse.equinox.simpleconfigurator/bundles.info` y las carpetas `plugins` y `features` de esta instalación. La presencia de componentes no equivale a haber ejecutado sus validaciones o transformaciones.

| Componente | Evidencia local | Conclusión |
|---|---|---|
| Obeo Designer Community | Feature `fr.obeo.dsl.designer.ce.feature_12.0.0.202607221118` | Versión 12.0.0 |
| Eclipse Platform | `.eclipseproduct` y bundle de plataforma | 4.37.0, distribución 2025-09 |
| EMF SDK | Feature `org.eclipse.emf.sdk_2.43.0.v20250825-1255` | Disponible |
| EMF Ecore | Bundle `org.eclipse.emf.ecore` | 2.40.0.v20250803-1633 |
| EMF Common | Bundle `org.eclipse.emf.common` | 2.43.0.v20250629-1403 |
| Ecore Tools | Feature `org.eclipse.emf.ecoretools.design` | 3.5.2.202409301715 |
| Sirius | Bundle `org.eclipse.sirius` | 7.5.0.202601281512 |
| OCL clásico/Ecore | Bundles `org.eclipse.ocl` y `org.eclipse.ocl.ecore` | 3.22.0.v20240902-1518 |
| Integración EMF Validation/OCL | Feature `org.eclipse.emf.validation.ocl` | 1.14.0.202408231629 |
| OCL Pivot y editores Complete OCL | No encontrados en el inventario consultado | No asumir disponibles |
| AQL | Bundles `org.eclipse.acceleo.query`, `.ide` y `.doc` | 8.1.2 |
| Generador Acceleo | No aparecen bundles del generador Acceleo 3 ni Acceleo 4/AQL | Pendiente de instalación y prueba |
| ATL | No aparecen bundles `org.eclipse.m2m.atl` | Pendiente de instalación y prueba |

Las versiones de bundles EMF no tienen por qué coincidir entre sí: se registran por componente, sin interpretar esa diferencia como incompatibilidad.

## Java

- `java -version` en la terminal: Oracle Java 17.0.9; el comando se resuelve mediante `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`.
- Se encontraron instalaciones Temurin 17.0.15 y 21.0.13 y Oracle JDK 17, además de otros runtimes.
- Se ejecutó directamente el Java de `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe`: confirmó Temurin 21.0.13+9.
- `obeodesigner.ini` declara `-Dosgi.requiredJavaVersion=21`.
- El módulo `jvm.dll` del proceso Obeo abierto procede de ese JDK Temurin 21.0.13. Por tanto, se comprobó el runtime real del IDE, además del disponible en la terminal.

Para futuras ejecuciones independientes se deberá seleccionar el JDK explícitamente y no suponer que el Java de la terminal coincide con el del IDE. No se cambió el PATH ni JAVA_HOME.

## Compatibilidad y decisiones

La [página oficial de descarga de Obeo Designer](https://www.obeosoft.com/en/products/obeo-designer/download/) indica JRE 21. El [historial oficial](https://www.obeosoft.com/en/products/obeo-designer/change-log/) identifica Obeo Designer 12.0 como basado en Eclipse 2025-09. Ambos datos concuerdan con la instalación encontrada.

La base instalada permite preparar el PIM con Ecore y su visualización. Aún se debe comprobar con un modelo real la carga, las referencias, la validación y la apertura del diagrama; el proceso abierto solo demuestra el arranque del IDE.

**AQL 8.1.2 no equivale a tener instalado el generador Acceleo.** La [documentación oficial de Acceleo](https://help.eclipse.org/latest/topic/org.eclipse.acceleo.aql.doc/doc/index.html) distingue Acceleo 3, basado en expresiones OCL, de Acceleo 4, basado en AQL, y describe una migración entre ellos. No se escribirán plantillas suponiendo que son intercambiables. Antes de M2T habrá que seleccionar una versión concreta, resolver sus dependencias y ejecutar una generación mínima.

Para OCL se propone evaluar primero el motor clásico ya incluido. Si se elige Pivot o Complete OCL, habrá que instalar y verificar sus herramientas antes de documentar instrucciones que dependan de ellas. La asignación automática de asientos y la concurrencia seguirán requiriendo validaciones de operaciones, según el CIM.

ATL sigue siendo la opción propuesta para M2M. Su compatibilidad con este entorno no se ha probado; antes de esa fase se deberán resolver sus dependencias y ejecutar una transformación mínima. No se ha sustituido ATL ni Acceleo por scripts.

## Próximo paso delimitado

### Comprobación posterior a la instalación — 15 de noviembre de 2026

El usuario instaló los complementos y solicitó volver a validar e iniciar el PIM. Se volvió a consultar `bundles.info` y `features` de la misma instalación:

- Acceleo 3.7.15.202402190857: feature `org.eclipse.acceleo`; bundles engine, parser e ide.ui presentes.
- ATL 4.12.0.v202505101449: feature `org.eclipse.m2m.atl`; motor, ADT y EMFTVM presentes.
- No se requirió instalar el launcher Equinox de Acceleo ni el adaptador CDO de ATL para construir el PIM. El SDK ATL completo inicialmente seleccionado exigía CDO; el usuario instaló la feature ATL principal.
- La tabla anterior conserva la inspección inicial: sus filas de ATL y Acceleo no describen ya el estado actual. La instalación de componentes está comprobada, pero la compilación de plantillas y ejecución M2M/M2T aún no se han probado.
- Se compiló y ejecutó un verificador Java con los tres JAR de EMF de esta instalación. Validó el Ecore, su instancia y un control negativo; evidencia en [PIM](../pim/README.md). Esto prueba uso real de EMF fuera del IDE, no la compatibilidad completa de ATL/Acceleo.

El PIM se construyó y validó; el usuario abrió su diagrama en Obeo. Después se implementaron 23 invariantes OCL y se evaluaron 12 modelos con el motor clásico instalado. El usuario confirmó en el IDE el rechazo por AsientosConfirmadosUnicos y la aceptación del asiento reutilizado de una reserva cancelada. Se construyó el [PSM](../psm/README.md) y se validó con EMF.

Posteriormente ATL 4.12.0 se ejecutó en modo standalone mediante Atl2006Compiler y EMFVMLauncher. Se compiló el módulo, generó un PSM válido y comprobó equivalencia y propagación de un atributo; el parser de esta distribución también utiliza las bibliotecas EMFTVM. Evidencias e instrucciones en [M2M](../../transformations/m2m/README.md).

Acceleo 3.7.15 también se ejecutó mediante AcceleoParser y AcceleoService, usando las bibliotecas estándar desde sus JAR locales. Generó 56 archivos y se comprobó repetibilidad y propagación hasta Java/SQL. Maven 3.9.10 está instalado, pero su intento de resolver Boot 3.5.16 fue rechazado por permisos de conexión. Las fuentes Java generadas se compilaron con JDK 21 y dependencias de caché Boot 3.5.3 / Spring 6.2.8; doce pruebas JUnit pasaron tanto en el backend base como en la variante de propagación. Esto no acredita el empaquetado Maven 3.5.16 ni la integración PostgreSQL. Detalle en [M2T](../../transformations/m2t/README.md).

El usuario priorizó después las pantallas. Se añadió frontend.mtl y se generaron cinco archivos desde el PSM de ATL, con ocho formularios. Se verificaron regeneración idéntica y propagación de telefono a HTML/metadatos; las pruebas del adaptador local pasaron con el runtime Node incorporado en VS Code en modo CLI. No se controló la interfaz de VS Code para ejecutar código. Computer Use bloqueó la revisión visual porque no pudo identificar la URL de Chrome de forma fiable; no se eludió el bloqueo. Detalle en la [guía gráfica](../frontend/README.md).
