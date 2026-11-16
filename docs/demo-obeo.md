# Guion de demostración en Obeo

Demostración de la cadena **PIM → ATL → PSM → Acceleo → interfaz** desde Obeo Designer. Las transformaciones se ejecutan desde **Run → External Tools** con los motores ATL 4.12 y Acceleo 3.7.15 de la propia instalación de Obeo. La salida aparece en la consola de Obeo y el workspace se refresca solo.

## Preparación (una vez)

1. En Obeo: **File → Import → General → Existing Projects into Workspace**. Seleccionar la carpeta raíz del repositorio y **no** marcar *Copy projects into workspace*. Importar los tres proyectos:
   - `ec.edu.ucuenca.mda.vuelos.modeling`
   - `ec.edu.ucuenca.mda.vuelos.m2m`
   - `ec.edu.ucuenca.mda.vuelos.m2t`
2. Revisar la vista **Problems** (Window → Show View → Problems). No debe haber errores rojos en los tres proyectos.
3. Abrir **Run → External Tools → External Tools Configurations…**. Deben aparecer bajo *Program*:
   - `1 ATL PIM a PSM`
   - `2 Acceleo PSM a Frontend`
   - `3 Acceleo PSM a Backend`

   Si no aparecen, crearlas a mano: *Program → New*.
   - **Location:** `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`
   - **Working Directory:** raíz del repositorio
   - **Arguments:** los mismos que contiene el `.launch` correspondiente, en `transformations/m2m/launch` o `transformations/m2t/launch`.
   - **Pestaña Refresh:** *Refresh resources upon completion → The entire workspace*.
   - **Pestaña Common:** marcar *Display in favorites menu → External Tools*.
4. Abrir `generated/frontend/index.html` en Chrome o Edge.

## Demostración

| Paso | En Obeo / navegador | Qué decir |
|---|---|---|
| 1 | Navegador: Vuelos → Pasajeros → Reservas → Emitir boleto | La aplicación funciona y fue generada desde el modelo |
| 2 | Abrir `modeling/metamodels/vuelos.aird` (o `vuelos.ecore`) | PIM: solo conceptos del negocio, sin tecnología |
| 3 | En **Pasajero** añadir un EAttribute `nacionalidad`, EType `EString`, Lower Bound `0`. **Ctrl+S** | Cambio el modelo, no el código |
| 4 | Abrir `transformations/m2m/src/PimToSpringPsm.atl`, reglas `ClassToEntity` y `AttributeToColumn` | Reglas M2M |
| 5 | **Run → External Tools → 1 ATL PIM a PSM** | Consola: «PSM producido por ATL» |
| 6 | Abrir `models/generated/spring-psm.xmi` con *Open With → Sample Reflective Ecore Model Editor* → Application → Entity Pasajero → attributes | `nacionalidad` ya es un Attribute con columna SQL |
| 7 | Abrir `transformations/m2t/src/frontend.mtl` y mostrar el `[for (a : Attribute ...)]` | Plantilla M2T |
| 8 | **Run → External Tools → 2 Acceleo PSM a Frontend** | Consola: «Archivos escritos por Acceleo: 5» |
| 9 | Navegador: **Ctrl+F5** → Pasajeros → Registrar | Aparece el campo **Nacionalidad** |
| 10 (opcional) | **3 Acceleo PSM a Backend** y abrir `generated/backend/.../domain/Pasajero.java` y `schema.sql` | El mismo cambio llega al backend |

Ejecutar siempre **1 antes de 2 o 3**: Acceleo comprueba los hashes de procedencia del PSM y rechaza un PSM desactualizado respecto al PIM.

## Cambios admitidos

- Atributos nuevos de tipo EString, EInt o EDate en las ocho clases existentes. Se recomiendan opcionales (lower bound 0).
- No admitidos: clases nuevas, herencia, atributos multivaluados, el nombre `id` o cambios en expresiones OCL. La transformación los rechaza de forma explícita.

## Volver al estado entregado

```powershell
git checkout -- modeling generated
```
