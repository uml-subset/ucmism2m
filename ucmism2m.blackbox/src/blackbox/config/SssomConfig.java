package blackbox.config;

/*
 * File   : SssomConfig.java
 * Package: blackbox.config
 * Purpose: POJO for entries in the optional "sssom" array on a class mapping.
 *          SSSOM stands for Simple Standard for Sharing Ontology Mappings.
 *          These fields provide structured provenance metadata that is
 *          incorporated into the RDF Turtle section of the provenance
 *          UML Comment generated for each Dependency relationship.
 *
 *          In the current DDI-CDI to DDSC configuration, SSSOM data is present
 *          on two class mappings: DataStore and WideDataSet.
 *
 *          The subject_id and object_id are NOT stored in the configuration —
 *          they are computed at runtime by JSONConfigLoader from the source
 *          model URI and class names.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to an entry in the "sssom" array of a class mapping in the configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SssomConfig {

    /** Human-readable label for the source (subject) class or group of classes. */
    @JsonProperty("subject_label")
    private String subjectLabel;

    /** Human-readable label for the target (object) class. */
    @JsonProperty("object_label")
    private String objectLabel;

    /**
     * SSSOM predicate identifier (CURIE or URI) expressing the mapping relationship
     * (e.g. "skos:narrowMatch", "skos:relatedMatch").
     */
    @JsonProperty("predicate_id")
    private String predicateId;

    /** Human-readable description of the mapping predicate. */
    @JsonProperty("predicate_label")
    private String predicateLabel;

    /**
     * Confidence score for this mapping, between 0.0 and 1.0.
     * Stored as Double to allow null when absent.
     */
    @JsonProperty("confidence")
    private Double confidence;

    /** Free-text comment explaining this mapping decision. */
    @JsonProperty("comment")
    private String comment;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getSubjectLabel()  { return subjectLabel; }
    public String getObjectLabel()   { return objectLabel; }
    public String getPredicateId()   { return predicateId; }
    public String getPredicateLabel(){ return predicateLabel; }
    public Double getConfidence()    { return confidence; }
    public String getComment()       { return comment; }
}
