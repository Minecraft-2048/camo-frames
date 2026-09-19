package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Inner-corner slope renderer for 26.1.2. JSON back walls + floor are retextured
 * by {@link FramedRetextureModel} (super); this adds the two perpendicular
 * inclined plane halves and the two V-opening closure triangles. Base layout is
 * facing=WEST; each triangle is rotated by n quarter-turns CW to the world facing
 * (matching the blockstate y-rotation table west=0/north=90/east=180/south=270).
 */
public class FramedSlopeInnerCornerModel extends FramedRetextureModel {
	private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	// Triangles in normalised 0..1 cube coords (base facing=WEST). {face-for-UV}
	private static final float[][] BASE_PLANE_A        = {{0,1,0},{0,1,1},{1,0,1}}; // EAST
	private static final float[][] BASE_PLANE_B        = {{0,1,0},{1,0,1},{1,1,0}}; // SOUTH
	private static final float[][] BASE_SOUTH_CLOSURE  = {{0,0,1},{1,0,1},{0,1,1}}; // SOUTH
	private static final float[][] BASE_EAST_CLOSURE   = {{1,0,0},{1,1,0},{1,0,1}}; // EAST

	private record Layer(float u0, float u1, float v0, float v1, int color, ChunkSectionLayer chunkLayer) {}

	public FramedSlopeInnerCornerModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		super.emitQuads(emitter, blockView, pos, state, random, cull);

		Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
		int n = rotationCount(facing);
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		BlockState camo = (data instanceof BlockState bs) ? bs : null;

		emitTri(emitter, blockView, pos, BASE_PLANE_A,       Direction.EAST,  n, camo, random);
		emitTri(emitter, blockView, pos, BASE_PLANE_B,       Direction.SOUTH, n, camo, random);
		emitTri(emitter, blockView, pos, BASE_SOUTH_CLOSURE, Direction.SOUTH, n, camo, random);
		emitTri(emitter, blockView, pos, BASE_EAST_CLOSURE,  Direction.EAST,  n, camo, random);
	}

	/** Bake the 4 icon triangles (base facing WEST, frame sprite) for the item
	 *  model wrapper — items render via a separate baked path, not emitQuads. */
	public static List<BakedQuad> bakeIconTriangles() {
		List<BakedQuad> out = new ArrayList<>();
		net.minecraft.client.renderer.texture.TextureAtlas atlas;
		try {
			atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		} catch (Throwable t) {
			return out;
		}
		TextureAtlasSprite frame = atlas.getSprite(Identifier.fromNamespaceAndPath("framedblocks", "block/frame"));
		if (frame == null) return out;
		// Base facing = WEST (n=0): the 4 triangles in their base orientation.
		bakeIconTriangle(out, BASE_PLANE_A,       Direction.EAST,  frame);
		bakeIconTriangle(out, BASE_PLANE_B,       Direction.SOUTH, frame);
		bakeIconTriangle(out, BASE_SOUTH_CLOSURE, Direction.SOUTH, frame);
		bakeIconTriangle(out, BASE_EAST_CLOSURE,  Direction.EAST,  frame);
		return out;
	}

	private static void bakeIconTriangle(List<BakedQuad> out, float[][] baseVerts, Direction face, TextureAtlasSprite frame) {
		float[][] verts = ensureOutwardCcw(rotateTriangle(baseVerts, 0), face);
		float[] mid = {
			(verts[1][0] + verts[2][0]) * 0.5f,
			(verts[1][1] + verts[2][1]) * 0.5f,
			(verts[1][2] + verts[2][2]) * 0.5f
		};
		float[][] quad = {verts[0], verts[1], mid, verts[2]};
		float u0 = frame.getU0(), u1 = frame.getU1(), v0 = frame.getV0(), v1 = frame.getV1();

		MutableMesh scratch = Renderer.get().mutableMesh();
		QuadEmitter e = scratch.emitter();
		e.nominalFace(face);
		e.cullFace(null);
		for (int i = 0; i < 4; i++) {
			float vx = quad[i][0], vy = quad[i][1], vz = quad[i][2];
			e.pos(i, vx, vy, vz);
			float[] uv = vertexUv(vx, vy, vz, face);
			e.uv(i, u0 + uv[0] * (u1 - u0), v0 + uv[1] * (v1 - v0));
		}
		e.emit();
		scratch.forEachMutable(q -> out.add(q.toBakedQuad(frame)));
	}


	private void emitTri(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                     float[][] baseVerts, Direction baseFace, int n, BlockState camo, RandomSource random) {
		float[][] world = rotateTriangle(baseVerts, n);
		Direction worldFace = rotateDirection(baseFace, n);
		world = ensureOutwardCcw(world, worldFace);

		Layer layer = layerFor(blockView, pos, camo, worldFace, random);
		if (layer == null) return;

		float[] mid = {
			(world[1][0] + world[2][0]) * 0.5f,
			(world[1][1] + world[2][1]) * 0.5f,
			(world[1][2] + world[2][2]) * 0.5f
		};
		float[][] quad = {world[0], world[1], mid, world[2]};

		emitter.nominalFace(worldFace);
		emitter.cullFace(null);
		for (int i = 0; i < 4; i++) {
			float vx = quad[i][0], vy = quad[i][1], vz = quad[i][2];
			emitter.pos(i, vx, vy, vz);
			float[] uv = vertexUv(vx, vy, vz, worldFace);
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
			BlockStateModel camoModel = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(camo);
			Layer l = sampleFace(camoModel, camo, blockView, pos, side, random, finder);
			if (l != null) {
				int color = -1;
				if (l.color != -1) {
					int rgb = Minecraft.getInstance().getBlockColors().getColor(camo, blockView, pos, l.color);
					if (rgb != -1) color = 0xFF000000 | rgb;
				}
				return new Layer(l.u0, l.u1, l.v0, l.v1, color, l.chunkLayer);
			}
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

	// ---- geometry (ported from the 1.20.1 FramedSlopeInnerCornerBakedModel) ----

	private static int rotationCount(Direction facing) {
		return switch (facing) {
			case WEST  -> 0;
			case NORTH -> 1;
			case EAST  -> 2;
			case SOUTH -> 3;
			default    -> 0;
		};
	}

	private static float[][] rotateTriangle(float[][] base, int n) {
		float[][] out = new float[base.length][3];
		for (int i = 0; i < base.length; i++) out[i] = rotatePoint(base[i], n);
		return out;
	}

	/** Apply n times: (x, z) -> (1-z, x). +90° CW viewed from above. */
	private static float[] rotatePoint(float[] p, int n) {
		float x = p[0], y = p[1], z = p[2];
		for (int i = 0; i < n; i++) {
			float nx = 1f - z, nz = x;
			x = nx; z = nz;
		}
		return new float[]{x, y, z};
	}

	private static Direction rotateDirection(Direction d, int n) {
		for (int i = 0; i < n; i++) d = d.getClockWise();
		return d;
	}

	private static float[][] ensureOutwardCcw(float[][] verts, Direction side) {
		float ax = verts[1][0] - verts[0][0], ay = verts[1][1] - verts[0][1], az = verts[1][2] - verts[0][2];
		float bx = verts[2][0] - verts[0][0], by = verts[2][1] - verts[0][1], bz = verts[2][2] - verts[0][2];
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float dot = nx * side.getStepX() + ny * side.getStepY() + nz * side.getStepZ();
		if (dot >= 0) return verts;
		return new float[][]{verts[0], verts[2], verts[1]};
	}

	private static float[] vertexUv(float x, float y, float z, Direction side) {
		return switch (side) {
			case WEST  -> new float[]{z,     1f - y};
			case EAST  -> new float[]{1f - z, 1f - y};
			case NORTH -> new float[]{1f - x, 1f - y};
			case SOUTH -> new float[]{x,     1f - y};
			default    -> new float[]{0f, 0f};
		};
	}
}
