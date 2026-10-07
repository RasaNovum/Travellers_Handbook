package net.rasanovum.travellershandbook.progression;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.tags.TagKey;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.data.JournalEntry;
import net.rasanovum.travellershandbook.data.JournalRequirement;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JournalProgressManager {
    private static final String DATA_KEY = "travellers_handbook_progress";
    private static final Map<MinecraftServer, ServerState> STATES = new IdentityHashMap<>();

    private JournalProgressManager() {
    }

    public static long progress(ServerPlayer player, JournalRequirement requirement) {
        return switch (requirement.type()) {
            case ADVANCEMENT -> advancementProgress(player, requirement.id());
            case ITEM -> itemProgress(player, requirement.id());
            case STRUCTURE -> structureProgress(player, requirement.id());
            case ENTRY -> entryProgress(player).getOrDefault(parsedId(requirement.id()),
                    new EntryProgress(List.of(), false, false)).completed() ? 1 : 0;
        };
    }

    public static Map<ResourceLocation, EntryProgress> entryProgress(ServerPlayer player) {
        if (player == null || player.getServer() == null || JournalDataLoader.SERVER_ENTRIES.isEmpty()) {
            return Map.of();
        }
        ServerState state = stateFor(player.getServer());
        Map<ResourceLocation, EntryEvaluation> initial = evaluate(player, state);
        updateGatedProgress(player, state, initial);
        Map<ResourceLocation, EntryEvaluation> finalValues = evaluate(player, state);
        Map<ResourceLocation, EntryProgress> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, EntryEvaluation> entry : finalValues.entrySet()) {
            result.put(entry.getKey(), new EntryProgress(entry.getValue().progress(), entry.getValue().unlocked(),
                    entry.getValue().completed()));
        }
        return result;
    }

    public static void tick(MinecraftServer server) {
        if (server.overworld() == null) {
            return;
        }
        if (JournalDataLoader.SERVER_ENTRIES.isEmpty() && !STATES.containsKey(server)) {
            return;
        }
        ServerState state = stateFor(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPosition current = new PlayerPosition(player.serverLevel().dimension(), player.blockPosition());
            if (!current.equals(state.lastPositions.put(player.getUUID(), current))) {
                entryProgress(player);
            }
        }
        if (++state.ticksSinceCheck < 20) {
            return;
        }
        state.ticksSinceCheck = 0;
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            syncIfChanged(player, state);
        }
        state.snapshots.keySet().removeIf(uuid -> !online.contains(uuid));
        state.activeEntries.keySet().removeIf(uuid -> !online.contains(uuid));
        state.lastPositions.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    public static void refresh(ServerPlayer player) {
        if (player == null || player.connection == null || JournalDataLoader.SERVER_ENTRIES.isEmpty()) {
            return;
        }
        entryProgress(player);
        syncIfChanged(player, stateFor(player.getServer()));
    }

    public static void refresh(ServerPlayer player, String advancementId, boolean changed) {
        if (changed && player != null && !JournalDataLoader.SERVER_ENTRIES.isEmpty()) {
            refresh(player);
        }
    }

    public static void onItemStatAwarded(ServerPlayer player, net.minecraft.stats.Stat<?> stat) {
        if (player != null && stat.getType() == Stats.ITEM_PICKED_UP) {
            entryProgress(player);
        }
    }

    public static void copyPlayerState(Player original, ServerPlayer replacement) {
        MinecraftServer server = replacement.getServer();
        if (server != null) {
            stateFor(server).data.copy(original.getUUID(), replacement.getUUID());
        }
    }

    public static void stop(MinecraftServer server) {
        STATES.remove(server);
    }

    private static void syncIfChanged(ServerPlayer player, ServerState state) {
        List<JournalNetworkManager.SyncEntry> entries = JournalNetworkManager.entriesFor(player);
        List<JournalNetworkManager.SyncEntry> previous = state.snapshots.put(player.getUUID(), entries);
        if (previous == null || !previous.equals(entries)) {
            JournalNetworkManager.syncToPlayer(player);
        }
    }

    private static Map<ResourceLocation, EntryEvaluation> evaluate(ServerPlayer player, ServerState state) {
        Map<ResourceLocation, EntryEvaluation> values = new LinkedHashMap<>();
        Map<ResourceLocation, VisitState> visits = new HashMap<>();
        for (ResourceLocation id : JournalDataLoader.SERVER_ENTRIES.keySet()) {
            resolve(player, state, id, values, visits);
        }
        return values;
    }

    private static EntryEvaluation resolve(ServerPlayer player, ServerState state, ResourceLocation id,
                                           Map<ResourceLocation, EntryEvaluation> values,
                                           Map<ResourceLocation, VisitState> visits) {
        EntryEvaluation existing = values.get(id);
        if (existing != null) {
            return existing;
        }
        if (visits.get(id) == VisitState.VISITING) {
            return new EntryEvaluation(List.of(), false, false);
        }
        JournalEntry entry = JournalDataLoader.SERVER_ENTRIES.get(id);
        if (entry == null) {
            return new EntryEvaluation(List.of(), false, false);
        }
        visits.put(id, VisitState.VISITING);
        boolean unlocked = true;
        for (JournalRequirement requirement : entry.requirements()) {
            if (requirement.mode() == JournalRequirement.Mode.LOCK
                    && !requirementSatisfied(player, state, id, entry, requirement, values, visits)) {
                unlocked = false;
            }
        }
        List<Long> progress = new ArrayList<>(entry.requirements().size());
        boolean completed = unlocked;
        for (int index = 0; index < entry.requirements().size(); index++) {
            JournalRequirement requirement = entry.requirements().get(index);
            long current = requirementProgress(player, state, id, entry, requirement, index, unlocked, values, visits);
            current = Math.min(requirement.amount(), Math.max(0, current));
            progress.add(current);
            if (requirement.mode() == JournalRequirement.Mode.LOCK || unlocked) {
                completed &= current >= requirement.amount();
            } else {
                completed = false;
            }
        }
        EntryEvaluation result = new EntryEvaluation(List.copyOf(progress), unlocked, completed);
        visits.put(id, VisitState.RESOLVED);
        values.put(id, result);
        return result;
    }

    private static boolean requirementSatisfied(ServerPlayer player, ServerState state, ResourceLocation entryId,
                                                JournalEntry entry, JournalRequirement requirement,
                                                Map<ResourceLocation, EntryEvaluation> values,
                                                Map<ResourceLocation, VisitState> visits) {
        if (requirement.type() == JournalRequirement.Type.ENTRY) {
            ResourceLocation referenced = parsedId(requirement.id());
            if (referenced == null || referenced.equals(entryId)) {
                return false;
            }
            return resolve(player, state, referenced, values, visits).completed();
        }
        return requirementProgress(player, state, entryId, entry, requirement, -1, true, values, visits)
                >= requirement.amount();
    }

    private static long requirementProgress(ServerPlayer player, ServerState state, ResourceLocation entryId,
                                            JournalEntry entry, JournalRequirement requirement, int index,
                                            boolean unlocked, Map<ResourceLocation, EntryEvaluation> values,
                                            Map<ResourceLocation, VisitState> visits) {
        if (requirement.mode() == JournalRequirement.Mode.GOAL && !unlocked) {
            return 0;
        }
        if (requirement.type() == JournalRequirement.Type.ENTRY) {
            ResourceLocation referenced = parsedId(requirement.id());
            if (referenced == null || referenced.equals(entryId)) {
                return 0;
            }
            return resolve(player, state, referenced, values, visits).completed() ? 1 : 0;
        }
        boolean gated = hasLocks(entry);
        if (requirement.mode() == JournalRequirement.Mode.GOAL && gated && index >= 0
                && (requirement.type() == JournalRequirement.Type.ITEM
                || requirement.type() == JournalRequirement.Type.STRUCTURE)) {
            return state.data.counter(player.getUUID(), counterKey(entryId, requirement));
        }
        return switch (requirement.type()) {
            case ADVANCEMENT -> advancementProgress(player, requirement.id());
            case ITEM -> itemProgress(player, requirement.id());
            case STRUCTURE -> structureProgress(player, requirement.id());
            case ENTRY -> 0;
        };
    }

    private static void updateGatedProgress(ServerPlayer player, ServerState state,
                                            Map<ResourceLocation, EntryEvaluation> evaluations) {
        UUID playerId = player.getUUID();
        Map<ResourceLocation, Boolean> active = state.activeEntries.computeIfAbsent(playerId, ignored -> new HashMap<>());
        for (Map.Entry<ResourceLocation, JournalEntry> loaded : JournalDataLoader.SERVER_ENTRIES.entrySet()) {
            ResourceLocation entryId = loaded.getKey();
            JournalEntry entry = loaded.getValue();
            if (!hasLocks(entry)) {
                continue;
            }
            boolean unlocked = evaluations.getOrDefault(entryId, new EntryEvaluation(List.of(), false, false)).unlocked();
            boolean wasActive = active.getOrDefault(entryId, false);
            for (int index = 0; index < entry.requirements().size(); index++) {
                JournalRequirement requirement = entry.requirements().get(index);
                if (requirement.mode() != JournalRequirement.Mode.GOAL) {
                    continue;
                }
                String key = counterKey(entryId, requirement);
                if (requirement.type() == JournalRequirement.Type.ITEM) {
                    long current = itemProgress(player, requirement.id());
                    long previous = state.data.window(playerId, key);
                    if (unlocked && wasActive && state.data.hasWindow(playerId, key)) {
                        long delta = Math.max(0, current - previous);
                        if (delta > 0) {
                            state.data.addCounter(playerId, key, delta);
                        }
                    }
                    state.data.setWindow(playerId, key, current);
                } else if (requirement.type() == JournalRequirement.Type.STRUCTURE && unlocked && wasActive
                        && isInsideStructure(player, requirement.id())) {
                    state.data.setCounter(playerId, key, requirement.amount());
                }
            }
            active.put(entryId, unlocked);
        }
    }

    private static boolean hasLocks(JournalEntry entry) {
        return entry.requirements().stream().anyMatch(requirement -> requirement.mode() == JournalRequirement.Mode.LOCK);
    }

    private static String counterKey(ResourceLocation entryId, JournalRequirement requirement) {
        return entryId + "|" + requirement.type() + "|" + requirement.id();
    }

    private static ResourceLocation parsedId(String id) {
        try {
            return TravellersHandbook.parseId(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ServerState stateFor(MinecraftServer server) {
        ServerState state = STATES.get(server);
        if (state == null) {
            state = new ServerState(loadData(server));
            STATES.put(server, state);
        }
        return state;
    }

    private static ProgressData loadData(MinecraftServer server) {
        //? if <1.21 {
        /*return server.overworld().getDataStorage().computeIfAbsent(ProgressData::load, ProgressData::new, DATA_KEY);
        *///?} else {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ProgressData::new, ProgressData::load, null), DATA_KEY);
        //?}
    }

    private static long advancementProgress(ServerPlayer player, String id) {
        if (player.getServer() == null) {
            return 0;
        }
        ResourceLocation resourceLocation = parsedId(id);
        if (resourceLocation == null) {
            return 0;
        }
        //? if <1.21 {
        /*net.minecraft.advancements.Advancement advancement = player.getServer().getAdvancements().getAdvancement(resourceLocation);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone() ? 1 : 0;
        *///?} else {
        net.minecraft.advancements.AdvancementHolder advancement = player.getServer().getAdvancements().get(resourceLocation);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone() ? 1 : 0;
        //?}
    }

    private static long itemProgress(ServerPlayer player, String id) {
        ResourceLocation resourceLocation = parsedId(id);
        if (resourceLocation == null || !BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.get(resourceLocation);
        return Math.max(0, player.getStats().getValue(Stats.ITEM_PICKED_UP, item));
    }

    private static long structureProgress(ServerPlayer player, String id) {
        ServerState state = stateFor(player.getServer());
        UUID playerId = player.getUUID();
        if (state.data.hasVisited(playerId, id)) {
            return 1;
        }
        if (isInsideStructure(player, id)) {
            state.data.markVisited(playerId, id);
            return 1;
        }
        return 0;
    }

    private static boolean isInsideStructure(ServerPlayer player, String id) {
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        LevelChunk chunk = player.serverLevel().getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return false;
        }
        StructureManager manager = player.serverLevel().structureManager();
        if (id.startsWith("#")) {
            ResourceLocation tagId = parsedId(id.substring(1));
            if (tagId == null) {
                return false;
            }
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
            return manager.getStructureWithPieceAt(player.blockPosition(), tag).isValid();
        }
        ResourceLocation resourceLocation = parsedId(id);
        if (resourceLocation == null) {
            return false;
        }
        Structure structure = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(resourceLocation);
        return structure != null && manager.getStructureWithPieceAt(player.blockPosition(), structure).isValid();
    }

    private enum VisitState {
        VISITING,
        RESOLVED
    }

    private record EntryEvaluation(List<Long> progress, boolean unlocked, boolean completed) {
    }

    private record PlayerPosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                  net.minecraft.core.BlockPos position) {
    }

    public record EntryProgress(List<Long> progress, boolean unlocked, boolean completed) {
        public EntryProgress {
            progress = List.copyOf(progress);
        }
    }

    private static final class ServerState {
        private final ProgressData data;
        private final Map<UUID, List<JournalNetworkManager.SyncEntry>> snapshots = new HashMap<>();
        private final Map<UUID, Map<ResourceLocation, Boolean>> activeEntries = new HashMap<>();
        private final Map<UUID, PlayerPosition> lastPositions = new HashMap<>();
        private int ticksSinceCheck;

        private ServerState(ProgressData data) {
            this.data = data;
        }
    }

    public static final class ProgressData extends SavedData {
        private final Map<UUID, Set<String>> visitedStructures = new HashMap<>();
        private final Map<UUID, Map<String, Long>> counters = new HashMap<>();
        private final Map<UUID, Map<String, Long>> windows = new HashMap<>();

        public static ProgressData load(CompoundTag tag
                //? if <1.21 {
                /*
                *///?} else {
                , HolderLookup.Provider registries
                //?}
        ) {
            ProgressData data = new ProgressData();
            ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                CompoundTag playerTag = players.getCompound(i);
                try {
                    UUID uuid = UUID.fromString(playerTag.getString("uuid"));
                    Set<String> structures = new HashSet<>();
                    ListTag visited = playerTag.getList("structures", Tag.TAG_STRING);
                    for (int j = 0; j < visited.size(); j++) {
                        structures.add(visited.getString(j));
                    }
                    data.visitedStructures.put(uuid, structures);
                    data.readLongMap(playerTag.getCompound("counters"), data.counters, uuid);
                    data.readLongMap(playerTag.getCompound("windows"), data.windows, uuid);
                } catch (Exception ignored) {
                }
            }
            return data;
        }

        private void readLongMap(CompoundTag tag, Map<UUID, Map<String, Long>> target, UUID uuid) {
            Map<String, Long> values = new HashMap<>();
            for (String key : tag.getAllKeys()) {
                values.put(key, tag.getLong(key));
            }
            if (!values.isEmpty()) {
                target.put(uuid, values);
            }
        }

        public boolean hasVisited(UUID player, String structure) {
            return visitedStructures.getOrDefault(player, Set.of()).contains(structure);
        }

        public void markVisited(UUID player, String structure) {
            if (visitedStructures.computeIfAbsent(player, ignored -> new HashSet<>()).add(structure)) {
                setDirty();
            }
        }

        private long counter(UUID player, String key) {
            return counters.getOrDefault(player, Map.of()).getOrDefault(key, 0L);
        }

        private void setCounter(UUID player, String key, long value) {
            if (counter(player, key) != value) {
                counters.computeIfAbsent(player, ignored -> new HashMap<>()).put(key, value);
                setDirty();
            }
        }

        private void addCounter(UUID player, String key, long value) {
            if (value > 0) {
                setCounter(player, key, counter(player, key) + value);
            }
        }

        private long window(UUID player, String key) {
            return windows.getOrDefault(player, Map.of()).getOrDefault(key, 0L);
        }

        private boolean hasWindow(UUID player, String key) {
            return windows.getOrDefault(player, Map.of()).containsKey(key);
        }

        private void setWindow(UUID player, String key, long value) {
            if (!hasWindow(player, key) || window(player, key) != value) {
                windows.computeIfAbsent(player, ignored -> new HashMap<>()).put(key, value);
                setDirty();
            }
        }

        public void copy(UUID original, UUID replacement) {
            Set<String> structures = visitedStructures.get(original);
            if (structures != null && !structures.isEmpty()) {
                visitedStructures.put(replacement, new HashSet<>(structures));
            }
            copyMap(counters, original, replacement);
            copyMap(windows, original, replacement);
            setDirty();
        }

        private void copyMap(Map<UUID, Map<String, Long>> source, UUID original, UUID replacement) {
            Map<String, Long> values = source.get(original);
            if (values != null && !values.isEmpty()) {
                source.put(replacement, new HashMap<>(values));
            }
        }

        //? if <1.21 {
        /*@Override
        public CompoundTag save(CompoundTag tag) {
        *///?} else {
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        //?}
            ListTag players = new ListTag();
            Set<UUID> playerIds = new HashSet<>();
            playerIds.addAll(visitedStructures.keySet());
            playerIds.addAll(counters.keySet());
            playerIds.addAll(windows.keySet());
            for (UUID uuid : playerIds) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putString("uuid", uuid.toString());
                ListTag structures = new ListTag();
                for (String structure : visitedStructures.getOrDefault(uuid, Set.of())) {
                    structures.add(net.minecraft.nbt.StringTag.valueOf(structure));
                }
                playerTag.put("structures", structures);
                writeLongMap(playerTag, "counters", counters.get(uuid));
                writeLongMap(playerTag, "windows", windows.get(uuid));
                players.add(playerTag);
            }
            tag.put("players", players);
            return tag;
        }

        private void writeLongMap(CompoundTag parent, String name, Map<String, Long> values) {
            CompoundTag output = new CompoundTag();
            if (values != null) {
                for (Map.Entry<String, Long> entry : values.entrySet()) {
                    output.putLong(entry.getKey(), entry.getValue());
                }
            }
            parent.put(name, output);
        }
    }
}
