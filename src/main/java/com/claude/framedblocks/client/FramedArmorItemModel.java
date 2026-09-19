package com.claude.framedblocks.client;

import com.claude.framedblocks.FramedBlocksMod;
import com.claude.framedblocks.item.FramedArmorItem;
import com.claude.framedblocks.mixin.BlockModelWrapperAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;

/**
 * Inventory icon for framed armour: the applied block's ACTUAL texture, not just
 * its colour.
 *
 * <p>The icon model has two layers: layer0 is the armour interior
 * ("..._fill") and layer1 the 1px outline. A plain texture swap cannot work: a
 * generated item model gets its SHAPE from the texture alpha, so pointing it at
 * an opaque block texture fills the whole square. Instead the interior is
 * rebuilt from the fill texture alpha mask, one quad per run of pixels sharing a
 * shading level, each sampling the matching texel of the block sprite.
 *
 * <p>Ported from the 1.20.1 model with one structural change: a 1.21.11
 * {@link BakedQuad} carries no per-vertex colour, so the armour own shading
 * travels as a TINT INDEX per run, with the greys supplied through
 * {@link ItemStackRenderState.LayerRenderState#prepareTintLayers}.
 *
 * <p>The wrapped model is drawn first and left in place: if the mask or the
 * sprite is unavailable, the icon simply stays the flat tinted one instead of
 * disappearing.
 */
public class FramedArmorItemModel extends WrapperBakedItemModel {
	/** Number of distinct shading levels kept from the mask. */
	private static final int QUANT = 32;
	/** The darkest interior pixel maps to this multiplier, the brightest to 1. */
	private static final float MIN_SHADE = 0.55f;
	/**
	 * Just in front of / behind the layers the wrapped model draws.
	 *
	 * <p>Coplanar does NOT work here, unlike the 1.20.1 model which could delete
	 * the fill quads and take their exact place. A layer cannot be removed, and
	 * layers are drawn grouped by render type rather than in the order they were
	 * added, so at equal depth the fill wins and the icon falls back to a flat
	 * tint. A tenth of a pixel in front is enough to settle it and is invisible.
	 */
	private static final float FRONT_Z = 8.6f / 16f;
	private static final float BACK_Z = 7.4f / 16f;

	private final Identifier maskId;
	private float[][] shade;
	private boolean maskFailed;

	public FramedArmorItemModel(ItemModel wrapped, String itemName) {
		super(wrapped);
		// The icon layer borrows its display transform from this model. Anything
		// other than the vanilla wrapper has no transforms to lend, and the layer
		// would render unplaced; say so rather than fail silently.
		if (!(wrapped instanceof BlockModelWrapper)) {
			FramedBlocksMod.LOGGER.warn("Framed armour icon: {} was baked as {}, display transforms unavailable",
				itemName, wrapped.getClass().getName());
		}
		this.maskId = Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID,
			"textures/item/" + itemName + "_fill.png");
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
	                   ItemDisplayContext ctx, ClientLevel level, ItemOwner owner, int seed) {
		super.update(state, stack, resolver, ctx, level, owner, seed);

		BlockState camo = FramedArmorItem.getCamo(stack);
		if (camo.isAir()) return;
		float[][] mask = shading();
		if (mask == null) return;

		TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
			.getBlockModelShaper().getBlockModel(camo).particleIcon();
		if (sprite == null) return;

		ItemStackRenderState.LayerRenderState layer = state.newLayer();
		// A layer carries its own display transform, and a fresh one has none: it
		// would draw unrotated at full block size, a slab hanging in front of the
		// player, while the rest of the icon sits correctly in the hand. Take the
		// transforms the wrapped model was baked with so this layer is placed with
		// the others in every context.
		if (wrapped instanceof BlockModelWrapper bmw) {
			((BlockModelWrapperAccessor) bmw).framedblocks$properties().applyToLayer(layer, ctx);
		}
		layer.setRenderType(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));

		// One tint per shading level: the quads reference these by index, which is
		// how the block texture arrives carrying the armour highlights and shadows.
		int[] tints = layer.prepareTintLayers(QUANT + 1);
		for (int i = 0; i <= QUANT; i++) {
			int grey = Math.min(255, Math.max(0, Math.round(i * 255f / QUANT)));
			tints[i] = 0xFF000000 | (grey << 16) | (grey << 8) | grey;
		}

		MutableMesh mesh = Renderer.get().mutableMesh();
		emitMasked(mesh.emitter(), mask, sprite);
		List<BakedQuad> quads = layer.prepareQuadList();
		mesh.forEachMutable(q -> quads.add(q.toBakedQuad(sprite)));
	}

	/** One quad per horizontal run of pixels sharing the same shading, front and
	 *  back, each sampling the matching texel of the block sprite. */
	private static void emitMasked(QuadEmitter e, float[][] m, TextureAtlasSprite sprite) {
		int size = m.length;
		for (int row = 0; row < size; row++) {
			int col = 0;
			while (col < size) {
				if (m[row][col] <= 0f) { col++; continue; }
				int start = col;
				int level = Math.round(m[row][col] * QUANT);
				while (col < size && m[row][col] > 0f && Math.round(m[row][col] * QUANT) == level) col++;

				// pixel-space run [start, col) on this row, all at the same shade
				float x0 = start / (float) size, x1 = col / (float) size;
				float y0 = (size - row - 1) / (float) size, y1 = (size - row) / (float) size;
				float u0 = start / (float) size, u1 = col / (float) size;
				float v0 = row / (float) size, v1 = (row + 1) / (float) size;

				quad(e, sprite, Direction.SOUTH, x0, y0, x1, y1, FRONT_Z, u0, v0, u1, v1, level);
				quad(e, sprite, Direction.NORTH, x0, y0, x1, y1, BACK_Z, u0, v0, u1, v1, level);
			}
		}
	}

	private static void quad(QuadEmitter e, TextureAtlasSprite sprite, Direction face,
	                         float x0, float y0, float x1, float y1, float z,
	                         float u0, float v0, float u1, float v1, int tintIndex) {
		boolean front = face == Direction.SOUTH;
		// Counter-clockwise seen from the side the face points at.
		float[][] p = front
			? new float[][]{{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}}
			: new float[][]{{x0, y0}, {x0, y1}, {x1, y1}, {x1, y0}};
		float[][] uv = front
			? new float[][]{{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}}
			: new float[][]{{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
		for (int i = 0; i < 4; i++) {
			e.pos(i, p[i][0], p[i][1], z);
			e.uv(i, uv[i][0], uv[i][1]);
			e.color(i, -1);
			e.normal(i, 0f, 0f, front ? 1f : -1f);
		}
		e.nominalFace(face);
		e.cullFace(null);
		e.tintIndex(tintIndex);
		e.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED);
		e.emit();
	}

	/**
	 * Shape and shading of the interior, read once from the fill texture: each
	 * opaque pixel brightness becomes a multiplier, so the applied block keeps the
	 * light and dark areas of the piece instead of looking flat.
	 */
	@Nullable
	private float[][] shading() {
		if (shade != null || maskFailed) return shade;
		try {
			Resource res = Minecraft.getInstance().getResourceManager().getResource(maskId).orElse(null);
			if (res == null) { maskFailed = true; return null; }
			try (InputStream in = res.open()) {
				NativeImage img = NativeImage.read(in);
				int w = img.getWidth(), h = img.getHeight();
				float[][] lum = new float[h][w];
				float lo = Float.MAX_VALUE, hi = 0f;
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int c = img.getPixel(x, y);          // ARGB in 1.21.11
						if (((c >>> 24) & 0xFF) <= 16) continue;
						int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
						float l = 0.299f * r + 0.587f * g + 0.114f * b;
						lum[y][x] = l;
						lo = Math.min(lo, l);
						hi = Math.max(hi, l);
					}
				}
				img.close();
				float span = Math.max(1f, hi - lo);
				float[][] s = new float[h][w];
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						if (lum[y][x] <= 0f) continue;
						s[y][x] = MIN_SHADE + (1f - MIN_SHADE) * ((lum[y][x] - lo) / span);
					}
				}
				shade = s;
			}
		} catch (Exception ex) {
			FramedBlocksMod.LOGGER.error("Framed armour icon mask failed for {}", maskId, ex);
			maskFailed = true;
		}
		return shade;
	}
}
