package blackbox.config;

/*
 * File   : AttributeConfig.java
 * Package: blackbox.config
 * Purpose: POJO for entries in the "attribute" array of a class mapping.
 *          Each entry selects one attribute by name and optionally overrides
 *          its multiplicity or definition. Where overrides are absent, the
 *          values are taken from the source model via getAllAttributes().
 *
 *          For "merge" mappings, fromSourceClass identifies which of the two
 *          source classes owns the attribute being copied.
 *
 *          The "dataType" override field is included for schema completeness
 *          but is not used in the current DDI-CDI to DDSC configuration.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to an entry in the "attribute" array of a class mapping in the configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeConfig {

    /**
     * Name of the attribute to select from the source class (including inherited
     * attributes reachable via getAllAttributes()). Required field.
     */
    @JsonProperty("name")
    private String name;

    /**
     * For "merge" mappings only: the name of the source class (one of the two
     * in the sourceClass array) from which this attribute is taken.
     * Null for "map" and "new" mappings.
     */
    @JsonProperty("fromSourceClass")
    private String fromSourceClass;

    /**
     * Optional override for the attribute data type. When set, replaces the
     * type from the source model with a type of this name. Not used in the
     * current configuration.
     */
    @JsonProperty("dataType")
    private String dataType;

    /**
     * Optional override for the attribute definition. When non-null, replaces
     * the OwnedComment body from the source Property. Used when the target
     * model requires a different definition from the source.
     */
    @JsonProperty("definition")
    private String definition;

    /**
     * Optional override for the attribute multiplicity. When present, replaces
     * both the lower and upper bounds from the source Property. Absent means
     * "copy from source".
     */
    @JsonProperty("multiplicity")
    private MultiplicityConfig multiplicity;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getName()             { return name; }
    public String getFromSourceClass()  { return fromSourceClass; }
    public String getDataType()         { return dataType; }
    public String getDefinition()       { return definition; }
    public MultiplicityConfig getMultiplicity() { return multiplicity; }
}
