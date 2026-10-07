package net.rasanovum.travellershandbook.loaders.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

public final class NeoForgeNetwork {
    private static boolean initialized;

    private NeoForgeNetwork() {
    }

    public static void initialize() {
        if (!initialized) {
            JournalNetworkManager.initialize((player, entries, journalOpened) ->
                    PacketDistributor.sendToPlayer(player, new NeoForgeSyncPayload(entries, journalOpened)));
            initialized = true;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToClient(
                NeoForgeSyncPayload.TYPE,
                NeoForgeSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        TravellersHandbookClient.receiveEntries(
                                Minecraft.getInstance(),
                                payload.entries(),
                                payload.journalOpened()))
        );
        event.registrar("3").playToServer(
                NeoForgeJournalOpenedPayload.TYPE,
                NeoForgeJournalOpenedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        JournalNetworkManager.markJournalOpened(
                                (net.minecraft.server.level.ServerPlayer) context.player()))
        );
    }
}
