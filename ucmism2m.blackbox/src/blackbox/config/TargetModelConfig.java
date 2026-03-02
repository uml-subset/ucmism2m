package blackbox.config;

/*
 * File   : TargetModelConfig.java
 * Package: blackbox.config
 * Purpose: POJO for the "transformation.targetModel" object in the mapping
 *          configuration. Contains two categories of data:
 *
 *          1. Metadata fields that become read-only String attributes of the
 *             "metadata" DataType in the "metadata" package of the target model:
 *             acronym, modelTitle, subTitle, majorVersion, minorVersion,
 *             language, uri.
 *
 *          2. Structural fields that control how the target model is organised
 *             but do not appear as metadata attributes:
 *             definition (root Model OwnedComment),
 *             mainPackage (name of the root package under the Model),
 *             mainPackageDefinition (OwnedComment on the main package),
 *             dataTypesPackage (name of the package where copied data types land).
 *
 *          In the DDI-CDI to DDSC configuration, dataTypesPackage equals
 *          mainPackage (both "DDSC"), so no separate data types package is
 *          created — data type sub-packages land directly under DDSC.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to the "transformation.targetModel" object in the mapping configuration JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetModelConfig {

    // ── Metadata attribute fields (become read-only attributes on metadata DataType) ──

    /** Short acronym for the target model (e.g. "CDIF"). Used as the root Model name. */
    @JsonProperty("acronym")
    private String acronym;

    /** Full title of the target model. */
    @JsonProperty("modelTitle")
    private String modelTitle;

    /** Subtitle of the target model. */
    @JsonProperty("subTitle")
    private String subTitle;

    /** Major version number string (e.g. "1"). */
    @JsonProperty("majorVersion")
    private String majorVersion;

    /** Minor version number string (e.g. "0"). */
    @JsonProperty("minorVersion")
    private String minorVersion;

    /** Language of the definitions in the model (e.g. "en"). */
    @JsonProperty("language")
    private String language;

    /** URI namespace of the target model. Used in provenance RDF Turtle generation. */
    @JsonProperty("uri")
    private String uri;

    // ── Structural fields (control model organisation, not metadata attributes) ──

    /**
     * Full definition text for the target model. Becomes the OwnedComment body
     * on the root UML Model element.
     */
    @JsonProperty("definition")
    private String definition;

    /**
     * Name of the main package directly under the root Model element
     * (e.g. "DDSC"). All classes, associations, and dependencies land in
     * sub-packages of this package.
     */
    @JsonProperty("mainPackage")
    private String mainPackage;

    /**
     * Definition text for the main package. Becomes the OwnedComment body
     * on the main package element.
     */
    @JsonProperty("mainPackageDefinition")
    private String mainPackageDefinition;

    /**
     * Name of the package under which copied data type sub-packages are placed.
     * In the DDI-CDI to DDSC configuration this equals mainPackage ("DDSC"),
     * so the transformation reuses the existing main package rather than
     * creating a separate data types package.
     */
    @JsonProperty("dataTypesPackage")
    private String dataTypesPackage;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getAcronym()               { return acronym; }
    public String getModelTitle()            { return modelTitle; }
    public String getSubTitle()              { return subTitle; }
    public String getMajorVersion()          { return majorVersion; }
    public String getMinorVersion()          { return minorVersion; }
    public String getLanguage()              { return language; }
    public String getUri()                   { return uri; }
    public String getDefinition()            { return definition; }
    public String getMainPackage()           { return mainPackage; }
    public String getMainPackageDefinition() { return mainPackageDefinition; }
    public String getDataTypesPackage()      { return dataTypesPackage; }
}
