package com.claude.framedblocks.util;

import com.claude.framedblocks.block.entity.FramedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Camo apply/strip logic, ported to 26.1.2. The old single {@code onUse} is
 * split into {@code useItemOn} (block in hand -> apply camo) and
 * {@code useWithoutItem} (empty hand + sneak -> strip camo). Renames:
 *   ActionResult        -> InteractionResult (SUCCESS / PASS, no client bool)
 *   getStackInHand      -> handled by the caller (useItemOn gives the stack)
 *   isSneaking          -> isShiftKeyDown
 *   getAbilities().creativeMode -> .instabuild
 *   Block.dropStack     -> Block.popResource
 *   getSoundGroup       -> getSoundType
 *   SoundCategory       -> SoundSource
 *   decrement           -> shrink
 *   Registries.BLOCK.getId -> BuiltInRegistries.BLOCK.getKey
 */
public final class FramedInteractionHelper {
	private FramedInteractionHelper() {}

	/** Block in hand on an empty frame -> apply that block as camo, oriented as if
	 *  the player were placing it (so directional camos — logs, carved pumpkins,
	 *  stairs… — follow the look/click direction instead of the default state). */
	public static InteractionResult useItemOn(Level level, BlockPos pos, Player player,
	                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
		FramedBlockEntity fbe = getEntity(level, pos);
		if (fbe == null) return InteractionResult.PASS;

		if (held.getItem() instanceof BlockItem bi && fbe.getCamo().isAir()) {
			Block heldBlock = bi.getBlock();
			if (!isValidCamo(heldBlock)) return InteractionResult.PASS;
			BlockState placed = heldBlock.getStateForPlacement(new BlockPlaceContext(player, hand, held, hit));
			BlockState newCamo = (placed != null) ? placed : heldBlock.defaultBlockState();
			if (!level.isClientSide()) {
				fbe.setCamo(newCamo);
				if (!player.getAbilities().instabuild) {
					held.shrink(1);
				}
				level.playSound(null, pos, newCamo.getSoundType().getPlaceSound(),
					SoundSource.BLOCKS, 0.8f, 1.0f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** Empty hand + sneak on a camo'd frame -> strip the camo. */
	public static InteractionResult useWithoutItem(Level level, BlockPos pos, Player player) {
		FramedBlockEntity fbe = getEntity(level, pos);
		if (fbe == null) return InteractionResult.PASS;

		if (player.isShiftKeyDown() && !fbe.getCamo().isAir()) {
			if (!level.isClientSide()) {
				BlockState camo = fbe.getCamo();
				fbe.setCamo(Blocks.AIR.defaultBlockState());
				if (!player.getAbilities().instabuild) {
					Block.popResource(level, pos, new ItemStack(camo.getBlock()));
				}
				level.playSound(null, pos, camo.getSoundType().getBreakSound(),
					SoundSource.BLOCKS, 0.6f, 0.9f);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** Clear stale camo on a freshly placed framed block (server side). */
	public static void resetIfFresh(Level level, BlockPos pos) {
		if (level.isClientSide()) return;
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof FramedBlockEntity f && !f.getCamo().isAir()) {
			f.setCamo(Blocks.AIR.defaultBlockState());
		}
	}

	private static FramedBlockEntity getEntity(Level level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		return (be instanceof FramedBlockEntity f) ? f : null;
	}

	private static boolean isValidCamo(Block block) {
		if (block == Blocks.AIR) return false;
		if (block.defaultBlockState().isAir()) return false;
		// Reject framed blocks themselves to avoid recursion.
		String ns = BuiltInRegistries.BLOCK.getKey(block).getNamespace();
		return !"framedblocks".equals(ns);
	}
}
