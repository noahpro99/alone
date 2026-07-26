package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: the authoritative progress (0..1) and ready-flag for the craft the player is currently
 * working. The client drives the result-slot progress bar and the take-gate from THIS, not its own copy of
 * the timer — so "bar full" is exactly "the server will let you take it," with no client/server drift that
 * made a normal-click take snap back.
 */
public record CraftProgressPayload(float progress, boolean ready) implements CustomPacketPayload {
    public static final Type<CraftProgressPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "craft_progress"));

    public static final StreamCodec<ByteBuf, CraftProgressPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, CraftProgressPayload::progress,
        ByteBufCodecs.BOOL, CraftProgressPayload::ready,
        CraftProgressPayload::new);

    @Override
    public Type<CraftProgressPayload> type() {
        return TYPE;
    }
}
