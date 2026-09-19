package com.claude.framedblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The 27 slots behind the secret storage block. Same container plumbing as the
 * framed chest, without any of the theatre: no lid, no openers counter, no
 * sound. A hiding place that announces itself is not one.
 */
public class FramedStorageBlockEntity extends FramedBlockEntity implements Container, MenuProvider {
	private static final int SIZE = 27;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	public FramedStorageBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_STORAGE, pos, state);
	}

	// ---------------- Container ----------------
	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack s : items) {
			if (!s.isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack r = ContainerHelper.removeItem(items, slot, amount);
		if (!r.isEmpty()) setChanged();
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
			stack.setCount(getMaxStackSize());
		}
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ---------------- NBT ----------------
	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input); // camo
		items.clear();
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output); // camo
		ContainerHelper.saveAllItems(output, items);
	}

	/** Break it and the contents spill, rather than vanishing with the block. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, this);
		}
		super.preRemoveSideEffects(pos, state);
	}

	// ---------------- Menu ----------------
	@Override
	public Component getDisplayName() {
		return Component.translatable("container.framedblocks.framed_secret_storage");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
		return ChestMenu.threeRows(syncId, playerInv, this);
	}
}
