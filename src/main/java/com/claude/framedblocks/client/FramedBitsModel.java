package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
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
 * Renders a framed-bits block: each present octant (8x8x8 px sub-cube) emits its
 * EXPOSED faces (a face is skipped when the neighbouring octant is also present)
 * textured with that octant's camo via geometry projection -- so 8 identical
 * octants tile into a seamless full block. Bare (AIR) octants use the frame sprite.
 */
public class FramedBitsModel extends WrapperBlockStateModel {
	private static final Direction[] DIRS = Direction.values();

	private record Layer(float u0, float u1, float v0, float v1, int color, ChunkSectionLayer chunkLayer) {}

	public FramedBitsModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		if (!(data instanceof BlockState[] octants) || octants.length != 8) return;

		TextureAtlas atlas;
		SpriteFinder finder;
		try {
			atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
			finder = SpriteFinder.get(atlas);
		} catch (Throwable t) {
			return;
		}
		TextureAtlasSprite frame = atlas.getSprite(Identifier.fromNamespaceAndPath("framedblocks", "block/frame"));

		for (int i = 0; i < 8; i++) {
			BlockState camo = octants[i];
			if (camo == null) continue;
			int ox = i & 1, oy = (i >> 1) & 1, oz = (i >> 2) & 1;
			for (Direction d : DIRS) {
				int ni = neighbor(i, d);
				if (ni >= 0 && octants[ni] != null) continue; // interior face
				emitFace(emitter, blockView, pos, ox, oy, oz, d, camo, finder, frame, random);
			}
		}
	}

	private void emitFace(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      int ox, int oy, int oz, Direction d, BlockState camo,
	                      SpriteFinder finder, TextureAtlasSprite frame, RandomSource random) {
		Layer layer = layerFor(blockView, pos, camo, d, finder, frame);
		if (layer == null) return;

		float x0 = ox * 0.5f, x1 = x0 + 0.5f;
		float y0 = oy * 0.5f, y1 = y0 + 0.5f;
		float z0 = oz * 0.5f, z1 = z0 + 0.5f;
		float[][] v = faceVerts(d, x0, y0, z0, x1, y1, z1);

		emitter.nominalFace(d);
		emitter.cullFace(null);
		for (int k = 0; k < 4; k++) {
			float vx = v[k][0], vy = v[k][1], vz = v[k][2];
			emitter.pos(k, vx, vy, vz);
			float[] uv = faceUv(d, vx, vy, vz);
			emitter.uv(k, layer.u0 + uv[0] * (layer.u1 - layer.u0),
			              layer.v0 + uv[1] * (layer.v1 - layer.v0));
			if (layer.color != -1) emitter.color(k, layer.color);
		}
		if (layer.chunkLayer != null) emitter.renderLayer(layer.chunkLayer);
		emitter.emit();
	}

	/** Camo's face sprite + resolved tint, or the frame sprite for a bare octant. */
	private Layer layerFor(BlockAndTintGetter blockView, BlockPos pos, BlockState camo,
	                       Direction face, SpriteFinder finder, TextureAtlasSprite frame) {
		if (camo != null && !camo.isAir()) {
			BlockStateModel camoModel = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(camo);
			Layer l = sampleFace(camoModel, camo, blockView, pos, face, finder);
			if (l != null) {
				int color = -1;
				if (l.color != -1) {
					int rgb = Minecraft.getInstance().getBlockColors().getColor(camo, blockView, pos, l.color);
					if (rgb != -1) color = 0xFF000000 | rgb;
				}
				return new Layer(l.u0, l.u1, l.v0, l.v1, color, l.chunkLayer);
			}
		}
		if (frame == null) return null;
		return new Layer(frame.getU0(), frame.getU1(), frame.getV0(), frame.getV1(), -1, null);
	}

	private Layer sampleFace(BlockStateModel model, BlockState st, BlockAndTintGetter blockView, BlockPos pos,
	                         Direction face, SpriteFinder finder) {
		MutableMesh scratch = Renderer.get().mutableMesh();
		model.emitQuads(scratch.emitter(), blockView, pos, st, RandomSource.create(42L), d -> false);
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

	private static int neighbor(int i, Direction d) {
		int x = (i & 1) + d.getStepX();
		int y = ((i >> 1) & 1) + d.getStepY();
		int z = ((i >> 2) & 1) + d.getStepZ();
		if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return -1;
		return x | (y << 1) | (z << 2);
	}

	/** Octant box face corners, wound CCW viewed from OUTSIDE. */
	private static float[][] faceVerts(Direction d, float x0, float y0, float z0, float x1, float y1, float z1) {
		return switch (d) {
			case DOWN  -> new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}};
			case UP    -> new float[][]{{x0,y1,z1},{x1,y1,z1},{x1,y1,z0},{x0,y1,z0}};
			case NORTH -> new float[][]{{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}};
			case SOUTH -> new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
			case WEST  -> new float[][]{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}};
			case EAST  -> new float[][]{{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}};
		};
	}

	/** Vanilla default-face UV from a block-local vertex position (0..1). */
	private static float[] faceUv(Direction d, float x, float y, float z) {
		return switch (d) {
			case DOWN  -> new float[]{x, z};
			case UP    -> new float[]{x, 1f - z};
			case NORTH -> new float[]{1f - x, 1f - y};
			case SOUTH -> new float[]{x, 1f - y};
			case WEST  -> new float[]{z, 1f - y};
			case EAST  -> new float[]{1f - z, 1f - y};
		};
	}
}
