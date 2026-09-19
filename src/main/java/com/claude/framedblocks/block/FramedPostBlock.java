package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin 4x4 post oriented on the axis it is placed against: the fence post
 * without the fence. Same axis property as the pillar, smaller box.
 */
public class FramedPostBlock extends RotatedPillarBlock implements EntityBlock {
	private static final VoxelShape SHAPE_Y = Block.box(6, 0, 6, 10, 16, 10);
	private static final VoxelShape SHAPE_X = Block.box(0, 6, 6, 16, 10, 10);
	private static final VoxelShape SHAPE_Z = Block.box(6, 6, 0, 10, 10, 16);

	public FramedPostBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state);
	}

	private static VoxelShape shapeFor(BlockState state) {
		return switch (state.getValue(AXIS)) {
			case X -> SHAPE_X;
			case Z -> SHAPE_Z;
			default -> SHAPE_Y;
		};
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
