package blackbox.config;

/*
 * File   : ParsedConfig.java
 * Package: blackbox.config
 * Purpose: Root POJO for the complete parsed mapping configuration.
 *          This class is the top-level object produced by Jackson ObjectMapper
 *          when deserialising the JSON mapping configuration file. It maps
 *          directly to the four top-level keys of the configuration JSON:
 *          "$schema", "transformation", "package", and "mapping".
 *
 *          The "$schema" key is ignored (only used for external validation).
 *          All other keys are bound to typed fields.
 *
 *          This class is internal to the ucmism2m.blackbox bundle and is
 *          never exposed to QVTo directly. QVTo accesses configuration data
 *          exclusively through the typed accessor methods on JSONConfigLoader.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Root deserialization target for the UCMIS mapping configuration JSON file.
 * Corresponds to the top-level object defined in ucmis_mapping_configuration.schema.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)  // tolerates "$schema", "_comment", and future fields
public class ParsedConfig {

    /** Transformation metadata: source model, target model identity and version. */
    @JsonProperty("transformation")
    private TransformationConfig transformation;

    /**
     * Additional packages to create in the target model beyond the main package.
     * May be absent in configurations that need no extra packages; defaults to empty list.
     */
    @JsonProperty("package")
    private List<PackageConfig> packages = Collections.emptyList();

    /** Class and association mapping definitions that drive the transformation. */
    @JsonProperty("mapping")
    private MappingConfig mapping;

    // ── Accessors ────────────────────────────────────────────────────────────

    public TransformationConfig getTransformation() {
        return transformation;
    }

    public List<PackageConfig> getPackages() {
        return packages != null ? packages : Collections.emptyList();
    }

    public MappingConfig getMapping() {
        return mapping;
    }

    // ── Inner class for the mapping container ────────────────────────────────

    /**
     * Container for the "mapping" top-level key, which holds the class and
     * association mapping arrays. Separated from ParsedConfig to match the
     * JSON structure exactly and to keep each class focused on one schema object.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MappingConfig {

        /** List of class mapping entries (map, merge, or new). */
        @JsonProperty("class")
        private List<ClassMappingConfig> classMappings = Collections.emptyList();

        /** List of association mapping entries. */
        @JsonProperty("association")
        private List<AssociationMappingConfig> associationMappings = Collections.emptyList();

        public List<ClassMappingConfig> getClassMappings() {
            return classMappings != null ? classMappings : Collections.emptyList();
        }

        public List<AssociationMappingConfig> getAssociationMappings() {
            return associationMappings != null ? associationMappings : Collections.emptyList();
        }
    }
}
