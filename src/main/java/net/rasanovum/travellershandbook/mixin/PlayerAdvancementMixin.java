package net.rasanovum.travellershandbook.mixin;

import net.minecraft.server.PlayerAdvancements;
import net.rasanovum.travellershandbook.progression.JournalProgressManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <1.21 {
/*import net.minecraft.advancements.Advancement;
*///?} else {
import net.minecraft.advancements.AdvancementHolder;
//?}

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementMixin {
    @Shadow
    private net.minecraft.server.level.ServerPlayer player;

    //? if <1.21 {
    /*@Inject(method = "award", at = @At("TAIL"))
    private void onAdvancementAwarded(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String advancementId = advancement.getId().toString();
    *///?} else {
    @Inject(method = "award", at = @At("TAIL"))
    private void onAdvancementAwarded(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String advancementId = advancement.id().toString();
    //?}
        JournalProgressManager.refresh(this.player, advancementId, cir.getReturnValue());
    }

    //? if <1.21 {
    /*@Inject(method = "revoke", at = @At("TAIL"))
    private void onAdvancementRevoked(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String advancementId = advancement.getId().toString();
    *///?} else {
    @Inject(method = "revoke", at = @At("TAIL"))
    private void onAdvancementRevoked(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String advancementId = advancement.id().toString();
    //?}
        JournalProgressManager.refresh(this.player, advancementId, cir.getReturnValue());
    }
}
