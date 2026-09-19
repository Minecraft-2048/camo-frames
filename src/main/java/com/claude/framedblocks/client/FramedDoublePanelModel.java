package com.claude.framedblocks.client;

import com.claude.framedblocks.block.FramedDoublePanelBlock;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Renders the Framed Double Panel: the frame model is two boxes split LEFT/RIGHT
 * along X. The left half is painted with camo[0], the right half with camo[1];
 * a bare (air) half keeps the frame texture (with the 2px seam line baked into
 * frame_panel). Reuses FramedRetextureModel's projection per half.
 */
public class FramedDoublePanelModel extends WrapperBlockStateModel {

	private record Layer(float u0, float u1, float v0, float v1, int tint, ChunkSectionLayer chunkLayer) {}

	public FramedDoublePanelModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		Object data = (blockView instanceof FabricBlockView g) ? g.getBlockEntityRenderData(pos) : null;
		if (!(data instanceof BlockState[] camos) || camos.length != 2) {
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			return;
		}
		Direction facing = state.hasProperty(FramedDoublePanelBlock.FACING)
			? state.getValue(FramedDoublePanelBlock.FACING) : Direction.NORTH;
		emitPanel(emitter, blockView, pos, state, random, cull, facing, true, camos[0]);   // left
		emitPanel(emitter, blockView, pos, state, random, cull, facing, false, camos[1]);  // right
	}

	private void emitPanel(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                       BlockState state, RandomSource random, Predicate<Direction> cull,
	                       Direction facing, boolean leftPanel, BlockState camo) {
		Predicate<QuadView> belongs = q -> isLeftQuad(q, facing) == leftPanel;

		if (camo == null || camo.isAir()) {
			emitter.pushTransform(belongs::test);
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			emitter.popTransform();
			return;
		}

		SpriteFinder spriteFinder;
		try {
			spriteFinder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
		} catch (Throwable t) {
			emitter.pushTransform(belongs::test);
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			emitter.popTransform();
			return;
		}
		BlockStateModel camoModel = Minecraft.getInstance().getModelManager()
			.getBlockModelShaper().getBlockModel(camo);

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

		List<Layer> fallback = null;
		for (int i = 0; i < 6; i++) {
			if (!camoLayers[i].isEmpty()) { fallback = camoLayers[i]; break; }
		}
		if (fallback == null) {
			emitter.pushTransform(belongs::test);
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			emitter.popTransform();
			return;
		}
		int maxLayers = 0;
		for (int i = 0; i < 6; i++) {
			if (camoLayers[i].isEmpty()) camoLayers[i] = fallback;
			maxLayers = Math.max(maxLayers, camoLayers[i].size());
		}

		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		for (int l = 0; l < maxLayers; l++) {
			final int layerIndex = l;
			emitter.pushTransform(q -> {
				if (!belongs.test(q)) return false;
				Direction f = faceOf(q);
				int i = (f != null) ? f.ordinal() : -1;
				if (i < 0 || layerIndex >= camoLayers[i].size()) return false;
				Layer layer = camoLayers[i].get(layerIndex);
				for (int v = 0; v < 4; v++) {
					float px = q.x(v), py = q.y(v), pz = q.z(v);
					float relU, relV;
					switch (f) {
						case DOWN  -> { relU = px;        relV = pz;        }
						case UP    -> { relU = px;        relV = 1f - pz;   }
						case NORTH -> { relU = 1f - px;   relV = 1f - py;   }
						case SOUTH -> { relU = px;        relV = 1f - py;   }
						case WEST  -> { relU = pz;        relV = 1f - py;   }
						case EAST  -> { relU = 1f - pz;   relV = 1f - py;   }
						default    -> { relU = 0f;        relV = 0f;        }
					}
					q.uv(v, layer.u0 + relU * (layer.u1 - layer.u0),
					        layer.v0 + relV * (layer.v1 - layer.v0));
				}
				if (layer.chunkLayer != null) q.renderLayer(layer.chunkLayer);
				if (layer.tint != -1) {
					int rgb = blockColors.getColor(camo, blockView, pos, layer.tint);
					if (rgb != -1) { int argb = 0xFF000000 | rgb; for (int cv = 0; cv < 4; cv++) q.color(cv, multiplyArgb(q.color(cv), argb)); }
					q.tintIndex(-1);
				}
				return true;
			});
			super.emitQuads(emitter, blockView, pos, state, random, cull);
			emitter.popTransform();
		}
	}

	/** Channel-wise multiply of two packed ARGB colours (1.21.8 has no
	 *  MutableQuadView.multiplyColor). */
	private static int multiplyArgb(int a, int b) {
		int al = ((a >>> 24) * (b >>> 24) + 127) / 255;
		int rl = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) + 127) / 255;
		int gl = (((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) + 127) / 255;
		int bl = ((a & 0xFF) * (b & 0xFF) + 127) / 255;
		return (al << 24) | (rl << 16) | (gl << 8) | bl;
	}

	/** Which half a frame quad belongs to, per the block's facing rotation. */
	private static boolean isLeftQuad(QuadView q, Direction facing) {
		double cx = 0, cz = 0;
		for (int v = 0; v < 4; v++) { cx += q.x(v); cz += q.z(v); }
		return com.claude.framedblocks.block.FramedDoublePanelBlock.isLeft(facing, cx / 4.0, cz / 4.0);
	}

	private static Direction faceOf(QuadView q) {
		Direction f = q.nominalFace();
		return f != null ? f : q.lightFace();
	}
}
