package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: "your grip just slipped." No data — the client drops its climb grip so the fall it
 *  predicts matches the fall the server is already applying (no rubber-band on a slip). */
public record ClimbSlipPayload() implements CustomPacketPayload {
    public static final ClimbSlipPayload INSTANCE = new ClimbSlipPayload();
    public static final Type<ClimbSlipPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "climb_slip"));
    public static final StreamCodec<ByteBuf, ClimbSlipPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClimbSlipPayload> type() {
        return TYPE;
    }
}
