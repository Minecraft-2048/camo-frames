package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedDoubleSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Two framed slabs sharing one block space, each keeping its own camo.
 *
 * <p>Vanilla merges two slabs into a single {@code type=double} block, which can
 * only ever hold one texture. Placing a framed slab into another one produces
 * this block instead: a full cube whose bottom and top halves are framed
 * separately, so the pair can be two different blocks.
 *
 * <p>It has no item of its own and is never placed directly. It is formed in
 * world by {@link FramedSlabBlock} and drops two framed slabs when broken.
 */
public class FramedDoubleSlabBlock extends Block implements EntityBlock {
	public FramedDoubleSlabBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedDoubleSlabBlockEntity(pos, state);
	}

	/** Which half the click landed on, matching the model split at y = 0.5. */
	public static boolean isBottomHalf(BlockPos pos, BlockHitResult hit) {
		return hit.getLocation().y - pos.getY() < 0.5;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedDoubleSlabBlockEntity be)) {
			return InteractionResult.PASS;
		}
		boolean bottom = isBottomHalf(pos, hit);
		if (stack.getItem() instanceof BlockItem bi && be.getCamo(bottom).isAir()) {
			Block heldBlock = bi.getBlock();
			if (!isValidCamo(heldBlock)) return InteractionResult.PASS;
			BlockState newCamo = heldBlock.defaultBlockState();
			if (!level.isClientSide()) {
				be.setCamo(bottom, newCamo);
				if (!player.getAbilities().instabuild) stack.shrink(1);
				level.playSound(null, pos, newCamo.getSoundType().getPlaceSound(),
					SoundSource.BLOCKS, 0.8f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedDoubleSlabBlockEntity be)) {
			return InteractionResult.PASS;
		}
		boolean bottom = isBottomHalf(pos, hit);
		BlockState camo = be.getCamo(bottom);
		if (player.isShiftKeyDown() && !camo.isAir()) {
			if (!level.isClientSide()) {
				be.setCamo(bottom, Blocks.AIR.defaultBlockState());
				if (!player.getAbilities().instabuild) {
					Block.popResource(level, pos, new ItemStack(camo.getBlock()));
				}
				level.playSound(null, pos, camo.getSoundType().getBreakSound(),
					SoundSource.BLOCKS, 0.6f, 0.9f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** Same rule as everywhere else: anything but air and our own framed blocks. */
	private static boolean isValidCamo(Block block) {
		if (block == Blocks.AIR || block.defaultBlockState().isAir()) return false;
		return !"framedblocks".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
	}
}
