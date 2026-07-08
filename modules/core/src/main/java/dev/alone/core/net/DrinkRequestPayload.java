package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: "I'm cupping water to drink." No data — the server re-validates the water. */
public record DrinkRequestPayload() implements CustomPacketPayload {
    public static final DrinkRequestPayload INSTANCE = new DrinkRequestPayload();
    public static final Type<DrinkRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "drink"));
    public static final StreamCodec<ByteBuf, DrinkRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<DrinkRequestPayload> type() {
        return TYPE;
    }
}
