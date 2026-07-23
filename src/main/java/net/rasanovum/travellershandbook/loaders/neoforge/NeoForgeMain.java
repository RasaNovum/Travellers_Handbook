package net.rasanovum.travellershandbook.loaders.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import net.minecraft.server.level.ServerPlayer;

@Mod(NeoForgeMain.NEOFORGE_MOD_ID)
public final class NeoForgeMain {
    public static final String NEOFORGE_MOD_ID = "travellers_handbook";

    public NeoForgeMain(IEventBus modEventBus) {
        TravellersHandbook.initialize();
        NeoForgeNetwork.initialize();
        modEventBus.addListener(NeoForgeNetwork::register);

        NeoForge.EVENT_BUS.addListener(NeoForgeMain::addReloadListener);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onPlayerClone);
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new JournalDataLoader());
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
