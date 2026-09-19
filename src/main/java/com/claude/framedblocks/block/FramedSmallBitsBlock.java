package com.claude.framedblocks.block;

import com.claude.framedblocks.block.entity.FramedSmallBitsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A framed micro-block: one block split into a 4x4x4 grid of 64 cells (4x4x4 px
 * sub-cubes), 1-64 placeable, each framed independently. Mini Chisel-&-Bits.
 * useItemOn (block in hand) camos the clicked octant; useWithoutItem (sneak)
 * removes it; the bits item's useOn adds an octant when both pass.
 */
public class FramedSmallBitsBlock extends Block implements EntityBlock {
	public FramedSmallBitsBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedSmallBitsBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeAt(level, pos);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeAt(level, pos);
	}

	private static VoxelShape shapeAt(BlockGetter level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FramedSmallBitsBlockEntity bits) || bits.count() == 0) {
			return Shapes.block();
		}
		VoxelShape s = Shapes.empty();
		for (int i = 0; i < 64; i++) {
			if (!bits.isPresent(i)) continue;
			int x = i & 3, y = (i >> 2) & 3, z = (i >> 4) & 3;
			s = Shapes.or(s, Block.box(x * 4, y * 4, z * 4, x * 4 + 4, y * 4 + 4, z * 4 + 4));
		}
		// optimize() merges the adjacent octant boxes and drops the internal faces
		// between them. Without it, the player catches on those seams and gets
		// pushed back when walking on octants arranged as steps.
		return s.optimize();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                      Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedSmallBitsBlockEntity bits)) return InteractionResult.PASS;
		int oct = clickedCell(pos, hit);
		if (stack.getItem() instanceof BlockItem bi && stack.getItem() != asItem()
				&& bits.isPresent(oct) && bits.getCamo(oct).isAir()) {
			Block b = bi.getBlock();
			if (isValidCamo(b)) {
				if (!level.isClientSide()) {
					bits.setCamo(oct, b.defaultBlockState());
					if (!player.getAbilities().instabuild) stack.shrink(1);
					level.playSound(null, pos, b.defaultBlockState().getSoundType().getPlaceSound(),
						SoundSource.BLOCKS, 0.8f, 1.0f);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS; // bits item's useOn adds an octant
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
	                                           Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof FramedSmallBitsBlockEntity bits)) return InteractionResult.PASS;
		int oct = clickedCell(pos, hit);
		if (player.isShiftKeyDown() && bits.isPresent(oct)) {
			if (!level.isClientSide()) {
				BlockState camo = bits.remove(oct);
				if (!player.getAbilities().instabuild) {
					popResource(level, pos, new ItemStack(asItem()));
					if (camo != null && !camo.isAir()) popResource(level, pos, new ItemStack(camo.getBlock()));
				}
				if (bits.count() == 0) level.destroyBlock(pos, false);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	// ---- octant raycasting helpers (shared with FramedSmallBitsItem) ----

	/** The octant just INSIDE the clicked face. */
	public static int clickedCell(BlockPos pos, BlockHitResult hit) {
		Vec3 r = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		Direction s = hit.getDirection();
		int x = cell(r.x - s.getStepX() * 0.01);
		int y = cell(r.y - s.getStepY() * 0.01);
		int z = cell(r.z - s.getStepZ() * 0.01);
		return FramedSmallBitsBlockEntity.cellIndex(x, y, z);
	}

	/** The empty octant in FRONT of the clicked face, or -1 if outside this block. */
	public static int addCell(BlockPos pos, BlockHitResult hit) {
		Vec3 r = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		Direction s = hit.getDirection();
		int cx = cell(r.x - s.getStepX() * 0.01);
		int cy = cell(r.y - s.getStepY() * 0.01);
		int cz = cell(r.z - s.getStepZ() * 0.01);
		int ax = cx + s.getStepX(), ay = cy + s.getStepY(), az = cz + s.getStepZ();
		if (ax < 0 || ax > 3 || ay < 0 || ay > 3 || az < 0 || az > 3) return -1;
		return FramedSmallBitsBlockEntity.cellIndex(ax, ay, az);
	}

	/** Octant for a brand-new block placed against {@code side} of {@code clicked}. */
	public static int firstCell(Direction side, Vec3 hitPos, BlockPos clicked) {
		Vec3 r = hitPos.subtract(clicked.getX(), clicked.getY(), clicked.getZ());
		int x = cell(r.x), y = cell(r.y), z = cell(r.z);
		switch (side.getAxis()) {
			case X -> x = (side == Direction.EAST) ? 0 : 3;
			case Y -> y = (side == Direction.UP) ? 0 : 3;
			case Z -> z = (side == Direction.SOUTH) ? 0 : 3;
		}
		return FramedSmallBitsBlockEntity.cellIndex(x, y, z);
	}

	/** Which of the four cells along an axis a coordinate falls in. */
	private static int cell(double v) {
		double c = v < 0 ? 0 : (v > 0.999 ? 0.999 : v);
		return (int) (c * 4);
	}

	static boolean isValidCamo(Block block) {
		if (block == Blocks.AIR || block.defaultBlockState().isAir()) return false;
		return !"framedblocks".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
	}
}
