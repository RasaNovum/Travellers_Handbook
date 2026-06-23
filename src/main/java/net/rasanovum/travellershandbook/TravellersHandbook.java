package net.rasanovum.travellershandbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TravellersHandbook implements ModInitializer {

	public static final String MOD_ID = "travellers-handbook";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		JournalNetworkManager.registerServerPackets();
		ResourceManagerHelper.get(PackType.SERVER_DATA)
				.registerReloadListener(new JournalDataLoader());

		// advancement listening
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			JournalNetworkManager.syncToPlayer(handler.player);
		});

		// advancement given to any player
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			JournalNetworkManager.syncToPlayer(newPlayer);
		});

		LOGGER.info("I used to be a traveller like you, until I took a handbook to the knee.");
	}
}