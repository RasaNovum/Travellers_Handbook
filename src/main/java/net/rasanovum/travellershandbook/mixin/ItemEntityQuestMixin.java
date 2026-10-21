package net.rasanovum.travellershandbook.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rasanovum.travellershandbook.progression.JournalProgressManager;
import net.rasanovum.travellershandbook.progression.QuestItemEntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityQuestMixin implements QuestItemEntityAccess {
    @Unique
    private static final String travellersHandbook$questEligibleTag = "TravellersHandbookQuestEligible";

    @Unique
    private int travellersHandbook$questEligibleCount = -1;

    @Unique
    private Item travellersHandbook$pickupItem;

    @Unique
    private int travellersHandbook$pickupEligibleCount;

    @Unique
    private int travellersHandbook$pickupInitialCount;

    @Unique
    private boolean travellersHandbook$recordPickup;

    @Override
    public int getQuestEligibleCount() {
        ItemEntity entity = (ItemEntity) (Object) this;
        int currentCount = entity.getItem().isEmpty() ? 0 : entity.getItem().getCount();
        int eligibleCount = this.travellersHandbook$questEligibleCount;
        if (eligibleCount < 0) {
            return currentCount;
        }
        return Math.min(currentCount, eligibleCount);
    }

    @Override
    public void setQuestEligibleCount(int count) {
        this.travellersHandbook$questEligibleCount = Math.max(0, count);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void travellersHandbook$beginItemPickup(Player player, CallbackInfo callbackInfo) {
        this.travellersHandbook$recordPickup = false;
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = ((ItemEntity) (Object) this).getItem();
            this.travellersHandbook$pickupItem = stack.getItem();
            this.travellersHandbook$pickupInitialCount = stack.getCount();
            this.travellersHandbook$pickupEligibleCount = getQuestEligibleCount();
            JournalProgressManager.beginItemPickup(serverPlayer, this.travellersHandbook$pickupItem);
        }
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z",
            shift = At.Shift.AFTER))
    private void travellersHandbook$recordPickupAfterInventory(Player player, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer) {
            ItemEntity entity = (ItemEntity) (Object) this;
            int remainingCount = entity.getItem().isEmpty() ? 0 : entity.getItem().getCount();
            int pickedUpCount = Math.max(0, this.travellersHandbook$pickupInitialCount - remainingCount);
            int eligiblePickedUp = Math.min(this.travellersHandbook$pickupEligibleCount, pickedUpCount);
            setQuestEligibleCount(this.travellersHandbook$pickupEligibleCount - eligiblePickedUp);
            this.travellersHandbook$pickupEligibleCount = eligiblePickedUp;
            this.travellersHandbook$recordPickup = true;
        }
    }

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void travellersHandbook$finishItemPickup(Player player, CallbackInfo callbackInfo) {
        if (this.travellersHandbook$recordPickup && player instanceof ServerPlayer serverPlayer) {
            JournalProgressManager.recordItemPickup(serverPlayer, this.travellersHandbook$pickupItem,
                    this.travellersHandbook$pickupEligibleCount);
        }
        this.travellersHandbook$recordPickup = false;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void travellersHandbook$saveQuestEligibleCount(CompoundTag tag, CallbackInfo callbackInfo) {
        int count = this.travellersHandbook$questEligibleCount;
        if (count < 0) {
            count = ((ItemEntity) (Object) this).getItem().getCount();
        }
        tag.putInt(travellersHandbook$questEligibleTag, Math.max(0, count));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void travellersHandbook$loadQuestEligibleCount(CompoundTag tag, CallbackInfo callbackInfo) {
        this.travellersHandbook$questEligibleCount = tag.contains(travellersHandbook$questEligibleTag, Tag.TAG_INT)
                ? Math.max(0, tag.getInt(travellersHandbook$questEligibleTag)) : -1;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void travellersHandbook$copyQuestEligibleCount(CallbackInfoReturnable<ItemEntity> callbackInfo) {
        ItemEntity copy = callbackInfo.getReturnValue();
        if (copy != null) {
            ((QuestItemEntityAccess) (Object) copy).setQuestEligibleCount(getQuestEligibleCount());
        }
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private static void travellersHandbook$prepareMerge(ItemEntity destination, ItemStack destinationStack,
                                                         ItemEntity source, ItemStack sourceStack,
                                                         CallbackInfo callbackInfo) {
        int capacity = Math.min(destinationStack.getMaxStackSize(), 64) - destinationStack.getCount();
        int transferred = Math.min(capacity, sourceStack.getCount());
        QuestItemEntityAccess destinationAccess = (QuestItemEntityAccess) (Object) destination;
        QuestItemEntityAccess sourceAccess = (QuestItemEntityAccess) (Object) source;
        int destinationEligible = destinationAccess.getQuestEligibleCount();
        int sourceEligible = sourceAccess.getQuestEligibleCount();
        if (transferred >= 0) {
            int eligibleTransferred = Math.min(sourceEligible, transferred);
            destinationAccess.setQuestEligibleCount(destinationEligible + eligibleTransferred);
            sourceAccess.setQuestEligibleCount(sourceEligible - eligibleTransferred);
        } else {
            int eligibleTransferred = Math.min(destinationEligible, -transferred);
            destinationAccess.setQuestEligibleCount(destinationEligible - eligibleTransferred);
            sourceAccess.setQuestEligibleCount(sourceEligible + eligibleTransferred);
        }
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL"))
    private static void travellersHandbook$validateMerge(ItemEntity destination, ItemStack destinationStack,
                                                          ItemEntity source, ItemStack sourceStack,
                                                          CallbackInfo callbackInfo) {
        QuestItemEntityAccess destinationAccess = (QuestItemEntityAccess) (Object) destination;
        QuestItemEntityAccess sourceAccess = (QuestItemEntityAccess) (Object) source;
        destinationAccess.setQuestEligibleCount(destinationAccess.getQuestEligibleCount());
        sourceAccess.setQuestEligibleCount(sourceAccess.getQuestEligibleCount());
    }
}
