package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedStorageBlockEntity;
import com.claude.framedblocks.util.FramedInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A full framed cube that quietly holds 27 slots.
 *
 * <p>Camouflaged it is indistinguishable from whatever block it copies: no lid,
 * no seam, no shape of its own. Right-click with a block still applies a camo
 * and sneak + empty hand still strips it, exactly like every other framed
 * block; an ordinary empty-handed right-click is what opens the inventory.
 *
 * <p>It deliberately does NOT answer comparators. A comparator reading a signal
 * out of a plain-looking wall would give the hiding place away, which is the
 * one thing this block exists to avoid.
 */
public class FramedSecretStorageBlock extends Block implements EntityBlock {
	public FramedSecretStorageBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedStorageBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		// Camo first: applying a texture must keep working, or the block could
		// never be disguised in the first place.
		InteractionResult r = FramedInteractionHelper.useItemOn(level, pos, player, hand, stack, hit);
		return r == InteractionResult.SUCCESS ? r : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		InteractionResult strip = FramedInteractionHelper.useWithoutItem(level, pos, player);
		if (strip == InteractionResult.SUCCESS) return strip;
		if (!level.isClientSide()) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof MenuProvider menu) {
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
}
