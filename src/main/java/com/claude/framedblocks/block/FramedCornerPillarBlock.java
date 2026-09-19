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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An 8x16x8 vertical pillar filling ONE quarter of the block footprint (full
 * height). FACING selects the corner: NORTH=NW, EAST=NE, SOUTH=SE, WEST=SW.
 * Placement is hit-position-driven (the footprint quadrant you aim at picks the
 * corner). Camo via the standard frame interaction; rendered with the camo
 * projected onto the pillar shape by FramedRetextureModel.
 */
public class FramedCornerPillarBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 0,   8, 16, 8);   // NW
	private static final VoxelShape SHAPE_EAST  = Block.box(8, 0, 0,  16, 16, 8);   // NE
	private static final VoxelShape SHAPE_SOUTH = Block.box(8, 0, 8,  16, 16, 16);  // SE
	private static final VoxelShape SHAPE_WEST  = Block.box(0, 0, 8,   8, 16, 16);  // SW

	public FramedCornerPillarBlock(BlockBehaviour.Properties props) {
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
		if (south && east)   facing = Direction.SOUTH;  // SE quadrant -> pillar at SE
		else if (south)      facing = Direction.WEST;   // SW quadrant -> pillar at SW
		else if (east)       facing = Direction.EAST;   // NE quadrant -> pillar at NE
		else                 facing = Direction.NORTH;  // NW quadrant -> pillar at NW
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
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state);
	}

	private static VoxelShape shapeFor(BlockState state) {
		return switch (state.getValue(FACING)) {
			case EAST  -> SHAPE_EAST;
			case SOUTH -> SHAPE_SOUTH;
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
}
