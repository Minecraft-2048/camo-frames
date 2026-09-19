package com.claude.framedblocks.block.entity;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity that holds one camo block state.
 *
 * <p>Most framed blocks get this from {@link FramedBlockEntity}, but a few have
 * to extend a vanilla block entity instead (the sign needs all of
 * {@code SignBlockEntity}), so the camo interaction is keyed on this interface
 * rather than on that one class.
 */
public interface FramedCamo {
	/** The applied camo, or AIR when the frame is bare. */
	BlockState getCamo();

	/** Apply a camo. Returns false when it was already that state. */
	boolean setCamo(BlockState camo);
}
