package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.function.Predicate;

/**
 * The vertical-slope counterpart to {@link FramedSlopeModel}. The wedge is
 * extruded along Y instead of X, so the two triangular caps are the TOP and
 * BOTTOM faces rather than the sides. Same reason for emitting them here: a
 * triangle cannot be expressed in model JSON, so it has to be injected at
 * quad-emit time. The rest of the shape (two walls + the diagonal plate) comes
 * from the JSON model and is retextured by the parent.
 */
public class FramedVerticalSlopeModel extends FramedRetextureModel {
	private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private record Layer(float u0, float u1, float v0, float v1, int color, ChunkSectionLayer chunkLayer) {}

	public FramedVerticalSlopeModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		// 1. Two walls + the diagonal plate, retextured with the camo.
		super.emitQuads(emitter, blockView, pos, state, random, cull);

		// 2. The triangular floor and ceiling.
		Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		BlockState camo = (data instanceof BlockState bs) ? bs : null;

		emitCap(emitter, blockView, pos, Direction.UP, facing, camo, random);
		emitCap(emitter, blockView, pos, Direction.DOWN, facing, camo, random);
	}

	/**
	 * One triangular cap. Emitted as a 4-vertex quad with the hypotenuse's
	 * midpoint inserted third: the renderer silently drops a quad with two
	 * coinciding vertices, so a genuinely degenerate triangle would just vanish.
	 */
	private void emitCap(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                     Direction cap, Direction facing, BlockState camo, RandomSource random) {
		Layer layer = layerFor(blockView, pos, camo, cap, random);
		if (layer == null) return;

		float[][] verts = capVertices(cap, facing);
		if (verts == null) return;
		verts = ensureOutwardCcw(verts, cap);
		float[] mid = {
			(verts[1][0] + verts[2][0]) * 0.5f,
			(verts[1][1] + verts[2][1]) * 0.5f,
			(verts[1][2] + verts[2][2]) * 0.5f
		};
		float[][] quad = {verts[0], verts[1], mid, verts[2]};

		emitter.nominalFace(cap);
		emitter.cullFace(null);          // a triangle never covers a whole face
		for (int i = 0; i < 4; i++) {
			float vx = quad[i][0], vy = quad[i][1], vz = quad[i][2];
			emitter.pos(i, vx, vy, vz);
			float[] uv = vertexUv(vx, vz, cap);
			emitter.uv(i, layer.u0 + uv[0] * (layer.u1 - layer.u0),
			              layer.v0 + uv[1] * (layer.v1 - layer.v0));
			if (layer.color != -1) emitter.color(i, layer.color);
		}
		if (layer.chunkLayer != null) emitter.renderLayer(layer.chunkLayer);
		emitter.emit();
	}

	/** Sprite bounds + resolved tint colour for {@code cap}: the camo's matching
	 *  face if a camo is set, otherwise the frame sprite. */
	private Layer layerFor(BlockAndTintGetter blockView, BlockPos pos, BlockState camo,
	                       Direction cap, RandomSource random) {
		TextureAtlas atlas;
		SpriteFinder finder;
		try {
			atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
			finder = SpriteFinder.get(atlas);
		} catch (Throwable t) {
			return null;
		}

		if (camo != null && !camo.isAir()) {
			BlockStateModel camoModel = Minecraft.getInstance().getModelManager()
				.getBlockModelShaper().getBlockModel(camo);
			Layer l = sampleFace(camoModel, camo, blockView, pos, cap, random, finder);
			if (l != null) {
				int color = -1;
				if (l.color != -1) { // l.color carries the tint index here
					BlockColors blockColors = Minecraft.getInstance().getBlockColors();
					int rgb = blockColors.getColor(camo, blockView, pos, l.color);
					if (rgb != -1) color = 0xFF000000 | rgb;
				}
				return new Layer(l.u0, l.u1, l.v0, l.v1, color, l.chunkLayer);
			}
			// camo has no quad on this face -> fall through to the frame sprite
		}

		TextureAtlasSprite frame = atlas.getSprite(Identifier.fromNamespaceAndPath("framedblocks", "block/frame"));
		if (frame == null) return null;
		return new Layer(frame.getU0(), frame.getU1(), frame.getV0(), frame.getV1(), -1, null);
	}

	/** First quad of {@code model} on {@code face}; returns sprite bounds with the
	 *  raw tint INDEX stored in {@code color} (resolved by the caller). */
	private Layer sampleFace(BlockStateModel model, BlockState st, BlockAndTintGetter blockView, BlockPos pos,
	                         Direction face, RandomSource random, SpriteFinder finder) {
		MutableMesh scratch = Renderer.get().mutableMesh();
		model.emitQuads(scratch.emitter(), blockView, pos, st, random, d -> false);
		final Layer[] result = new Layer[1];
		scratch.forEachMutable(q -> {
			if (result[0] != null) return;
			Direction f = q.nominalFace() != null ? q.nominalFace() : q.lightFace();
			if (f != face) return;
			TextureAtlasSprite s = finder.find(q);
			result[0] = new Layer(s.getU0(), s.getU1(), s.getV0(), s.getV1(), q.tintIndex(), q.renderLayer());
		});
		return result[0];
	}

	// ---- geometry (ported from the 1.20.1 FramedVerticalSlopeBakedModel) ----

	/**
	 * The three corners of a cap, in local block coords. Index 0 is the right
	 * angle - the corner where the two solid walls meet - and 1/2 are the far
	 * ends of each wall, so the hypotenuse is the diagonal face.
	 *
	 * <p>The plane is nudged just outside the block on the cap's axis, the same
	 * trick the regular slope uses: sitting exactly on y=0 or y=1 it z-fights
	 * with the walls own edges and drops out at most viewing angles.
	 */
	private static float[][] capVertices(Direction cap, Direction facing) {
		// (x,z) of the right-angle corner, then the two leg ends.
		float[][] xz = switch (facing) {
			case NORTH -> new float[][]{{0, 0}, {1, 0}, {0, 1}};   // walls north + west
			case EAST  -> new float[][]{{1, 0}, {1, 1}, {0, 0}};   // walls north + east
			case SOUTH -> new float[][]{{1, 1}, {0, 1}, {1, 0}};   // walls south + east
			case WEST  -> new float[][]{{0, 1}, {0, 0}, {1, 1}};   // walls south + west
			default    -> null;
		};
		if (xz == null) return null;

		final float NUDGE = 0.001f;
		float y = (cap == Direction.UP) ? 1f + NUDGE : -NUDGE;
		return new float[][]{
			{xz[0][0], y, xz[0][1]},
			{xz[1][0], y, xz[1][1]},
			{xz[2][0], y, xz[2][1]}
		};
	}

	/** Flip the winding if the face normal ends up pointing into the block:
	 *  the renderer would back-face-cull it otherwise. */
	private static float[][] ensureOutwardCcw(float[][] verts, Direction face) {
		float ax = verts[1][0] - verts[0][0], ay = verts[1][1] - verts[0][1], az = verts[1][2] - verts[0][2];
		float bx = verts[2][0] - verts[0][0], by = verts[2][1] - verts[0][1], bz = verts[2][2] - verts[0][2];
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float dot = nx * face.getStepX() + ny * face.getStepY() + nz * face.getStepZ();
		return dot >= 0 ? verts : new float[][]{verts[0], verts[2], verts[1]};
	}

	/** The default UVs for a horizontal face: UP is (x, z), DOWN is (x, 1-z). */
	private static float[] vertexUv(float x, float z, Direction cap) {
		return (cap == Direction.UP) ? new float[]{x, z} : new float[]{x, 1f - z};
	}
}
