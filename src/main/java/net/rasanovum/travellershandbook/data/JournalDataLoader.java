package net.rasanovum.travellershandbook.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JournalDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<ResourceLocation, JournalEntry> SERVER_ENTRIES = new LinkedHashMap<>();

    public JournalDataLoader() {
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
                    SERVER_ENTRIES.put(id, JournalEntry.fromJson(jsonObject));
                }
            } catch (Exception e) {
                net.rasanovum.travellershandbook.TravellersHandbook.LOGGER.error(
                        "Failed to parse sorted journal entry JSON: {}", id, e);
            }
        }
    }
}
