package com.claude.framedblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * The framed torch wired as a redstone torch: a permanent power source that
 * INVERTS its support, exactly like the vanilla one. Lit and powering while the
 * block it is attached to is unpowered, dark and inert once that block is
 * powered.
 *
 * <p>The one vanilla behaviour left out is burnout: a real redstone torch stops
 * responding after being toggled too fast in a short window, to break runaway
 * clocks. This one keeps switching.
 */
public class FramedRedstoneTorchBlock extends FramedTorchBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** The redstone-dust red the vanilla torch particles use. */
	private static final DustParticleOptions DUST = DustParticleOptions.REDSTONE;

	public FramedRedstoneTorchBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(defaultBlockState().setValue(LIT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}

	@Override
	protected ParticleOptions flameParticle() {
		return DUST;
	}

	@Override
	protected boolean isBurning(BlockState state) {
		return state.getValue(LIT);
	}

	// ---------------------------------------------------------- redstone
	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/**
	 * Powers every side except the one it hangs from: the classic torch rule,
	 * and what lets a torch invert the block it is stuck to without feeding
	 * itself. FACING points AWAY from the support, so the support queries with
	 * direction == FACING.
	 */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(LIT) && state.getValue(FACING) != direction ? 15 : 0;
	}

	/** Strong power only straight up, so a torch under a block powers it fully. */
	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return direction == Direction.UP ? getSignal(state, level, pos, direction) : 0;
	}

	/** The direction from the torch toward the block holding it up. */
	private static Direction supportSide(BlockState state) {
		Direction facing = state.getValue(FACING);
		return facing == Direction.UP ? Direction.DOWN : facing.getOpposite();
	}

	private static boolean supportIsPowered(Level level, BlockPos pos, BlockState state) {
		Direction side = supportSide(state);
		return level.hasSignal(pos.relative(side), side);
	}

	private void notifyNeighbours(Level level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			level.updateNeighborsAt(pos.relative(d), this, null);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		notifyNeighbours(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (!movedByPiston) {
			notifyNeighbours(level, pos);
		}
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
	                               @Nullable Orientation orientation, boolean movedByPiston) {
		// Only schedule when the support's state actually disagrees with ours,
		// and only once - otherwise a redstone loop reschedules every tick.
		if (state.getValue(LIT) == supportIsPowered(level, pos, state)
			&& !level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, 2);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) == supportIsPowered(level, pos, state)) {
			level.setBlock(pos, state.cycle(LIT), Block.UPDATE_ALL);
			notifyNeighbours(level, pos);
		}
	}
}
