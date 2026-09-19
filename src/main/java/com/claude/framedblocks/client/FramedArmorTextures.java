package com.claude.framedblocks.client;

import com.claude.framedblocks.FramedBlocksMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the texture worn framed armour uses: the VANILLA iron armour layer with
 * its colours replaced by the applied block.
 *
 * <p>Folding the armour model UVs into a single block sprite throws away the two
 * things that make armour readable: the layer transparency, which carves out the
 * silhouette, and its shading. So the vanilla layer keeps its alpha and its
 * light/dark variation, and only the hue comes from the block, tiled across the
 * sheet.
 *
 * <p>One texture per (block, layer), built on first use and cached.
 *
 * <p>Ported from 1.20.1 with one substantive change: {@link NativeImage} reads
 * and writes pixels in ARGB here, where the old version worked in ABGR. Keeping
 * the old arithmetic would silently swap red and blue on every camo.
 */
public final class FramedArmorTextures {
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	/** Bare armour falls back to the frame texture. */
	private static final Identifier FRAME_TEX =
		Identifier.fromNamespaceAndPath("framedblocks", "textures/block/frame.png");
	/** Shading range applied to the colour of the block; 1.0 leaves it untouched. */
	private static final float MIN_SHADE = 0.60f, MAX_SHADE = 1.40f;

	private FramedArmorTextures() {}

	/** @param layer 1 for helmet/chestplate/boots, 2 for leggings. */
	@Nullable
	public static Identifier get(Minecraft mc, BlockState camo, int layer) {
		Identifier blockTex = FRAME_TEX;
		if (!camo.isAir()) {
			Identifier spriteId = mc.getModelManager().getBlockModelShaper()
				.getBlockModel(camo).particleIcon().contents().name();
			blockTex = Identifier.fromNamespaceAndPath(spriteId.getNamespace(),
				"textures/" + spriteId.getPath() + ".png");
		}
		String key = blockTex + "#" + layer;
		if (CACHE.containsKey(key)) return CACHE.get(key);
		Identifier built = build(mc, blockTex, layer);
		CACHE.put(key, built);
		return built;
	}

	@Nullable
	private static Identifier build(Minecraft mc, Identifier blockTex, int layer) {
		try {
			NativeImage armor = read(mc, Identifier.withDefaultNamespace(
				"textures/entity/equipment/humanoid" + (layer == 2 ? "_leggings" : "") + "/iron.png"));
			NativeImage block = read(mc, blockTex);
			int bw = block.getWidth();
			// Animated textures are a vertical strip of frames: keep the first.
			int bh = Math.min(block.getHeight(), bw);

			// Luminance range of the armour sheet, to normalise its shading.
			float lo = Float.MAX_VALUE, hi = 0f;
			for (int y = 0; y < armor.getHeight(); y++) {
				for (int x = 0; x < armor.getWidth(); x++) {
					int c = armor.getPixel(x, y);
					if (((c >>> 24) & 0xFF) == 0) continue;
					float l = lum(c);
					lo = Math.min(lo, l);
					hi = Math.max(hi, l);
				}
			}
			float span = Math.max(1f, hi - lo);

			NativeImage out = new NativeImage(armor.getWidth(), armor.getHeight(), false);
			for (int y = 0; y < armor.getHeight(); y++) {
				for (int x = 0; x < armor.getWidth(); x++) {
					int ac = armor.getPixel(x, y);
					int alpha = (ac >>> 24) & 0xFF;
					if (alpha == 0) { out.setPixel(x, y, 0); continue; }
					float shade = MIN_SHADE + (MAX_SHADE - MIN_SHADE) * ((lum(ac) - lo) / span);
					int bc = block.getPixel(x % bw, y % bh);
					int r = clamp(((bc >> 16) & 0xFF) * shade);
					int g = clamp(((bc >> 8) & 0xFF) * shade);
					int b = clamp((bc & 0xFF) * shade);
					int ba = (bc >>> 24) & 0xFF;
					int a = Math.min(alpha, ba == 0 ? alpha : ba);
					out.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
				}
			}
			armor.close();
			block.close();

			Identifier id = Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID,
				"armor/" + blockTex.getNamespace() + "_"
					+ blockTex.getPath().replaceAll("[^a-z0-9_]", "_") + "_" + layer);
			mc.getTextureManager().register(id, new DynamicTexture(id::toString, out));
			return id;
		} catch (Exception ex) {
			FramedBlocksMod.LOGGER.error("Framed armour texture failed for {} layer {}", blockTex, layer, ex);
			return null;
		}
	}

	/** ARGB luminance. */
	private static float lum(int argb) {
		int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		return 0.299f * r + 0.587f * g + 0.114f * b;
	}

	private static int clamp(float v) {
		return Math.min(255, Math.max(0, Math.round(v)));
	}

	private static NativeImage read(Minecraft mc, Identifier path) throws Exception {
		Resource res = mc.getResourceManager().getResource(path).orElseThrow(
			() -> new IllegalStateException("missing texture " + path));
		try (InputStream in = res.open()) {
			return NativeImage.read(in);
		}
	}
}
