package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Framed slime block: bounces entities and sticks to pistons (all inherited
 * from vanilla {@link SlimeBlock}), while still taking a camo.
 */
public class FramedBouncyCubeBlock extends SlimeBlock implements EntityBlock {
	public FramedBouncyCubeBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedBlockEntity(pos, state);
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
