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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores the camo BlockState for a framed block. Ported to 26.1.2:
 *   - readNbt/writeNbt(NbtCompound)  -> loadAdditional/saveAdditional(ValueInput/ValueOutput)
 *     with codec-based storage: store/read("Camo", BlockState.CODEC, ...).
 *   - markDirty() -> setChanged(); world -> level; pos -> worldPosition;
 *     getCachedState() -> getBlockState(); isClient -> isClientSide;
 *     updateListeners() -> sendBlockUpdated().
 *   - toUpdatePacket -> getUpdatePacket (ClientboundBlockEntityDataPacket);
 *     toInitialChunkDataNbt -> getUpdateTag(HolderLookup.Provider).
 *
 * NOTE: the FRAPI render-data hook (old RenderAttachmentBlockEntity) is added
 * back in the rendering slice, once the new model pipeline is ported.
 */
public class FramedBlockEntity extends BlockEntity implements RenderDataBlockEntity, FramedCamoDrops {
	private BlockState camo = Blocks.AIR.defaultBlockState();

	@Override
	public java.util.List<BlockState> getDroppedCamos() {
		return camo.isAir() ? java.util.List.of() : java.util.List.of(camo);
	}

	public FramedBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.FRAMED, pos, state);
	}

	protected FramedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public BlockState getCamo() {
		return camo;
	}

	public boolean setCamo(BlockState newCamo) {
		if (camo.equals(newCamo)) return false;
		this.camo = newCamo;
		setChanged();
		// Re-mesh on BOTH sides: the server pushes the change to trackers, and a
		// client-side change (a predicted apply/strip) needs the same re-render —
		// the block state itself never changes, so MC won't schedule it for us.
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		return true;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		BlockState old = this.camo;
		this.camo = input.read("Camo", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		// On the client this runs when the server pushes a BE update. The block
		// state didn't change, so MC won't re-mesh on its own — force it.
		if (level != null && level.isClientSide() && !old.equals(this.camo)) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!camo.isAir()) {
			output.store("Camo", BlockState.CODEC, camo);
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

	/** Render data handed to the model (FramedBlockStateModel) so it can draw
	 *  the camo. Replaces the old RenderAttachmentBlockEntity.getRenderAttachmentData. */
	@Override
	public Object getRenderData() {
		return camo;
	}
}
