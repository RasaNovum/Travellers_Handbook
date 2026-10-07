package net.rasanovum.travellershandbook.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.rasanovum.travellershandbook.TravellersHandbookConfig;
import net.rasanovum.travellershandbook.client.toast.JournalNotificationManager;
import net.rasanovum.travellershandbook.data.JournalRequirement;
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

    public static final List<JournalNetworkManager.SyncEntry> CLIENT_ENTRIES_CACHE = new ArrayList<>();
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
        List<JournalNetworkManager.SyncEntry> receivedEntries = new ArrayList<>(entries);
        java.util.Set<String> displayedAdvancements = new HashSet<>();

        for (JournalNetworkManager.SyncEntry entry : receivedEntries) {
            String advancement = entry.advancementRequirement();
            if (entry.advancementHasDisplay() && !advancement.isEmpty()) {
                displayedAdvancements.add(advancement);
            }
        }

        client.execute(() -> {
            DISPLAYED_ADVANCEMENTS.clear();
            DISPLAYED_ADVANCEMENTS.addAll(displayedAdvancements);
            JournalTutorialOverlay.setJournalOpened(journalOpened);
            applyEntries(receivedEntries);
        });
    }

    public static void applyEntries(List<JournalNetworkManager.SyncEntry> entries) {
        if (!CLIENT_ENTRIES_CACHE.isEmpty()) {
            for (JournalNetworkManager.SyncEntry newEntry : entries) {
                JournalNetworkManager.SyncEntry oldEntry = CLIENT_ENTRIES_CACHE.stream()
                        .filter(cachedEntry -> cachedEntry.id().equals(newEntry.id()))
                        .findFirst()
                        .orElse(null);
                if (oldEntry == null) {
                    continue;
                }

                boolean goalCompleted = !oldEntry.completed() && newEntry.completed() && newEntry.hasGoals();
                boolean lockUnlocked = !oldEntry.unlocked() && newEntry.unlocked() && hasLockRequirement(newEntry);
                if (goalCompleted || lockUnlocked) {
                    UNREAD_ENTRIES_SET.add(newEntry.id());
                    JournalNotificationManager.notifyNewEntry(
                            newEntry.title(),
                            goalCompleted ? "gui.travellershandbook.toast.goal_message" : "gui.travellershandbook.toast.message",
                            notificationDelay(newEntry)
                    );
                }
            }
        }

        CLIENT_ENTRIES_CACHE.clear();
        CLIENT_ENTRIES_CACHE.addAll(entries);
        java.util.Set<String> currentEntryIds = new HashSet<>();
        for (JournalNetworkManager.SyncEntry entry : entries) {
            currentEntryIds.add(entry.id());
        }
        UNREAD_ENTRIES_SET.retainAll(currentEntryIds);
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof JournalScreen journalScreen) {
            journalScreen.refreshEntries();
        }
    }

    public static void clearClientState() {
        CLIENT_ENTRIES_CACHE.clear();
        UNREAD_ENTRIES_SET.clear();
        DISPLAYED_ADVANCEMENTS.clear();
        JournalNotificationManager.clearPendingToasts();
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof JournalScreen journalScreen) {
            journalScreen.refreshEntries();
        }
    }

    private static boolean hasLockRequirement(JournalNetworkManager.SyncEntry entry) {
        return entry.requirements().stream().anyMatch(progress ->
                progress.requirement().mode() == JournalRequirement.Mode.LOCK);
    }

    private static int notificationDelay(JournalNetworkManager.SyncEntry entry) {
        return entry.advancementHasDisplay() && DISPLAYED_ADVANCEMENTS.contains(entry.advancementRequirement())
                ? Math.max(0, TravellersHandbookConfig.entryToastDelayTicks)
                : 0;
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
