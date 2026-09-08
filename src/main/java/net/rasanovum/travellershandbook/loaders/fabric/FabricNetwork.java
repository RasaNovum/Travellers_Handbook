package net.rasanovum.travellershandbook.loaders.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

//? if <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
*///?} else {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}

public final class FabricNetwork {
    private static boolean initialized;

    private FabricNetwork() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        //? if <1.21 {
        /*JournalNetworkManager.initialize((player, entries, journalOpened) -> {
            var buffer = PacketByteBufs.create();
            JournalNetworkManager.writeSync(buffer, entries, journalOpened);
            ServerPlayNetworking.send(player, JournalNetworkManager.SYNC_PACKET_ID, buffer);
        });
        ServerPlayNetworking.registerGlobalReceiver(
                JournalNetworkManager.JOURNAL_OPENED_PACKET_ID,
                (server, player, handler, buffer, responseSender) ->
                        server.execute(() -> JournalNetworkManager.markJournalOpened(player))
        );
        *///?} else {
        PayloadTypeRegistry.playS2C().register(FabricSyncPayload.TYPE, FabricSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(
                FabricJournalOpenedPayload.TYPE,
                FabricJournalOpenedPayload.STREAM_CODEC
        );
        JournalNetworkManager.initialize((player, entries, journalOpened) ->
                ServerPlayNetworking.send(player, new FabricSyncPayload(entries, journalOpened)));
        ServerPlayNetworking.registerGlobalReceiver(
                FabricJournalOpenedPayload.TYPE,
                (payload, context) -> JournalNetworkManager.markJournalOpened(context.player())
        );
        //?}

        initialized = true;
    }

}
