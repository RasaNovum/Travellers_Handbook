package net.rasanovum.travellershandbook.loaders.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.DefaultEntriesPackManager;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.UnaryOperator;

@Mod(NeoForgeMain.NEOFORGE_MOD_ID)
public final class NeoForgeMain {
    public static final String NEOFORGE_MOD_ID = "travellers_handbook";

    public NeoForgeMain(IEventBus modEventBus) {
        TravellersHandbook.initialize();
        NeoForgeNetwork.initialize();
        modEventBus.addListener(NeoForgeNetwork::register);
        modEventBus.addListener(NeoForgeMain::addPackFinders);

        NeoForge.EVENT_BUS.addListener(NeoForgeMain::addReloadListener);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onServerStarted);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onServerTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onPlayerClone);
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        ResourceLocation packLocation = ResourceLocation.fromNamespaceAndPath(
                NEOFORGE_MOD_ID,
                "datapacks/default_entries"
        );
        event.addPackFinders(
                packLocation,
                PackType.SERVER_DATA,
                Component.translatable("pack.travellers-handbook.default_entries"),
                PackSource.create(UnaryOperator.identity(), false),
                false,
                Pack.Position.TOP
        );
        DefaultEntriesPackManager.initialize("mod/" + packLocation);
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new JournalDataLoader());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        DefaultEntriesPackManager.onServerStarted(event.getServer());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        DefaultEntriesPackManager.tick(event.getServer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JournalNetworkManager.syncToPlayer(player);
        }
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JournalNetworkManager.syncToPlayer(player);
        }
    }
}
