package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "I just pressed JUMP." Free-climbing is <em>deliberately initiated</em> — you
 *  grab a rock face by pressing jump while pushing into it, not automatically by brushing against it.
 *  Player movement is client-authoritative, so the jump-key edge is only visible client-side; this
 *  packet mirrors that intent to the server so its climb grant (which drives fall/slip/stamina) agrees
 *  with the client that actually moves you. No data — the server re-validates the wall itself. */
public record ClimbJumpPayload() implements CustomPacketPayload {
    public static final ClimbJumpPayload INSTANCE = new ClimbJumpPayload();
    public static final Type<ClimbJumpPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "climb_jump"));
    public static final StreamCodec<ByteBuf, ClimbJumpPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClimbJumpPayload> type() {
        return TYPE;
    }
}
