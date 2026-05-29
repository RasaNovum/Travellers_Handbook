package net.rasanovum.travellershandbook.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.travellershandbook.data.JournalDataLoader;

public class JournalNetworkManager {
    public static final ResourceLocation SYNC_PACKET_ID = new ResourceLocation("travellers-handbook", "sync_entries");
    public static final ResourceLocation UNLOCK_NOTIFY_PACKET_ID = new ResourceLocation("travellers-handbook", "unlock_notify");

    public static void registerServerPackets() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            syncToPlayer(handler.player);
        });
    }

    public static void syncToPlayer(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(JournalDataLoader.SERVER_ENTRIES.size());

        JournalDataLoader.SERVER_ENTRIES.forEach((id, entry) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(entry.getCategory());

            String req = entry.getAdvancementRequirement();
            boolean isUnlocked = req == null || req.isEmpty() || hasAdvancement(player, req);

            buf.writeBoolean(isUnlocked);
            buf.writeUtf(entry.getTitle());
            buf.writeUtf(entry.getDescription());
            buf.writeUtf(req != null ? req : "");
        });

        ServerPlayNetworking.send(player, SYNC_PACKET_ID, buf);
    }

    private static boolean hasAdvancement(ServerPlayer player, String advancementId) {
        if (player.getServer() == null) return false;

        Advancement adv = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(advancementId));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }
}
