import java.nio.file.*;
import java.util.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.m2m.atl.core.*;
import org.eclipse.m2m.atl.core.emf.*;
import org.eclipse.m2m.atl.engine.compiler.atl2006.Atl2006Compiler;
import org.eclipse.m2m.atl.engine.emfvm.launch.EMFVMLauncher;

/** Solo orquesta el compilador y VM oficiales de ATL; no construye objetos PSM. */
public final class RunAtl {
    static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException("Perfil PIM no soportado: " + message);
    }
    static void preflight(Path pim, Path profile) throws Exception {
        var rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",new EcoreResourceFactoryImpl());
        var resource = rs.getResource(org.eclipse.emf.common.util.URI.createFileURI(pim.toAbsolutePath().toString()),true);
        require(resource.getContents().size() == 1 && resource.getContents().get(0) instanceof EPackage,"se requiere un EPackage raiz");
        EPackage pkg = (EPackage)resource.getContents().get(0);
        require(Diagnostician.INSTANCE.validate(pkg).getSeverity() == 0,"Ecore invalido");
        require(pkg.getNsURI().equals("https://ucuenca.edu.ec/mda/vuelos/1.0") && pkg.getESubpackages().isEmpty(),"namespace o subpaquetes");
        var expected = new Properties();
        try (var reader = Files.newBufferedReader(profile)) { expected.load(reader); }
        var actual = new Properties();
        var classNames = new HashSet<String>();
        for (EClassifier type : pkg.getEClassifiers()) {
            if (type instanceof EClass c) {
                classNames.add(c.getName());
                require(c.getESuperTypes().isEmpty() && c.getEOperations().isEmpty() && !c.isAbstract(),"herencia, operaciones o clases abstractas");
                if (!c.getName().equals("SistemaVuelos")) for (EStructuralFeature field : c.getEStructuralFeatures()) {
                    require(!field.isDerived() && !field.isTransient() && !field.isVolatile() && !field.isUnsettable(),"campo calculado o transitorio");
                    require(!field.getName().equals("id"),"id esta reservado para la clave tecnologica");
                    if (field instanceof EAttribute a) {
                        require(a.getUpperBound() == 1 && !a.isID() && a.getDefaultValueLiteral() == null,"atributos multivaluados, EID o defaults explicitos");
                        require(Set.of("EString","EInt","EDate","EstadoReserva").contains(a.getEAttributeType().getName()),"tipo de atributo: " + a.getEAttributeType().getName());
                    } else if (field instanceof EReference r) {
                        require(!r.isContainment() && r.getEOpposite()!=null && r.getEReferenceType().getEPackage()==pkg,"referencia sin opuesta, externa o contenida");
                        require(r.getUpperBound()==1 || r.getUpperBound()==-1,"multiplicidad no soportada");
                        boolean own = r.getUpperBound()==1 && (r.getEOpposite().getUpperBound()==-1 || r.getLowerBound()==1);
                        EReference opposite = r.getEOpposite();
                        boolean otherOwn = opposite.getUpperBound()==1 && (opposite.getEOpposite().getUpperBound()==-1 || opposite.getLowerBound()==1);
                        require(own!=otherOwn,"relacion sin propietario inequívoco");
                    }
                }
            } else if (type instanceof EEnum en) {
                require(en.getName().equals("EstadoReserva") && en.getELiterals().stream().map(EEnumLiteral::getName).toList().equals(List.of("CONFIRMADA","CANCELADA")),"enum no soportado");
            } else require(false,"clasificador adicional");
            EAnnotation ocl = type.getEAnnotation("http://www.eclipse.org/emf/2002/Ecore/OCL");
            if (ocl != null) for (var d : ocl.getDetails()) actual.setProperty(type.getName()+"."+d.getKey(),d.getValue());
            EAnnotation names = type.getEAnnotation(EcorePackage.eNS_URI);
            Set<String> declared = names == null || names.getDetails().get("constraints")==null ? Set.of() : new HashSet<>(Arrays.asList(names.getDetails().get("constraints").split("\\s+")));
            require(declared.equals(ocl == null ? Set.of() : ocl.getDetails().keySet()),"lista de invariantes inconsistente");
        }
        require(classNames.equals(Set.of("SistemaVuelos","Aeropuerto","Aerolinea","Avion","Ruta","Vuelo","Pasajero","Reserva","Boleto")),"se requiere el perfil de ocho entidades");
        require(actual.equals(expected),"invariantes OCL nuevas, modificadas o ausentes; revisar el perfil y las reglas ATL");
        require(pkg.getEAnnotation(EcorePackage.eNS_URI).getDetails().get("validationDelegates").equals("http://www.eclipse.org/emf/2002/Ecore/OCL"),"delegado OCL");
    }
    public static void main(String[] args) throws Exception {
        Path atl = Path.of(args[0]), pim = Path.of(args[1]), metamodel = Path.of(args[2]);
        Path asm = Path.of(args[3]), output = Path.of(args[4]);
        preflight(pim,Path.of(args[5]));
        Files.createDirectories(asm.toAbsolutePath().getParent());
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (var source = Files.newInputStream(atl); var compiled = Files.newOutputStream(asm)) {
            EObject[] problems = new Atl2006Compiler().compileWithProblemModel(source, compiled);
            for (EObject problem : problems) {
                System.err.println(problem);
                for (var feature : problem.eClass().getEAllStructuralFeatures())
                    System.err.println(feature.getName() + ": " + problem.eGet(feature));
            }
            if (problems.length != 0) throw new IllegalStateException("ATL: diagnosticos de compilacion: " + problems.length);
        }
        System.out.println("ATL compilado: " + asm);
        EMFModelFactory factory = new EMFModelFactory();
        EMFInjector injector = new EMFInjector();
        IReferenceModel ecore = factory.getMetametamodel();
        IReferenceModel spring = factory.newReferenceModel();
        injector.inject(spring, metamodel.toAbsolutePath().toUri().toString());
        IModel input = factory.newModel(ecore), result = factory.newModel(spring);
        injector.inject(input, pim.toAbsolutePath().toUri().toString());
        EMFVMLauncher vm = new EMFVMLauncher();
        vm.initialize(new HashMap<>());
        vm.addInModel(input,"IN","ECORE"); vm.addOutModel(result,"OUT","SPRINGPSM");
        try (var module = Files.newInputStream(asm)) {
            vm.launch("run",null,Collections.emptyMap(),module);
        }
        new EMFExtractor().extract(result, output.toAbsolutePath().toUri().toString(),
            Map.of(XMLResource.OPTION_SCHEMA_LOCATION,true,XMLResource.OPTION_ENCODING,"UTF-8"));
        System.out.println("PSM producido por ATL: " + output);
    }
}
