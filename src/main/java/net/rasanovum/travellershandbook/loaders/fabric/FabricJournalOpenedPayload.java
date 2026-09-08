/*? if >=1.21 {*/
package net.rasanovum.travellershandbook.loaders.fabric;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rasanovum.travellershandbook.TravellersHandbook;

public record FabricJournalOpenedPayload() implements CustomPacketPayload {
    public static final Type<FabricJournalOpenedPayload> TYPE =
            new Type<>(TravellersHandbook.id("journal_opened"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FabricJournalOpenedPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                    },
                    buffer -> new FabricJournalOpenedPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
/*?}*/
