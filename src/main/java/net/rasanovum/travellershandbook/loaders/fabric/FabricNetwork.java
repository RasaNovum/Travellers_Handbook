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
        /*JournalNetworkManager.initialize((player, entries) -> {
            var buffer = PacketByteBufs.create();
            JournalNetworkManager.writeSync(buffer, entries);
            ServerPlayNetworking.send(player, JournalNetworkManager.SYNC_PACKET_ID, buffer);
        });
        *///?} else {
        PayloadTypeRegistry.playS2C().register(FabricSyncPayload.TYPE, FabricSyncPayload.STREAM_CODEC);
        JournalNetworkManager.initialize((player, entries) ->
                ServerPlayNetworking.send(player, new FabricSyncPayload(entries)));
        //?}

        initialized = true;
    }

}
