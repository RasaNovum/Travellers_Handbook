package net.rasanovum.travellershandbook.client.toast;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class JournalToast implements Toast {
    private static final ResourceLocation TEXTURE =
            //? if <1.21 {
            /*new ResourceLocation("textures/gui/toasts.png");
            *///?} else {
            ResourceLocation.withDefaultNamespace("toast/advancement");
            //?}
    private final Component title;
    private final Component description;
    private final ItemStack icon;
    private final int calculatedWidth;

    public JournalToast(Component title, Component description, Font font) {
        this.title = title;
        this.description = description;
        this.icon = new ItemStack(Items.WRITABLE_BOOK);

        int titleWidth = font.width(title);
        int descWidth = font.width(description);
        int requiredTextWidth = Math.max(titleWidth, descWidth) + 40;

        this.calculatedWidth = Math.max(160, requiredTextWidth);
    }

    @Override
    public int width() {
        return this.calculatedWidth;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceShow) {
        //? if <1.21 {
        /*int leftBorder = 4;
        int rightBorder = 4;
        int vanillaWidth = 160;
        int toastHeight = this.height();

        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, leftBorder, toastHeight);

        int middleWidth = this.width() - leftBorder - rightBorder;
        guiGraphics.blit(TEXTURE, leftBorder, 0, middleWidth, toastHeight, leftBorder, 0, vanillaWidth - leftBorder - rightBorder, toastHeight, 256, 256);

        guiGraphics.blit(TEXTURE, this.width() - rightBorder, 0, vanillaWidth - rightBorder, 0, rightBorder, toastHeight);
        *///?} else {
        guiGraphics.blitSprite(TEXTURE, 0, 0, this.width(), this.height());
        //?}

        guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, 0xFF55FF55, false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.description, 30, 18, 0xFFFFFFFF, false);

        guiGraphics.renderFakeItem(this.icon, 8, 8);

        return timeSinceShow >= 15000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
