package net.rasanovum.travellershandbook.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.rasanovum.travellershandbook.progression.JournalProgressManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerQuestStatMixin {
    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("HEAD"))
    private void travellersHandbook$beforeAwardStat(Stat<?> stat, int amount, CallbackInfo callbackInfo) {
        JournalProgressManager.onStatAwarded((ServerPlayer) (Object) this, stat);
    }

    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("TAIL"))
    private void travellersHandbook$afterAwardStat(Stat<?> stat, int amount, CallbackInfo callbackInfo) {
        JournalProgressManager.onStatAwarded((ServerPlayer) (Object) this, stat);
    }
}
