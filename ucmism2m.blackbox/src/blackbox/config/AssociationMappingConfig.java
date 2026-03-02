package blackbox.config;

/*
 * File   : AssociationMappingConfig.java
 * Package: blackbox.config
 * Purpose: POJO for entries in the "mapping.association" array of the configuration.
 *          Each entry maps one source association (identified by its triple name)
 *          to one target association. A single source association may appear in
 *          multiple entries (fan-out pattern), producing multiple target associations.
 *
 *          Association names follow the UCMIS triple convention:
 *            SubjectClass_predicateName_ObjectClass
 *          The subject and object class names are parsed from the target triple
 *          by splitting on the first and last underscores.
 *
 *          All override fields are optional. When absent, values are taken from
 *          the corresponding end of the source association in the source model.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to an entry in the "mapping.association" array of the configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssociationMappingConfig {

    /**
     * Fully qualified triple name of the source association to map from
     * (e.g. "DataStore_has_LogicalRecord"). Required. Globally unique in the
     * source model. May appear in multiple entries (fan-out).
     */
    @JsonProperty("sourceAssociationName")
    private String sourceAssociationName;

    /**
     * Fully qualified triple name for the target association to create
     * (e.g. "DataStore_has_LogicalRecord"). Required.
     * The subject class, predicate, and object class are parsed from this
     * triple by JSONConfigLoader.
     */
    @JsonProperty("targetAssociationName")
    private String targetAssociationName;

    /**
     * Optional multiplicity override for the object (target) end of the association.
     * When absent, the multiplicity is copied from the source association object end.
     */
    @JsonProperty("objectClassMultiplicity")
    private MultiplicityConfig objectClassMultiplicity;

    /**
     * Optional multiplicity override for the subject (source) end of the association.
     * When absent, the multiplicity is copied from the source association subject end.
     * Not used in the current DDI-CDI to DDSC configuration.
     */
    @JsonProperty("subjectClassMultiplicity")
    private MultiplicityConfig subjectClassMultiplicity;

    /**
     * Optional definition override for the target association. When non-null,
     * replaces the OwnedComment body copied from the source association.
     * Not used in the current DDI-CDI to DDSC configuration.
     */
    @JsonProperty("definition")
    private String definition;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getSourceAssociationName()          { return sourceAssociationName; }
    public String getTargetAssociationName()           { return targetAssociationName; }
    public MultiplicityConfig getObjectClassMultiplicity()  { return objectClassMultiplicity; }
    public MultiplicityConfig getSubjectClassMultiplicity() { return subjectClassMultiplicity; }
    public String getDefinition()                      { return definition; }
}
