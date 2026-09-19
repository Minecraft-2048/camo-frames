package com.claude.framedblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A big 12x12 button pad: same mechanics as {@link FramedButtonBlock}, just a
 * larger press surface. The model is the enlarged pad; the outline boxes are
 * overridden to match, because vanilla ButtonBlock hardcodes a 6x4 one.
 */
public class FramedLargeButtonBlock extends FramedButtonBlock {
	public FramedLargeButtonBlock(BlockSetType setType, int ticksToStayPressed, BlockBehaviour.Properties props) {
		super(setType, ticksToStayPressed, props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		double h = state.getValue(POWERED) ? 1 : 2;
		// Compared by NAME so this compiles on every mapping generation (the
		// wall-mount enum has been renamed across versions).
		String face = state.getValue(FACE).name();
		if ("FLOOR".equals(face)) {
			return Block.box(2, 0, 2, 14, h, 14);
		}
		if ("CEILING".equals(face)) {
			return Block.box(2, 16 - h, 2, 14, 16, 14);
		}
		return switch (state.getValue(FACING)) {
			case SOUTH -> Block.box(2, 2, 0, 14, 14, h);
			case WEST  -> Block.box(16 - h, 2, 2, 16, 14, 14);
			case EAST  -> Block.box(0, 2, 2, h, 14, 14);
			default    -> Block.box(2, 2, 16 - h, 14, 14, 16); // NORTH
		};
	}
}
