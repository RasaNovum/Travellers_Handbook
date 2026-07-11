package net.rasanovum.travellershandbook.client;

import com.mojang.blaze3d.platform.InputConstants;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.rasanovum.travellershandbook.client.hud.JournalTutorialOverlay;
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
	public static final java.util.Set<String> UNREAD_ENTRIES_SET = new java.util.HashSet<>();

	@Override
	public void onInitializeClient() {
		MidnightConfig.init("travellers-handbook", TravellersHandbookConfig.class);

		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(JournalTutorialOverlay::render);

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
					desc = Component.translatable("gui.travellershandbook.locked_description").getString();
				}

				tempEntries.add(new JournalEntry(cat, title, desc, adv));
			}

			client.execute(() -> {
				if (client.player != null && !CLIENT_ENTRIES_CACHE.isEmpty()) {
					String lockedDescMarker = Component.translatable("gui.travellershandbook.locked_description").getString();
					for (JournalEntry newEntry : tempEntries) {
						boolean isNewEntryLocked = newEntry.description().equals(lockedDescMarker);
						if (!isNewEntryLocked) {

							boolean wasPreviouslyLocked = CLIENT_ENTRIES_CACHE.stream()
									.anyMatch(cachedEntry -> cachedEntry.advancementRequirement().equals(newEntry.advancementRequirement())
											&& cachedEntry.description().equals(lockedDescMarker));

							if (wasPreviouslyLocked) {
								UNREAD_ENTRIES_SET.add(newEntry.advancementRequirement());
								JournalNotificationManager.notifyNewEntry(
										newEntry.title(),
										"gui.travellershandbook.toast.message"
								);
							}
						}
					}
				}

				CLIENT_ENTRIES_CACHE.clear();
				CLIENT_ENTRIES_CACHE.addAll(tempEntries);
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
				int currentItemCount = 0;
				for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
					ItemStack stack = client.player.getInventory().getItem(i);
					if (!stack.isEmpty()) {
						currentItemCount += stack.getCount();
					}
				}

				if (currentItemCount > JournalTutorialOverlay.lastInventoryCount) {
					JournalTutorialOverlay.lastPickupTime = client.player.tickCount;
				}

				while (openJournalKey.consumeClick()) {
					client.player.playSound(
							net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
							1.0F,
							1.0F
					);

					JournalTutorialOverlay.HAS_OPENED_JOURNAL = true;
					Minecraft.getInstance().setScreen(new JournalScreen());
				}
			}
		});
	}
}