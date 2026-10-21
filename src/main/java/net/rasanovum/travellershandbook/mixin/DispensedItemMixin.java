package net.rasanovum.travellershandbook.mixin;

import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.rasanovum.travellershandbook.progression.QuestItemEntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DispensedItemMixin {
    @ModifyArg(method = "spawnItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), index = 0)
    private static Entity travellersHandbook$excludeDispensedItem(Entity entity) {
        if (entity instanceof ItemEntity) {
            ((QuestItemEntityAccess) (Object) entity).setQuestEligibleCount(0);
        }
        return entity;
    }
}
