package com.claude.framedblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Framed item frame: the camo (inherited from {@link FramedBlockEntity}) plus
 * one displayed {@link ItemStack} with an 8-step rotation, both synced to the
 * client through the same update-packet path as the camo.
 */
public class FramedItemFrameBlockEntity extends FramedBlockEntity {
	private ItemStack displayedItem = ItemStack.EMPTY;
	private int rotation = 0;

	public FramedItemFrameBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_ITEM_FRAME, pos, state);
	}

	public ItemStack getDisplayedItem() {
		return displayedItem;
	}

	public int getRotation() {
		return rotation;
	}

	public void setDisplayedItem(ItemStack stack) {
		this.displayedItem = stack;
		this.rotation = 0;
		sync();
	}

	/** Rotate the displayed item one step (8 steps of 45 degrees, like vanilla). */
	public void rotateItem() {
		this.rotation = (this.rotation + 1) % 8;
		sync();
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);   // camo
		this.displayedItem = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		this.rotation = input.getIntOr("ItemRotation", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!displayedItem.isEmpty()) {
			output.store("Item", ItemStack.CODEC, displayedItem);
		}
		output.putInt("ItemRotation", rotation);
	}

	/** Breaking the frame drops what it was displaying, not just the frame. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !displayedItem.isEmpty()) {
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				displayedItem);
			displayedItem = ItemStack.EMPTY;
		}
		super.preRemoveSideEffects(pos, state);
	}
}
