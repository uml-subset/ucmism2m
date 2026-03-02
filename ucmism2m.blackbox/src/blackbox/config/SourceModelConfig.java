package blackbox.config;

/*
 * File   : SourceModelConfig.java
 * Package: blackbox.config
 * Purpose: POJO for the "transformation.sourceModel" object in the mapping
 *          configuration. Identifies the source UML model by name, main
 *          package, and URI namespace.
 *
 *          The URI is used as a namespace prefix when constructing supplier
 *          proxy URIs for Dependency relationships in the target model:
 *          supplierUri = sourceModel.uri + "#" + sourceClassName
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to the "transformation.sourceModel" object in the mapping configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceModelConfig {

    /** Name of the source model (e.g. "DDI-CDI"). */
    @JsonProperty("name")
    private String name;

    /**
     * Name of the main package in the source model that contains all referenced
     * classes and associations, possibly in sub-packages. Class names are unique
     * within the scope of this package and all its descendants.
     */
    @JsonProperty("mainPackage")
    private String mainPackage;

    /**
     * URI namespace of the source model (e.g. "http://ddialliance.org/Specification/DDI-CDI/1.0/XMI/").
     * Used as the prefix for Dependency supplier proxy URIs.
     */
    @JsonProperty("uri")
    private String uri;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public String getMainPackage() {
        return mainPackage;
    }

    public String getUri() {
        return uri;
    }
}
