package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.block.entity.FramedDoubleSlabBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;

public class FramedSlabBlock extends SlabBlock implements EntityBlock {
	public FramedSlabBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedBlockEntity(pos, state);
	}

	/**
	 * A framed slab placed into another one merges the pair into a single block.
	 *
	 * <p>Two BARE slabs become a plain Framed Block: nothing has been applied yet,
	 * so there is no reason to keep the halves apart, and the result is framed in
	 * one go like any other full block.
	 *
	 * <p>If the slab already there wears a block, the pair becomes a Framed Double
	 * Slab instead, which keeps a separate camo per half. Vanilla would merge them
	 * into a single {@code type=double} slab holding only one texture.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == this
				&& state.getValue(TYPE) != SlabType.DOUBLE
				&& fillsOtherHalf(state, pos, hit)) {
			if (!level.isClientSide()) {
				BlockState existing = level.getBlockEntity(pos) instanceof FramedBlockEntity fbe
					? fbe.getCamo() : Blocks.AIR.defaultBlockState();
				boolean existingIsBottom = state.getValue(TYPE) == SlabType.BOTTOM;
				if (existing.isAir()) {
					level.setBlockAndUpdate(pos, ModBlocks.FRAMED_BLOCK.defaultBlockState());
				} else {
					level.setBlockAndUpdate(pos, ModBlocks.FRAMED_DOUBLE_SLAB.defaultBlockState());
					if (level.getBlockEntity(pos) instanceof FramedDoubleSlabBlockEntity be) {
						// The slab already there keeps its texture and its half; the
						// one just placed starts bare in the other half.
						be.setCamo(existingIsBottom, existing);
					}
				}
				if (!player.getAbilities().instabuild) stack.shrink(1);
				level.playSound(null, pos, state.getSoundType().getPlaceSound(),
					SoundSource.BLOCKS, 1.0f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}
		return FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
	}

	/**
	 * Whether this click is aimed at the EMPTY half, which is what vanilla
	 * requires before merging two slabs. Without it any right-click would merge,
	 * so placing a slab against the side of another one would swallow it instead
	 * of putting a new slab next to it.
	 */
	private static boolean fillsOtherHalf(BlockState state, BlockPos pos, BlockHitResult hit) {
		boolean upper = hit.getLocation().y - pos.getY() > 0.5;
		Direction face = hit.getDirection();
		return state.getValue(TYPE) == SlabType.BOTTOM
			? face == Direction.UP || (upper && face.getAxis().isHorizontal())
			: face == Direction.DOWN || (!upper && face.getAxis().isHorizontal());
	}

	/** Stops vanilla merging two framed slabs into a single-camo double slab;
	 *  {@link #useItemOn} turns them into the two-camo block instead. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (ctx.getItemInHand().getItem() instanceof BlockItem bi && bi.getBlock() == this) {
			return false;
		}
		return super.canBeReplaced(state, ctx);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		return FramedInteractionHelper.useWithoutItem(level, pos, player);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FramedInteractionHelper.resetIfFresh(level, pos);
	}
}
