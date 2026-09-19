package com.claude.framedblocks.client;

import com.claude.framedblocks.block.entity.FramedSignBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Framed sign renderer.
 *
 * <p>BARE sign: vanilla end to end, with {@link #getSignMaterial} pointed at the
 * 64x32 framed board texture.
 *
 * <p>CAMO sign: vanilla still drives the flow (placement, rotation, text) and
 * only the BOARD drawing is replaced. {@link #renderSign} is the seam vanilla
 * leaves open: the board and the stick are emitted as custom quads carrying the
 * camo sprites, so front, back and side edges take the side texture of the camo,
 * the top edge its top texture and the bottom edge its bottom one, and a framed
 * sign matches the framed blocks around it. The text stays 100% vanilla.
 */
public class FramedSignRenderer extends SignRenderer {
	private static final Material FRAME_BOARD = new Material(TextureAtlas.LOCATION_BLOCKS,
		Identifier.fromNamespaceAndPath("framedblocks", "block/framed_sign"));

	// Vanilla sign model, in model pixels /16: board x[-12,12] y[-14,-2] z[-1,1];
	// stick x[-1,1] y[-2,12] z[-1,1]. Model y is INVERTED by the (s,-s,-s) scale,
	// so y=-14 is the board top in the world and y=12 is the ground.
	private static final float BX0 = -12f / 16f, BX1 = 12f / 16f;
	private static final float BY0 = -14f / 16f, BY1 = -2f / 16f;
	private static final float BZ0 = -1f / 16f,  BZ1 = 1f / 16f;
	private static final float SX0 = -1f / 16f,  SX1 = 1f / 16f;
	private static final float SY0 = -2f / 16f,  SY1 = 12f / 16f;
	private static final float SZ0 = -1f / 16f,  SZ1 = 1f / 16f;

	private record Layer(TextureAtlasSprite sprite, int tint) {}

	private final RandomSource random = RandomSource.create();

	/**
	 * What a framed sign needs on top of a vanilla one. The board is drawn from
	 * the applied block, and the drawing happens long after the block entity is
	 * out of reach, so the camo travels on the render state like everything else.
	 */
	public static class FramedSignState extends SignRenderState {
		BlockState camo = null;
		BlockState signState = null;
		BlockPos signPos = null;
		BlockAndTintGetter signLevel = null;
	}

	/** The sign currently being submitted: {@link #submitSign} is handed no state,
	 *  and submission is single-threaded. */
	private FramedSignState current = null;
	private BlockState camo = null;
	private BlockState signState = null;
	private BlockPos signPos = null;
	private BlockAndTintGetter signLevel = null;

	public FramedSignRenderer(BlockEntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public SignRenderState createRenderState() {
		return new FramedSignState();
	}

	@Override
	public void extractRenderState(SignBlockEntity be, SignRenderState st, float partialTick,
	                               Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		super.extractRenderState(be, st, partialTick, cameraPos, crumbling);
		if (st instanceof FramedSignState f) {
			if (be instanceof FramedSignBlockEntity framed) {
				f.camo = framed.getCamo();
				f.signState = framed.getBlockState();
				f.signPos = framed.getBlockPos();
				f.signLevel = framed.getLevel();
			} else {
				f.camo = null;
				f.signState = null;
				f.signPos = null;
				f.signLevel = null;
			}
		}
	}

	@Override
	public void submit(SignRenderState st, PoseStack poseStack, SubmitNodeCollector collector,
	                   CameraRenderState cameraState) {
		current = (st instanceof FramedSignState f) ? f : null;
		camo = current == null ? null : current.camo;
		signState = current == null ? null : current.signState;
		signPos = current == null ? null : current.signPos;
		signLevel = current == null ? null : current.signLevel;
		try {
			super.submit(st, poseStack, collector, cameraState);
		} finally {
			current = null;
		}
	}

	/** Bare board texture. The camo path never reaches this. */
	@Override
	protected Material getSignMaterial(WoodType woodType) {
		return FRAME_BOARD;
	}

	@Override
	protected void submitSign(PoseStack poseStack, int light, WoodType woodType, Model.Simple model,
	                          ModelFeatureRenderer.CrumblingOverlay crumbling, SubmitNodeCollector collector) {
		if (camo == null || camo.isAir()) {
			super.submitSign(poseStack, light, woodType, model, crumbling, collector);
			return;
		}

		Map<Direction, Layer> layers = camoLayers();
		Layer side = layers.get(Direction.NORTH);
		Layer top = layers.get(Direction.UP);
		Layer bottom = layers.get(Direction.DOWN);
		if (side == null || top == null || bottom == null) {
			super.submitSign(poseStack, light, woodType, model, crumbling, collector);
			return;
		}

		poseStack.pushPose();
		// The same scale vanilla applies before drawing the sign model.
		float s = getSignModelRenderScale();
		poseStack.scale(s, -s, -s);

		boolean standing = signState != null && signState.getBlock() instanceof StandingSignBlock;
		collector.submitCustomGeometry(poseStack,
			RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
			(pose, vc) -> {
				emitBoard(pose, vc, side, top, bottom, light, OverlayTexture.NO_OVERLAY);
				if (standing) {
					emitStick(pose, vc, side, bottom, light, OverlayTexture.NO_OVERLAY);
				}
			});
		poseStack.popPose();
	}

	/** The sprite (and tint) of the camo per face, from its own baked model. */
	private Map<Direction, Layer> camoLayers() {
		Map<Direction, Layer> out = new EnumMap<>(Direction.class);
		Minecraft mc = Minecraft.getInstance();
		TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		if (atlas == null || signLevel == null || signPos == null) return out;

		SpriteFinder finder = SpriteFinder.get(atlas);
		BlockStateModel camoModel = mc.getModelManager().getBlockModelShaper().getBlockModel(camo);
		MutableMesh scratch = Renderer.get().mutableMesh();
		random.setSeed(42L);
		camoModel.emitQuads(scratch.emitter(), signLevel, signPos, camo, random, d -> false);

		scratch.forEachMutable(q -> {
			Direction f = q.nominalFace() != null ? q.nominalFace() : q.lightFace();
			if (f == null || out.containsKey(f)) return;   // first quad per face wins
			int tint = -1;
			int ti = q.tintIndex();
			if (ti != -1) {
				int rgb = mc.getBlockColors().getColor(camo, signLevel, signPos, ti);
				if (rgb != -1) tint = 0xFF000000 | rgb;
			}
			out.put(f, new Layer(finder.find(q), tint));
		});

		// A model may not emit every face (a cross-shaped plant, say), so fall back
		// to any face we did get and never leave the board untextured.
		Layer any = out.values().stream().findFirst().orElse(null);
		if (any == null) return out;
		for (Direction d : Direction.values()) {
			out.putIfAbsent(d, any);
		}
		return out;
	}

	/**
	 * Board faces. Texel mapping on the 16x16 sprites of the camo: width = the
	 * full 0..16; front and back height = the middle band (rows 4..12, eight world
	 * pixels 1:1); top and bottom edges = the TOP/BOTTOM sprite over an exact 2x16
	 * strip; left and right edges = 2-texel-wide side bands.
	 */
	private static void emitBoard(PoseStack.Pose pose, VertexConsumer vc,
	                              Layer side, Layer top, Layer bottom, int light, int overlay) {
		// front face (model -z, the text side)
		quad(pose, vc, side, light, overlay, new float[][] {
			{BX0, BY0, BZ0, 0, 4}, {BX1, BY0, BZ0, 16, 4}, {BX1, BY1, BZ0, 16, 12}, {BX0, BY1, BZ0, 0, 12}
		}, 0, 0, -1);
		// back face (model +z), mirrored u
		quad(pose, vc, side, light, overlay, new float[][] {
			{BX1, BY0, BZ1, 0, 4}, {BX0, BY0, BZ1, 16, 4}, {BX0, BY1, BZ1, 16, 12}, {BX1, BY1, BZ1, 0, 12}
		}, 0, 0, 1);
		// top edge (model y=-14 maps to world up): TOP texture, middle 2-texel band
		quad(pose, vc, top, light, overlay, new float[][] {
			{BX0, BY0, BZ1, 0, 7}, {BX1, BY0, BZ1, 16, 7}, {BX1, BY0, BZ0, 16, 9}, {BX0, BY0, BZ0, 0, 9}
		}, 0, -1, 0);
		// bottom edge (model y=-2 maps to world down): BOTTOM texture, middle band
		quad(pose, vc, bottom, light, overlay, new float[][] {
			{BX0, BY1, BZ0, 0, 7}, {BX1, BY1, BZ0, 16, 7}, {BX1, BY1, BZ1, 16, 9}, {BX0, BY1, BZ1, 0, 9}
		}, 0, 1, 0);
		// left edge (model x=-12): 2-texel side band, middle height
		quad(pose, vc, side, light, overlay, new float[][] {
			{BX0, BY0, BZ1, 0, 4}, {BX0, BY0, BZ0, 2, 4}, {BX0, BY1, BZ0, 2, 12}, {BX0, BY1, BZ1, 0, 12}
		}, -1, 0, 0);
		// right edge (model x=12): 2-texel side band, middle height
		quad(pose, vc, side, light, overlay, new float[][] {
			{BX1, BY0, BZ0, 14, 4}, {BX1, BY0, BZ1, 16, 4}, {BX1, BY1, BZ1, 16, 12}, {BX1, BY1, BZ0, 14, 12}
		}, 1, 0, 0);
	}

	/**
	 * Stick (standing sign only), framed like the torch: the four sides sample the
	 * bottom-middle band of the side texture (2 texels wide, columns 7-9, rows
	 * 6.67-16 = the real height band down to the ground) and the bottom cap takes
	 * the middle 2x2 of the BOTTOM texture. The top cap is buried in the board.
	 */
	private static void emitStick(PoseStack.Pose pose, VertexConsumer vc,
	                              Layer side, Layer bottom, int light, int overlay) {
		float vTop = 6.667f, vBot = 16f;
		quad(pose, vc, side, light, overlay, new float[][] {          // model -z
			{SX0, SY0, SZ0, 7, vTop}, {SX1, SY0, SZ0, 9, vTop}, {SX1, SY1, SZ0, 9, vBot}, {SX0, SY1, SZ0, 7, vBot}
		}, 0, 0, -1);
		quad(pose, vc, side, light, overlay, new float[][] {          // model +z
			{SX1, SY0, SZ1, 7, vTop}, {SX0, SY0, SZ1, 9, vTop}, {SX0, SY1, SZ1, 9, vBot}, {SX1, SY1, SZ1, 7, vBot}
		}, 0, 0, 1);
		quad(pose, vc, side, light, overlay, new float[][] {          // model -x
			{SX0, SY0, SZ1, 7, vTop}, {SX0, SY0, SZ0, 9, vTop}, {SX0, SY1, SZ0, 9, vBot}, {SX0, SY1, SZ1, 7, vBot}
		}, -1, 0, 0);
		quad(pose, vc, side, light, overlay, new float[][] {          // model +x
			{SX1, SY0, SZ0, 7, vTop}, {SX1, SY0, SZ1, 9, vTop}, {SX1, SY1, SZ1, 9, vBot}, {SX1, SY1, SZ0, 7, vBot}
		}, 1, 0, 0);
		quad(pose, vc, bottom, light, overlay, new float[][] {        // bottom cap
			{SX0, SY1, SZ0, 7, 7}, {SX1, SY1, SZ0, 9, 7}, {SX1, SY1, SZ1, 9, 9}, {SX0, SY1, SZ1, 7, 9}
		}, 0, 1, 0);
	}

	/** verts = {x, y, z, uTexel, vTexel} x4. The tables are wound CCW for the
	 *  INWARD normal, so they are emitted in REVERSE for outward faces: the
	 *  (s,-s,-s) scale is a 180 degree rotation about x, which flips handedness. */
	private static void quad(PoseStack.Pose pose, VertexConsumer vc, Layer layer, int light, int overlay,
	                         float[][] verts, float nx, float ny, float nz) {
		TextureAtlasSprite sprite = layer.sprite();
		if (sprite == null) return;
		float u0 = sprite.getU0(), du = sprite.getU1() - u0;
		float v0 = sprite.getV0(), dv = sprite.getV1() - v0;
		int tint = layer.tint();
		int r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
		for (int i = verts.length - 1; i >= 0; i--) {
			float[] v = verts[i];
			vc.addVertex(pose, v[0], v[1], v[2])
				.setColor(r, g, b, 255)
				.setUv(u0 + v[3] / 16f * du, v0 + v[4] / 16f * dv)
				.setOverlay(overlay)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
		}
	}
}
