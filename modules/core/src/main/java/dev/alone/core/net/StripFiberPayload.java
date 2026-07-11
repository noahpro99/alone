package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "one tug stripping fibre from the plant you're looking at." Enough tugs free the fibre. */
public record StripFiberPayload() implements CustomPacketPayload {
    public static final StripFiberPayload INSTANCE = new StripFiberPayload();
    public static final Type<StripFiberPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "strip_fiber"));
    public static final StreamCodec<ByteBuf, StripFiberPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<StripFiberPayload> type() {
        return TYPE;
    }
}
