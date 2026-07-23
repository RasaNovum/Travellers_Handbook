package net.rasanovum.travellershandbook.mixin;

import net.minecraft.server.PlayerAdvancements;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
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
        String earnedId = advancement.getId().toString();
    *///?} else {
    @Inject(method = "award", at = @At("TAIL"))
    private void onAdvancementAwarded(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String earnedId = advancement.id().toString();
    //?}
        boolean unlockedNewNote = JournalDataLoader.SERVER_ENTRIES.values().stream()
                .anyMatch(entry -> entry.advancementRequirement().equals(earnedId));

        if (unlockedNewNote) {
            JournalNetworkManager.syncToPlayer(this.player);
        }
    }
}
