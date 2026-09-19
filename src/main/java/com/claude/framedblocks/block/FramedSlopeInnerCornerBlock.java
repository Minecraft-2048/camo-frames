package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Inner-corner slope: two slopes meeting at a concave 90° corner. FACING points
 * at the LOW outside corner; placement is hit-quadrant based. Visual = back
 * walls + floor (JSON, retextured) + two inclined plane halves + two V-closure
 * triangles (FramedSlopeInnerCornerModel). Collision = fine 16x1px staircase.
 */
public class FramedSlopeInnerCornerBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
	private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
	private static final VoxelShape SHAPE_EAST  = makeShape(Direction.EAST);
	private static final VoxelShape SHAPE_WEST  = makeShape(Direction.WEST);

	public FramedSlopeInnerCornerBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockPos pos = ctx.getClickedPos();
		Vec3 hit = ctx.getClickLocation();
		double dx = Math.max(0.0, Math.min(1.0, hit.x - pos.getX()));
		double dz = Math.max(0.0, Math.min(1.0, hit.z - pos.getZ()));
		boolean east = dx > 0.5;
		boolean south = dz > 0.5;
		Direction facing;
		if (south && east)   facing = Direction.WEST;   // SE quadrant -> low SE
		else if (south)      facing = Direction.NORTH;  // SW quadrant -> low SW
		else if (east)       facing = Direction.SOUTH;  // NE quadrant -> low NE
		else                 facing = Direction.EAST;   // NW quadrant -> low NW
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SHAPE_SOUTH;
			case EAST  -> SHAPE_EAST;
			case WEST  -> SHAPE_WEST;
			default    -> SHAPE_NORTH;
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

	/** Fine 16x1px staircase layers (full block minus an i×i low-corner notch),
	 *  used only as the context-less collision fallback; entities use the dynamic
	 *  bowl box above. */
	private static VoxelShape makeShape(Direction facing) {
		VoxelShape shape = Shapes.empty();
		for (int i = 0; i < 16; i++) {
			int c = i, y0 = i, y1 = i + 1;
			VoxelShape s1, s2;
			switch (facing) {
				case NORTH -> {
					s1 = Block.box(0, y0, 0, 16, y1, 16 - c);
					s2 = Block.box(c, y0, 0, 16, y1, 16);
				}
				case EAST -> {
					s1 = Block.box(0, y0, c, 16, y1, 16);
					s2 = Block.box(c, y0, 0, 16, y1, 16);
				}
				case SOUTH -> {
					s1 = Block.box(0, y0, c, 16, y1, 16);
					s2 = Block.box(0, y0, 0, 16 - c, y1, 16);
				}
				case WEST -> {
					s1 = Block.box(0, y0, 0, 16, y1, 16 - c);
					s2 = Block.box(0, y0, 0, 16 - c, y1, 16);
				}
				default -> { continue; }
			}
			shape = Shapes.or(shape, Shapes.or(s1, s2));
		}
		return shape;
	}
}
