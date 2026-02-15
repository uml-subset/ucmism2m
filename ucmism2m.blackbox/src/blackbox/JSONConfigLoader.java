package blackbox;

import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.eclipse.m2m.qvt.oml.blackbox.java.Operation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Black-box operations for JSON configuration loading.
 * Uses org.json.JSONObject for parsing.
 * 
 * Compatible with Eclipse 2025-12, QVTo 3.11.1, Java 21.
 */
@Module(packageURIs = {"http://www.eclipse.org/uml2/5.0.0/UML"})
public class JSONConfigLoader {
    
    /**
     * Load JSON configuration file and return as Map.
     * 
     * @param jsonPath Path to JSON configuration file
     * @return Map representation of JSON structure
     */
    @Operation(contextual = false)
    public static Object loadConfig(String jsonPath) {
        return switch (validatePath(jsonPath)) {
            case Path p when Files.exists(p) && Files.isReadable(p) -> {
                try {
                    String content = Files.readString(p);
                    JSONObject json = new JSONObject(content);
                    yield jsonObjectToMap(json);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read JSON file: " + jsonPath, e);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse JSON: " + jsonPath, e);
                }
            }
            case Path p when !Files.exists(p) -> 
                throw new RuntimeException("Configuration file not found: " + p);
            case Path p -> 
                throw new RuntimeException("Configuration file not readable: " + p);
            case null -> 
                throw new IllegalArgumentException("Configuration path cannot be null or empty");
        };
    }
    
    /**
     * Get string value from JSON configuration.
     * 
     * @param jsonPath Path to JSON file
     * @param key Key to retrieve
     * @return String value or null if not found
     */
    @Operation(contextual = false)
    public static String getStringValue(String jsonPath, String key) {
        try {
            Path p = Path.of(jsonPath);
            String content = Files.readString(p);
            JSONObject json = new JSONObject(content);
            return json.optString(key, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get string value for key: " + key, e);
        }
    }
    
    /**
     * Get list of strings from JSON configuration.
     * 
     * @param jsonPath Path to JSON file
     * @param key Key to retrieve array
     * @return List of strings
     */
    @Operation(contextual = false)
    public static List<String> getStringList(String jsonPath, String key) {
        try {
            Path p = Path.of(jsonPath);
            String content = Files.readString(p);
            JSONObject json = new JSONObject(content);
            
            if (!json.has(key)) {
                return new ArrayList<>();
            }
            
            JSONArray array = json.getJSONArray(key);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get string list for key: " + key, e);
        }
    }
    
    /**
     * Get boolean value from JSON configuration.
     * 
     * @param jsonPath Path to JSON file
     * @param key Key to retrieve
     * @param defaultValue Default value if key not found
     * @return Boolean value
     */
    @Operation(contextual = false)
    public static Boolean getBooleanValue(String jsonPath, String key, Boolean defaultValue) {
        try {
            Path p = Path.of(jsonPath);
            String content = Files.readString(p);
            JSONObject json = new JSONObject(content);
            return json.optBoolean(key, defaultValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get boolean value for key: " + key, e);
        }
    }
    
    /**
     * Check if JSON configuration contains a key.
     * 
     * @param jsonPath Path to JSON file
     * @param key Key to check
     * @return true if key exists, false otherwise
     */
    @Operation(contextual = false)
    public static Boolean hasKey(String jsonPath, String key) {
        try {
            Path p = Path.of(jsonPath);
            String content = Files.readString(p);
            JSONObject json = new JSONObject(content);
            return json.has(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check key: " + key, e);
        }
    }
    
    // Helper methods
    
    private static Path validatePath(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return null;
        }
        return Path.of(pathStr);
    }
    
    private static Map<String, Object> jsonObjectToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        
        for (String key : json.keySet()) {
            Object value = json.get(key);
            
            if (value instanceof JSONObject nestedJson) {
                map.put(key, jsonObjectToMap(nestedJson));
            } else if (value instanceof JSONArray array) {
                map.put(key, jsonArrayToList(array));
            } else {
                map.put(key, value);
            }
        }
        
        return map;
    }
    
    private static List<Object> jsonArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            
            if (value instanceof JSONObject json) {
                list.add(jsonObjectToMap(json));
            } else if (value instanceof JSONArray nestedArray) {
                list.add(jsonArrayToList(nestedArray));
            } else {
                list.add(value);
            }
        }
        
        return list;
    }
}
