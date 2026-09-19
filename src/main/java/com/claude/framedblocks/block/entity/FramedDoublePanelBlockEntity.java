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

/**
 * Stores TWO camo BlockStates — the FRONT and BACK panels of a Framed Double
 * Panel. The block is a full cube split into two 16x16x8 halves along its
 * facing axis; each half is framed independently. Mirrors {@link FramedBlockEntity}
 * but with two camos. Render data handed to {@code FramedDoublePanelModel} is a
 * {@code BlockState[]{front, back}}.
 */
public class FramedDoublePanelBlockEntity extends BlockEntity implements RenderDataBlockEntity, FramedCamoDrops {
	private BlockState front = Blocks.AIR.defaultBlockState();
	private BlockState back = Blocks.AIR.defaultBlockState();

	public FramedDoublePanelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_DOUBLE_PANEL, pos, state);
	}

	/** Both panel camos, so breaking the double panel drops each applied block. */
	@Override
	public java.util.List<BlockState> getDroppedCamos() {
		java.util.List<BlockState> list = new java.util.ArrayList<>();
		if (!front.isAir()) list.add(front);
		if (!back.isAir()) list.add(back);
		return list;
	}

	public BlockState getCamo(boolean frontPanel) {
		return frontPanel ? front : back;
	}

	public boolean setCamo(boolean frontPanel, BlockState newCamo) {
		BlockState cur = frontPanel ? front : back;
		if (cur.equals(newCamo)) return false;
		if (frontPanel) front = newCamo; else back = newCamo;
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		return true;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		BlockState oldFront = front, oldBack = back;
		front = input.read("CamoFront", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		back = input.read("CamoBack", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		// Client-side BE update: force a re-mesh (the block state didn't change).
		if (level != null && level.isClientSide()
				&& (!oldFront.equals(front) || !oldBack.equals(back))) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!front.isAir()) output.store("CamoFront", BlockState.CODEC, front);
		if (!back.isAir()) output.store("CamoBack", BlockState.CODEC, back);
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
		return new BlockState[]{front, back};
	}
}
