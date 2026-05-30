package net.rasanovum.travellershandbook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
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
				boolean isUnlocked = buf.readBoolean();
				String title = buf.readUtf();
				String desc = buf.readUtf();
				String adv = buf.readUtf();

				if (!isUnlocked) {
					title = "???";
					desc = Component.translatable("gui.travellershandbook.locked_description").getString();
				}

				tempEntries.add(new JournalEntry(cat, title, desc, adv));
			}

			client.execute(() -> {
				CLIENT_ENTRIES_CACHE.clear();
				CLIENT_ENTRIES_CACHE.addAll(tempEntries);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(JournalNetworkManager.UNLOCK_NOTIFY_PACKET_ID, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					client.player.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F);

					client.getToasts().addToast(new SystemToast(
							SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
							Component.translatable("gui.travellershandbook.toast.title").withStyle(ChatFormatting.GOLD),
							Component.translatable("gui.travellershandbook.toast.message")
					));
				}
			});
		});

		openJournalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.travellers-handbook.open_journal",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				"key.categories.travellers-handbook"
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