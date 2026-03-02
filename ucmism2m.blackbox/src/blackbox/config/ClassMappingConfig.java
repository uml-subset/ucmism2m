package blackbox.config;

/*
 * File   : ClassMappingConfig.java
 * Package: blackbox.config
 * Purpose: POJO for entries in the "mapping.class" array of the configuration.
 *          Three mapping types are supported, all sharing this class:
 *
 *          "map"   — one source class maps to one target class.
 *                    sourceClasses has exactly one element.
 *
 *          "merge" — two source classes are combined into one target class.
 *                    sourceClasses has exactly two elements.
 *                    definitionFrom names the source class whose UML Comment
 *                    becomes the target class definition.
 *
 *          "new"   — a target class is created with no source equivalent.
 *                    sourceClasses is null. No Dependency is created.
 *
 *          The JSON Schema uses oneOf(String, array) for "sourceClass". Jackson
 *          cannot deserialise a union type directly, so a custom deserialiser
 *          normalises both forms to List<String>. See SourceClassDeserializer.
 *
 *          The optional "sssom" array provides SSSOM provenance metadata that
 *          is incorporated into the RDF Turtle section of each Dependency
 *          comment. In practice each class mapping has at most one SSSOM entry,
 *          and the same entry applies to all Dependencies generated from the
 *          mapping (e.g. both Dependencies in a merge mapping).
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;

/**
 * Maps to an entry in the "mapping.class" array of the configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassMappingConfig {

    /**
     * Mapping type discriminator. One of: "map", "merge", "new".
     * Controls which fields are relevant and how the transformation
     * processes this entry.
     */
    @JsonProperty("mappingType")
    private String mappingType;

    /**
     * Source class name(s). Normalised to List<String> regardless of whether
     * the JSON value is a plain string (map) or an array of two strings (merge).
     * Null for "new" mappings (no source class).
     * Uses a custom deserialiser to handle the oneOf(String, array) schema type.
     */
    @JsonProperty("sourceClass")
    @JsonDeserialize(using = SourceClassDeserializer.class)
    private List<String> sourceClasses;

    /** Name of the target class to create. */
    @JsonProperty("targetClass")
    private String targetClass;

    /** Name of the target package in which the target class is placed. */
    @JsonProperty("targetPackage")
    private String targetPackage;

    /**
     * For "merge" mappings: the name of one of the two source classes whose
     * UML Comment body is used as the definition of the target class.
     * Null for "map" and "new" mappings.
     */
    @JsonProperty("definitionFrom")
    private String definitionFrom;

    /**
     * Selected attributes for this class mapping, with optional overrides.
     * When absent or empty, the target class gets no attributes (the transformation
     * only copies attributes explicitly listed here).
     */
    @JsonProperty("attribute")
    private List<AttributeConfig> attributes = Collections.emptyList();

    /**
     * Optional SSSOM provenance entries. Each entry contributes to the RDF Turtle
     * section of the provenance comment on the generated Dependency relationships.
     * At most one entry is expected per class mapping in practice.
     */
    @JsonProperty("sssom")
    private List<SssomConfig> sssom = Collections.emptyList();

    // ── Convenience accessors ─────────────────────────────────────────────────

    /**
     * Returns the single source class name for "map" mappings.
     * Throws if called on a "merge" or "new" mapping.
     */
    public String getSingleSourceClass() {
        if (sourceClasses == null || sourceClasses.isEmpty()) {
            throw new IllegalStateException("No sourceClass for mappingType=" + mappingType);
        }
        return sourceClasses.get(0);
    }

    /**
     * Returns the first SSSOM entry, or null if no SSSOM data is present.
     * Used by buildProvenanceCommentBody when generating provenance comments.
     */
    public SssomConfig getFirstSssom() {
        return (sssom != null && !sssom.isEmpty()) ? sssom.get(0) : null;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getMappingType()        { return mappingType; }
    public List<String> getSourceClasses(){ return sourceClasses != null ? sourceClasses : Collections.emptyList(); }
    public String getTargetClass()        { return targetClass; }
    public String getTargetPackage()      { return targetPackage; }
    public String getDefinitionFrom()     { return definitionFrom; }
    public List<AttributeConfig> getAttributes() { return attributes != null ? attributes : Collections.emptyList(); }
    public List<SssomConfig> getSssom()   { return sssom != null ? sssom : Collections.emptyList(); }
}
