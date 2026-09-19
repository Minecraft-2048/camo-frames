package com.claude.framedblocks.item;

import com.claude.framedblocks.FramedBlocksMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Rotates the clicked FRAMED block 90° clockwise (camo and block entity are
 * preserved — the block instance doesn't change, only its state). Runs from the
 * same {@code UseBlockCallback} as the hammer so it fires before the block's own
 * use handler (a framed door/lever would otherwise just open/toggle).
 *
 * <p>No-op on blocks with no orientation (plain cube, slab…). A rotation that
 * would leave the block unsupported is refused. Doors rotate BOTH halves.
 */
public class FramedWrenchItem extends Item {
	public FramedWrenchItem(Properties props) {
		super(props);
	}

	public static InteractionResult use(Level level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);
		if (!FramedBlocksMod.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) {
			return InteractionResult.PASS;
		}
		BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
		if (rotated == state) {
			return InteractionResult.PASS; // nothing orientable on this block
		}

		// Doors are two blocks: rotate both halves so they stay consistent.
		BlockPos otherPos = null;
		BlockState otherRotated = null;
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
			otherPos = lower ? pos.above() : pos.below();
			BlockState other = level.getBlockState(otherPos);
			if (other.is(state.getBlock())) {
				otherRotated = other.rotate(Rotation.CLOCKWISE_90);
			}
		}

		if (!rotated.canSurvive(level, pos)) {
			return InteractionResult.FAIL; // rotation would leave it unsupported
		}
		if (!level.isClientSide()) {
			if (otherRotated != null) {
				level.setBlock(pos, rotated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				level.setBlock(otherPos, otherRotated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			} else {
				level.setBlock(pos, rotated, Block.UPDATE_ALL);
			}
			level.playSound(null, pos, state.getSoundType().getPlaceSound(),
				SoundSource.BLOCKS, 0.7f, 1.1f);
		}
		return InteractionResult.SUCCESS;
	}
}
