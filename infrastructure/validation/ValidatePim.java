import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/** Infraestructura manual de verificacion EMF. No es un generador ni una transformacion. */
public final class ValidatePim {
    static Object value(EObject o, String name) {
        return o.eGet(o.eClass().getEStructuralFeature(name));
    }
    @SuppressWarnings("unchecked")
    static List<EObject> objects(EObject o, String name) {
        return (List<EObject>) value(o, name);
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    static void print(Diagnostic d, String indent) {
        System.out.println(indent + d.getSeverity() + " " + d.getMessage());
        for (Diagnostic child : d.getChildren()) print(child, indent + "  ");
    }
    static void valid(EObject root, String label) {
        Diagnostic d = Diagnostician.INSTANCE.validate(root);
        print(d, label + ": ");
        require(d.getSeverity() == Diagnostic.OK, "Diagnostico no OK: " + label);
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Uso: ValidatePim metamodelo.ecore instancia.xmi");
        ResourceSet rs = new ResourceSetImpl();
        org.eclipse.ocl.ecore.OCL.initialize(rs);
        org.eclipse.ocl.ecore.delegate.OCLDelegateDomain.initialize(rs);
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource meta = rs.getResource(URI.createFileURI(Path.of(args[0]).toAbsolutePath().toString()), true);
        require(meta.getErrors().isEmpty(), "Errores cargando Ecore: " + meta.getErrors());
        EPackage pkg = (EPackage) meta.getContents().get(0);
        EcoreUtil.resolveAll(rs);
        valid(pkg, "Metamodelo");
        require(pkg.getEClassifiers().size() == 10, "Se esperan 9 EClasses y 1 EEnum");
        rs.getPackageRegistry().put(pkg.getNsURI(), pkg);
        Resource model = rs.getResource(URI.createFileURI(Path.of(args[1]).toAbsolutePath().toString()), true);
        require(model.getErrors().isEmpty(), "Errores cargando XMI: " + model.getErrors());
        EcoreUtil.resolveAll(rs);
        require(EcoreUtil.UnresolvedProxyCrossReferencer.find(rs).isEmpty(), "Referencias sin resolver");
        EObject root = model.getContents().get(0);
        valid(root, "Instancia");
        List<EObject> reservas = objects(root, "reservas");
        EObject vuelo = objects(root, "vuelos").get(0);
        require(reservas.size() == 2, "Se esperan dos reservas");
        require(objects(vuelo, "reservas").size() == 2, "Relacion inversa vuelo-reservas incompleta");
        EObject boleto = objects(root, "boletos").get(0);
        require(value(reservas.get(0), "boleto") == boleto, "Relacion inversa reserva-boleto incorrecta");
        require(value(boleto, "reserva") == reservas.get(0), "Relacion boleto-reserva incorrecta");
        require(value(vuelo, "ruta") == objects(root, "rutas").get(0), "Ruta incorrecta");
        require(objects((EObject)value(vuelo, "avion"), "vuelos").contains(vuelo), "Relacion inversa avion-vuelos incorrecta");
        require(((java.util.Date)value(vuelo, "llegada")).after((java.util.Date)value(vuelo, "salida")), "Horario del ejemplo incorrecto");
        var seats = new HashSet<Integer>();
        int capacity = (Integer)value((EObject)value(vuelo, "avion"), "capacidad");
        for (EObject r : reservas) {
            int seat = (Integer)value(r, "numeroAsiento");
            require(seat >= 1 && seat <= capacity && seats.add(seat), "Asientos del ejemplo incorrectos");
        }
        System.out.println("Ejemplo: referencias inversas, fechas y asientos 1/2 comprobados (no es validacion OCL).");

        // Control negativo estructural: se modifica una copia, nunca el archivo del usuario.
        EObject invalid = EcoreUtil.copy(root);
        EObject badReservation = objects(invalid, "reservas").get(0);
        badReservation.eSet(badReservation.eClass().getEStructuralFeature("pasajero"), null);
        Diagnostic negative = Diagnostician.INSTANCE.validate(invalid);
        print(negative, "Control negativo: ");
        require(negative.getSeverity() == Diagnostic.ERROR, "EMF debio rechazar reserva sin pasajero");

        // Serializar y volver a cargar comprueba que el modelo no depende solo de memoria.
        URI roundtripURI = URI.createURI("memory:/roundtrip.xmi");
        Resource roundtrip = rs.createResource(roundtripURI);
        roundtrip.getContents().add(EcoreUtil.copy(root));
        var bytes = new java.io.ByteArrayOutputStream();
        roundtrip.save(bytes, null);
        roundtrip.unload();
        roundtrip.load(new java.io.ByteArrayInputStream(bytes.toByteArray()), null);
        valid(roundtrip.getContents().get(0), "Recarga XMI");
        require(roundtrip.getErrors().isEmpty(), "Error al recargar XMI");
        System.out.println("RESULTADO: PIM verificado con EMF; control negativo rechazado correctamente.");
    }
}
