import java.nio.file.Path;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.ocl.ecore.OCL;

/** Ejecuta OCL real, embebido en EAnnotations; no reproduce reglas mediante Java. */
public final class ValidateOcl {
    static final String DELEGATE = "http://www.eclipse.org/emf/2002/Ecore/OCL";
    static String messages(Diagnostic d) {
        String text = d.getMessage() + "\n";
        for (Diagnostic child : d.getChildren()) text += messages(child);
        return text;
    }
    public static void main(String[] args) throws Exception {
        ResourceSet rs = new ResourceSetImpl();
        OCL.initialize(rs);
        org.eclipse.ocl.ecore.delegate.OCLDelegateDomain.initialize(rs);
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        EPackage pkg = (EPackage)rs.getResource(URI.createFileURI(Path.of(args[0]).toAbsolutePath().toString()), true).getContents().get(0);
        rs.getPackageRegistry().put(pkg.getNsURI(), pkg);
        EcoreUtil.resolveAll(rs);
        OCL ocl = OCL.newInstance();
        int count = 0;
        try {
            // Fail immediately for malformed or ill-typed OCL, even if no instance uses the class.
            for (EClassifier type : pkg.getEClassifiers()) {
                EAnnotation annotation = type.getEAnnotation(DELEGATE);
                if (annotation == null) continue;
                var helper = ocl.createOCLHelper();
                helper.setContext(type);
                for (var expression : annotation.getDetails()) {
                    helper.createInvariant(expression.getValue());
                    count++;
                }
            }
        } finally { ocl.dispose(); }
        ValidatePim.require(count == 23, "Se esperaban 23 invariantes OCL compilados");
        Resource model = rs.getResource(URI.createFileURI(Path.of(args[1]).toAbsolutePath().toString()), true);
        EcoreUtil.resolveAll(rs);
        ValidatePim.require(model.getErrors().isEmpty(), "XMI con errores: " + model.getErrors());
        ValidatePim.require(EcoreUtil.UnresolvedProxyCrossReferencer.find(rs).isEmpty(), "Proxies sin resolver");
        Diagnostic d = Diagnostician.INSTANCE.validate(model.getContents().get(0));
        String expected = args.length > 2 ? args[2] : "";
        String detail = messages(d);
        ValidatePim.require(!detail.contains("An exception occurred") && !detail.contains("Unable to find delegate"), "Fallo de evaluacion/delegado OCL: " + detail);
        if (expected.isEmpty()) {
            ValidatePim.require(d.getSeverity() == Diagnostic.OK, "Se esperaba modelo valido: " + detail);
        } else {
            ValidatePim.require(d.getSeverity() == Diagnostic.ERROR && detail.contains("'" + expected + "'"), "No se detecto la restriccion esperada " + expected + ": " + detail);
        }
        System.out.println("OK " + Path.of(args[1]).getFileName() + ": 23 invariantes compilados; " + (expected.isEmpty() ? "modelo valido" : "violacion detectada: " + expected));
    }
}
