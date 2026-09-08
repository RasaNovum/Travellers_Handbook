package net.rasanovum.travellershandbook.loaders.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rasanovum.travellershandbook.TravellersHandbook;

public record NeoForgeJournalOpenedPayload() implements CustomPacketPayload {
    public static final Type<NeoForgeJournalOpenedPayload> TYPE =
            new Type<>(TravellersHandbook.id("journal_opened"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeJournalOpenedPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                    },
                    buffer -> new NeoForgeJournalOpenedPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
