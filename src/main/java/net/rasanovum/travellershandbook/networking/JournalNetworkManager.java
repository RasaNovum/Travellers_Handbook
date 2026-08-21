package net.rasanovum.travellershandbook.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.data.JournalEntry;

import java.util.ArrayList;
import java.util.List;

//? if <1.21 {
/*import net.minecraft.advancements.Advancement;
*///?} else {
import net.minecraft.advancements.AdvancementHolder;
//?}

public final class JournalNetworkManager {
    public static final ResourceLocation SYNC_PACKET_ID = TravellersHandbook.id("sync_entries");

    private static NetworkBridge networkBridge;

    private JournalNetworkManager() {
    }

    public static void initialize(NetworkBridge bridge) {
        networkBridge = bridge;
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (networkBridge == null) {
            return;
        }

        networkBridge.sendSync(player, entriesFor(player));
    }

    public static List<SyncEntry> entriesFor(ServerPlayer player) {
        List<SyncEntry> entries = new ArrayList<>(JournalDataLoader.SERVER_ENTRIES.size());

        for (JournalEntry entry : JournalDataLoader.SERVER_ENTRIES.values()) {
            String requirement = entry.advancementRequirement();
            boolean unlocked = requirement.isEmpty() || hasAdvancement(player, requirement);
            boolean advancementHasDisplay = !requirement.isEmpty() && advancementHasDisplay(player, requirement);
            entries.add(new SyncEntry(entry.category(), entry.title(), entry.description(), requirement, unlocked, advancementHasDisplay));
        }

        return entries;
    }

    public static void writeSync(FriendlyByteBuf buffer, List<SyncEntry> entries) {
        buffer.writeInt(entries.size());

        for (SyncEntry entry : entries) {
            buffer.writeUtf(entry.category());
            buffer.writeBoolean(entry.unlocked());
            buffer.writeUtf(entry.title());
            buffer.writeUtf(entry.description());
            buffer.writeUtf(entry.advancementRequirement());
            buffer.writeBoolean(entry.advancementHasDisplay());
        }
    }

    public static List<SyncEntry> readSync(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<SyncEntry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String category = buffer.readUtf();
            boolean unlocked = buffer.readBoolean();
            String title = buffer.readUtf();
            String description = buffer.readUtf();
            String advancementRequirement = buffer.readUtf();
            boolean advancementHasDisplay = buffer.readBoolean();
            entries.add(new SyncEntry(category, title, description, advancementRequirement, unlocked, advancementHasDisplay));
        }

        return entries;
    }

    private static boolean hasAdvancement(ServerPlayer player, String advancementId) {
        if (player.getServer() == null) {
            return false;
        }

        ResourceLocation id = TravellersHandbook.parseId(advancementId);
        //? if <1.21 {
        /*Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
        *///?} else {
        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
        //?}
    }

    private static boolean advancementHasDisplay(ServerPlayer player, String advancementId) {
        if (player.getServer() == null) {
            return false;
        }

        ResourceLocation id = TravellersHandbook.parseId(advancementId);
        //? if <1.21 {
        /*Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        return advancement != null && advancement.getDisplay() != null;
        *///?} else {
        AdvancementHolder advancement = player.getServer().getAdvancements().get(id);
        return advancement != null && advancement.value().display().isPresent();
        //?}
    }

    public record SyncEntry(String category, String title, String description,
                            String advancementRequirement, boolean unlocked, boolean advancementHasDisplay) {
    }

    @FunctionalInterface
    public interface NetworkBridge {
        void sendSync(ServerPlayer player, List<SyncEntry> entries);
    }
}
