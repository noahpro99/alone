package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "one strike of the hammerstone against the flint." Enough strikes flake a shard. */
public record KnapStrikePayload() implements CustomPacketPayload {
    public static final KnapStrikePayload INSTANCE = new KnapStrikePayload();
    public static final Type<KnapStrikePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "knap_strike"));
    public static final StreamCodec<ByteBuf, KnapStrikePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<KnapStrikePayload> type() {
        return TYPE;
    }
}
