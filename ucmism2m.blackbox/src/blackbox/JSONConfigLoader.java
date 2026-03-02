package blackbox;

/*
 * File   : JSONConfigLoader.java
 * Package: blackbox
 * Purpose: Java black-box operations for reading and interpreting the UCMIS
 *          mapping configuration JSON file. This class is the sole interface
 *          between the JSON configuration and the QVTo transformation layer.
 *
 *          Responsibilities:
 *            1. Load and cache the parsed configuration (Jackson ObjectMapper,
 *               static cache keyed by canonical file path).
 *            2. Expose all configuration data through typed, purpose-built
 *               accessor methods that QVTo calls as native queries.
 *            3. Generate provenance comment body text (reStructuredText + RDF Turtle).
 *            4. Create unresolved EMF proxy objects for Dependency suppliers.
 *
 *          Design principle: QVTo never navigates raw JSON or generic maps.
 *          This class answers "what" (names, text, URIs); QVTo answers "how"
 *          (how to construct EMF model elements).
 *
 *          All public methods are annotated @Operation(contextual=false) so
 *          that QVTo can call them as non-contextual queries (i.e. they are
 *          not called on a receiver object). The configPath argument is always
 *          the first parameter and drives cache lookup.
 *
 *          Thread safety: the cache is populated once per path in a
 *          single-threaded QVTo execution. No locking is required.
 */

import blackbox.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.eclipse.m2m.qvt.oml.blackbox.java.Operation;
import org.eclipse.uml2.uml.UMLPackage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * QVTo black-box operations for UCMIS mapping configuration loading and interpretation.
 * Registered with the QVTo runtime via plugin.xml and exposed to QVTo scripts
 * through the JSONConfigLoaderLib.qvto wrapper.
 */
@Module(packageURIs = {"http://www.eclipse.org/uml2/5.0.0/UML"})
public class JSONConfigLoader {

    // ── Configuration cache ───────────────────────────────────────────────────

    /**
     * Static cache mapping canonical file path to parsed configuration.
     * Populated on first access per path; reused for all subsequent calls
     * within the same JVM session. ConcurrentHashMap for defensive safety,
     * though QVTo execution is single-threaded in practice.
     */
    private static final Map<String, ParsedConfig> CONFIG_CACHE = new ConcurrentHashMap<>();

    /**
     * Reusable Jackson ObjectMapper. ObjectMapper is thread-safe after configuration
     * so a single shared instance is appropriate.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── Cache access ──────────────────────────────────────────────────────────

    /**
     * Returns the cached ParsedConfig for the given path, loading and caching
     * it on first access. Throws RuntimeException on I/O or parse failure.
     *
     * @param configPath Path to the JSON configuration file
     * @return Parsed and cached configuration
     */
    private static ParsedConfig getConfig(String configPath) {
        return CONFIG_CACHE.computeIfAbsent(canonicalize(configPath), path -> {
            try {
                return OBJECT_MAPPER.readValue(new File(path), ParsedConfig.class);
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to parse UCMIS mapping configuration: " + path, e);
            }
        });
    }

    /**
     * Resolves a path to its canonical (absolute, symlink-resolved) form.
     * This ensures that two paths pointing to the same file share one cache entry.
     */
    private static String canonicalize(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            // Fall back to absolute path if canonical resolution fails
            return new File(path).getAbsolutePath();
        }
    }

    // ── Source model metadata ─────────────────────────────────────────────────

    @Operation(contextual = false)
    public static String getSourceModelName(String configPath) {
        return getConfig(configPath).getTransformation().getSourceModel().getName();
    }

    @Operation(contextual = false)
    public static String getSourceModelMainPackage(String configPath) {
        return getConfig(configPath).getTransformation().getSourceModel().getMainPackage();
    }

    @Operation(contextual = false)
    public static String getSourceModelUri(String configPath) {
        return getConfig(configPath).getTransformation().getSourceModel().getUri();
    }

    // ── Target model metadata ─────────────────────────────────────────────────

    @Operation(contextual = false)
    public static String getTargetModelAcronym(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getAcronym();
    }

    @Operation(contextual = false)
    public static String getTargetModelTitle(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getModelTitle();
    }

    @Operation(contextual = false)
    public static String getTargetModelSubTitle(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getSubTitle();
    }

    @Operation(contextual = false)
    public static String getTargetModelMajorVersion(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getMajorVersion();
    }

    @Operation(contextual = false)
    public static String getTargetModelMinorVersion(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getMinorVersion();
    }

    @Operation(contextual = false)
    public static String getTargetModelLanguage(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getLanguage();
    }

    @Operation(contextual = false)
    public static String getTargetModelUri(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getUri();
    }

    @Operation(contextual = false)
    public static String getTargetModelDefinition(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getDefinition();
    }

    @Operation(contextual = false)
    public static String getTargetModelMainPackage(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getMainPackage();
    }

    @Operation(contextual = false)
    public static String getTargetModelMainPackageDefinition(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getMainPackageDefinition();
    }

    @Operation(contextual = false)
    public static String getTargetModelDataTypesPackage(String configPath) {
        return getConfig(configPath).getTransformation().getTargetModel().getDataTypesPackage();
    }

    // ── Package definitions ───────────────────────────────────────────────────

    /**
     * Returns the names of all additional packages defined in the "package" array.
     * In the DDI-CDI to DDSC configuration this returns ["Classes"].
     */
    @Operation(contextual = false)
    public static List<String> getDefinedPackageNames(String configPath) {
        return getConfig(configPath).getPackages().stream()
            .map(PackageConfig::getName)
            .collect(Collectors.toList());
    }

    /**
     * Returns the parent package name for the named additional package.
     *
     * @param packageName Name of the package as returned by getDefinedPackageNames
     */
    @Operation(contextual = false)
    public static String getDefinedPackageParent(String configPath, String packageName) {
        return getConfig(configPath).getPackages().stream()
            .filter(p -> packageName.equals(p.getName()))
            .map(PackageConfig::getParent)
            .findFirst()
            .orElse("");
    }

    /**
     * Returns the definition text for the named additional package.
     */
    @Operation(contextual = false)
    public static String getDefinedPackageDefinition(String configPath, String packageName) {
        return getConfig(configPath).getPackages().stream()
            .filter(p -> packageName.equals(p.getName()))
            .map(PackageConfig::getDefinition)
            .findFirst()
            .orElse("");
    }

    // ── Class mapping — iteration and typing ──────────────────────────────────

    /**
     * Returns all sourceClass values from "map" mappings, in config file order.
     * QVTo iterates this list to drive the map class creation phase.
     */
    @Operation(contextual = false)
    public static List<String> getMapSourceClasses(String configPath) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "map".equals(c.getMappingType()))
            .map(ClassMappingConfig::getSingleSourceClass)
            .collect(Collectors.toList());
    }

    /**
     * Returns all targetClass values from "merge" mappings, in config file order.
     * QVTo iterates this list to drive the merge class creation phase.
     */
    @Operation(contextual = false)
    public static List<String> getMergeTargetClasses(String configPath) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "merge".equals(c.getMappingType()))
            .map(ClassMappingConfig::getTargetClass)
            .collect(Collectors.toList());
    }

    /**
     * Returns all targetClass values from "new" mappings, in config file order.
     */
    @Operation(contextual = false)
    public static List<String> getNewTargetClasses(String configPath) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "new".equals(c.getMappingType()))
            .map(ClassMappingConfig::getTargetClass)
            .collect(Collectors.toList());
    }

    /**
     * Returns the targetClass name for the given sourceClass in a "map" mapping.
     */
    @Operation(contextual = false)
    public static String getTargetClassForSourceClass(String configPath, String sourceClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "map".equals(c.getMappingType())
                      && sourceClass.equals(c.getSingleSourceClass()))
            .map(ClassMappingConfig::getTargetClass)
            .findFirst()
            .orElse("");
    }

    /**
     * Returns the targetPackage for the given sourceClass in a "map" mapping.
     */
    @Operation(contextual = false)
    public static String getTargetPackageForSourceClass(String configPath, String sourceClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "map".equals(c.getMappingType())
                      && sourceClass.equals(c.getSingleSourceClass()))
            .map(ClassMappingConfig::getTargetPackage)
            .findFirst()
            .orElse("");
    }

    /**
     * Returns the targetPackage for the given targetClass in a "merge" or "new" mapping.
     */
    @Operation(contextual = false)
    public static String getTargetPackageForTargetClass(String configPath, String targetClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> !("map".equals(c.getMappingType()))
                      && targetClass.equals(c.getTargetClass()))
            .map(ClassMappingConfig::getTargetPackage)
            .findFirst()
            .orElse("");
    }

    // ── Class mapping — merge specifics ───────────────────────────────────────

    /**
     * Returns the two source class names for the given "merge" target class.
     * The order matches the order in the configuration's sourceClass array.
     */
    @Operation(contextual = false)
    public static List<String> getMergeSourceClasses(String configPath, String targetClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "merge".equals(c.getMappingType())
                      && targetClass.equals(c.getTargetClass()))
            .map(ClassMappingConfig::getSourceClasses)
            .findFirst()
            .orElse(Collections.emptyList());
    }

    /**
     * Returns the definitionFrom class name for the given "merge" target class.
     * This is the source class whose UML Comment becomes the target class definition.
     */
    @Operation(contextual = false)
    public static String getMergeDefinitionFromClass(String configPath, String targetClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "merge".equals(c.getMappingType())
                      && targetClass.equals(c.getTargetClass()))
            .map(ClassMappingConfig::getDefinitionFrom)
            .findFirst()
            .orElse("");
    }

    // ── Class mapping — new specifics ─────────────────────────────────────────

    /**
     * Returns the definition text for the given "new" target class.
     */
    @Operation(contextual = false)
    public static String getNewClassDefinition(String configPath, String targetClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "new".equals(c.getMappingType())
                      && targetClass.equals(c.getTargetClass()))
            .map(ClassMappingConfig::getTargetClass) // placeholder — "new" uses config definition field
            .findFirst()
            .orElse("");
    }

    // ── Attribute selection ───────────────────────────────────────────────────

    /**
     * Returns the ordered list of selected attribute names for a class mapping.
     * For "map" mappings, classKey is the sourceClass name.
     * For "merge" and "new" mappings, classKey is the targetClass name.
     * Returns an empty list if the mapping has no attribute array (all attributes
     * inherited from the source, none explicitly selected).
     */
    @Operation(contextual = false)
    public static List<String> getClassAttributeNames(String configPath, String classKey) {
        // Find the matching class mapping — classKey may be sourceClass (map) or targetClass (merge/new)
        Optional<ClassMappingConfig> mapping = getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> classKey.equals(c.getMappingType().equals("map")
                                          ? c.getSingleSourceClass()
                                          : c.getTargetClass()))
            .findFirst();

        return mapping
            .map(m -> m.getAttributes().stream()
                .map(AttributeConfig::getName)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    /**
     * Returns the fromSourceClass value for the named attribute in a "merge" mapping.
     * Identifies which of the two source classes provides this attribute.
     */
    @Operation(contextual = false)
    public static String getMergeAttributeSourceClass(String configPath, String targetClass,
                                                       String attrName) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> "merge".equals(c.getMappingType())
                      && targetClass.equals(c.getTargetClass()))
            .flatMap(c -> c.getAttributes().stream())
            .filter(a -> attrName.equals(a.getName()))
            .map(AttributeConfig::getFromSourceClass)
            .findFirst()
            .orElse("");
    }

    /**
     * Returns the lower multiplicity override for the named attribute, or empty string if absent.
     * classKey is the sourceClass for "map", targetClass for "merge"/"new".
     */
    @Operation(contextual = false)
    public static String getAttributeLower(String configPath, String classKey, String attrName) {
        return findAttributeConfig(configPath, classKey, attrName)
            .map(a -> a.getMultiplicity() != null && a.getMultiplicity().getLower() != null
                      ? a.getMultiplicity().getLower() : "")
            .orElse("");
    }

    /**
     * Returns the upper multiplicity override for the named attribute, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAttributeUpper(String configPath, String classKey, String attrName) {
        return findAttributeConfig(configPath, classKey, attrName)
            .map(a -> a.getMultiplicity() != null && a.getMultiplicity().getUpper() != null
                      ? a.getMultiplicity().getUpper() : "")
            .orElse("");
    }

    /**
     * Returns the definition override for the named attribute, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAttributeDefinition(String configPath, String classKey, String attrName) {
        return findAttributeConfig(configPath, classKey, attrName)
            .map(a -> a.getDefinition() != null ? a.getDefinition() : "")
            .orElse("");
    }

    /**
     * Finds the AttributeConfig for the given classKey and attribute name.
     * classKey is matched against sourceClass (for map) or targetClass (for merge/new).
     */
    private static Optional<AttributeConfig> findAttributeConfig(String configPath,
                                                                   String classKey,
                                                                   String attrName) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> classKey.equals("map".equals(c.getMappingType())
                                          ? c.getSingleSourceClass()
                                          : c.getTargetClass()))
            .flatMap(c -> c.getAttributes().stream())
            .filter(a -> attrName.equals(a.getName()))
            .findFirst();
    }

    // ── Association mappings ──────────────────────────────────────────────────

    /**
     * Returns all targetAssociationName values in configuration file order.
     * QVTo iterates this list to drive the full association creation phase.
     * Fan-out (multiple entries with the same sourceAssociationName) is handled
     * naturally: each targetAssociationName is an independent association to create.
     */
    @Operation(contextual = false)
    public static List<String> getTargetAssociationNames(String configPath) {
        return getConfig(configPath).getMapping().getAssociationMappings().stream()
            .map(AssociationMappingConfig::getTargetAssociationName)
            .collect(Collectors.toList());
    }

    /**
     * Returns the sourceAssociationName for the given target association name.
     */
    @Operation(contextual = false)
    public static String getSourceAssociationName(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(AssociationMappingConfig::getSourceAssociationName)
            .orElse("");
    }

    /**
     * Parses and returns the subject class (first segment) from the target association triple.
     * Example: "DataStore_has_LogicalRecord" -> "DataStore"
     */
    @Operation(contextual = false)
    public static String getAssociationSubjectClass(String configPath, String targetAssocName) {
        return parseTriple(targetAssocName)[0];
    }

    /**
     * Parses and returns the object class (last segment) from the target association triple.
     * Example: "DataStore_has_LogicalRecord" -> "LogicalRecord"
     */
    @Operation(contextual = false)
    public static String getAssociationObjectClass(String configPath, String targetAssocName) {
        return parseTriple(targetAssocName)[2];
    }

    /**
     * Parses and returns the predicate (middle segment(s)) from the target association triple.
     * Example: "RepresentedVariable_takesSubstantiveValuesFrom_SubstantiveValueDomain"
     *          -> "takesSubstantiveValuesFrom"
     * Compound predicates (containing underscores) are returned as a single string
     * because the split uses only the first and last underscore positions.
     */
    @Operation(contextual = false)
    public static String getAssociationPredicate(String configPath, String targetAssocName) {
        return parseTriple(targetAssocName)[1];
    }

    /**
     * Returns the object end lower multiplicity override, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAssociationObjectLower(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(a -> a.getObjectClassMultiplicity() != null
                      ? nvl(a.getObjectClassMultiplicity().getLower()) : "")
            .orElse("");
    }

    /**
     * Returns the object end upper multiplicity override, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAssociationObjectUpper(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(a -> a.getObjectClassMultiplicity() != null
                      ? nvl(a.getObjectClassMultiplicity().getUpper()) : "")
            .orElse("");
    }

    /**
     * Returns the subject end lower multiplicity override, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAssociationSubjectLower(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(a -> a.getSubjectClassMultiplicity() != null
                      ? nvl(a.getSubjectClassMultiplicity().getLower()) : "")
            .orElse("");
    }

    /**
     * Returns the subject end upper multiplicity override, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAssociationSubjectUpper(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(a -> a.getSubjectClassMultiplicity() != null
                      ? nvl(a.getSubjectClassMultiplicity().getUpper()) : "")
            .orElse("");
    }

    /**
     * Returns the definition override for the target association, or empty string if absent.
     */
    @Operation(contextual = false)
    public static String getAssociationDefinition(String configPath, String targetAssocName) {
        return findAssocConfig(configPath, targetAssocName)
            .map(a -> nvl(a.getDefinition()))
            .orElse("");
    }

    /**
     * Finds the AssociationMappingConfig matching the given target association name.
     */
    private static Optional<AssociationMappingConfig> findAssocConfig(String configPath,
                                                                        String targetAssocName) {
        return getConfig(configPath).getMapping().getAssociationMappings().stream()
            .filter(a -> targetAssocName.equals(a.getTargetAssociationName()))
            .findFirst();
    }

    /**
     * Parses a UCMIS association triple name into [subjectClass, predicate, objectClass].
     * Uses the first and last underscore as delimiters so that compound predicate
     * names (e.g. "takesSubstantiveValuesFrom") containing no underscores are
     * returned as-is. Class names always start with an uppercase letter; predicates
     * start with a lowercase letter — this is the canonical UCMIS convention.
     *
     * Example: "RepresentedVariable_takesSubstantiveValuesFrom_SubstantiveValueDomain"
     *       -> ["RepresentedVariable", "takesSubstantiveValuesFrom", "SubstantiveValueDomain"]
     */
    private static String[] parseTriple(String tripleName) {
        int first = tripleName.indexOf('_');
        int last  = tripleName.lastIndexOf('_');
        if (first < 0 || last < 0 || first == last) {
            throw new IllegalArgumentException(
                "Invalid UCMIS association triple name: '" + tripleName
                + "'. Expected SubjectClass_predicate_ObjectClass.");
        }
        return new String[] {
            tripleName.substring(0, first),
            tripleName.substring(first + 1, last),
            tripleName.substring(last + 1)
        };
    }

    // ── Provenance and proxy ──────────────────────────────────────────────────

    /**
     * Constructs the supplier URI for a Dependency relationship.
     * Format: sourceModel.uri + "#" + sourceClassName
     * Example: "http://ddialliance.org/Specification/DDI-CDI/1.0/XMI/#InstanceVariable"
     */
    @Operation(contextual = false)
    public static String getSupplierUri(String configPath, String sourceClass) {
        String baseUri = getSourceModelUri(configPath);
        // Ensure exactly one "#" separator between URI and fragment
        return baseUri.endsWith("#")
            ? baseUri + sourceClass
            : baseUri + "#" + sourceClass;
    }

    /**
     * Generates the full body text for the provenance UML Comment attached to
     * each Dependency relationship. Contains two sections separated by a blank line:
     *
     *   Section 1: reStructuredText field list with human-readable provenance.
     *   Section 2: RDF Turtle with machine-readable SSSOM mapping provenance.
     *
     * SSSOM fields are taken from the first sssom entry on the class mapping if
     * present; otherwise sensible defaults are used. The same body is generated
     * for both Dependencies in a merge mapping (with their respective sourceClass).
     *
     * @param configPath  Path to the JSON configuration file
     * @param targetClass Name of the target (client) class
     * @param sourceClass Name of the source (supplier) class
     * @return Complete provenance comment body string
     */
    @Operation(contextual = false)
    public static String buildProvenanceCommentBody(String configPath,
                                                     String targetClass,
                                                     String sourceClass) {
        // Locate the class mapping to determine type and SSSOM data
        ClassMappingConfig classMapping = findClassMappingForProvenance(
            configPath, targetClass, sourceClass);

        String mappingType = classMapping != null ? classMapping.getMappingType() : "map";
        SssomConfig sssom  = classMapping != null ? classMapping.getFirstSssom() : null;

        // Resolve URIs for use in both sections
        String subjectUri  = getSupplierUri(configPath, sourceClass);
        String objectUri   = getTargetModelUri(configPath)
                             + (getTargetModelUri(configPath).endsWith("#") ? "" : "#")
                             + targetClass;
        String sourceModelUri = getSourceModelUri(configPath);
        String targetModelUri = getTargetModelUri(configPath);

        // SSSOM field values — use configured values where present, defaults otherwise
        String subjectLabel  = sssom != null && sssom.getSubjectLabel()  != null
                               ? sssom.getSubjectLabel()  : sourceClass;
        String objectLabel   = sssom != null && sssom.getObjectLabel()   != null
                               ? sssom.getObjectLabel()   : targetClass;
        String predicateId   = sssom != null && sssom.getPredicateId()   != null
                               ? sssom.getPredicateId()   : "sssom:mappingRelation";
        String predicateLabel= sssom != null && sssom.getPredicateLabel()!= null
                               ? sssom.getPredicateLabel(): "";
        String confidence    = sssom != null && sssom.getConfidence()    != null
                               ? String.valueOf(sssom.getConfidence())   : "";
        String comment       = sssom != null && sssom.getComment()       != null
                               ? sssom.getComment()       : "";

        // Derive namespace prefixes from the model URIs for the Turtle section
        String srcPrefix = "src";
        String tgtPrefix = "tgt";

        // ── Section 1: reStructuredText ───────────────────────────────────────
        StringBuilder rst = new StringBuilder();
        rst.append("Provenance\n");
        rst.append("==========\n\n");
        rst.append("Transformation Information\n");
        rst.append("--------------------------\n\n");
        rst.append("- Mapping Type: ").append(mappingType).append("\n");
        rst.append("- Source Class: ").append(sourceClass).append("\n");
        rst.append("- Target Class: ").append(targetClass).append("\n\n");
        rst.append("SSSOM Mapping\n");
        rst.append("-------------\n\n");
        rst.append("- Subject ID: ").append(subjectUri).append("\n");
        rst.append("- Subject Label: ").append(subjectLabel).append("\n");
        rst.append("- Object ID: ").append(objectUri).append("\n");
        rst.append("- Object Label: ").append(objectLabel).append("\n");
        rst.append("- Predicate ID: ").append(predicateId).append("\n");
        rst.append("- Predicate Label: ").append(predicateLabel).append("\n");
        rst.append("- Subject Category: UML Class\n");
        rst.append("- Object Category: UML Class\n");
        rst.append("- Confidence: ").append(confidence).append("\n");
        rst.append("- Comment: ").append(comment).append("\n");

        // ── Section 2: RDF Turtle ─────────────────────────────────────────────
        StringBuilder ttl = new StringBuilder();
        ttl.append("@prefix mapping: <http://example.org/transformation/mapping#> .\n");
        ttl.append("@prefix sssom:   <https://w3id.org/sssom#> .\n");
        ttl.append("@prefix ").append(srcPrefix).append(": <").append(sourceModelUri).append("> .\n");
        ttl.append("@prefix ").append(tgtPrefix).append(": <").append(targetModelUri).append("> .\n\n");
        // Mapping identifier combines target and source class names for uniqueness
        ttl.append("mapping:Mapping_").append(targetClass).append("_").append(sourceClass)
           .append(" a sssom:Mapping ;\n");
        ttl.append("    sssom:subject_id <").append(subjectUri).append("> ;\n");
        ttl.append("    sssom:subject_label \"").append(escapeTurtle(subjectLabel)).append("\" ;\n");
        ttl.append("    sssom:object_id <").append(objectUri).append("> ;\n");
        ttl.append("    sssom:object_label \"").append(escapeTurtle(objectLabel)).append("\" ;\n");
        ttl.append("    sssom:predicate_id ").append(predicateId).append(" ;\n");
        ttl.append("    sssom:predicate_label \"").append(escapeTurtle(predicateLabel)).append("\" ;\n");
        ttl.append("    sssom:subjectCategory \"UML Class\" ;\n");
        ttl.append("    sssom:objectCategory \"UML Class\" ;\n");
        if (!confidence.isEmpty()) {
            ttl.append("    sssom:confidence ").append(confidence).append(" ;\n");
        }
        ttl.append("    sssom:comment \"").append(escapeTurtle(comment)).append("\" ;\n");
        ttl.append("    mapping:mappingType \"").append(mappingType).append("\" .\n");

        return rst.toString() + "\n" + ttl.toString();
    }

    /**
     * Finds the ClassMappingConfig that produced the given target class and
     * involves the given source class. Used by buildProvenanceCommentBody to
     * locate the mapping type and SSSOM data.
     */
    private static ClassMappingConfig findClassMappingForProvenance(String configPath,
                                                                      String targetClass,
                                                                      String sourceClass) {
        return getConfig(configPath).getMapping().getClassMappings().stream()
            .filter(c -> {
                // For "map": targetClass matches and sourceClass matches
                if ("map".equals(c.getMappingType())) {
                    return targetClass.equals(c.getTargetClass())
                        && sourceClass.equals(c.getSingleSourceClass());
                }
                // For "merge": targetClass matches and sourceClass is one of the two
                if ("merge".equals(c.getMappingType())) {
                    return targetClass.equals(c.getTargetClass())
                        && c.getSourceClasses().contains(sourceClass);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * Escapes special characters in string literals for RDF Turtle output.
     * Handles backslash, double-quote, and newline characters.
     */
    private static String escapeTurtle(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    /**
     * Creates an unresolved EMF proxy object representing a source model class.
     * The proxy carries the supplier URI as its proxy URI. When the target model
     * is serialised, EMF writes this as an href attribute in the XMI output,
     * providing a human- and machine-readable provenance link to the source class.
     *
     * The source model does NOT need to be loaded at serialisation time because
     * the proxy is intentionally left unresolved — EcoreUtil.resolveAll() in the
     * application layer will not resolve it since the source model resource is
     * not registered in the ResourceSet.
     *
     * @param supplierUri  The fully qualified URI of the source class
     *                     (e.g. "http://ddialliance.org/.../XMI/#InstanceVariable")
     * @return An unresolved UML Class proxy with the given URI
     */
    @Operation(contextual = false)
    public static Object createSupplierProxy(String supplierUri) {
        // Create a UML Class EObject without loading any resource.
        // Return type is Object (= OclAny in QVTo) so the blackbox binding matches
        // the 'blackbox helper createSupplierProxy(...) : OclAny' declaration in
        // ConfigLib.qvto. The call site casts the result to UML::NamedElement via
        // oclAsType() where the UML modeltype is in scope.
        org.eclipse.uml2.uml.Class proxy =
            (org.eclipse.uml2.uml.Class) EcoreUtil.create(UMLPackage.Literals.CLASS);

        // Set the proxy URI using the EMF internal API. This marks the object as
        // an unresolved proxy. EMF will serialise it as href="<supplierUri>" in XMI.
        ((InternalEObject) proxy).eSetProxyURI(URI.createURI(supplierUri));

        return proxy;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Returns empty string for null, the value itself otherwise. */
    private static String nvl(String value) {
        return value != null ? value : "";
    }
}
