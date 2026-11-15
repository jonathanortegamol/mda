import java.nio.file.*;
import java.util.*;
import org.eclipse.emf.common.util.*;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.xmi.impl.*;
import org.eclipse.acceleo.parser.*;
import org.eclipse.acceleo.model.mtl.resource.*;
import org.eclipse.acceleo.engine.service.AcceleoService;

/** Orquestacion del compilador y motor Acceleo 3. No renderiza codigo Java ni SQL. */
public final class RunAcceleo {
    static final class TrackedStrategy extends org.eclipse.acceleo.engine.generation.strategy.DefaultStrategy {
        final Set<Path> emitted = new TreeSet<>();
        @Override public org.eclipse.acceleo.engine.generation.writers.AbstractAcceleoWriter createWriterFor(
                java.io.File file, org.eclipse.acceleo.engine.generation.writers.AbstractAcceleoWriter parent,
                boolean append, boolean lost, String encoding) throws java.io.IOException {
            emitted.add(file.toPath().toAbsolutePath().normalize());
            return super.createWriterFor(file,parent,append,lost,encoding);
        }
        @Override public org.eclipse.acceleo.engine.generation.writers.AbstractAcceleoWriter createWriterFor(
                java.io.File file, org.eclipse.acceleo.engine.generation.writers.AbstractAcceleoWriter parent,
                boolean append, boolean lost) throws java.io.IOException {
            emitted.add(file.toPath().toAbsolutePath().normalize());
            return super.createWriterFor(file,parent,append,lost);
        }
    }
    public static void main(String[] args) throws Exception {
        Path template = Path.of(args[0]), metamodel = Path.of(args[1]), model = Path.of(args[2]);
        Path emtl = Path.of(args[3]), destination = Path.of(args[4]);
        org.eclipse.emf.ecore.resource.URIConverter.URI_MAP.put(
            URI.createURI("platform:/resource/ec.edu.ucuenca.mda.vuelos.modeling/metamodels/spring-psm.ecore"),
            URI.createFileURI(metamodel.toAbsolutePath().toString()));
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore",new EcoreResourceFactoryImpl());
        for (String library : List.of("mtlstdlib.ecore","mtlnonstdlib.ecore")) {
            var local = RunAcceleo.class.getClassLoader().getResource("model/"+library);
            if (local == null) throw new IllegalStateException("Falta biblioteca local Acceleo: "+library);
            org.eclipse.emf.ecore.resource.URIConverter.URI_MAP.put(
                URI.createURI("http://www.eclipse.org/acceleo/mtl/3.0/"+library),URI.createURI(local.toExternalForm()));
        }
        ResourceSet rs = new AcceleoResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi",new XMIResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("emtl",new EMtlResourceFactoryImpl());
        EPackage psm = (EPackage)rs.getResource(URI.createFileURI(metamodel.toAbsolutePath().toString()),true).getContents().get(0);
        EPackage.Registry.INSTANCE.put(psm.getNsURI(),psm);
        EPackage.Registry.INSTANCE.put("platform:/resource/ec.edu.ucuenca.mda.vuelos.modeling/metamodels/spring-psm.ecore",psm);
        rs.getPackageRegistry().put(psm.getNsURI(),psm);
        org.eclipse.ocl.ecore.OCL.initialize(rs);
        Files.createDirectories(emtl.toAbsolutePath().getParent());
        Resource moduleResource = rs.createResource(URI.createFileURI(emtl.toAbsolutePath().toString()));
        AcceleoSourceBuffer buffer = new AcceleoSourceBuffer(template.toFile());
        new AcceleoParser(false).parse(buffer,moduleResource,List.of());
        if (!buffer.getProblems().getList().isEmpty()) throw new IllegalStateException("Errores Acceleo: " + buffer.getProblems().getMessage());
        moduleResource.save(null);
        System.out.println("Acceleo compilado: " + emtl);
        EObject application = rs.getResource(URI.createFileURI(model.toAbsolutePath().toString()),true).getContents().get(0);
        var strategy = new TrackedStrategy();
        var service = new AcceleoService(strategy);
        try {
            service.doGenerate(buffer.getAST(),"generate",application,destination.toFile(),new BasicMonitor());
            strategy.awaitCompletion();
            if (!service.hasGenerationOccurred()) throw new IllegalStateException("Acceleo no genero archivos");
            if (strategy.emitted.isEmpty()) throw new IllegalStateException("No se abrieron archivos de salida");
            Path target = destination.toAbsolutePath().normalize();
            for (Path file : strategy.emitted) if (!file.startsWith(target)) throw new IllegalStateException("Salida fuera de destino: "+file);
            Files.write(destination.resolve(".acceleo-files.txt"),strategy.emitted.stream().map(target::relativize).map(Path::toString).toList());
            System.out.println("Archivos escritos por Acceleo: " + strategy.emitted.size());
        } finally { service.dispose(); }
        System.out.println("Archivos producidos por Acceleo: " + destination);
    }
}
