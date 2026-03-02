package blackbox.config;

/*
 * File   : SourceClassDeserializer.java
 * Package: blackbox.config
 * Purpose: Custom Jackson deserialiser for the "sourceClass" field of a class
 *          mapping entry. The JSON Schema defines this field as oneOf(String, array):
 *
 *            "map"   mappings use a plain string:  "sourceClass": "DataStore"
 *            "merge" mappings use an array:        "sourceClass": ["WideDataSet", "PhysicalDataSet"]
 *
 *          Jackson cannot deserialise a union type directly into a List<String>
 *          without help. This deserialiser normalises both forms to List<String>
 *          so that ClassMappingConfig.sourceClasses is always a list, regardless
 *          of the mapping type. This simplifies all downstream code.
 */

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deserialises the "sourceClass" JSON field into a List&lt;String&gt;,
 * accepting either a plain JSON string or a JSON array of strings.
 */
public class SourceClassDeserializer extends StdDeserializer<List<String>> {

    // Required by Java serialization contract: StdDeserializer implements Serializable,
    // so all subclasses should declare serialVersionUID to suppress IDE warnings and
    // ensure stable serialisation across recompilations. Jackson deserializers are
    // never actually serialised in practice, but the field is harmless.
    private static final long serialVersionUID = 1L;

    public SourceClassDeserializer() {
        super(List.class);
    }

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {

        // Determine the JSON token type to decide how to read the value.
        JsonToken token = parser.currentToken();

        if (token == JsonToken.VALUE_STRING) {
            // "map" mapping: sourceClass is a plain string — wrap in a single-element list.
            return Collections.singletonList(parser.getText());

        } else if (token == JsonToken.START_ARRAY) {
            // "merge" mapping: sourceClass is an array of exactly two strings.
            List<String> result = new ArrayList<>(2);
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                result.add(parser.getText());
            }
            return result;

        } else if (token == JsonToken.VALUE_NULL) {
            // "new" mapping: sourceClass is absent/null — return empty list.
            return Collections.emptyList();

        } else {
            // Unexpected token type — report as a deserialisation problem.
            throw context.wrongTokenException(parser, String.class, token,
                    "Expected string or array for sourceClass field");
        }
    }
}
