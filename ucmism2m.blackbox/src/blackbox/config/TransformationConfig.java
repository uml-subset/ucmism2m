package blackbox.config;

/*
 * File   : TransformationConfig.java
 * Package: blackbox.config
 * Purpose: POJO for the "transformation" top-level key of the mapping configuration.
 *          Holds general transformation metadata and the source/target model
 *          identity objects. The description and version fields are informational
 *          and are not used by the transformation itself.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to the "transformation" object in the mapping configuration JSON.
 * Contains the human-readable description, configuration version, and
 * the source and target model identity objects.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransformationConfig {

    /** Human-readable description of this transformation configuration. */
    @JsonProperty("description")
    private String description;

    /** Version of this mapping configuration file (informational). */
    @JsonProperty("version")
    private String version;

    /** Source model identity: name, main package, and URI namespace. */
    @JsonProperty("sourceModel")
    private SourceModelConfig sourceModel;

    /** Target model identity, metadata, and package names. */
    @JsonProperty("targetModel")
    private TargetModelConfig targetModel;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public SourceModelConfig getSourceModel() {
        return sourceModel;
    }

    public TargetModelConfig getTargetModel() {
        return targetModel;
    }
}
