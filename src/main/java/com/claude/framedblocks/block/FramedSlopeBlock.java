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
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 45° slope. Visual is a tilted plane + back wall + floor (model JSON) plus two
 * triangular sides emitted by FramedSlopeModel. FACING = the high side.
 *
 * <p>HALF flips the wedge over, exactly as it does on stairs: BOTTOM rests on the
 * floor, TOP hangs from the ceiling so the ramp faces downwards. Placement picks
 * the half from where the block was clicked, again like stairs, so an inverted
 * slope needs no separate item.
 *
 * <p>Collision is a fine 16x1px staircase. Vanilla step-assist carries the player
 * the full horizontal distance each tick and settles them onto the ramp at the new
 * spot, so the climb runs at full walking speed; the 1px risers plus the camera's
 * step-smoothing make it look continuous. Selection outline is the full cube.
 */
public class FramedSlopeBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH, false);
	private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH, false);
	private static final VoxelShape SHAPE_EAST  = makeShape(Direction.EAST,  false);
	private static final VoxelShape SHAPE_WEST  = makeShape(Direction.WEST,  false);
	private static final VoxelShape SHAPE_NORTH_TOP = makeShape(Direction.NORTH, true);
	private static final VoxelShape SHAPE_SOUTH_TOP = makeShape(Direction.SOUTH, true);
	private static final VoxelShape SHAPE_EAST_TOP  = makeShape(Direction.EAST,  true);
	private static final VoxelShape SHAPE_WEST_TOP  = makeShape(Direction.WEST,  true);

	public FramedSlopeBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(HALF, Half.BOTTOM));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState()
			.setValue(FACING, ctx.getHorizontalDirection())
			.setValue(HALF, clickedHalf(ctx));
	}

	/** Vanilla's stairs rule: the underside of a block, or the upper half of a
	 *  side, places the piece upside down. */
	private static Half clickedHalf(BlockPlaceContext ctx) {
		Direction face = ctx.getClickedFace();
		if (face == Direction.DOWN) return Half.TOP;
		if (face == Direction.UP) return Half.BOTTOM;
		double y = ctx.getClickLocation().y - ctx.getClickedPos().getY();
		return y > 0.5 ? Half.TOP : Half.BOTTOM;
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
		boolean top = state.getValue(HALF) == Half.TOP;
		return switch (state.getValue(FACING)) {
			case SOUTH -> top ? SHAPE_SOUTH_TOP : SHAPE_SOUTH;
			case EAST  -> top ? SHAPE_EAST_TOP  : SHAPE_EAST;
			case WEST  -> top ? SHAPE_WEST_TOP  : SHAPE_WEST;
			default    -> top ? SHAPE_NORTH_TOP : SHAPE_NORTH;
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

	/** Fine 16x1px staircase, used only as the context-less collision fallback
	 *  (the player uses the dynamic ramp box above). High side = {@code facing}.
	 *  When {@code top}, every step hangs from y=16 instead of standing on y=0,
	 *  which mirrors the wedge over exactly as the model is mirrored. */
	private static VoxelShape makeShape(Direction facing, boolean top) {
		VoxelShape shape = Shapes.empty();
		for (int i = 0; i < 16; i++) {
			int thickness = i + 1;
			int y0 = top ? 16 - thickness : 0;
			int y1 = top ? 16 : thickness;
			VoxelShape step = switch (facing) {
				case NORTH -> Block.box(0, y0, 15 - i, 16, y1, 16 - i); // thick at z=0
				case SOUTH -> Block.box(0, y0, i, 16, y1, i + 1);       // thick at z=16
				case EAST  -> Block.box(i, y0, 0, i + 1, y1, 16);       // thick at x=16
				case WEST  -> Block.box(15 - i, y0, 0, 16 - i, y1, 16); // thick at x=0
				default    -> Shapes.empty();
			};
			shape = Shapes.or(shape, step);
		}
		return shape;
	}
}
