package net.rasanovum.travellershandbook.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.rasanovum.travellershandbook.client.toast.JournalToast;

public class JournalNotificationManager {

    public static void notifyNewEntry(String titleKey, String descKey) {
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

        client.getToasts().addToast(new JournalToast(
                Component.translatable(titleKey).withStyle(ChatFormatting.GOLD),
                Component.translatable(descKey)
        ));
    }
}
