package com.claude.framedblocks.client;

import com.claude.framedblocks.block.FramedChestBlock;
import com.claude.framedblocks.block.entity.FramedChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the framed chest's animated LID (+ front latch) on 26.1.2's deferred
 * block-entity render pipeline (render state extract -> submit). The body is a
 * chunk model (camo via FramedRetextureModel); the lid + latch live here so they
 * can rotate open/closed (driven by the BE's ChestLidController).
 *
 * <p>Camo on the lid is sampled PER FACE (top face -> camo's top texture, sides ->
 * side textures, + biome tint) by emitting the camo model into a scratch mesh and
 * resolving each face's sprite — same approach as FramedRetextureModel — so the
 * lid matches the body. The latch is never camo'd (always the frame sprite).
 */
public class FramedChestRenderer implements BlockEntityRenderer<FramedChestBlockEntity, FramedChestRenderer.ChestState> {
	private static final float MAX_OPEN_DEGREES = 90f;

	private static final float LID_X0 = 1f,  LID_X1 = 15f;
	private static final float LID_Y0 = 10f, LID_Y1 = 14f;
	private static final float LID_Z0 = 1f,  LID_Z1 = 15f;
	private static final float HINGE_Y = LID_Y0 / 16f;
	private static final float HINGE_Z = LID_Z1 / 16f;

	private static final float LATCH_X0 = 7f,  LATCH_X1 = 9f;
	private static final float LATCH_Y0 = 7f,  LATCH_Y1 = 11f;
	private static final float LATCH_Z0 = 0f,  LATCH_Z1 = 1f;

	private static final Direction[] LID_FACES = {
		Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
	};
	private static final Direction[] LATCH_FACES = {
		Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
	};

	private final RandomSource random = RandomSource.create();

	public FramedChestRenderer(BlockEntityRendererProvider.Context ctx) {}

	/** One camo texture layer on one face (e.g. grass = dirt base + green overlay). */
	public record Layer(TextureAtlasSprite sprite, int tint) {}

	/** Per-frame extracted render state for one chest. */
	public static class ChestState extends BlockEntityRenderState {
		public float open;
		public Direction facing = Direction.NORTH;
		/** ALL camo (or frame) layers per face, indexed by Direction.ordinal(). */
		@SuppressWarnings("unchecked")
		public final List<Layer>[] faceLayers = new List[6];
		/** Frame sprite — the latch always uses this (never camo'd). */
		public TextureAtlasSprite frameSprite;
	}

	@Override
	public ChestState createRenderState() {
		return new ChestState();
	}

	@Override
	public void extractRenderState(FramedChestBlockEntity be, ChestState st, float partialTick,
	                               Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, st, crumbling);
		st.open = be.getOpenNess(partialTick);
		BlockState bs = be.getBlockState();
		st.facing = bs.hasProperty(FramedChestBlock.FACING) ? bs.getValue(FramedChestBlock.FACING) : Direction.NORTH;

		Minecraft mc = Minecraft.getInstance();
		var models = mc.getModelManager().getBlockModelShaper();
		st.frameSprite = models.getBlockModel(bs).particleIcon();

		// Default: no camo -> a single frame layer on every face.
		List<Layer> frameOnly = List.of(new Layer(st.frameSprite, -1));
		for (int i = 0; i < 6; i++) st.faceLayers[i] = frameOnly;

		BlockState camo = be.getCamo();
		// ClientLevel implements the client BlockAndTintGetter needed by emitQuads.
		if (camo == null || camo.isAir() || !(be.getLevel() instanceof BlockAndTintGetter view)) return;

		SpriteFinder finder;
		try {
			finder = SpriteFinder.get(mc.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS));
		} catch (Throwable t) {
			return; // keep the frame fallback
		}

		BlockStateModel camoModel = models.getBlockModel(camo);
		BlockColors blockColors = mc.getBlockColors();
		@SuppressWarnings("unchecked")
		final List<Layer>[] faces = new List[6];
		for (int i = 0; i < 6; i++) faces[i] = new ArrayList<>();

		// Record EVERY layer per face (base + overlays, in model order, e.g. grass =
		// opaque dirt base + tinted grass overlay) so the lid matches the body.
		MutableMesh scratch = Renderer.get().mutableMesh();
		random.setSeed(42L);
		camoModel.emitQuads(scratch.emitter(), view, be.getBlockPos(), camo, random, d -> false);
		scratch.forEachMutable(q -> {
			Direction f = q.nominalFace() != null ? q.nominalFace() : q.lightFace();
			if (f == null) return;
			TextureAtlasSprite s = finder.find(q);
			int tint = -1;
			int ti = q.tintIndex();
			if (ti != -1) {
				int rgb = blockColors.getColor(camo, view, be.getBlockPos(), ti);
				if (rgb != -1) tint = 0xFF000000 | rgb;
			}
			faces[f.ordinal()].add(new Layer(s, tint));
		});

		// Faces the camo model didn't emit borrow the first populated face.
		List<Layer> fallback = null;
		for (int i = 0; i < 6; i++) {
			if (!faces[i].isEmpty()) { fallback = faces[i]; break; }
		}
		if (fallback == null) return; // camo emitted nothing -> keep frame fallback
		for (int i = 0; i < 6; i++) {
			st.faceLayers[i] = faces[i].isEmpty() ? fallback : faces[i];
		}
	}

	@Override
	public void submit(ChestState st, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cam) {
		if (st.frameSprite == null) return;

		float g = 1f - st.open;
		g = 1f - g * g * g;
		float openDeg = g * MAX_OPEN_DEGREES;

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-facingYDegrees(st.facing)));
		poseStack.translate(-0.5, -0.5, -0.5);
		poseStack.translate(0, HINGE_Y, HINGE_Z);
		poseStack.mulPose(Axis.XP.rotationDegrees(openDeg));
		poseStack.translate(0, -HINGE_Y, -HINGE_Z);

		int light = st.lightCoords;
		Direction facing = st.facing;
		List<Layer>[] faceLayers = st.faceLayers;
		TextureAtlasSprite latchSprite = st.frameSprite;

		// Lid: translucent (frame's see-through interior + glass camos match the
		// body); camo sampled by WORLD face + world-position projection (same as the
		// body's FramedRetextureModel) so the lid is world-aligned and seamless with
		// the body regardless of the chest's facing. Every camo layer is drawn in
		// order (e.g. grass = dirt base then green overlay) so nothing is see-through.
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
			(pose, vc) -> {
				for (Direction f : LID_FACES) {
					Direction wf = worldFace(f, facing);
					for (Layer layer : faceLayers[wf.ordinal()]) {
						emitLidFace(pose, vc, f, facing, layer.sprite(), layer.tint(), light);
					}
				}
			});

		// Latch: never camo'd (always frame) and opaque (cutout) — a solid button.
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
			(pose, vc) -> {
				for (Direction f : LATCH_FACES) {
					emitFace(pose, vc, LATCH_X0, LATCH_Y0, LATCH_Z0, LATCH_X1, LATCH_Y1, LATCH_Z1, f, latchSprite, light, -1);
				}
			});
		poseStack.popPose();
	}

	private static float facingYDegrees(Direction facing) {
		return switch (facing) {
			case EAST  -> 90f;
			case SOUTH -> 180f;
			case WEST  -> 270f;
			default    -> 0f;
		};
	}

	private static void emitFace(PoseStack.Pose pose, VertexConsumer vc,
	                             float x0, float y0, float z0, float x1, float y1, float z1,
	                             Direction face, TextureAtlasSprite sprite, int light, int color) {
		if (sprite == null) return;
		float[][] verts = cuboidFaceVerts(x0, y0, z0, x1, y1, z1, face);
		float u0 = sprite.getU0(), spanU = sprite.getU1() - u0;
		float v0 = sprite.getV0(), spanV = sprite.getV1() - v0;
		int nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
		int r = (color >> 16) & 0xFF, gg = (color >> 8) & 0xFF, b = color & 0xFF; // color = -1 -> white
		for (int i = 0; i < 4; i++) {
			float vx = verts[i][0] / 16f, vy = verts[i][1] / 16f, vz = verts[i][2] / 16f;
			float[] uv = vertexUv(vx, vy, vz, face);
			vc.addVertex(pose, vx, vy, vz)
				.setColor(r, gg, b, 255)
				.setUv(u0 + uv[0] * spanU, v0 + uv[1] * spanV)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
		}
	}

	/** Emit a lid face world-aligned: sample the camo by the face's WORLD direction
	 *  (after the FACING rotation) and project the UV from the world position with
	 *  the same formula the body uses (FramedRetextureModel) -> seamless with the
	 *  body at any facing. We emit LOCAL positions (the pose matrix rotates them);
	 *  the world position is computed only to choose the sprite + UV. */
	private static void emitLidFace(PoseStack.Pose pose, VertexConsumer vc, Direction localFace, Direction facing,
	                                TextureAtlasSprite sprite, int color, int light) {
		if (sprite == null) return;
		Direction wf = worldFace(localFace, facing);
		float[][] verts = cuboidFaceVerts(LID_X0, LID_Y0, LID_Z0, LID_X1, LID_Y1, LID_Z1, localFace);
		float u0 = sprite.getU0(), spanU = sprite.getU1() - u0;
		float v0 = sprite.getV0(), spanV = sprite.getV1() - v0;
		int nx = localFace.getStepX(), ny = localFace.getStepY(), nz = localFace.getStepZ();
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF; // color = -1 -> white
		for (int i = 0; i < 4; i++) {
			float lx = verts[i][0], ly = verts[i][1], lz = verts[i][2];
			float[] w = rotatePos(lx, ly, lz, facing);
			float[] uv = projUv(w[0] / 16f, w[1] / 16f, w[2] / 16f, wf);
			vc.addVertex(pose, lx / 16f, ly / 16f, lz / 16f)
				.setColor(r, g, b, 255)
				.setUv(u0 + uv[0] * spanU, v0 + uv[1] * spanV)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
		}
	}

	private static final Direction[] H = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private static int hIndex(Direction d) {
		return switch (d) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
	}

	/** Local lid face -> its world direction after the FACING rotation (NORTH maps
	 *  to {@code facing}); UP/DOWN are unchanged by a Y-rotation. */
	private static Direction worldFace(Direction local, Direction facing) {
		if (local == Direction.UP || local == Direction.DOWN) return local;
		return H[(hIndex(local) + hIndex(facing)) % 4];
	}

	/** Rotate a block-coord position by the FACING (around the block centre),
	 *  matching the pose's {@code Axis.YP.rotationDegrees(-facingYDegrees)}. */
	private static float[] rotatePos(float x, float y, float z, Direction facing) {
		return switch (facing) {
			case EAST  -> new float[]{16 - z, y, x};
			case SOUTH -> new float[]{16 - x, y, 16 - z};
			case WEST  -> new float[]{z, y, 16 - x};
			default    -> new float[]{x, y, z}; // NORTH
		};
	}

	/** World-position camo projection — identical to FramedRetextureModel so the
	 *  lid matches the body. Coords are 0..1. */
	private static float[] projUv(float px, float py, float pz, Direction face) {
		return switch (face) {
			case UP    -> new float[]{px,      pz};
			case DOWN  -> new float[]{px,      1f - pz};
			case NORTH -> new float[]{1f - px, 1f - py};
			case SOUTH -> new float[]{px,      1f - py};
			case WEST  -> new float[]{pz,      1f - py};
			case EAST  -> new float[]{1f - pz, 1f - py};
		};
	}

	private static float[][] cuboidFaceVerts(float x0, float y0, float z0, float x1, float y1, float z1, Direction face) {
		return switch (face) {
			case NORTH -> new float[][] {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}};
			case SOUTH -> new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
			case EAST  -> new float[][] {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}};
			case WEST  -> new float[][] {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
			case UP    -> new float[][] {{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}};
			case DOWN  -> new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
		};
	}

	private static float[] vertexUv(float x, float y, float z, Direction side) {
		return switch (side) {
			case WEST  -> new float[]{z,      1f - y};
			case EAST  -> new float[]{1f - z, 1f - y};
			case NORTH -> new float[]{1f - x, 1f - y};
			case SOUTH -> new float[]{x,      1f - y};
			case UP    -> new float[]{x,      z};
			case DOWN  -> new float[]{x,      1f - z};
		};
	}
}
