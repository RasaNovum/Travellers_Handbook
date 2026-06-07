package net.rasanovum.travellershandbook.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.client.screen.JournalScreen;

public class JournalTutorialOverlay {
    public static boolean HAS_OPENED_JOURNAL = false;

    private static final ItemStack ICON = new ItemStack(Items.WRITABLE_BOOK);

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.options.hideGui || HAS_OPENED_JOURNAL) return;
        if (mc.screen != null) {
            if (mc.screen instanceof JournalScreen) {
                HAS_OPENED_JOURNAL = true;
            }
            return;
        }

        // sin for wave fade effect
        float totalTicks = mc.player.tickCount + partialTick;
        float alpha = (float) (Math.sin(totalTicks * 0.12f) * 0.425f) + 0.575f;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int xPos = screenWidth / 2 - 8;
        int yPos = screenHeight - 52;

        Font font = mc.font;
        String keyName = "N"; // fallback default
        if (TravellersHandbookClient.getOpenJournalKey() != null) {
            keyName = TravellersHandbookClient.getOpenJournalKey().getTranslatedKeyMessage().getString().toUpperCase();
        }
        Component promptText = Component.literal("Press [" + keyName + "]");
        int textWidth = font.width(promptText);
        int textX = (screenWidth / 2) - (textWidth / 2);
        int textY = yPos - 10;

        // draw HUD element and apply alpha
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        guiGraphics.renderFakeItem(ICON, xPos, yPos);

        // text prompt with matching opacity levels
        int textAlphaColor = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        guiGraphics.drawString(font, promptText, textX, textY, textAlphaColor, true);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
