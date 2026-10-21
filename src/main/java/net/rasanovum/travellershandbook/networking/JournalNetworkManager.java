package net.rasanovum.travellershandbook.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.data.JournalEntry;
import net.rasanovum.travellershandbook.data.JournalRequirement;
import net.rasanovum.travellershandbook.progression.JournalProgressManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//? if <1.21 {
/*import net.minecraft.advancements.Advancement;
*///?} else {
import net.minecraft.advancements.AdvancementHolder;
//?}

public final class JournalNetworkManager {
    public static final ResourceLocation SYNC_PACKET_ID = TravellersHandbook.id("sync_entries");
    public static final ResourceLocation JOURNAL_OPENED_PACKET_ID = TravellersHandbook.id("journal_opened");
    private static final String JOURNAL_OPENED_TAG = "travellers_handbook_journal_opened";

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

        networkBridge.sendSync(player, entriesFor(player), hasJournalBeenOpened(player));
    }

    public static void markJournalOpened(ServerPlayer player) {
        player.addTag(JOURNAL_OPENED_TAG);
    }

    public static void copyJournalState(Player original, ServerPlayer replacement) {
        if (hasJournalBeenOpened(original)) {
            markJournalOpened(replacement);
        }
    }

    private static boolean hasJournalBeenOpened(Player player) {
        return player.getTags().contains(JOURNAL_OPENED_TAG);
    }

    public static List<SyncEntry> entriesFor(ServerPlayer player) {
        List<SyncEntry> entries = new ArrayList<>(JournalDataLoader.SERVER_ENTRIES.size());
        Map<ResourceLocation, JournalProgressManager.EntryProgress> progressByEntry = JournalProgressManager.entryProgress(player);

        for (Map.Entry<ResourceLocation, JournalEntry> loadedEntry : JournalDataLoader.SERVER_ENTRIES.entrySet()) {
            JournalEntry entry = loadedEntry.getValue();
            JournalProgressManager.EntryProgress entryProgress = progressByEntry.get(loadedEntry.getKey());
            List<RequirementProgress> requirements = new ArrayList<>(entry.requirements().size());
            boolean advancementHasDisplay = false;

            for (int i = 0; i < entry.requirements().size(); i++) {
                JournalRequirement requirement = entry.requirements().get(i);
                long progress = Math.min(requirement.amount(), Math.max(0, entryProgress.progress().get(i)));
                boolean satisfied = progress >= requirement.amount();
                requirements.add(new RequirementProgress(requirement, progress, satisfied));
                if (requirement.type() == JournalRequirement.Type.ADVANCEMENT && satisfied) {
                    advancementHasDisplay |= advancementHasDisplay(player, requirement.id());
                }
            }

            String description = entry.descriptionFor(entryProgress.completed());
            entries.add(new SyncEntry(loadedEntry.getKey().toString(), entry.category(), entry.title(), description,
                    requirements, entryProgress.unlocked(), entryProgress.completed(), advancementHasDisplay,
                    entry.subcategory()));
        }

        return entries;
    }

    public static void writeSync(FriendlyByteBuf buffer, List<SyncEntry> entries, boolean journalOpened) {
        buffer.writeBoolean(journalOpened);
        buffer.writeInt(entries.size());

        for (SyncEntry entry : entries) {
            buffer.writeUtf(entry.id());
            buffer.writeUtf(entry.category());
            buffer.writeUtf(entry.subcategory());
            buffer.writeBoolean(entry.unlocked());
            buffer.writeUtf(entry.title());
            buffer.writeUtf(entry.description());
            buffer.writeVarInt(entry.requirements().size());
            for (RequirementProgress requirementProgress : entry.requirements()) {
                JournalRequirement requirement = requirementProgress.requirement();
                buffer.writeEnum(requirement.type());
                buffer.writeUtf(requirement.id());
                buffer.writeVarLong(requirement.amount());
                buffer.writeEnum(requirement.mode());
                buffer.writeUtf(requirement.displayName());
                buffer.writeVarLong(requirementProgress.progress());
                buffer.writeBoolean(requirementProgress.completed());
            }
            buffer.writeBoolean(entry.completed());
            buffer.writeBoolean(entry.advancementHasDisplay());
        }
    }

    public static SyncData readSync(FriendlyByteBuf buffer) {
        boolean journalOpened = buffer.readBoolean();
        int size = buffer.readInt();
        List<SyncEntry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String id = buffer.readUtf();
            String category = buffer.readUtf();
            String subcategory = buffer.readUtf();
            boolean unlocked = buffer.readBoolean();
            String title = buffer.readUtf();
            String description = buffer.readUtf();
            int requirementCount = buffer.readVarInt();
            List<RequirementProgress> requirements = new ArrayList<>(requirementCount);
            for (int j = 0; j < requirementCount; j++) {
                JournalRequirement requirement = new JournalRequirement(buffer.readEnum(JournalRequirement.Type.class),
                        buffer.readUtf(), buffer.readVarLong(), buffer.readEnum(JournalRequirement.Mode.class), buffer.readUtf());
                requirements.add(new RequirementProgress(requirement, buffer.readVarLong(), buffer.readBoolean()));
            }
            boolean completed = buffer.readBoolean();
            boolean advancementHasDisplay = buffer.readBoolean();
            entries.add(new SyncEntry(id, category, title, description, requirements, unlocked, completed,
                    advancementHasDisplay, subcategory));
        }

        return new SyncData(entries, journalOpened);
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

    public record SyncEntry(String id, String category, String title, String description,
                            List<RequirementProgress> requirements, boolean unlocked, boolean completed,
                            boolean advancementHasDisplay, String subcategory) {
        public SyncEntry {
            requirements = List.copyOf(requirements);
        }

        public boolean hasRequirements() {
            return !requirements.isEmpty();
        }

        public boolean hasGoals() {
            return requirements.stream().anyMatch(progress -> progress.requirement().isGoal());
        }

        public boolean hasOnlyLocks() {
            return !requirements.isEmpty() && requirements.stream()
                    .allMatch(progress -> progress.requirement().isLock());
        }

        public String advancementRequirement() {
            return requirements.stream()
                    .map(RequirementProgress::requirement)
                    .filter(requirement -> requirement.type() == JournalRequirement.Type.ADVANCEMENT)
                    .map(JournalRequirement::id)
                    .findFirst().orElse("");
        }
    }

    public record RequirementProgress(JournalRequirement requirement, long progress, boolean completed) {
    }

    public record SyncData(List<SyncEntry> entries, boolean journalOpened) {
    }

    @FunctionalInterface
    public interface NetworkBridge {
        void sendSync(ServerPlayer player, List<SyncEntry> entries, boolean journalOpened);
    }
}
