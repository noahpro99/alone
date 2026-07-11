package dev.alone.core.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state for the travois — carries the block model + heading needed to draw the two-pole sled. */
public class TravoisRenderState extends EntityRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    public float yRot; // the sled's heading, so the poles trail the right way
}
