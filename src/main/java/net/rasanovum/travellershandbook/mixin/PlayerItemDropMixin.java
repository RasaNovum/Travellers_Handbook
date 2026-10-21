package net.rasanovum.travellershandbook.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rasanovum.travellershandbook.progression.QuestItemEntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerItemDropMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void travellersHandbook$markPlayerDrop(ItemStack stack, boolean throwRandomly,
                                                    boolean retainOwnership,
                                                    CallbackInfoReturnable<ItemEntity> callbackInfo) {
        ItemEntity entity = callbackInfo.getReturnValue();
        if (entity != null) {
            ((QuestItemEntityAccess) (Object) entity).markPlayerDropped();
        }
    }
}
