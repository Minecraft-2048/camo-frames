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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The slope stood on its side: a 45 degree cut through a VERTICAL plane. Full
 * height, with a right-triangle footprint - two full walls meeting at a corner
 * and a diagonal face across the opposite one.
 *
 * <p>FACING picks the corner, matching {@link FramedVerticalStairsBlock} so the
 * two line up when placed side by side: facing=north keeps the north and west
 * walls solid and cuts the south-east corner away.
 */
public class FramedVerticalSlopeBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
	private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
	private static final VoxelShape SHAPE_EAST  = makeShape(Direction.EAST);
	private static final VoxelShape SHAPE_WEST  = makeShape(Direction.WEST);
	/** Same call as the regular slope: a diagonal has no axis-aligned outline,
	 *  and a 16-step one draws as a flight of little black ledges. */
	private static final VoxelShape OUTLINE_SHAPE = Shapes.block();

	public FramedVerticalSlopeBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return OUTLINE_SHAPE;
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

	/** Sub-pixel resolution of the diagonal's collision: 64 columns of 0.25px.
	 *  Minecraft collision is axis-aligned boxes only - a true diagonal surface
	 *  cannot be expressed - so the diagonal is a staircase either way. At 1px
	 *  the player feels every step when sliding along the face; at a quarter of
	 *  that the hitch is 1/64 of a block and reads as a smooth wall. */
	private static final int STEPS = 64;

	/**
	 * The wedge as {@link #STEPS} full-height columns, each reaching as far as
	 * the diagonal allows - the horizontal counterpart of the regular slope's
	 * staircase, but fine enough not to catch. Rounded outward, so the collision
	 * never leaves a sliver the player could stand inside the visible face.
	 */
	private static VoxelShape makeShape(Direction facing) {
		final double s = 16.0 / STEPS;
		VoxelShape shape = Shapes.empty();
		for (int i = 0; i < STEPS; i++) {
			double lo = i * s, hi = lo + s;
			VoxelShape column = switch (facing) {
				// solid corner north-west: depth shrinks going east
				case NORTH -> Block.box(lo, 0, 0, hi, 16, 16 - lo);
				// solid corner north-east: depth grows going east
				case EAST  -> Block.box(lo, 0, 0, hi, 16, hi);
				// solid corner south-east
				case SOUTH -> Block.box(lo, 0, 16 - hi, hi, 16, 16);
				// solid corner south-west
				default    -> Block.box(lo, 0, lo, hi, 16, 16);
			};
			shape = Shapes.or(shape, column);
		}
		return shape.optimize();
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
