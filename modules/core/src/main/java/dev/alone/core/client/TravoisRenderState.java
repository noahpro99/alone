package dev.alone.core.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state for the travois — carries the block model to draw, mirroring the falling-block renderer. */
public class TravoisRenderState extends EntityRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
