package net.rasanovum.travellershandbook.client.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class JournalToast implements Toast {
    private final Component title;
    private final Component description;
    private final ItemStack icon;

    public JournalToast(Component title, Component description) {
        this.title = title;
        this.description = description;
        this.icon = new ItemStack(Items.WRITABLE_BOOK);
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceShow) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());

        guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 30, 7, 0xFF55FF55, false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, this.description, 30, 18, 0xFFFFFFFF, false);

        guiGraphics.renderFakeItem(this.icon, 8, 8);

        return timeSinceShow >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
