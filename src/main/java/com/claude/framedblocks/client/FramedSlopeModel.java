package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
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
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Slope renderer for 26.1.2. The JSON model (tilted plane + back wall + floor)
 * is retextured with the camo by {@link FramedRetextureModel} (super); this
 * class adds the two triangular SIDES that JSON models can't express, emitting
 * them as degenerate-free 4-vertex quads (hypotenuse midpoint as the 3rd vertex)
 * with the camo's matching side sprite, or the frame sprite when bare.
 */
public class FramedSlopeModel extends FramedRetextureModel {
	private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	private record Layer(float u0, float u1, float v0, float v1, int color, ChunkSectionLayer chunkLayer) {}

	public FramedSlopeModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		// 1. Tilted plane + back wall + floor, retextured with the camo.
		super.emitQuads(emitter, blockView, pos, state, random, cull);

		// 2. The two triangular sides.
		Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
		// half=top is the wedge mirrored over, so its sides are the same triangles
		// with the height flipped. The JSON model is flipped by the blockstate's
		// x:180; only these two emitted sides have to follow by hand.
		boolean top = state.hasProperty(HALF) && state.getValue(HALF) == Half.TOP;
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		BlockState camo = (data instanceof BlockState bs) ? bs : null;

		Direction sideA, sideB;
		if (facing.getAxis() == Direction.Axis.Z) { sideA = Direction.WEST; sideB = Direction.EAST; }
		else { sideA = Direction.NORTH; sideB = Direction.SOUTH; }

		emitTriangle(emitter, blockView, pos, sideA, facing, top, camo, random);
		emitTriangle(emitter, blockView, pos, sideB, facing, top, camo, random);
	}

	/** Bake the two triangle sides as item-icon quads (base facing NORTH, frame
	 *  sprite). Used by the item-model wrapper so the inventory icon shows the
	 *  sides — items render via a separate baked path, not emitQuads. */
	public static List<BakedQuad> bakeIconTriangles() {
		List<BakedQuad> out = new ArrayList<>();
		TextureAtlas atlas;
		try {
			atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		} catch (Throwable t) {
			return out;
		}
		TextureAtlasSprite frame = atlas.getSprite(Identifier.fromNamespaceAndPath("framedblocks", "block/frame"));
		if (frame == null) return out;
		// Base facing = NORTH -> triangle sides are WEST and EAST.
		bakeIconTriangle(out, Direction.WEST, Direction.NORTH, frame);
		bakeIconTriangle(out, Direction.EAST, Direction.NORTH, frame);
		return out;
	}

	private static void bakeIconTriangle(List<BakedQuad> out, Direction side, Direction facing, TextureAtlasSprite frame) {
		float[][] verts = triangleVertices(side, facing, false);
		if (verts == null) return;
		verts = ensureOutwardCcw(verts, side);
		float[] mid = {
			(verts[1][0] + verts[2][0]) * 0.5f,
			(verts[1][1] + verts[2][1]) * 0.5f,
			(verts[1][2] + verts[2][2]) * 0.5f
		};
		float[][] quad = {verts[0], verts[1], mid, verts[2]};
		float u0 = frame.getU0(), u1 = frame.getU1(), v0 = frame.getV0(), v1 = frame.getV1();

		MutableMesh scratch = Renderer.get().mutableMesh();
		QuadEmitter e = scratch.emitter();
		e.nominalFace(side);
		e.cullFace(null);
		for (int i = 0; i < 4; i++) {
			float vx = quad[i][0], vy = quad[i][1], vz = quad[i][2];
			e.pos(i, vx, vy, vz);
			float[] uv = vertexUv(vx, vy, vz, side);
			e.uv(i, u0 + uv[0] * (u1 - u0), v0 + uv[1] * (v1 - v0));
		}
		e.emit();
		scratch.forEachMutable(q -> out.add(q.toBakedQuad(frame)));
	}

	private void emitTriangle(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                          Direction side, Direction facing, boolean top, BlockState camo, RandomSource random) {
		Layer layer = layerFor(blockView, pos, camo, side, random);
		if (layer == null) return;

		float[][] verts = triangleVertices(side, facing, top);
		if (verts == null) return;
		verts = ensureOutwardCcw(verts, side);
		float[] mid = {
			(verts[1][0] + verts[2][0]) * 0.5f,
			(verts[1][1] + verts[2][1]) * 0.5f,
			(verts[1][2] + verts[2][2]) * 0.5f
		};
		float[][] quad = {verts[0], verts[1], mid, verts[2]};

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

	/** Sprite bounds + resolved tint colour for {@code side}: the camo's matching
	 *  face if a camo is set, otherwise the frame sprite. */
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

	// ---- geometry (ported verbatim from the 1.20.1 FramedSlopeBakedModel) ----

	/** The flat side of the wedge. {@code top} mirrors it vertically: the right
	 *  angle moves from the floor to the ceiling, so the hypotenuse runs the other
	 *  way and the piece hangs instead of standing. */
	private static float[][] triangleVertices(Direction side, Direction facing, boolean top) {
		float highCoord, lowCoord;
		if (facing == Direction.NORTH || facing == Direction.WEST) { highCoord = 0f; lowCoord = 1f; }
		else if (facing == Direction.SOUTH || facing == Direction.EAST) { highCoord = 1f; lowCoord = 0f; }
		else return null;

		// The corner the right angle sits in, and the far end of the vertical edge.
		float flat = top ? 1f : 0f;
		float peak = top ? 0f : 1f;

		final float NUDGE = 0.001f;
		if (facing.getAxis() == Direction.Axis.Z) {
			float sideX = (side == Direction.WEST) ? -NUDGE : 1f + NUDGE;
			return new float[][] {
				{sideX, flat, highCoord},
				{sideX, peak, highCoord},
				{sideX, flat, lowCoord}
			};
		} else {
			float sideZ = (side == Direction.NORTH) ? -NUDGE : 1f + NUDGE;
			return new float[][] {
				{highCoord, flat, sideZ},
				{highCoord, peak, sideZ},
				{lowCoord, flat, sideZ}
			};
		}
	}

	private static float[][] ensureOutwardCcw(float[][] verts, Direction side) {
		float ax = verts[1][0] - verts[0][0], ay = verts[1][1] - verts[0][1], az = verts[1][2] - verts[0][2];
		float bx = verts[2][0] - verts[0][0], by = verts[2][1] - verts[0][1], bz = verts[2][2] - verts[0][2];
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float dot = nx * side.getStepX() + ny * side.getStepY() + nz * side.getStepZ();
		if (dot >= 0) return verts;
		return new float[][] {verts[0], verts[2], verts[1]};
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
