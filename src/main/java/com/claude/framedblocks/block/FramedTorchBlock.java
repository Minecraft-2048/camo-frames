package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A camo-able torch: emits light and flame/smoke particles, placeable on the
 * floor (FACING=UP) or on a wall (FACING=horizontal). The torch body is the
 * frame, so it takes a camo like every other framed block. No support
 * requirement (it never pops off) and no collision, like a vanilla torch.
 */
public class FramedTorchBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class,
		Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

	// Outlines hug the actual model: the standing post is exactly [6,6]-[10,10] x[0,10].
	private static final VoxelShape STANDING_SHAPE = Block.box(6, 0, 6, 10, 10, 10);
	// A wall torch leans on the side OPPOSITE its facing; the box bounds the tilted post.
	private static final VoxelShape WALL_EAST  = Block.box(0, 3, 6, 5, 14, 10);
	private static final VoxelShape WALL_WEST  = Block.box(11, 3, 6, 16, 14, 10);
	private static final VoxelShape WALL_SOUTH = Block.box(6, 3, 0, 10, 14, 5);
	private static final VoxelShape WALL_NORTH = Block.box(6, 3, 11, 10, 14, 16);

	public FramedTorchBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction side = ctx.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side : Direction.UP;
		return defaultBlockState().setValue(FACING, facing);
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
		return switch (state.getValue(FACING)) {
			case EAST  -> WALL_EAST;
			case WEST  -> WALL_WEST;
			case SOUTH -> WALL_SOUTH;
			case NORTH -> WALL_NORTH;
			default    -> STANDING_SHAPE;   // UP = standing
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

	/** The flame sitting on the ember. Subclasses swap it for soul fire or
	 *  redstone dust; the smoke and the placement maths stay shared. */
	protected ParticleOptions flameParticle() {
		return ParticleTypes.FLAME;
	}

	/** Whether the torch is currently burning. Always true here; the redstone
	 *  torch turns it off when its support is powered. */
	protected boolean isBurning(BlockState state) {
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!isBurning(state)) return;
		Direction facing = state.getValue(FACING);
		double x = pos.getX() + 0.5, y = pos.getY() + 0.7, z = pos.getZ() + 0.5;
		if (facing.getAxis().isHorizontal()) {
			Direction back = facing.getOpposite();
			x += 0.27 * back.getStepX();
			z += 0.27 * back.getStepZ();
		}
		ParticleOptions flame = flameParticle();
		if (flame == ParticleTypes.FLAME || flame == ParticleTypes.SOUL_FIRE_FLAME) {
			level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
		}
		level.addParticle(flame, x, y, z, 0.0, 0.0, 0.0);
	}
}
