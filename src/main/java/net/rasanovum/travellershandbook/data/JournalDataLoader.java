package net.rasanovum.travellershandbook.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JournalDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<ResourceLocation, JournalEntry> SERVER_ENTRIES = new LinkedHashMap<>();

    public JournalDataLoader() {
        // "data/<namespace>/traveller_entries/"
        super(GSON, "traveller_entries");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        SERVER_ENTRIES.clear();

        List<ResourceLocation> sortedKeys = new ArrayList<>(prepared.keySet());

        sortedKeys.sort((id1, id2) -> id1.getPath().compareTo(id2.getPath()));

        for (ResourceLocation id : sortedKeys) {
            try {
                JsonElement element = prepared.get(id);
                if (element != null) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    JournalEntry journalEntry = JournalEntry.fromJson(jsonObject);
                    SERVER_ENTRIES.put(id, journalEntry);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse sorted journal entry JSON: " + id);
            }
        }
    }

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation("travellers-handbook", "journal_entries");
    }
}
