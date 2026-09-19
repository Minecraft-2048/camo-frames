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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A framed pyramid: full 16x16 base, four triangular faces meeting at the apex
 * (8,16,8). Symmetric under rotation, so no facing property. The whole shape is
 * emitted by FramedPyramidModel (triangles aren't expressible in model JSON);
 * collision is a 16-step staircase shrinking toward the apex.
 */
public class FramedPyramidBlock extends Block implements EntityBlock {
	private static final VoxelShape COLLISION_SHAPE = makeShape();

	public FramedPyramidBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return COLLISION_SHAPE;
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

	/** 16 square 1px layers shrinking toward the apex: layer i spans
	 *  [i/2, 16-i/2] on both horizontal axes. Climbable via step-assist. */
	private static VoxelShape makeShape() {
		VoxelShape shape = Shapes.empty();
		for (int i = 0; i < 16; i++) {
			double inset = i * 0.5;
			double min = inset, max = 16 - inset;
			if (max - min <= 0) break;
			shape = Shapes.or(shape, Block.box(min, i, min, max, i + 1, max));
		}
		return shape.optimize();
	}
}
