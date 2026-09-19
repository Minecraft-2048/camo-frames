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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A framed door. Two of them side by side act as a DOUBLE door: opening one
 * swings the other with it, so a two-wide doorway no longer has to be worked
 * one leaf at a time.
 *
 * <p>A pair is what vanilla already builds when you place the second door
 * beside the first: same facing, opposite hinge, shoulder to shoulder. Nothing
 * to align by hand, and an iron framed door still only answers to redstone
 * because vanilla refuses the hand-open before this ever runs.
 */
public class FramedDoorBlock extends DoorBlock implements EntityBlock {
	public FramedDoorBlock(BlockSetType setType, BlockBehaviour.Properties props) {
		super(setType, props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedBlockEntity(pos, state);
	}

	// Each door half keeps its own camo (the player can apply a different
	// texture to the top and bottom), so no half-sync here.
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult r = FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
		return r != InteractionResult.PASS ? r : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		InteractionResult r = FramedInteractionHelper.useWithoutItem(level, pos, player);
		if (r != InteractionResult.PASS) return r;
		InteractionResult vanilla = super.useWithoutItem(state, level, pos, player, hit);
		if (vanilla.consumesAction() && !level.isClientSide()) {
			swingPartner(level, pos, player);
		}
		return vanilla;
	}

	/**
	 * Swings the other leaf of a double door, if there is one.
	 *
	 * <p>Read back from the level rather than from {@code state}: vanilla has
	 * just toggled this door, so the state handed to the interaction is the old
	 * one. Both sides are checked instead of deriving one from the hinge, which
	 * keeps this correct whichever way round the pair was built.
	 */
	private void swingPartner(Level level, BlockPos pos, Player player) {
		BlockState self = level.getBlockState(pos);
		if (self.getBlock() != this) return;
		Direction facing = self.getValue(FACING);
		boolean open = self.getValue(OPEN);
		DoorHingeSide hinge = self.getValue(HINGE);

		for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
			BlockPos other = pos.relative(side);
			BlockState s = level.getBlockState(other);
			if (s.getBlock() == this
					&& s.getValue(FACING) == facing
					&& s.getValue(HINGE) != hinge
					&& s.getValue(HALF) == self.getValue(HALF)
					&& s.getValue(OPEN) != open) {
				setOpen(player, level, s, other, open);
				return;
			}
		}
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FramedInteractionHelper.resetIfFresh(level, pos);
	}
}
