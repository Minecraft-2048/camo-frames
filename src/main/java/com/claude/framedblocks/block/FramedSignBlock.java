package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedSignBlockEntity;
import com.claude.framedblocks.block.entity.ModBlockEntities;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Standing framed sign: vanilla {@link StandingSignBlock} for the whole sign
 * behaviour (16-way placement, edit screen, dye, glow, waxing) plus the camo
 * interaction. A block in hand applies the camo first; anything else falls
 * through to vanilla, so the edit screen still opens.
 */
public class FramedSignBlock extends StandingSignBlock {
	public FramedSignBlock(WoodType woodType, BlockBehaviour.Properties props) {
		super(woodType, props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedSignBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModBlockEntities.FRAMED_SIGN, SignBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult r = FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
		return r != InteractionResult.PASS ? r
			: super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		InteractionResult r = FramedInteractionHelper.useWithoutItem(level, pos, player);
		return r != InteractionResult.PASS ? r : super.useWithoutItem(state, level, pos, player, hit);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FramedInteractionHelper.resetIfFresh(level, pos);
	}
}
