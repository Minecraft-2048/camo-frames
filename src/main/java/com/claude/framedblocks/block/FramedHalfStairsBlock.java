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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Half stair: a normal stair sliced down the middle along its width, half as
 * wide (8px), full height and depth, keeping the stair side profile. Two pieces
 * (one {@code right}, one not) placed side by side rebuild a full stair.
 *
 * <p>{@code facing} = the horizontal direction the player faced when placing
 * (the tall step is on the far side, like a vanilla stair). {@code right} =
 * which width-half the piece occupies, derived from where on the block the
 * player clicked so the two halves snap together. Bottom-half only for now.
 */
public class FramedHalfStairsBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	public FramedHalfStairsBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH).setValue(RIGHT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RIGHT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction facing = ctx.getHorizontalDirection();
		return defaultBlockState().setValue(FACING, facing).setValue(RIGHT, hitIsRight(facing, ctx));
	}

	/** Which width-half (in facing-local space) the click landed on, so two
	 *  halves placed on opposite sides of a face snap into a full stair. */
	private static boolean hitIsRight(Direction facing, BlockPlaceContext ctx) {
		Vec3 hit = ctx.getClickLocation();
		BlockPos pos = ctx.getClickedPos();
		double fx = hit.x - pos.getX();
		double fz = hit.z - pos.getZ();
		double u = switch (facing) {
			case EAST  -> fz;
			case SOUTH -> 1.0 - fx;
			case WEST  -> 1.0 - fz;
			default    -> fx;            // NORTH
		};
		return u > 0.5;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		// Mirroring flips the facing AND the handedness.
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
			.setValue(RIGHT, !state.getValue(RIGHT));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state.getValue(FACING), state.getValue(RIGHT));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state.getValue(FACING), state.getValue(RIGHT));
	}

	/** A full-footprint bottom slab + the upper step at the back, over one
	 *  width-half. RIGHT = the east half (x 8-16); !RIGHT mirrors to x 0-8. The
	 *  NORTH-base boxes are rotated to the actual facing with the SAME convention
	 *  as the blockstate "y" rotation, so collision lines up with the model. */
	private static VoxelShape shapeFor(Direction facing, boolean right) {
		double x1 = right ? 8 : 0, x2 = right ? 16 : 8;
		return Shapes.or(
			rotatedBox(facing, x1, 0, 0, x2, 8, 16),     // slab (this half, full run)
			rotatedBox(facing, x1, 8, 0, x2, 16, 8));    // upper step (back/north)
	}

	/** Map a NORTH-base box (pixel coords 0-16) to {@code facing}: north to east
	 *  sends z=0 to x=16, matching the blockstate "y" rotation. */
	private static VoxelShape rotatedBox(Direction facing,
	                                     double x1, double y1, double z1, double x2, double y2, double z2) {
		double ax1, az1, ax2, az2;
		switch (facing) {
			case EAST  -> { ax1 = 16 - z2; az1 = x1;      ax2 = 16 - z1; az2 = x2;      }
			case SOUTH -> { ax1 = 16 - x2; az1 = 16 - z2; ax2 = 16 - x1; az2 = 16 - z1; }
			case WEST  -> { ax1 = z1;      az1 = 16 - x2; ax2 = z2;      az2 = 16 - x1; }
			default    -> { ax1 = x1;      az1 = z1;      ax2 = x2;      az2 = z2;      }
		}
		return Block.box(ax1, y1, az1, ax2, y2, az2);
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
