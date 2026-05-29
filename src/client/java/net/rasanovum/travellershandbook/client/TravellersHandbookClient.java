package net.rasanovum.travellershandbook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.rasanovum.travellershandbook.client.screen.JournalScreen;
import net.rasanovum.travellershandbook.data.JournalEntry;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class TravellersHandbookClient implements ClientModInitializer {

	private static KeyMapping openJournalKey;
	public static KeyMapping getOpenJournalKey() {
		return openJournalKey;
	}
	public static final java.util.List<JournalEntry> CLIENT_ENTRIES_CACHE = new java.util.ArrayList<>();

	@Override
	public void onInitializeClient() {

		ClientPlayNetworking.registerGlobalReceiver(JournalNetworkManager.SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
			int size = buf.readInt();
			List<JournalEntry> tempEntries = new ArrayList<>();

			for (int i = 0; i < size; i++) {
				buf.readResourceLocation();
				String cat = buf.readUtf();
				String title = buf.readUtf();
				String desc = buf.readUtf();
				tempEntries.add(new JournalEntry(cat, title, desc));
			}

			client.execute(() -> {
				CLIENT_ENTRIES_CACHE.clear();
				CLIENT_ENTRIES_CACHE.addAll(tempEntries);
			});
		});

		openJournalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.traveller_handbook.open_journal",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				"key.categories.traveller_handbook"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				while (openJournalKey.consumeClick()) {
					Minecraft.getInstance().setScreen(new JournalScreen());
				}
			}
		});
	}
}