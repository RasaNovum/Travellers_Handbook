package net.rasanovum.travellershandbook.loaders.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

import java.util.List;

public record NeoForgeSyncPayload(List<JournalNetworkManager.SyncEntry> entries) implements CustomPacketPayload {
    public static final Type<NeoForgeSyncPayload> TYPE = new Type<>(TravellersHandbook.id("sync_entries"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> JournalNetworkManager.writeSync(buffer, payload.entries()),
                    buffer -> new NeoForgeSyncPayload(JournalNetworkManager.readSync(buffer))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
