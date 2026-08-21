package net.rasanovum.travellershandbook.loaders.neoforge;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;

@EventBusSubscriber(modid = NeoForgeMain.NEOFORGE_MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class NeoForgeClientGameEvents {
    private NeoForgeClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TravellersHandbookClient.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        DeltaTracker partialTick = event.getPartialTick();
        TravellersHandbookClient.renderHud(
                event.getGuiGraphics(),
                partialTick.getGameTimeDeltaPartialTick(false)
        );
    }
}
