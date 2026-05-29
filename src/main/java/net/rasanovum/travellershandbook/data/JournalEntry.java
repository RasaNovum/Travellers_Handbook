package net.rasanovum.travellershandbook.data;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class JournalEntry {
    private final String category;
    private final String title;
    private final String description;
    private final String advancementRequirement;

    public JournalEntry(String category, String title, String description, String advancementRequirement) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.advancementRequirement = advancementRequirement;
    }

    public static JournalEntry fromJson(JsonObject json) {
        String category = GsonHelper.getAsString(json, "category", "General");
        String title = GsonHelper.getAsString(json, "title", "Unknown Entry");
        String description = GsonHelper.getAsString(json, "description", "");
        String advancement = GsonHelper.getAsString(json, "advancement", "");
        return new JournalEntry(category, title, description, advancement);
    }

    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAdvancementRequirement() { return advancementRequirement; }
}
