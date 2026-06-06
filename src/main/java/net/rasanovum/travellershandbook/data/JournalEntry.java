package net.rasanovum.travellershandbook.data;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public record JournalEntry(String category, String title, String description, String advancementRequirement) {

    public static JournalEntry fromJson(JsonObject json) {
        String category = GsonHelper.getAsString(json, "category", "General");
        String title = GsonHelper.getAsString(json, "title", "Unknown Entry");
        String description = GsonHelper.getAsString(json, "description", "");
        String advancement = GsonHelper.getAsString(json, "advancement", "");
        return new JournalEntry(category, title, description, advancement);
    }
}
