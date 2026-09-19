package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Camo rendering for ALL framed blocks: keeps the FRAME's geometry (the block's
 * shape) and paints the camo's texture onto it — the 26.1.2 port of the 1.20.1
 * FramedBakedModel.retexture. Used for the full cube too, so a slab/stairs camo
 * stretches its texture over the whole frame shape instead of rendering its own
 * (smaller) geometry.
 *
 * Per face, every camo LAYER is recorded (base + overlays, in model order — e.g.
 * grass = opaque dirt base + tinted grass overlay) with its sprite bounds, tint
 * and chunk render layer (so stained-glass camo stays translucent). The frame
 * quads are then emitted once per layer, projecting each vertex's POSITION onto
 * that layer's camo sprite (vanilla default-face-UV convention) rather than
 * reusing the frame's own UVs.
 *
 * Geometry projection makes the cut automatic — a slab side spans y∈[0,0.5] so
 * relV = 1-y ∈ [0.5,1] = the lower half of the camo — and dodges per-quad
 * frame-sprite lookups (unreliable for faces using only a sprite sub-region).
 * Faces where the camo model has no quad (e.g. slab camo's missing upper sides)
 * fall back to the first face that has one, so no face is ever left as a hole.
 */
public class FramedRetextureModel extends WrapperBlockStateModel {

	/** One camo texture layer on one face. */
	private record Layer(float u0, float u1, float v0, float v1, int tint, ChunkSectionLayer chunkLayer) {}

	public FramedRetextureModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		if (!(data instanceof BlockState camo) || camo.isAir()) {
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			return;
		}

		SpriteFinder spriteFinder;
		try {
			spriteFinder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
		} catch (Throwable t) {
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			return;
		}
		BlockStateModel camoModel = Minecraft.getInstance().getModelManager()
			.getBlockModelShaper().getBlockModel(camo);

		// 1. Emit the camo model into a scratch mesh; record ALL layers per face.
		@SuppressWarnings("unchecked")
		final List<Layer>[] camoLayers = new List[6];
		for (int i = 0; i < 6; i++) camoLayers[i] = new ArrayList<>();

		MutableMesh scratch = Renderer.get().mutableMesh();
		camoModel.emitQuads(scratch.emitter(), blockView, pos, camo, random, d -> false);
		scratch.forEachMutable(q -> {
			Direction f = faceOf(q);
			if (f == null) return;
			TextureAtlasSprite s = spriteFinder.find(q);
			camoLayers[f.ordinal()].add(new Layer(
				s.getU0(), s.getU1(), s.getV0(), s.getV1(), q.tintIndex(), q.renderLayer()));
		});

		// Faces the camo model leaves empty (slab camo has no upper-side quads)
		// borrow the layers of the first populated face so the frame shape stays
		// fully covered.
		List<Layer> fallback = null;
		for (int i = 0; i < 6; i++) {
			if (!camoLayers[i].isEmpty()) { fallback = camoLayers[i]; break; }
		}
		if (fallback == null) {
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			return;
		}
		int maxLayers = 0;
		for (int i = 0; i < 6; i++) {
			if (camoLayers[i].isEmpty()) camoLayers[i] = fallback;
			maxLayers = Math.max(maxLayers, camoLayers[i].size());
		}

		// 2. Emit the frame quads once per layer; project each vertex onto the
		//    layer's camo sprite. Faces lacking that layer are dropped.
		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		for (int l = 0; l < maxLayers; l++) {
			final int layerIndex = l;
			emitter.pushTransform(q -> {
				Direction f = faceOf(q);
				Direction cf = camoFace(state, f);
				int i = (cf != null) ? cf.ordinal() : -1;
				if (i < 0 || layerIndex >= camoLayers[i].size()) return false;
				Layer layer = camoLayers[i].get(layerIndex);
				for (int v = 0; v < 4; v++) {
					float px = q.x(v), py = q.y(v), pz = q.z(v);
					float relU, relV;
					switch (f) {
						case UP    -> { relU = px;        relV = pz;        }
						case DOWN  -> { relU = px;        relV = 1f - pz;   }
						case NORTH -> { relU = 1f - px;   relV = 1f - py;   }
						case SOUTH -> { relU = px;        relV = 1f - py;   }
						case WEST  -> { relU = pz;        relV = 1f - py;   }
						case EAST  -> { relU = 1f - pz;   relV = 1f - py;   }
						default    -> { relU = 0f;        relV = 0f;        }
					}
					q.uv(v, layer.u0 + relU * (layer.u1 - layer.u0),
					        layer.v0 + relV * (layer.v1 - layer.v0));
				}
				if (layer.chunkLayer != null) {
					q.renderLayer(layer.chunkLayer);
				}
				if (layer.tint != -1) {
					int rgb = blockColors.getColor(camo, blockView, pos, layer.tint);
					if (rgb != -1) {
						int argb = 0xFF000000 | rgb; for (int v = 0; v < 4; v++) q.color(v, multiplyArgb(q.color(v), argb));
					}
					q.tintIndex(-1);
				}
				return true;
			});
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			emitter.popTransform();
		}
	}

	/** Channel-wise multiply of two packed ARGB colours (1.21.8 has no
	 *  MutableQuadView.multiplyColor like 26.1.2 does). */
	private static int multiplyArgb(int a, int b) {
		int al = ((a >>> 24) * (b >>> 24) + 127) / 255;
		int rl = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) + 127) / 255;
		int gl = (((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) + 127) / 255;
		int bl = ((a & 0xFF) * (b & 0xFF) + 127) / 255;
		return (al << 24) | (rl << 16) | (gl << 8) | bl;
	}

	/**
	 * Which face of the CAMO model a world face should sample.
	 *
	 * <p>Identity for almost every block, but a pillar's model is turned by its
	 * blockstate (axis=x adds x:90 y:90, axis=z adds x:90), so the camo must be
	 * turned the same way — undo that rotation and a log camo lands its end grain
	 * on the pillar's ends, like a real log laid on its side. Sampling by world
	 * face left the top texture stuck on top whatever the axis.
	 */
	private static Direction camoFace(BlockState state, Direction worldFace) {
		if (worldFace == null || !state.hasProperty(RotatedPillarBlock.AXIS)) return worldFace;
		return switch (state.getValue(RotatedPillarBlock.AXIS)) {
			case X -> switch (worldFace) {
				case EAST  -> Direction.UP;
				case WEST  -> Direction.DOWN;
				case DOWN  -> Direction.NORTH;
				case UP    -> Direction.SOUTH;
				case SOUTH -> Direction.EAST;
				case NORTH -> Direction.WEST;
			};
			case Z -> switch (worldFace) {
				case NORTH -> Direction.UP;
				case SOUTH -> Direction.DOWN;
				case DOWN  -> Direction.NORTH;
				case UP    -> Direction.SOUTH;
				default    -> worldFace;
			};
			default -> worldFace;
		};
	}

	private static Direction faceOf(QuadView q) {
		Direction f = q.nominalFace();
		return f != null ? f : q.lightFace();
	}
}
