package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Pyramid renderer: square base (DOWN) plus four triangular faces meeting at
 * the apex (8,16,8). The JSON model is empty (particle only) — every face is
 * emitted here, sampling the camo's matching face sprite (or the frame sprite
 * when bare). Same emission technique as FramedSlopeModel's triangle sides.
 */
public class FramedPyramidModel extends FramedRetextureModel {
	private static final float[] APEX = {0.5f, 1f, 0.5f};
	private static final float[] NW = {0f, 0f, 0f};
	private static final float[] NE = {1f, 0f, 0f};
	private static final float[] SE = {1f, 0f, 1f};
	private static final float[] SW = {0f, 0f, 1f};

	private record Layer(float u0, float u1, float v0, float v1, int color, ChunkSectionLayer chunkLayer) {}

	public FramedPyramidModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		// The wrapped JSON model is empty (particle only) — nothing to retexture.
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		BlockState camo = (data instanceof BlockState bs) ? bs : null;

		emitFace(emitter, blockView, pos, Direction.DOWN,  new float[][]{NW, NE, SE, SW}, camo, random);
		emitFace(emitter, blockView, pos, Direction.NORTH, new float[][]{NW, NE, APEX},   camo, random);
		emitFace(emitter, blockView, pos, Direction.EAST,  new float[][]{NE, SE, APEX},   camo, random);
		emitFace(emitter, blockView, pos, Direction.SOUTH, new float[][]{SE, SW, APEX},   camo, random);
		emitFace(emitter, blockView, pos, Direction.WEST,  new float[][]{SW, NW, APEX},   camo, random);
	}

	private void emitFace(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      Direction side, float[][] verts, BlockState camo, RandomSource random) {
		Layer layer = layerFor(blockView, pos, camo, side, random);
		if (layer == null) return;

		float[][] quad;
		if (verts.length == 3) {
			float[][] v = ensureOutwardCcw(verts, side);
			float[] mid = {(v[1][0] + v[2][0]) * 0.5f, (v[1][1] + v[2][1]) * 0.5f, (v[1][2] + v[2][2]) * 0.5f};
			quad = new float[][]{v[0], v[1], mid, v[2]};
		} else {
			quad = ensureOutwardCcw(verts, side);
		}

		emitter.nominalFace(side);
		emitter.cullFace(null);
		for (int i = 0; i < 4; i++) {
			float vx = quad[i][0], vy = quad[i][1], vz = quad[i][2];
			emitter.pos(i, vx, vy, vz);
			float[] uv = vertexUv(vx, vy, vz, side);
			emitter.uv(i, layer.u0 + uv[0] * (layer.u1 - layer.u0),
			              layer.v0 + uv[1] * (layer.v1 - layer.v0));
			if (layer.color != -1) emitter.color(i, layer.color);
		}
		if (layer.chunkLayer != null) emitter.renderLayer(layer.chunkLayer);
		emitter.emit();
	}

	private Layer layerFor(BlockAndTintGetter blockView, BlockPos pos, BlockState camo,
	                       Direction side, RandomSource random) {
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
			Layer l = sampleFace(camoModel, camo, blockView, pos, side, random, finder);
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

	private static float[][] ensureOutwardCcw(float[][] verts, Direction side) {
		float ax = verts[1][0] - verts[0][0], ay = verts[1][1] - verts[0][1], az = verts[1][2] - verts[0][2];
		float bx = verts[2][0] - verts[0][0], by = verts[2][1] - verts[0][1], bz = verts[2][2] - verts[0][2];
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float dot = nx * side.getStepX() + ny * side.getStepY() + nz * side.getStepZ();
		if (dot >= 0) return verts;
		if (verts.length == 3) return new float[][] {verts[0], verts[2], verts[1]};
		return new float[][] {verts[3], verts[2], verts[1], verts[0]};
	}

	private static float[] vertexUv(float x, float y, float z, Direction side) {
		return switch (side) {
			case WEST  -> new float[]{z,      1f - y};
			case EAST  -> new float[]{1f - z, 1f - y};
			case NORTH -> new float[]{1f - x, 1f - y};
			case SOUTH -> new float[]{x,      1f - y};
			case DOWN  -> new float[]{x, z};
			default    -> new float[]{x, 1f - z};
		};
	}
}
