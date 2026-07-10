package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "open my backpack" (the keybind). No data — the server finds the pack. */
public record BackpackOpenPayload() implements CustomPacketPayload {
    public static final BackpackOpenPayload INSTANCE = new BackpackOpenPayload();
    public static final Type<BackpackOpenPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "open_backpack"));
    public static final StreamCodec<ByteBuf, BackpackOpenPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<BackpackOpenPayload> type() {
        return TYPE;
    }
}
