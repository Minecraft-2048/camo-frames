package com.claude.framedblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * A camo-able sign: vanilla {@link SignBlockEntity} handles all of the text and
 * edit-screen machinery, and this only adds the stored camo state, which the
 * sign renderer paints onto the board.
 *
 * <p>It extends a vanilla block entity instead of {@code FramedBlockEntity}, so
 * the camo interaction finds it through the {@link FramedCamo} interface.
 */
public class FramedSignBlockEntity extends SignBlockEntity implements FramedCamo, FramedCamoDrops {
	private BlockState camo = Blocks.AIR.defaultBlockState();

	public FramedSignBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FRAMED_SIGN, pos, state);
	}

	@Override
	public BlockState getCamo() {
		return camo;
	}

	@Override
	public boolean setCamo(BlockState newCamo) {
		if (camo.equals(newCamo)) return false;
		this.camo = newCamo;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		return true;
	}

	@Override
	public List<BlockState> getDroppedCamos() {
		return camo.isAir() ? List.of() : List.of(camo);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);   // text, colour, glow, waxed
		this.camo = input.read("Camo", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!camo.isAir()) {
			output.store("Camo", BlockState.CODEC, camo);
		}
	}
}
