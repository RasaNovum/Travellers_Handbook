package net.rasanovum.travellershandbook.mixin;

import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.rasanovum.travellershandbook.client.toast.ToastComponentAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;
import java.util.List;

@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin implements ToastComponentAccess {
    @Shadow
    @Final
    private List<?> visible;

    @Shadow
    @Final
    private Deque<?> queued;

    @Override
    public boolean travellersHandbook$hasToasts() {
        return !this.visible.isEmpty() || !this.queued.isEmpty();
    }
}
