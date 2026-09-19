package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedDoublePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

/**
 * Framed Double Panel — a full cube split into two panels with a vertical seam.
 * FACING = the player's look direction at placement, so the seam is always
 * perpendicular to the view (a vertical line on the face you look at), and the
 * two halves sit left/right. Each half is framed independently.
 */
public class FramedDoublePanelBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public FramedDoublePanelBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		// FACING = how the player is looking, so the seam is perpendicular to the view.
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedDoublePanelBlockEntity(pos, state);
	}

	/** Which half the click landed on, matching FramedDoublePanelModel's split. */
	public static boolean isLeftPanel(BlockState state, BlockPos pos, BlockHitResult hit) {
		Vec3 r = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		return isLeft(state.getValue(FACING), r.x, r.z);
	}

	/** Left/right classification for a point (local 0..1), per facing rotation
	 *  (blockstate y: north=0, east=90, south=180, west=270; model split on X). */
	public static boolean isLeft(Direction facing, double x, double z) {
		return switch (facing) {
			case EAST  -> z < 0.5;
			case SOUTH -> x > 0.5;
			case WEST  -> z > 0.5;
			default    -> x < 0.5; // NORTH
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedDoublePanelBlockEntity be)) return InteractionResult.PASS;
		boolean left = isLeftPanel(state, pos, hit);
		if (stack.getItem() instanceof BlockItem bi && be.getCamo(left).isAir()) {
			Block heldBlock = bi.getBlock();
			if (!isValidCamo(heldBlock)) return InteractionResult.PASS;
			BlockState newCamo = heldBlock.defaultBlockState();
			if (!level.isClientSide()) {
				be.setCamo(left, newCamo);
				if (!player.getAbilities().instabuild) stack.shrink(1);
				level.playSound(null, pos, newCamo.getSoundType().getPlaceSound(),
					SoundSource.BLOCKS, 0.8f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedDoublePanelBlockEntity be)) return InteractionResult.PASS;
		if (!player.isShiftKeyDown()) return InteractionResult.PASS;
		boolean left = isLeftPanel(state, pos, hit);
		if (!be.getCamo(left).isAir()) {
			if (!level.isClientSide()) {
				BlockState camo = be.getCamo(left);
				be.setCamo(left, Blocks.AIR.defaultBlockState());
				if (!player.getAbilities().instabuild) {
					Block.popResource(level, pos, new ItemStack(camo.getBlock()));
				}
				level.playSound(null, pos, camo.getSoundType().getBreakSound(),
					SoundSource.BLOCKS, 0.6f, 0.9f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static boolean isValidCamo(Block block) {
		if (block == Blocks.AIR || block.defaultBlockState().isAir()) return false;
		return !"framedblocks".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
	}
}
