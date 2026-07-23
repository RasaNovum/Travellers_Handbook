package net.rasanovum.travellershandbook.loaders.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;

public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TravellersHandbookClient.initialize();
        TravellersHandbookClient.setOpenJournalKey(
                KeyBindingHelper.registerKeyBinding(TravellersHandbookClient.createOpenJournalKey()));

        FabricClientNetwork.registerReceiver();
        ClientTickEvents.END_CLIENT_TICK.register(TravellersHandbookClient::tick);
        //? if <1.21 {
        /*HudRenderCallback.EVENT.register(TravellersHandbookClient::renderHud);
        *///?} else {
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) ->
                TravellersHandbookClient.renderHud(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        //?}
    }
}
