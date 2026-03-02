package blackbox.config;

/*
 * File   : PackageConfig.java
 * Package: blackbox.config
 * Purpose: POJO for entries in the top-level "package" array of the mapping
 *          configuration. Each entry defines an additional package to create
 *          in the target model beyond the structurally implied packages
 *          (main package, metadata package, data types package).
 *
 *          In the DDI-CDI to DDSC configuration there is one entry:
 *          the "Classes" package as a child of "DDSC". This is where all
 *          mapped target classes, associations, and dependencies are placed.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to an entry in the top-level "package" array of the mapping configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageConfig {

    /** Name of the package to create (e.g. "Classes"). */
    @JsonProperty("name")
    private String name;

    /**
     * Name of the parent package or model that will contain this package.
     * Must refer to either the mainPackage name or the root model name.
     */
    @JsonProperty("parent")
    private String parent;

    /**
     * Definition text for this package. Becomes the OwnedComment body
     * on the created package element.
     */
    @JsonProperty("definition")
    private String definition;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getName()       { return name; }
    public String getParent()     { return parent; }
    public String getDefinition() { return definition; }
}
