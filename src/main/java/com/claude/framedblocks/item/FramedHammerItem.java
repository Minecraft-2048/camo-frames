package com.claude.framedblocks.item;

import com.claude.framedblocks.block.FramedBitsBlock;
import com.claude.framedblocks.block.entity.FramedBitsBlockEntity;
import com.claude.framedblocks.block.entity.FramedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Strips the camo off a framed block and puts the camo block straight into the
 * player's inventory (sneak+empty-hand stripping drops it on the ground instead).
 *
 * <p>The actual work runs from a {@code UseBlockCallback} registered in
 * {@link com.claude.framedblocks.FramedBlocksMod}, which fires BEFORE the
 * block's own use handler. That's what lets the hammer work on interactive
 * framed blocks (door, lever, chest…) without the block swallowing the click.
 */
public class FramedHammerItem extends Item {
	public FramedHammerItem(Properties props) {
		super(props);
	}

	/** Remove the camo the player is pointing at (if any) and hand it over. */
	public static InteractionResult use(Level level, BlockHitResult hit, Player player) {
		BlockPos pos = hit.getBlockPos();
		BlockEntity be = level.getBlockEntity(pos);

		// Bits hold one camo PER OCTANT, so the hammer strips only the octant
		// under the cursor — same octant the camo was applied to. Uses the exact
		// hit-to-octant mapping the block itself uses for applying a camo.
		if (be instanceof FramedBitsBlockEntity bits) {
			int oct = FramedBitsBlock.clickedOctant(pos, hit);
			BlockState camo = bits.getCamo(oct);
			if (camo == null || camo.isAir()) return InteractionResult.PASS;
			bits.setCamo(oct, Blocks.AIR.defaultBlockState());
			reward(level, pos, player, camo);
			return InteractionResult.SUCCESS;
		}

		if (!(be instanceof FramedBlockEntity fbe) || fbe.getCamo().isAir()) {
			return InteractionResult.PASS;
		}
		BlockState camo = fbe.getCamo();
		// Clear on BOTH sides: the callback cancels the client-side block use, so
		// the client would otherwise keep showing the old camo until (if ever) a
		// server sync re-meshes it. Predicting the strip here clears it instantly.
		fbe.setCamo(Blocks.AIR.defaultBlockState());
		reward(level, pos, player, camo);
		return InteractionResult.SUCCESS;
	}

	/** Server side only: the stripped block goes to the player, plus a break sound. */
	private static void reward(Level level, BlockPos pos, Player player, BlockState camo) {
		if (level.isClientSide()) return;
		if (player != null && !player.getAbilities().instabuild) {
			player.getInventory().placeItemBackInInventory(new ItemStack(camo.getBlock()));
		}
		level.playSound(null, pos, camo.getSoundType().getBreakSound(),
			SoundSource.BLOCKS, 0.6f, 0.9f);
	}
}
