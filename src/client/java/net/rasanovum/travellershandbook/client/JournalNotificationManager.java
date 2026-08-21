package net.rasanovum.travellershandbook.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.rasanovum.travellershandbook.client.toast.JournalToast;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class JournalNotificationManager {
    private static final List<PendingToast> PENDING_TOASTS = new ArrayList<>();

    public static void notifyNewEntry(String titleKey, String descKey) {
        notifyNewEntry(titleKey, descKey, 0);
    }

    public static void notifyNewEntry(String titleKey, String descKey, int delayTicks) {
        if (delayTicks > 0) {
            PENDING_TOASTS.add(new PendingToast(titleKey, descKey, delayTicks));
            return;
        }

        showToast(titleKey, descKey);
    }

    public static void tick() {
        ListIterator<PendingToast> iterator = PENDING_TOASTS.listIterator();
        while (iterator.hasNext()) {
            PendingToast pendingToast = iterator.next();
            int remainingTicks = pendingToast.remainingTicks() - 1;
            if (remainingTicks <= 0) {
                showToast(pendingToast.titleKey(), pendingToast.descKey());
                iterator.remove();
            } else {
                iterator.set(new PendingToast(
                        pendingToast.titleKey(),
                        pendingToast.descKey(),
                        remainingTicks
                ));
            }
        }
    }

    public static void clearPendingToasts() {
        PENDING_TOASTS.clear();
    }

    private static void showToast(String titleKey, String descKey) {
        Minecraft client = Minecraft.getInstance();

        if (client == null) return;

        LocalPlayer player = client.player;
        if (player != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                    1.0F,
                    1.0F
            ));
        }

        client.getToasts().addToast(new JournalToast(Component.translatable(titleKey).withStyle(ChatFormatting.GOLD),Component.translatable(descKey), client.font));
    }

    private record PendingToast(String titleKey, String descKey, int remainingTicks) {
    }
}
