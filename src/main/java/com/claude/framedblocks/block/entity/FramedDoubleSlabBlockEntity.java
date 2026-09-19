package com.claude.framedblocks.block.entity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores TWO camo BlockStates: the BOTTOM and TOP halves of a Framed Double
 * Slab. Two framed slabs stacked in one block space keep their own texture
 * instead of merging into a single one. Same shape as
 * {@link FramedDoublePanelBlockEntity}, split horizontally rather than by
 * facing. Render data handed to {@code FramedDoubleSlabModel} is a
 * {@code BlockState[]{bottom, top}}.
 */
public class FramedDoubleSlabBlockEntity extends BlockEntity implements RenderDataBlockEntity, FramedCamoDrops {
	private BlockState bottom = Blocks.AIR.defaultBlockState();
	private BlockState top = Blocks.AIR.defaultBlockState();

	public FramedDoubleSlabBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_DOUBLE_SLAB, pos, state);
	}

	/** Both halves, so breaking the double slab drops each applied block. */
	@Override
	public List<BlockState> getDroppedCamos() {
		List<BlockState> list = new ArrayList<>();
		if (!bottom.isAir()) list.add(bottom);
		if (!top.isAir()) list.add(top);
		return list;
	}

	public BlockState getCamo(boolean bottomHalf) {
		return bottomHalf ? bottom : top;
	}

	public boolean setCamo(boolean bottomHalf, BlockState newCamo) {
		BlockState cur = bottomHalf ? bottom : top;
		if (cur.equals(newCamo)) return false;
		if (bottomHalf) bottom = newCamo; else top = newCamo;
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		return true;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		BlockState oldBottom = bottom, oldTop = top;
		bottom = input.read("CamoBottom", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		top = input.read("CamoTop", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		// Client-side BE update: force a re-mesh (the block state didn't change).
		if (level != null && level.isClientSide()
				&& (!oldBottom.equals(bottom) || !oldTop.equals(top))) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!bottom.isAir()) output.store("CamoBottom", BlockState.CODEC, bottom);
		if (!top.isAir()) output.store("CamoTop", BlockState.CODEC, top);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public Object getRenderData() {
		return new BlockState[]{bottom, top};
	}
}
