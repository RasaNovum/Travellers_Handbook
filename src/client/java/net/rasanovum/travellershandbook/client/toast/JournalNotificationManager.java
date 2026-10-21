package net.rasanovum.travellershandbook.client.toast;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class JournalNotificationManager {
    private static final List<PendingToast> PENDING_TOASTS = new ArrayList<>();

    public static void notifyNewEntry(String titleKey, String descKey, int delayTicks) {
        PENDING_TOASTS.add(new PendingToast(titleKey, descKey, Math.max(0, delayTicks)));
    }

    public static void tick() {
        for (int index = 0; index < PENDING_TOASTS.size(); index++) {
            PendingToast pendingToast = PENDING_TOASTS.get(index);
            int remainingTicks = pendingToast.remainingTicks() - 1;
            PENDING_TOASTS.set(index, new PendingToast(
                    pendingToast.titleKey(),
                    pendingToast.descKey(),
                    remainingTicks
            ));
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || hasToasts(client)) {
            return;
        }

        for (int index = 0; index < PENDING_TOASTS.size(); index++) {
            PendingToast pendingToast = PENDING_TOASTS.get(index);
            if (pendingToast.remainingTicks() <= 0) {
                PENDING_TOASTS.remove(index);
                showToast(pendingToast.titleKey(), pendingToast.descKey());
                return;
            }
        }
    }

    public static void clearPendingToasts() {
        PENDING_TOASTS.clear();
    }

    private static boolean hasToasts(Minecraft client) {
        return ((ToastComponentAccess) client.getToasts()).travellersHandbook$hasToasts();
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
