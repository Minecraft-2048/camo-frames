package com.claude.framedblocks.item;

import com.claude.framedblocks.block.FramedSmallBitsBlock;
import com.claude.framedblocks.block.ModBlocks;
import com.claude.framedblocks.block.entity.FramedSmallBitsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Places framed-bits octants. Used on an existing bits block -> adds the octant
 * in front of the clicked sub-face; used on any other block face -> creates a
 * new bits block in the adjacent space with the octant nearest the click.
 */
public class FramedSmallBitsItem extends BlockItem {
	public FramedSmallBitsItem(net.minecraft.world.level.block.Block block, Properties props) {
		super(block, props);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos clicked = ctx.getClickedPos();
		Direction side = ctx.getClickedFace();
		BlockHitResult hit = new BlockHitResult(ctx.getClickLocation(), side, clicked, false);

		if (level.getBlockState(clicked).is(ModBlocks.FRAMED_SMALL_BITS)) {
			int oct = FramedSmallBitsBlock.addCell(clicked, hit);
			if (oct >= 0) return fill(ctx, clicked, oct);
		}
		return placeAdjacent(ctx, clicked, side, hit);
	}

	private InteractionResult placeAdjacent(UseOnContext ctx, BlockPos clicked, Direction side, BlockHitResult hit) {
		Level level = ctx.getLevel();
		BlockPos target = clicked.relative(side);
		BlockState cur = level.getBlockState(target);
		if (cur.is(ModBlocks.FRAMED_SMALL_BITS)) {
			return fill(ctx, target, FramedSmallBitsBlock.clickedCell(target, hit));
		}
		if (!cur.canBeReplaced()) return InteractionResult.PASS;
		int oct = FramedSmallBitsBlock.firstCell(side, ctx.getClickLocation(), clicked);
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(target, ModBlocks.FRAMED_SMALL_BITS.defaultBlockState());
		}
		return fill(ctx, target, oct);
	}

	private InteractionResult fill(UseOnContext ctx, BlockPos pos, int oct) {
		Level level = ctx.getLevel();
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof FramedSmallBitsBlockEntity bits)) return InteractionResult.PASS;
		if (bits.isPresent(oct)) return InteractionResult.PASS;
		bits.place(oct, Blocks.AIR.defaultBlockState());
		Player player = ctx.getPlayer();
		if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
		level.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.0f);
		return InteractionResult.CONSUME;
	}
}
