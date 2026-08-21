package net.rasanovum.travellershandbook.loaders.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void registerReceiver() {
        //? if <1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(JournalNetworkManager.SYNC_PACKET_ID,
                (client, handler, buffer, responseSender) ->
                        TravellersHandbookClient.receiveEntries(client, JournalNetworkManager.readSync(buffer)));
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(FabricSyncPayload.TYPE,
                (payload, context) -> TravellersHandbookClient.receiveEntries(context.client(), payload.entries()));
        //?}
    }
}
