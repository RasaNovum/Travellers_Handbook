package net.rasanovum.travellershandbook.loaders.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        TravellersHandbook.initialize();
        FabricNetwork.initialize();
        registerDataPackListener();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                JournalNetworkManager.syncToPlayer(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                JournalNetworkManager.syncToPlayer(newPlayer));
    }

    private static void registerDataPackListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            private final PreparableReloadListener delegate = new JournalDataLoader();

            @Override
            public ResourceLocation getFabricId() {
                return TravellersHandbook.id("journal_entries");
            }

            @Override
            public CompletableFuture<Void> reload(
                    PreparationBarrier preparationBarrier,
                    ResourceManager resourceManager,
                    ProfilerFiller preparationsProfiler,
                    ProfilerFiller reloadProfiler,
                    Executor backgroundExecutor,
                    Executor gameExecutor
            ) {
                return delegate.reload(preparationBarrier, resourceManager, preparationsProfiler,
                        reloadProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }
}
