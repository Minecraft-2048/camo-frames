package com.claude.framedblocks.block.entity;

import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A framed block entity that can enumerate every camo block currently applied to
 * it, so that breaking the block drops ALL of them. Single-camo blocks return one;
 * multi-camo blocks (bits, double panel…) return several. Any block added later
 * just implements this to have its camos drop on break.
 */
public interface FramedCamoDrops {
    /** Every applied camo block (never AIR); empty if the block is bare. */
    List<BlockState> getDroppedCamos();
}
