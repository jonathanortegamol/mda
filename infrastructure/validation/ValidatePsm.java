import java.nio.file.Path;
import java.util.*;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/** Verificacion manual de un PSM. No construye modelos ni genera codigo de aplicacion. */
public final class ValidatePsm {
    static Object get(EObject o, String field) { return o.eGet(o.eClass().getEStructuralFeature(field)); }
    static String str(EObject o, String field) { return String.valueOf(get(o, field)); }
    @SuppressWarnings("unchecked")
    static List<EObject> list(EObject o, String field) { return (List<EObject>)get(o, field); }
    static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    static void unique(Set<String> names, String name, String scope) {
        require(!name.isBlank() && names.add(name), "Nombre vacio o repetido en " + scope + ": " + name);
    }
    static void emf(EObject o, String label) {
        Diagnostic d = Diagnostician.INSTANCE.validate(o);
        require(d.getSeverity() == Diagnostic.OK, label + ": " + detail(d));
    }
    static String detail(Diagnostic d) {
        String s = d.getMessage();
        for (Diagnostic child : d.getChildren()) s += "\n" + detail(child);
        return s;
    }
    static void semantic(EObject app, EPackage pim) {
        List<EObject> entities = list(app, "entities");
        List<EObject> enums = list(app, "enums");
        require(entities.size() == 8 && enums.size() == 1, "Referencia esperada: 8 entidades y 1 enum");
        require((Integer)get(app,"javaVersion") == 21, "Java objetivo esperado: 21");
        EObject database = (EObject)get(app,"database");
        require(str(database,"product").equals("PostgreSQL"), "Base objetivo incorrecta");
        Set<String> tables = new HashSet<>(), types = new HashSet<>(), componentNames = new HashSet<>(), sqlNames = new HashSet<>(), sourceClasses = new HashSet<>();
        for (EObject en : enums) {
            unique(types,str(en,"name"),"tipos");
            require(str(en,"storage").equals("STRING"), "Enum debe persistirse por nombre");
            Set<String> literals = new HashSet<>();
            for (EObject lit : list(en,"literals")) unique(literals,str(lit,"name"),"literales");
            EEnum source = (EEnum)pim.getEClassifier(str(en,"name"));
            require(source != null && literals.equals(new HashSet<>(source.getELiterals().stream().map(EEnumLiteral::getName).toList())), "Enum difiere del PIM");
        }
        for (EObject e : entities) {
            String name = str(e,"name");
            unique(types,name,"tipos"); unique(tables,str(e,"tableName"),"tablas");
            unique(sourceClasses,str(e,"sourceClass"),"clases origen");
            EClass source = (EClass)pim.getEClassifier(str(e,"sourceClass"));
            require(source != null && !source.getName().equals("SistemaVuelos"), "Origen de entidad incorrecto: " + name);
            require(name.equals(source.getName()), "Nombre de entidad debe conservar clase PIM");
            Set<String> fields = new HashSet<>(), columns = new HashSet<>();
            int primaryKeys = 0;
            for (EObject a : list(e,"attributes")) {
                unique(fields,str(a,"name"),name+".campos"); unique(columns,str(a,"columnName"),name+".columnas");
                if ((Boolean)get(a,"primaryKey")) {
                    primaryKeys++;
                    require(str(a,"name").equals("id") && str(a,"javaType").equals("LONG") && (Boolean)get(a,"generated") && !(Boolean)get(a,"nullable") && str(a,"idStrategy").equals("IDENTITY"), "ID tecnologico incorrecto");
                } else {
                    EStructuralFeature feature = source.getEStructuralFeature(str(a,"name"));
                    require(feature instanceof EAttribute, "Atributo sin correspondencia PIM: " + name + "." + str(a,"name"));
                    String expected = switch (feature.getEType().getName()) {
                        case "EString" -> "STRING";
                        case "EInt" -> "INTEGER";
                        case "EDate" -> "OFFSET_DATE_TIME";
                        default -> "ENUM";
                    };
                    require(str(a,"javaType").equals(expected), "Mapeo de tipo incorrecto: " + name + "." + str(a,"name"));
                    require((Boolean)get(a,"nullable") == (feature.getLowerBound() == 0), "Nulabilidad difiere del PIM");
                }
                String sql = switch (str(a,"javaType")) {
                    case "STRING" -> "varchar(255)";
                    case "INTEGER" -> "integer";
                    case "LONG" -> "bigint";
                    case "OFFSET_DATE_TIME" -> "timestamp with time zone";
                    default -> "varchar(32)";
                };
                require(str(a,"sqlType").equals(sql), "Tipo SQL inconsistente");
                require(str(a,"javaType").equals("ENUM") == (get(a,"enumType") != null), "Referencia al enum inconsistente");
                if (get(a,"enumType") != null) require(enums.contains(get(a,"enumType")), "Enum fuera de la aplicacion");
            }
            require(primaryKeys == 1, "Cada entidad requiere una PK");
            for (EObject a : list(e,"associations")) {
                unique(fields,str(a,"name"),name+".campos");
                EObject target = (EObject)get(a,"target"), opposite = (EObject)get(a,"opposite");
                require(entities.contains(target) && opposite.eContainer() == target && get(opposite,"target") == e && get(opposite,"opposite") == a, "Asociacion inversa inconsistente");
                EStructuralFeature sourceFeature = source.getEStructuralFeature(str(a,"name"));
                require(sourceFeature instanceof EReference, "Referencia no existe en PIM");
                require(sourceFeature.getEType().getName().equals(str(target,"sourceClass")) && sourceFeature.getLowerBound() == (Integer)get(a,"lower") && sourceFeature.getUpperBound() == (Integer)get(a,"upper"), "Destino o cardinalidad difiere del PIM");
                boolean owner = (Boolean)get(a,"owning");
                require(str(a,"fetch").equals("LAZY"), "Fetch esperado: LAZY");
                require(owner != (Boolean)get(opposite,"owning"), "Relacion debe tener un solo propietario");
                String kind = str(a,"kind");
                require(kind.equals("ONE_TO_ONE") ? str(opposite,"kind").equals("ONE_TO_ONE") : kind.equals("MANY_TO_ONE") ? str(opposite,"kind").equals("ONE_TO_MANY") : str(opposite,"kind").equals("MANY_TO_ONE"), "Tipos JPA opuestos inconsistentes");
                require((Integer)get(a,"upper") == (kind.equals("ONE_TO_MANY") ? -1 : 1), "Multiplicidad JPA inconsistente");
                if (owner) {
                    require(str(a,"onDelete").equals("RESTRICT"), "FK debe conservar registros referenciados");
                    unique(columns,str(a,"joinColumn"),name+".FK");
                    require(get(a,"mappedBy") == null, "Propietario no debe usar mappedBy");
                } else {
                    require(str(a,"mappedBy").equals(str(opposite,"name")) && get(a,"joinColumn") == null, "mappedBy incorrecto");
                }
            }
            for (EStructuralFeature feature : source.getEStructuralFeatures()) require(fields.contains(feature.getName()), "Falta mapear PIM: " + name+"."+feature.getName());
            for (EObject c : list(e,"constraints")) {
                unique(sqlNames,str(c,"name"),"restricciones SQL");
                @SuppressWarnings("unchecked") List<String> cols = (List<String>)get(c,"columns");
                require(columns.containsAll(cols), "Restriccion sobre columnas inexistentes");
                if (str(c,"kind").equals("CHECK")) require(cols.isEmpty() && get(c,"expression") != null, "CHECK requiere expresion");
                else require(!cols.isEmpty(), "Unicidad requiere columnas");
                if (str(c,"kind").equals("PARTIAL_UNIQUE")) require(str(c,"expression").equals("estado = 'CONFIRMADA'"), "Unicidad activa debe excluir canceladas");
            }
            for (String component : List.of("repository","service","controller")) unique(componentNames,str((EObject)get(e,component),"name"),"componentes");
            EObject service = (EObject)get(e,"service"), controller = (EObject)get(e,"controller");
            require((Boolean)get(service,"transactional"), "Servicio requiere transacciones");
            require(str(controller,"basePath").startsWith("/api/"), "Ruta REST incorrecta");
            Set<String> routes = new HashSet<>();
            for (EObject op : list(controller,"operations")) {
                unique(routes,str(op,"httpMethod")+" "+str(op,"path"),"endpoints de "+name);
                require(Set.of("LIST","DETAIL","AVAILABLE").contains(str(op,"kind")) == str(op,"httpMethod").equals("GET"), "Verbo HTTP incorrecto");
            }
            require(!list(service,"policies").isEmpty(), "Faltan politicas del servicio");
        }
    }
    public static void main(String[] args) throws Exception {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi",new XMIResourceFactoryImpl());
        EPackage meta = (EPackage)rs.getResource(URI.createFileURI(Path.of(args[0]).toAbsolutePath().toString()),true).getContents().get(0);
        EPackage pim = (EPackage)rs.getResource(URI.createFileURI(Path.of(args[2]).toAbsolutePath().toString()),true).getContents().get(0);
        rs.getPackageRegistry().put(meta.getNsURI(),meta);
        emf(meta,"Metamodelo PSM");
        Resource model = rs.getResource(URI.createFileURI(Path.of(args[1]).toAbsolutePath().toString()),true);
        EcoreUtil.resolveAll(rs);
        require(model.getErrors().isEmpty() && EcoreUtil.UnresolvedProxyCrossReferencer.find(rs).isEmpty(), "Errores o referencias sin resolver");
        EObject app = model.getContents().get(0);
        emf(app,"Instancia PSM"); semantic(app,pim);
        System.out.println("OK: Ecore PSM y referencia XMI; mapeos PIM, columnas, relaciones JPA y rutas REST coherentes.");
        EObject broken = EcoreUtil.copy(app);
        EObject relation = list(list(broken,"entities").get(0),"associations").get(0);
        relation.eSet(relation.eClass().getEStructuralFeature("target"),null);
        require(Diagnostician.INSTANCE.validate(broken).getSeverity() == Diagnostic.ERROR,"Debio fallar target obligatorio");
        EObject duplicate = EcoreUtil.copy(app);
        EObject field = list(list(duplicate,"entities").get(0),"attributes").get(1);
        field.eSet(field.eClass().getEStructuralFeature("columnName"),"id");
        boolean rejected = false;
        try { semantic(duplicate,pim); } catch (IllegalStateException expected) { rejected = expected.getMessage().contains("columnas"); }
        require(rejected,"Debio fallar columna repetida");
        Resource roundtrip = rs.createResource(URI.createURI("memory:/psm-roundtrip.xmi"));
        roundtrip.getContents().add(EcoreUtil.copy(app));
        var bytes = new java.io.ByteArrayOutputStream();roundtrip.save(bytes,null);roundtrip.unload();
        roundtrip.load(new java.io.ByteArrayInputStream(bytes.toByteArray()),null);
        emf(roundtrip.getContents().get(0),"Recarga PSM"); semantic(roundtrip.getContents().get(0),pim);
        System.out.println("OK: recarga XMI y controles negativos (target ausente y columna repetida).");
    }
}
