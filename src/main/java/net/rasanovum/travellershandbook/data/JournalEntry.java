package net.rasanovum.travellershandbook.data;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class JournalEntry {
    private final String category;
    private final String title;
    private final String description;

    public JournalEntry(String category, String title, String description) {
        this.category = category;
        this.title = title;
        this.description = description;
    }

    public static JournalEntry fromJson(JsonObject json) {
        String category = GsonHelper.getAsString(json, "category", "General");
        String title = GsonHelper.getAsString(json, "title", "Unknown Entry");
        String description = GsonHelper.getAsString(json, "description", "");
        return new JournalEntry(category, title, description);
    }

    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
