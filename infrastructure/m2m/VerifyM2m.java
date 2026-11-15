import java.nio.file.*;
import java.util.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/** Pruebas de ATL: comparacion semantica y variacion de entrada, sin construir objetos PSM. */
public final class VerifyM2m {
    static ResourceSet resources(Path root) {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi",new XMIResourceFactoryImpl());
        EPackage psm = (EPackage)load(rs,root.resolve("modeling/metamodels/spring-psm.ecore"));
        rs.getPackageRegistry().put(psm.getNsURI(),psm);
        return rs;
    }
    static EObject load(ResourceSet rs, Path path) {
        return rs.getResource(URI.createFileURI(path.toAbsolutePath().toString()),true).getContents().get(0);
    }
    static String key(EObject o) {
        EStructuralFeature name = o.eClass().getEStructuralFeature("name");
        return (o.eContainer()==null ? "" : key(o.eContainer())+"/") + o.eClass().getName()+":"+(name==null ? "" : o.eGet(name));
    }
    static String scalar(Object v) { return v==null ? "<null>" : v.toString().length()+":"+v; }
    static String canonical(EObject o) {
        StringBuilder result = new StringBuilder(o.eClass().getName()+"{");
        for (EStructuralFeature f : o.eClass().getEAllStructuralFeatures()) {
            result.append(f.getName()).append('=');
            Object value = o.eGet(f);
            if (f instanceof EReference r) {
                List<?> values = f.isMany() ? (List<?>)value : value==null ? List.of() : List.of(value);
                var parts = new ArrayList<String>();
                for (Object item : values) parts.add(r.isContainment() ? canonical((EObject)item) : key((EObject)item));
                // El orden de literales y de columnas se conserva; el orden de componentes no es semantico.
                if (!f.getName().equals("literals")) Collections.sort(parts);
                result.append(parts);
            } else if (f.isMany()) result.append(((List<?>)value).stream().map(VerifyM2m::scalar).toList());
            else result.append(scalar(value));
            result.append(';');
        }
        return result.append('}').toString();
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), generated = Path.of(args[1]);
        ResourceSet rs = resources(root);
        EObject reference = load(rs,root.resolve("modeling/models/spring-psm-referencia.xmi"));
        EObject actual = load(rs,generated);
        EcoreUtil.resolveAll(rs);
        String expected = canonical(reference), observed = canonical(actual);
        if (!expected.equals(observed)) {
            Path report = root.resolve("transformations/m2m/build/comparison");Files.createDirectories(report);
            Files.writeString(report.resolve("reference.txt"),expected);Files.writeString(report.resolve("actual.txt"),observed);
            throw new IllegalStateException("PSM generado difiere semanticamente de la referencia; ver build/comparison");
        }
        System.out.println("OK: PSM ATL semanticamente equivalente a la referencia manual (no se comparan IDs XMI ni orden de componentes).");
        Path tests = root.resolve("transformations/m2m/build/tests");Files.createDirectories(tests);
        EPackage original = (EPackage)load(rs,root.resolve("modeling/metamodels/vuelos.ecore"));
        EPackage changed = EcoreUtil.copy(original);
        EAttribute phone = EcoreFactory.eINSTANCE.createEAttribute();
        phone.setName("telefono");phone.setEType(EcorePackage.eINSTANCE.getEString());phone.setLowerBound(1);
        ((EClass)changed.getEClassifier("Pasajero")).getEStructuralFeatures().add(phone);
        Path variant = tests.resolve("vuelos-telefono.ecore");
        Resource variantResource = rs.createResource(URI.createFileURI(variant.toAbsolutePath().toString()));
        variantResource.getContents().add(changed);variantResource.save(null);
        String[] transform = {
            root.resolve("transformations/m2m/src/PimToSpringPsm.atl").toString(),variant.toString(),
            root.resolve("modeling/metamodels/spring-psm.ecore").toString(),tests.resolve("propagation.asm").toString(),
            tests.resolve("spring-psm-telefono.xmi").toString(),root.resolve("transformations/m2m/supported-ocl.properties").toString()
        };
        RunAtl.main(transform);
        ValidatePsm.main(new String[]{transform[2],transform[4],variant.toString()});
        EObject propagated = load(rs,Path.of(transform[4]));
        @SuppressWarnings("unchecked") List<EObject> entities = (List<EObject>)propagated.eGet(propagated.eClass().getEStructuralFeature("entities"));
        EObject passenger = entities.stream().filter(e -> e.eGet(e.eClass().getEStructuralFeature("name")).equals("Pasajero")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") List<EObject> fields = (List<EObject>)passenger.eGet(passenger.eClass().getEStructuralFeature("attributes"));
        if (fields.stream().noneMatch(a -> a.eGet(a.eClass().getEStructuralFeature("name")).equals("telefono"))) throw new IllegalStateException("No se propago telefono");
        System.out.println("OK: telefono agregado a una copia PIM aparece como Attribute del PSM producido por ATL.");
        EPackage unsupported = EcoreUtil.copy(original);
        ((EClass)unsupported.getEClassifier("Reserva")).getEAnnotation("http://www.eclipse.org/emf/2002/Ecore/OCL")
            .getDetails().put("AsientoEnRango","false");
        Path unsupportedPath = tests.resolve("vuelos-ocl-no-soportado.ecore");
        Resource unsupportedResource = rs.createResource(URI.createFileURI(unsupportedPath.toAbsolutePath().toString()));
        unsupportedResource.getContents().add(unsupported);unsupportedResource.save(null);
        boolean rejected = false;
        try { RunAtl.preflight(unsupportedPath,Path.of(transform[5])); }
        catch (IllegalArgumentException expectedError) { rejected = expectedError.getMessage().contains("invariantes OCL"); }
        if (!rejected) throw new IllegalStateException("No se rechazo cambio OCL no soportado");
        System.out.println("OK: una expresion OCL modificada exige revisar el perfil; no se ignora silenciosamente.");
    }
}
