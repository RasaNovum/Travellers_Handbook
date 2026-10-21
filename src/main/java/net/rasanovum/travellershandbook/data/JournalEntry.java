package net.rasanovum.travellershandbook.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public record JournalEntry(String category, String subcategory, String title, String description,
                           String incompleteDescription, List<JournalRequirement> requirements) {
    public JournalEntry {
        requirements = List.copyOf(requirements);
    }

    public static JournalEntry fromJson(JsonObject json) {
        String category = GsonHelper.getAsString(json, "category", "General");
        String subcategory = parseSubcategory(json);
        String title = GsonHelper.getAsString(json, "title", "Unknown Entry");
        String description = GsonHelper.getAsString(json, "description", "");
        String incompleteDescription = GsonHelper.getAsString(json, "incompleteDescription", description);
        JournalRequirement.Mode defaultMode = parseMode(json, JournalRequirement.Mode.LOCK);
        List<JournalRequirement> requirements = new ArrayList<>();

        parseField(json, "advancement", JournalRequirement.Type.ADVANCEMENT, defaultMode, requirements);
        parseField(json, "item", JournalRequirement.Type.ITEM, defaultMode, requirements);
        parseField(json, "structure", JournalRequirement.Type.STRUCTURE, defaultMode, requirements);
        parseField(json, "entry", JournalRequirement.Type.ENTRY, defaultMode, requirements);
        parseAliasedField(json, "mine", "Mine", JournalRequirement.Type.MINE, defaultMode, requirements);
        parseAliasedField(json, "craft", "Craft", JournalRequirement.Type.CRAFT, defaultMode, requirements);
        parseRequirementArray(json, defaultMode, requirements);

        return new JournalEntry(category, subcategory, title, description, incompleteDescription, requirements);
    }

    public String descriptionFor(boolean completed) {
        return completed ? description : incompleteDescription;
    }

    private static String parseSubcategory(JsonObject json) {
        if (!json.has("subcategory")) {
            return "";
        }
        JsonElement element = json.get("subcategory");
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Journal entry subcategory must be a string");
        }
        String subcategory = element.getAsString();
        return subcategory.isBlank() ? "" : subcategory;
    }

    private static void parseAliasedField(JsonObject json, String fieldName, String alias,
                                          JournalRequirement.Type type, JournalRequirement.Mode defaultMode,
                                          List<JournalRequirement> requirements) {
        parseField(json, fieldName, type, defaultMode, requirements);
        if (!json.has(fieldName)) {
            parseField(json, alias, type, defaultMode, requirements);
        }
    }

    private static void parseField(JsonObject json, String fieldName, JournalRequirement.Type type,
                                   JournalRequirement.Mode defaultMode, List<JournalRequirement> requirements) {
        if (!json.has(fieldName)) {
            return;
        }

        JsonElement element = json.get(fieldName);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String id = element.getAsString();
            if (type == JournalRequirement.Type.ADVANCEMENT && id.isEmpty()) {
                return;
            }
            requirements.add(new JournalRequirement(type, id, 1, defaultMode, ""));
            return;
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Requirement field must be a string or object: " + fieldName);
        }

        JsonObject object = element.getAsJsonObject();
        String id = getRequiredString(object, "id", fieldName);
        JournalRequirement.Mode mode = parseMode(object, defaultMode);
        long amount = parseAmount(object, type, fieldName);
        String displayName = parseDisplayName(object, fieldName);
        requirements.add(new JournalRequirement(type, id, amount, mode, displayName));
    }

    private static void parseRequirementArray(JsonObject json, JournalRequirement.Mode defaultMode,
                                              List<JournalRequirement> requirements) {
        if (!json.has("requirements")) {
            return;
        }
        JsonElement element = json.get("requirements");
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Requirements must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement requirementElement : array) {
            if (!requirementElement.isJsonObject()) {
                throw new IllegalArgumentException("Requirement array entries must be objects");
            }
            JsonObject object = requirementElement.getAsJsonObject();
            JournalRequirement.Type type = parseRequirementClass(object);
            String id = getRequiredString(object, "id", "requirements");
            JournalRequirement.Mode mode = parseMode(object, defaultMode);
            long amount = parseAmount(object, type, "requirements");
            String displayName = parseDisplayName(object, "requirements");
            requirements.add(new JournalRequirement(type, id, amount, mode, displayName));
        }
    }

    private static JournalRequirement.Type parseRequirementClass(JsonObject object) {
        String requirementClass = getRequiredString(object, "class", "requirements");
        return switch (requirementClass) {
            case "advancement" -> JournalRequirement.Type.ADVANCEMENT;
            case "item" -> JournalRequirement.Type.ITEM;
            case "structure" -> JournalRequirement.Type.STRUCTURE;
            case "entry" -> JournalRequirement.Type.ENTRY;
            case "mine" -> JournalRequirement.Type.MINE;
            case "craft" -> JournalRequirement.Type.CRAFT;
            default -> throw new IllegalArgumentException("Unknown requirement class: " + requirementClass);
        };
    }

    private static JournalRequirement.Mode parseMode(JsonObject object, JournalRequirement.Mode defaultMode) {
        if (!object.has("type")) {
            return defaultMode;
        }
        String mode = getRequiredString(object, "type", "type");
        return switch (mode) {
            case "lock" -> JournalRequirement.Mode.LOCK;
            case "goal" -> JournalRequirement.Mode.GOAL;
            default -> throw new IllegalArgumentException("Unknown requirement type: " + mode);
        };
    }

    private static long parseAmount(JsonObject object, JournalRequirement.Type type, String fieldName) {
        if (!object.has("amount")) {
            return 1;
        }
        if (!JournalRequirement.supportsAmount(type)) {
            throw new IllegalArgumentException("Amount is only supported for item, mine, and craft requirements: " + fieldName);
        }
        JsonElement amountElement = object.get("amount");
        if (!amountElement.isJsonPrimitive() || !amountElement.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Requirement amount must be a positive integer: " + fieldName);
        }
        JsonPrimitive primitive = amountElement.getAsJsonPrimitive();
        String amountString = primitive.getAsString();
        long amount;
        try {
            amount = Long.parseLong(amountString);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Requirement amount must be a positive integer: " + fieldName, exception);
        }
        if (amount <= 0 || amount > Integer.MAX_VALUE || !amountString.equals(Long.toString(amount))) {
            throw new IllegalArgumentException("Requirement amount must be a positive integer no greater than "
                    + Integer.MAX_VALUE + ": " + fieldName);
        }
        return amount;
    }

    private static String parseDisplayName(JsonObject object, String fieldName) {
        if (!object.has("displayName")) {
            return "";
        }
        return getRequiredString(object, "displayName", fieldName);
    }

    private static String getRequiredString(JsonObject object, String fieldName, String context) {
        if (!object.has(fieldName)) {
            throw new IllegalArgumentException("Missing requirement " + fieldName + ": " + context);
        }
        JsonElement element = object.get(fieldName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Requirement " + fieldName + " must be a string: " + context);
        }
        return element.getAsString();
    }
}
