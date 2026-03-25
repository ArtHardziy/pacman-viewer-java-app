package com.example.configviewer.parser;

import com.example.configviewer.model.ParamRecord;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigJsonParser {

    public List<ParamRecord> parse(Path file) throws IOException {
        String json = Files.readString(file);
        JsonArray root = JsonParser.parseString(json).getAsJsonArray();
        List<ParamRecord> result = new ArrayList<>();

        for (JsonElement element : root) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject item = element.getAsJsonObject();
            JsonObject key = getObj(item, "key");
            JsonObject parameter = getObj(item, "parameter");

            String tenant = getString(key, "tenant");
            String scope = getString(key, "scope");
            String location = getString(key, "location");
            String name = getString(parameter, "name");
            String description = getString(parameter, "description");
            String dataType = getString(parameter, "dataType");

            JsonArray bundles = getArr(parameter, "bundles");
            if (bundles == null || bundles.isEmpty()) {
                result.add(newParam(name, description, dataType, "", "", tenant, scope, location));
                continue;
            }

            for (JsonElement bundleEl : bundles) {
                if (!bundleEl.isJsonObject()) {
                    continue;
                }

                JsonObject bundle = bundleEl.getAsJsonObject();
                String path = pathToString(getArr(bundle, "path"));
                JsonArray values = getArr(bundle, "values");

                if (values == null || values.isEmpty()) {
                    result.add(newParam(name, description, dataType, "", path, tenant, scope, location));
                    continue;
                }

                for (JsonElement value : values) {
                    result.add(newParam(name, description, dataType, jsonPrimitiveToString(value), path, tenant, scope, location));
                }
            }
        }

        return result;
    }

    private ParamRecord newParam(String name,
                                 String description,
                                 String dataType,
                                 String value,
                                 String path,
                                 String tenant,
                                 String scope,
                                 String location) {
        String compositeKey = buildCompositeKey(tenant, scope, location, path, name);
        return new ParamRecord(compositeKey, name, description, dataType, value, path, tenant, scope, location);
    }

    private static JsonObject getObj(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return obj.getAsJsonObject(key);
    }

    private static JsonArray getArr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonArray()) {
            return null;
        }
        return obj.getAsJsonArray(key);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return jsonPrimitiveToString(obj.get(key));
    }

    private static String pathToString(JsonArray pathArr) {
        if (pathArr == null || pathArr.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (JsonElement el : pathArr) {
            String part = jsonPrimitiveToString(el).trim();
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return String.join("/", parts);
    }

    private static String jsonPrimitiveToString(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return "";
        }

        if (el.isJsonPrimitive()) {
            return el.getAsJsonPrimitive().getAsString();
        }
        return el.toString();
    }

    private static String buildCompositeKey(String tenant, String scope, String location, String path, String name) {
        return String.join("|",
                nullToEmpty(tenant),
                nullToEmpty(scope),
                nullToEmpty(location),
                nullToEmpty(path),
                nullToEmpty(name));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
