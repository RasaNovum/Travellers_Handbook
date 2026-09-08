package net.rasanovum.travellershandbook.loaders.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

//? if <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
*///?}

public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void registerReceiver() {
        //? if <1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(JournalNetworkManager.SYNC_PACKET_ID,
                (client, handler, buffer, responseSender) -> {
                    JournalNetworkManager.SyncData syncData = JournalNetworkManager.readSync(buffer);
                    TravellersHandbookClient.receiveEntries(
                            client,
                            syncData.entries(),
                            syncData.journalOpened());
                });
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(FabricSyncPayload.TYPE,
                (payload, context) -> TravellersHandbookClient.receiveEntries(
                        context.client(), payload.entries(), payload.journalOpened()));
        //?}
    }

    public static void sendJournalOpened() {
        //? if <1.21 {
        /*ClientPlayNetworking.send(JournalNetworkManager.JOURNAL_OPENED_PACKET_ID, PacketByteBufs.create());
        *///?} else {
        ClientPlayNetworking.send(new FabricJournalOpenedPayload());
        //?}
    }
}
