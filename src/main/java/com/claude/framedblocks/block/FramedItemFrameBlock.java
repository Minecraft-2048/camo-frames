package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedItemFrameBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A camo-able item frame, as a BLOCK (vanilla item frames are entities, so
 * everything is recreated): a 12x12x1 frame attached to any of the 6 faces that
 * displays one item.
 *
 * <p>Interactions, in priority order:
 * <ol>
 *   <li>sneak + empty hand: pop the displayed item out, else strip the camo;</li>
 *   <li>an item is displayed: rotate it (8 steps of 45 degrees);</li>
 *   <li>bare frame + block in hand: apply the camo, like every framed block;</li>
 *   <li>anything in hand: display it in the frame.</li>
 * </ol>
 * So to DISPLAY a block item, apply a camo first (non-block items go in any
 * time). The frame body renders through the retexture model; the displayed item
 * is drawn by FramedItemFrameRenderer.
 */
public class FramedItemFrameBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 15, 14, 14, 16);
	private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0, 14, 14, 1);
	private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 2, 1, 14, 14);
	private static final VoxelShape SHAPE_WEST  = Block.box(15, 2, 2, 16, 14, 14);
	private static final VoxelShape SHAPE_UP    = Block.box(2, 0, 2, 14, 1, 14);
	private static final VoxelShape SHAPE_DOWN  = Block.box(2, 15, 2, 14, 16, 14);

	public FramedItemFrameBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState().setValue(FACING, ctx.getClickedFace());
		return state.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? state : null;
	}

	/** Needs a sturdy face on the block behind it, like a vanilla item frame. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos support = pos.relative(facing.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, facing);
	}

	/** Pop off (dropping the frame and its displayed item) when the block behind
	 *  is broken, like a vanilla item frame. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tick, BlockPos pos,
	                                 Direction direction, BlockPos neighborPos, BlockState neighborState,
	                                 RandomSource random) {
		if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, tick, pos, direction, neighborPos, neighborState, random);
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
			case SOUTH -> SHAPE_SOUTH;
			case EAST  -> SHAPE_EAST;
			case WEST  -> SHAPE_WEST;
			case UP    -> SHAPE_UP;
			case DOWN  -> SHAPE_DOWN;
			default    -> SHAPE_NORTH;
		};
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedItemFrameBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedItemFrameBlockEntity frame)) {
			return InteractionResult.PASS;
		}

		// An item is displayed: any click rotates it.
		if (!frame.getDisplayedItem().isEmpty()) {
			if (!level.isClientSide()) {
				frame.rotateItem();
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}

		// Bare frame + block in hand: apply the camo (framing has priority, so a
		// block item disguises the frame instead of going on display).
		if (frame.getCamo().isAir() && stack.getItem() instanceof BlockItem) {
			InteractionResult r = FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
			if (r != InteractionResult.PASS) return r;
		}

		// Otherwise put the held item on display.
		if (!stack.isEmpty()) {
			if (!level.isClientSide()) {
				frame.setDisplayedItem(stack.copyWithCount(1));
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedItemFrameBlockEntity frame)) {
			return InteractionResult.PASS;
		}

		// Sneak + empty hand: pop the displayed item out; if there is none, this
		// falls through to the usual camo strip.
		if (player.isShiftKeyDown() && !frame.getDisplayedItem().isEmpty()) {
			if (!level.isClientSide()) {
				Block.popResource(level, pos, frame.getDisplayedItem());
				frame.setDisplayedItem(ItemStack.EMPTY);
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}

		// An item is displayed: an empty-handed click rotates it too.
		if (!frame.getDisplayedItem().isEmpty()) {
			if (!level.isClientSide()) {
				frame.rotateItem();
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}

		return FramedInteractionHelper.useWithoutItem(level, pos, player);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FramedInteractionHelper.resetIfFresh(level, pos);
	}
}
