package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedChestBlockEntity;
import com.claude.framedblocks.block.entity.ModBlockEntities;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A chest-shaped framed container: a 14×14×14 box that stores 27 items and
 * accepts a camo. Camo interactions take priority on an empty frame (a block in
 * hand applies camo; sneak + empty hand strips it); any other right-click opens
 * the chest GUI.
 *
 * <p>Static model (no animated lid) — see {@link FramedChestBlockEntity}. Built
 * as {@code Block + EntityBlock} (not BaseEntityBlock) to match the other framed
 * blocks and avoid the {@code codec()} requirement.
 */
public class FramedChestBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Vanilla chest box: 1px gap on every side, 2px on top. */
	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

	public FramedChestBlock(BlockBehaviour.Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedChestBlockEntity(pos, state);
	}

	/** Client ticker steps the lid animation; server ticker re-scans viewers. */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide()
			? checkType(type, ModBlockEntities.FRAMED_CHEST, FramedChestBlockEntity::clientTick)
			: checkType(type, ModBlockEntities.FRAMED_CHEST, FramedChestBlockEntity::serverTick);
	}

	@SuppressWarnings("unchecked")
	private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(
			BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
		return expected == given ? (BlockEntityTicker<A>) ticker : null;
	}

	/** Forward block events (the lid open/close sync) to the BE. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity be = level.getBlockEntity(pos);
		return be != null && be.triggerEvent(id, param);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		// Block in hand on an empty frame -> apply camo (priority). Otherwise fall
		// through to the empty-hand path so the chest opens (or camo is stripped).
		InteractionResult r = FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
		return r == InteractionResult.SUCCESS ? r : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		// Sneak + empty hand on a camo'd frame -> strip camo (priority).
		InteractionResult strip = FramedInteractionHelper.useWithoutItem(level, pos, player);
		if (strip == InteractionResult.SUCCESS) return strip;
		// Otherwise open the chest GUI.
		if (!level.isClientSide()) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof MenuProvider menu) {
				// Opening the menu calls startOpen -> the openers counter plays the
				// open sound and animates the lid.
				player.openMenu(menu);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FramedInteractionHelper.resetIfFresh(level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}
}
