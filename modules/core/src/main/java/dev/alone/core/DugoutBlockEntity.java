package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A log being worked into a dugout canoe (§ boats). Hollowing a canoe out of a whole log is days of
 * labour: <b>char the inside with fire, scrape out the burnt wood with an adze, and repeat</b>. The state —
 * how many scrapes are done, and whether it's currently charred and so ready to scrape — lives on the block
 * entity as attachments (like the kiln's fuel/progress). The interactions live in {@link DugoutBlock}.
 */
public class DugoutBlockEntity extends BlockEntity {
    /** How many char-and-scrape cycles it takes to hollow a whole log through — real, laborious work. */
    public static final int STAGES_TO_FINISH = 6;

    /** Whether the inside is currently charred and ready to be scraped out (each scrape needs a fresh char). */
    public static final AttachmentType<Boolean> CHARRED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "dugout_charred"), Codec.BOOL);
    /** How many scrapes are done (0..STAGES_TO_FINISH). */
    public static final AttachmentType<Integer> STAGE = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "dugout_stage"), Codec.INT);

    public DugoutBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.DUGOUT_BLANK_BLOCK_ENTITY, pos, state);
    }
}
