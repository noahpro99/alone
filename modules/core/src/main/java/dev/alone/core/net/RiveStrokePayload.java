package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "one blow of the froe/hatchet, splitting the log." Many, and slow, make boards. */
public record RiveStrokePayload() implements CustomPacketPayload {
    public static final RiveStrokePayload INSTANCE = new RiveStrokePayload();
    public static final Type<RiveStrokePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "rive_stroke"));
    public static final StreamCodec<ByteBuf, RiveStrokePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<RiveStrokePayload> type() {
        return TYPE;
    }
}
