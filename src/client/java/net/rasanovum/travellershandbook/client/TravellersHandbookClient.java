package net.rasanovum.travellershandbook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.rasanovum.travellershandbook.TravellersHandbookConfig;
import net.rasanovum.travellershandbook.client.toast.JournalNotificationManager;
import net.rasanovum.travellershandbook.data.JournalEntry;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import net.rasanovum.travellershandbook.client.hud.JournalTutorialOverlay;
import net.rasanovum.travellershandbook.client.screen.JournalScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TravellersHandbookClient {
    private static KeyMapping openJournalKey;
    private static Runnable journalOpenedSender = () -> {
    };

    public static final List<JournalEntry> CLIENT_ENTRIES_CACHE = new ArrayList<>();
    public static final java.util.Set<String> UNREAD_ENTRIES_SET = new HashSet<>();
    private static final java.util.Set<String> DISPLAYED_ADVANCEMENTS = new HashSet<>();

    private TravellersHandbookClient() {
    }

    public static KeyMapping createOpenJournalKey() {
        return new KeyMapping(
                "key.traveller_handbook.open_journal",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.traveller_handbook"
        );
    }

    public static void setOpenJournalKey(KeyMapping key) {
        openJournalKey = key;
    }

    public static void setJournalOpenedSender(Runnable sender) {
        journalOpenedSender = sender;
    }

    public static KeyMapping getOpenJournalKey() {
        return openJournalKey;
    }

    public static void receiveEntries(
            Minecraft client,
            List<JournalNetworkManager.SyncEntry> entries,
            boolean journalOpened
    ) {
        String lockedDescription = Component.translatable("gui.travellershandbook.locked_description").getString();
        List<JournalEntry> tempEntries = new ArrayList<>(entries.size());
        java.util.Set<String> displayedAdvancements = new HashSet<>();

        for (JournalNetworkManager.SyncEntry entry : entries) {
            String description = entry.unlocked() ? entry.description() : lockedDescription;
            tempEntries.add(new JournalEntry(entry.category(), entry.title(), description, entry.advancementRequirement()));
            if (entry.advancementHasDisplay()) {
                displayedAdvancements.add(entry.advancementRequirement());
            }
        }

        client.execute(() -> {
            DISPLAYED_ADVANCEMENTS.clear();
            DISPLAYED_ADVANCEMENTS.addAll(displayedAdvancements);
            JournalTutorialOverlay.setJournalOpened(journalOpened);
            applyEntries(tempEntries);
        });
    }

    public static void applyEntries(List<JournalEntry> entries) {
        String lockedDescription = Component.translatable("gui.travellershandbook.locked_description").getString();

        if (!CLIENT_ENTRIES_CACHE.isEmpty()) {
            for (JournalEntry newEntry : entries) {
                if (!newEntry.description().equals(lockedDescription)) {
                    boolean wasPreviouslyLocked = CLIENT_ENTRIES_CACHE.stream()
                            .anyMatch(cachedEntry -> cachedEntry.advancementRequirement().equals(newEntry.advancementRequirement())
                                    && cachedEntry.description().equals(lockedDescription));

                    if (wasPreviouslyLocked) {
                        UNREAD_ENTRIES_SET.add(newEntry.advancementRequirement());
                        JournalNotificationManager.notifyNewEntry(
                                newEntry.title(),
                                "gui.travellershandbook.toast.message",
                                DISPLAYED_ADVANCEMENTS.contains(newEntry.advancementRequirement())
                                        ? Math.max(0, TravellersHandbookConfig.entryToastDelayTicks)
                                        : 0
                        );
                    }
                }
            }
        }

        CLIENT_ENTRIES_CACHE.clear();
        CLIENT_ENTRIES_CACHE.addAll(entries);
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            JournalNotificationManager.clearPendingToasts();
            JournalTutorialOverlay.resetForWorld();
            return;
        }

        JournalNotificationManager.tick();

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
        JournalTutorialOverlay.lastInventoryCount = currentItemCount;

        while (openJournalKey != null && openJournalKey.consumeClick()) {
            client.player.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            JournalTutorialOverlay.markJournalOpened();
            journalOpenedSender.run();
            client.setScreen(new JournalScreen());
        }
    }

    public static void renderHud(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick) {
        JournalTutorialOverlay.render(guiGraphics, partialTick);
    }
}
