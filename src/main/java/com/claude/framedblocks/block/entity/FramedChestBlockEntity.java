package com.claude.framedblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Camo-aware chest BE: 27-slot inventory + a 3-row {@link ChestMenu} GUI + a
 * vanilla-style animated lid.
 *
 * <p>Animation pipeline mirrors vanilla {@code ChestBlockEntity}:
 *   - {@link ContainerOpenersCounter} (server) tracks how many players have the
 *     inventory open, plays the open/close sounds and fires a block event each
 *     time the count crosses zero.
 *   - {@link ChestLidController} (client) smooths the lid 0..1; {@link #clientTick}
 *     steps it; the BER reads it via {@link #getOpenNess}.
 *   - {@link #triggerEvent} (client) receives the block event and flips the
 *     controller open/closed.
 * The body is rendered by the chunk model (camo via FramedRetextureModel); only
 * the lid + latch are drawn by FramedChestRenderer.
 */
public class FramedChestBlockEntity extends FramedBlockEntity
		implements Container, MenuProvider, LidBlockEntity {
	private static final int SIZE = 27;
	/** Block-event id: param = current viewer count (drives the lid). */
	public static final int EVENT_SET_OPEN_COUNT = 1;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final ChestLidController lidController = new ChestLidController();
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, SoundEvents.CHEST_OPEN);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, SoundEvents.CHEST_CLOSE);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
			// Sync to clients so their lid controller can open/close.
			level.blockEvent(pos, state.getBlock(), EVENT_SET_OPEN_COUNT, newCount);
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu
				&& menu.getContainer() == FramedChestBlockEntity.this;
		}
	};

	public FramedChestBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_CHEST, pos, state);
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

	@Override
	public void startOpen(ContainerUser user) {
		if (!this.isRemoved() && this.level != null) {
			this.openersCounter.incrementOpeners(user.getLivingEntity(), this.level,
				this.worldPosition, this.getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!this.isRemoved() && this.level != null) {
			this.openersCounter.decrementOpeners(user.getLivingEntity(), this.level,
				this.worldPosition, this.getBlockState());
		}
	}

	/** Re-scan viewers (safety net for disconnects / out-of-range). Server tick. */
	public void recheckOpen() {
		if (!this.isRemoved() && this.level != null) {
			this.openersCounter.recheckOpeners(this.level, this.worldPosition, this.getBlockState());
		}
	}

	// ---------------- Lid animation ----------------
	public static void clientTick(Level level, BlockPos pos, BlockState state, FramedChestBlockEntity be) {
		be.lidController.tickLid();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FramedChestBlockEntity be) {
		be.recheckOpen();
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == EVENT_SET_OPEN_COUNT) {
			this.lidController.shouldBeOpen(param > 0);
			return true;
		}
		return super.triggerEvent(id, param);
	}

	@Override
	public float getOpenNess(float partialTick) {
		return this.lidController.getOpenness(partialTick);
	}

	private void playSound(Level level, BlockPos pos, SoundEvent sound) {
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			sound, SoundSource.BLOCKS, 0.5f, level.getRandom().nextFloat() * 0.1f + 0.9f);
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
		return Component.translatable("container.framedblocks.framed_chest");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
		return ChestMenu.threeRows(syncId, playerInv, this);
	}
}
