package net.rasanovum.travellershandbook.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.TravellersHandbookConfig;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DefaultEntriesPackManager {
    private static volatile String packId;
    private static volatile Boolean appliedState;
    private static volatile CompletableFuture<Void> reloadFuture;

    private DefaultEntriesPackManager() {
    }

    public static void initialize(String registeredPackId) {
        packId = registeredPackId;
    }

    public static void onServerStarted(MinecraftServer server) {
        appliedState = null;
        reloadFuture = null;
    }

    public static void tick(MinecraftServer server) {
        if (reloadFuture != null && !reloadFuture.isDone()) {
            return;
        }

        applyIfChanged(server);
    }

    private static void applyIfChanged(MinecraftServer server) {
        boolean shouldLoad = TravellersHandbookConfig.loadDefaultEntries;
        if (appliedState != null && appliedState == shouldLoad) {
            return;
        }

        if (packId == null) {
            TravellersHandbook.LOGGER.error("The default entries datapack was not registered.");
            appliedState = shouldLoad;
            return;
        }

        PackRepository repository = server.getPackRepository();
        repository.reload();
        if (repository.getPack(packId) == null) {
            TravellersHandbook.LOGGER.error("The default entries datapack {} could not be found.", packId);
            appliedState = shouldLoad;
            return;
        }

        List<String> selectedPacks = new ArrayList<>(repository.getSelectedIds());
        boolean isLoaded = selectedPacks.contains(packId);
        appliedState = shouldLoad;
        if (isLoaded == shouldLoad) {
            TravellersHandbook.LOGGER.info("Default journal entries are {}.", shouldLoad ? "enabled" : "disabled");
            return;
        }

        if (shouldLoad) {
            selectedPacks.add(packId);
        } else {
            selectedPacks.remove(packId);
        }

        reloadFuture = server.reloadResources(selectedPacks).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                TravellersHandbook.LOGGER.error("Failed to reload the default entries datapack.", throwable);
                appliedState = null;
                return;
            }

            TravellersHandbook.LOGGER.info("Default journal entries {}.", shouldLoad ? "enabled" : "disabled");
            server.getPlayerList().getPlayers().forEach(JournalNetworkManager::syncToPlayer);
        });
    }
}
