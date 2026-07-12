package dev.alone.core.net;

import dev.alone.core.AloneCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client sync of the player's survival state, so the HUD has something to draw.
 * One packet carries every meter; add fields here as meters land.
 */
public record SurvivalSyncPayload(float stamina, float thirst, float temperature, int conditions, float fatigue, float gut) implements CustomPacketPayload {
    public static final Type<SurvivalSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "survival_sync"));

    public static final StreamCodec<ByteBuf, SurvivalSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, SurvivalSyncPayload::stamina,
        ByteBufCodecs.FLOAT, SurvivalSyncPayload::thirst,
        ByteBufCodecs.FLOAT, SurvivalSyncPayload::temperature,
        ByteBufCodecs.VAR_INT, SurvivalSyncPayload::conditions,
        ByteBufCodecs.FLOAT, SurvivalSyncPayload::fatigue,
        ByteBufCodecs.FLOAT, SurvivalSyncPayload::gut,
        SurvivalSyncPayload::new);

    @Override
    public Type<SurvivalSyncPayload> type() {
        return TYPE;
    }
}
