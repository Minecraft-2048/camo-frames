package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A framed block that catches a fall: landing on it does no damage at all,
 * however far you dropped.
 *
 * <p>The counterpart of the framed bouncy cube, which throws you back up. This
 * one just absorbs, so it can sit at the bottom of a shaft or under a jump
 * without launching anyone. Like every framed block it takes the look of
 * whatever you apply to it, so the landing pad can be hidden in the floor.
 */
public class FramedCushionBlock extends Block implements EntityBlock {
	public FramedCushionBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedBlockEntity(pos, state);
	}

	/**
	 * Swallows the fall. Vanilla's implementation is what deals the damage, so
	 * not calling it is exactly what makes this a cushion; hay bales do the same
	 * thing with a multiplier instead of a full stop.
	 */
	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		return FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
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
