package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "one stroke of the fire drill." No data — the server re-validates the spot. */
public record FireDrillPayload() implements CustomPacketPayload {
    public static final FireDrillPayload INSTANCE = new FireDrillPayload();
    public static final Type<FireDrillPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "fire_drill"));
    public static final StreamCodec<ByteBuf, FireDrillPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<FireDrillPayload> type() {
        return TYPE;
    }
}
