# Restricciones OCL del PIM

La fuente ejecutable está en [vuelos.ecore](../metamodels/vuelos.ecore), dentro de EAnnotations. Se utiliza **Eclipse OCL clásico**, disponible en Obeo, sin requerir Pivot ni Complete OCL. Las expresiones no se duplican en un archivo `.ocl` independiente para evitar divergencias y tener que instalar otro editor.

El EPackage declara `validationDelegates = http://www.eclipse.org/emf/2002/Ecore/OCL`. Cada EClass lista sus nombres de restricciones en la anotación Ecore `constraints`; la anotación con el URI OCL contiene las expresiones. Esta es la [integración estándar de delegados EMF/OCL](https://help.eclipse.org/latest/topic/org.eclipse.ocl.doc/help/Delegates.html).

## Catálogo ejecutable

| Contexto | Restricciones |
|---|---|
| SistemaVuelos | AeropuertoIdentidadUnica, AerolineaIdentidadUnica, AvionIdentidadUnica, VueloIdentidadUnica, PasajeroIdentidadUnica, ReservaIdentidadUnica, BoletoIdentidadUnica, RutasUnicas, ReferenciasDentroDelSistema |
| Aeropuerto | TextosNoVacios |
| Aerolinea | TextosNoVacios |
| Avion | TextosNoVacios, CapacidadPositiva |
| Ruta | OrigenDistintoDestino |
| Vuelo | TextosNoVacios, CapacidadRespetada, AsientosConfirmadosUnicos, PasajerosConfirmadosUnicos |
| Pasajero | TextosNoVacios |
| Reserva | TextosNoVacios, AsientoEnRango |
| Boleto | TextosNoVacios, ReservaConfirmada |

Son **23 invariantes**: `TextosNoVacios` se declara por separado en siete contextos. Las identidades se comparan dentro de cada colección de SistemaVuelos, sin usar `allInstances()`. Las referencias de negocio deben apuntar a elementos contenidos en la misma raíz; se comprueba desde SistemaVuelos.

Ejemplo de expresión en el contexto Vuelo, `AsientosConfirmadosUnicos`:

```ocl
self.reservas->select(r | r.estado = EstadoReserva::CONFIRMADA)
    ->isUnique(r | r.numeroAsiento)
```

En Reserva, `AsientoEnRango`:

```ocl
self.numeroAsiento >= 1 and self.numeroAsiento <= self.vuelo.avion.capacidad
```

Las reservas canceladas conservan un número dentro del rango; no bloquean la unicidad de asientos ni de pasajeros entre confirmadas. El asiento del boleto se obtiene por su reserva, sin nuevo campo.

## Distribución de las reglas del CIM

| Reglas | Implementación en esta fase | Trabajo posterior |
|---|---|---|
| RN-01 | Cardinalidades de Ruta y OrigenDistintoDestino | Rechazo de operaciones en aplicación |
| RN-02 | Fechas requeridas por Ecore; aserción sobre el ejemplo base | Comparar llegada y salida en validación temporal adicional |
| RN-03 | CapacidadRespetada | Garantía transaccional ante concurrencia |
| RN-04 | PasajerosConfirmadosUnicos | Garantía transaccional ante concurrencia |
| RN-05, RN-06 | Referencias requeridas y ReferenciasDentroDelSistema | Protección de operaciones y persistencia |
| RN-07 | Cardinalidades opuestas y ReservaConfirmada | Emisión idempotente y concurrencia |
| RN-08 | No implementada como invariante OCL | Comprobar solapamientos temporales al programar y ante concurrencia |
| RN-09 | CapacidadPositiva | Validación de entrada |
| RN-10 | Siete invariantes de identidad por colección | Protección de unicidad en persistencia |
| RN-11 | RutasUnicas, sobre par dirigido | Protección en aplicación/persistencia |
| RN-12 | No implementada: depende del momento de operación | Validar salidas futuras y momento de reserva/emisión/cancelación |
| RN-13 | Los conteos filtran confirmadas; un boleto requiere confirmada | Validar transición, ausencia de boleto, plazo y conservación histórica |
| RN-14 | Cardinalidad obligatoria de aerolínea; pertenencia a la raíz | Derivar aerolínea operadora en consultas |
| RN-15 | Cardinalidades y TextosNoVacios | Prohibición de borrado y normalización de texto |
| RN-16 | AsientoEnRango | Validación de entrada |
| RN-17 | AsientosConfirmadosUnicos por vuelo | Garantía transaccional ante concurrencia |
| RN-18 | Rango, cupo y unicidad validan el resultado | Asignar menor asiento libre durante la operación |
| RN-19 | Boleto comparte asiento por referencia; canceladas no bloquean | Liberación, conservación del número y prohibición de cambios durante operaciones |

**Límite temporal deliberado:** se conserva EDate para interoperabilidad EMF. No se introducen operadores de fecha ni servicios Java dentro de expresiones OCL que dependan de extensiones particulares. RN-02 y RN-08 quedan para validadores temporales adicionales del sistema generado, junto con RN-12. Esta fase no afirma haber implementado esas reglas. No se cambian las reglas del CIM ni se reemplazan transformaciones con scripts.

`TextosNoVacios` rechaza cadenas vacías y valores indefinidos; todavía no normaliza mayúsculas ni elimina espacios. La interfaz y los servicios futuros deberán normalizar entradas antes de comprobar identidades. Las restricciones trabajan sobre el estado del modelo: no reconstruyen su historial ni prueban concurrencia.

## Ejecución en Obeo

1. Guardar el `.aird` que estabas organizando. Cerrar los editores `.ecore` y `.xmi`, sin sobrescribir con copias antiguas los cambios externos del metamodelo.
2. Seleccionar el proyecto y pulsar **F5 / Refresh**. Si Obeo ofrece recargar archivos modificados externamente, aceptar la recarga. No se cambió la distribución de tu diagrama.
3. Abrir `models/vuelos-ejemplo.xmi` con **Sample Reflective Ecore Model Editor**.
4. Seleccionar la raíz **SistemaVuelos** y usar **Validate**. Se espera éxito. Validar solo el `.ecore` comprueba el metamodelo, no las reglas de su instancia.
5. Abrir `models/validation/invalido-asiento-repetido.xmi` con el mismo editor y validar su raíz. Debe aparecer `AsientosConfirmadosUnicos`.
6. Repetir con `invalido-origen-destino.xmi` y comprobar `OrigenDistintoDestino`.
7. Validar `valido-asiento-reutilizado.xmi`: debe aceptarse aunque dos reservas tengan el número 2, porque una está cancelada.

Los diagnósticos pueden aparecer en una ventana de validación; expandir sus detalles. Dependiendo del editor también pueden mostrarse en Problems. No se ha ejecutado esta interacción en tu IDE; sí se ejecutó el mismo mecanismo Diagnostician y el motor OCL fuera del IDE.

Si aparece `Unable to find delegate`, falta la activación/integración del motor OCL clásico; no es un rechazo correcto de la regla. Revisar `org.eclipse.ocl.ecore` en Installation Details y reiniciar Obeo. No aceptar como prueba una validación que solo funciona porque omite el delegado.

## Prueba automática reproducible

Desde la raíz del repositorio:

```powershell
& './infrastructure/validation/validate-pim.ps1' `
  -ObeoHome 'C:\Users\Jonathan Ortega\Downloads\ObeoDesigner-Community' `
  -JdkHome 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
```

El script usa los JAR de EMF, Eclipse OCL y sus dependencias instaladas. Compila `ValidatePim.java` y `ValidateOcl.java`; inicializa OCL y OCLDelegateDomain para la ejecución standalone. Compila las 23 expresiones con el parser/tipador de OCL y valida mediante **Diagnostician**, que invoca los delegados OCL. Las reglas no están reescritas como condiciones Java.

El [manifiesto de escenarios](../models/validation/cases.json) asocia cada XMI con el nombre de la restricción esperada. Las pruebas inválidas requieren severidad ERROR y el nombre concreto en el diagnóstico; una excepción del delegado no cuenta como resultado satisfactorio. Algunos modelos incumplen más de una regla: por ejemplo, bajar la capacidad a 1 también deja el asiento 2 fuera de rango.

Resultado ejecutado el 15 de noviembre de 2026: **12 modelos evaluados correctamente** (ejemplo base, diez inválidos y un válido adicional), con 23 invariantes compiladas en cada ejecución. El verificador estructural anterior y su control negativo también pasan. No se han probado todavía PSM, ATL, Acceleo ni aplicación.

| Archivo en `models/validation/` | Resultado esperado y observado |
|---|---|
| invalido-origen-destino.xmi | OrigenDistintoDestino |
| invalido-capacidad.xmi | CapacidadPositiva |
| invalido-sobrecupo.xmi | CapacidadRespetada |
| invalido-asiento-repetido.xmi | AsientosConfirmadosUnicos |
| invalido-asiento-fuera-rango.xmi | AsientoEnRango |
| invalido-pasajero-duplicado.xmi | PasajerosConfirmadosUnicos |
| invalido-boleto-reserva-cancelada.xmi | ReservaConfirmada |
| invalido-identidad.xmi | AeropuertoIdentidadUnica |
| invalido-texto-vacio.xmi | TextosNoVacios |
| invalido-ruta-duplicada.xmi | RutasUnicas |
| valido-asiento-reutilizado.xmi | Aceptado |

Los diez escenarios negativos demuestran violaciones concretas, sin pretender probar exhaustivamente las 23 invariantes ni todas sus combinaciones. Los archivos son datos de prueba persistidos; no son salidas de una transformación PIM → PSM.

Comprobación posterior en Obeo: el usuario mostró el diagnóstico `AsientosConfirmadosUnicos` sobre DEMO-001 y confirmó que `valido-asiento-reutilizado.xmi` pasó la validación. Por tanto, esos dos casos también quedaron comprobados mediante el editor Reflective del IDE. Después autorizó continuar con el PSM.
