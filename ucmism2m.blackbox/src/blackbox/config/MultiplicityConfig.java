package blackbox.config;

/*
 * File   : MultiplicityConfig.java
 * Package: blackbox.config
 * Purpose: POJO for multiplicity objects that appear in both attribute entries
 *          and association mapping entries in the configuration. Holds a lower
 *          bound and an upper bound, both as strings to match the JSON Schema
 *          pattern constraint (single digit 0-9 or "*" for unlimited).
 *
 *          Examples: { "lower": "1", "upper": "1" }
 *                    { "lower": "0", "upper": "*" }
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to any "multiplicity" object in the mapping configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiplicityConfig {

    /**
     * Lower bound of the multiplicity range.
     * Valid values: "0" through "9" as per the JSON Schema pattern constraint.
     */
    @JsonProperty("lower")
    private String lower;

    /**
     * Upper bound of the multiplicity range.
     * Valid values: "0" through "9", or "*" for unlimited (LiteralUnlimitedNatural).
     */
    @JsonProperty("upper")
    private String upper;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getLower() { return lower; }
    public String getUpper() { return upper; }

    /**
     * Returns true if this multiplicity config has both bounds set.
     * Used by JSONConfigLoader to distinguish an explicit override from absent.
     */
    public boolean isPresent() {
        return lower != null && upper != null;
    }
}
