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

import java.util.Arrays;

/**
 * Stores the 64 cells (4x4x4 grid of 4x4x4 px sub-cubes) of a framed-bits
 * block. octants[i]: null = empty; AIR = present bare frame; else = present +
 * camo. Cell index i = x | (y<<2) | (z<<4), each axis 0..3 along that axis.
 * Render data handed to FramedSmallBitsModel is a BlockState[64] clone.
 */
public class FramedSmallBitsBlockEntity extends BlockEntity implements RenderDataBlockEntity, FramedCamoDrops {
	private final BlockState[] octants = new BlockState[64];

	public FramedSmallBitsBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_SMALL_BITS, pos, state);
	}

	/** Every camo'd octant's block, so breaking the whole bits block drops them all. */
	@Override
	public java.util.List<BlockState> getDroppedCamos() {
		java.util.List<BlockState> list = new java.util.ArrayList<>();
		for (BlockState o : octants) {
			if (o != null && !o.isAir()) list.add(o);
		}
		return list;
	}

	public static int cellIndex(int x, int y, int z) {
		return (x & 3) | ((y & 3) << 2) | ((z & 3) << 4);
	}

	public boolean isPresent(int i) { return octants[i] != null; }
	public BlockState getCamo(int i) { return octants[i]; }

	public int count() {
		int c = 0;
		for (BlockState o : octants) if (o != null) c++;
		return c;
	}

	public boolean place(int i, BlockState camo) {
		if (octants[i] != null) return false;
		octants[i] = camo;
		sync();
		return true;
	}

	public boolean setCamo(int i, BlockState camo) {
		if (octants[i] == null || octants[i].equals(camo)) return false;
		octants[i] = camo;
		sync();
		return true;
	}

	public BlockState remove(int i) {
		BlockState old = octants[i];
		if (old == null) return null;
		octants[i] = null;
		sync();
		return old;
	}

	private void sync() {
		setChanged();
		// Re-mesh on BOTH sides: an octant change never touches the block state, so
		// MC won't re-render on its own, and a client-side prediction (the hammer
		// stripping an octant) would stay invisible until the next server sync.
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		BlockState[] old = octants.clone();
		Arrays.fill(octants, null);
		for (int i = 0; i < 64; i++) {
			// A stored octant (AIR for a bare frame) means present; absent = empty.
			octants[i] = input.read("Bit" + i, BlockState.CODEC).orElse(null);
		}
		if (level != null && level.isClientSide() && !Arrays.equals(old, octants)) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		for (int i = 0; i < 64; i++) {
			if (octants[i] != null) {
				output.store("Bit" + i, BlockState.CODEC, octants[i]);
			}
		}
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
		return octants.clone();
	}
}
