package net.rasanovum.travellershandbook.loaders.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;

@EventBusSubscriber(modid = NeoForgeMain.NEOFORGE_MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoForgeClientModEvents {
    private NeoForgeClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping key = TravellersHandbookClient.createOpenJournalKey();
        TravellersHandbookClient.setOpenJournalKey(key);
        event.register(key);
    }
}
