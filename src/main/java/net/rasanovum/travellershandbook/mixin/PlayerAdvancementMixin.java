package net.rasanovum.travellershandbook.mixin;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.travellershandbook.data.JournalDataLoader;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("TAIL"))
    private void onAdvancementAwarded(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        String earnedId = advancement.getId().toString();

        boolean unlockedNewNote = JournalDataLoader.SERVER_ENTRIES.values().stream()
                .anyMatch(entry -> entry.getAdvancementRequirement().equals(earnedId));

        if (unlockedNewNote) {
            JournalNetworkManager.syncToPlayer(this.player);
            ServerPlayNetworking.send(this.player, JournalNetworkManager.UNLOCK_NOTIFY_PACKET_ID, PacketByteBufs.create());
        }
    }
}
