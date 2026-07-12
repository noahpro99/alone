package dev.alone.core.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Render state for the tanning rack — just the one hide (or finished leather) stretched on it (visual only). */
public class TanningRackRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public boolean hasItem = false;
}
