package com.claude.framedblocks.client;

import com.claude.framedblocks.block.ModBlocks;
import com.claude.framedblocks.block.entity.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Client init: wraps the framed block's baked model with FramedBlockStateModel
 * so the camo is drawn. Equivalent of the 1.20.1 ModelLoadingPlugin +
 * modifyModelAfterBake.
 */
public class FramedBlocksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Inventory icon: the interior is rebuilt with the applied block texture.
		net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(ctx ->
			ctx.modifyItemModelAfterBake().register((model, modelCtx) -> {
				String path = modelCtx.itemId().getPath();
				return switch (path) {
					case "framed_helmet", "framed_chestplate", "framed_leggings", "framed_boots" ->
						new FramedArmorItemModel(model, path);
					default -> model;
				};
			}));

		// Worn framed armour: drawn from a texture built per applied block.
		net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
			new FramedArmorRenderer(),
			com.claude.framedblocks.item.ModItems.FRAMED_HELMET,
			com.claude.framedblocks.item.ModItems.FRAMED_CHESTPLATE,
			com.claude.framedblocks.item.ModItems.FRAMED_LEGGINGS,
			com.claude.framedblocks.item.ModItems.FRAMED_BOOTS);

		// The frame texture has transparent holes (the see-through wireframe) and
		// camo overlays (e.g. grass's side overlay) ride on a cutout layer. Without
		// this the blocks default to SOLID, where those transparent pixels render
		// as a flat gray/black fill (bare frame = gray; grass overlay = black sheet
		// over the dirt). Dropped in the 26.1.2 port — restored here. The older
		// versions used blockrenderlayer.v1; 1.21.8 moved it to rendering.v1 with
		// static methods + ChunkSectionLayer.
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
			ModBlocks.FRAMED_BLOCK,
			ModBlocks.FRAMED_SLAB,
			ModBlocks.FRAMED_VERTICAL_SLAB,
			ModBlocks.FRAMED_STAIRS,
			ModBlocks.FRAMED_TRAPDOOR,
			ModBlocks.FRAMED_BUTTON,
			ModBlocks.FRAMED_LEVER,
			ModBlocks.FRAMED_PRESSURE_PLATE,
			ModBlocks.FRAMED_FENCE,
			ModBlocks.FRAMED_FENCE_GATE,
			ModBlocks.FRAMED_WALL,
			ModBlocks.FRAMED_SLOPE,
			ModBlocks.FRAMED_SLOPE_INNER_CORNER,
			ModBlocks.FRAMED_PYRAMID,
			ModBlocks.FRAMED_CORNER_PILLAR,
			ModBlocks.FRAMED_PILLAR,
			ModBlocks.FRAMED_HALF_PILLAR,
			ModBlocks.FRAMED_DOUBLE_PANEL,
			ModBlocks.FRAMED_DOUBLE_SLAB,
			ModBlocks.FRAMED_CHEST,
			ModBlocks.FRAMED_BITS,
			ModBlocks.FRAMED_SMALL_BITS,
			ModBlocks.FRAMED_GLOWING_CUBE,
			ModBlocks.FRAMED_BOUNCY_CUBE,
			ModBlocks.FRAMED_CUSHION,
			ModBlocks.FRAMED_LADDER,
			ModBlocks.FRAMED_BARS,
			ModBlocks.FRAMED_POST,
			ModBlocks.FRAMED_LARGE_BUTTON,
			ModBlocks.FRAMED_PANEL,
			ModBlocks.FRAMED_FLOOR_BOARD,
			ModBlocks.FRAMED_WALL_BOARD,
			ModBlocks.FRAMED_SLAB_EDGE,
			ModBlocks.FRAMED_SLAB_CORNER,
			ModBlocks.FRAMED_HALF_STAIRS,
			ModBlocks.FRAMED_VERTICAL_STAIRS,
			ModBlocks.FRAMED_BOOKSHELF,
			ModBlocks.FRAMED_STONE_BUTTON,
			ModBlocks.FRAMED_STONE_PRESSURE_PLATE,
			ModBlocks.FRAMED_TORCH,
			ModBlocks.FRAMED_SOUL_TORCH,
			ModBlocks.FRAMED_REDSTONE_TORCH,
			ModBlocks.FRAMED_SECRET_STORAGE,
			ModBlocks.FRAMED_VERTICAL_SLOPE,
			ModBlocks.FRAMED_ITEM_FRAME);
		BlockRenderLayerMap.putBlock(ModBlocks.FRAMED_DOOR, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.FRAMED_IRON_DOOR, ChunkSectionLayer.CUTOUT);

		// Animated lid + latch for the framed chest (the body is a chunk model).
		BlockEntityRenderers.register(ModBlockEntities.FRAMED_CHEST, FramedChestRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.FRAMED_ITEM_FRAME, FramedItemFrameRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.FRAMED_SIGN, FramedSignRenderer::new);

		ModelLoadingPlugin.register(ctx ->
			ctx.modifyBlockModelAfterBake().register((model, modelCtx) -> {
				var block = modelCtx.state().getBlock();
				// Double Panel: two camos (front/back half), its own model wrapper.
				if (block == ModBlocks.FRAMED_DOUBLE_PANEL) {
					return new FramedDoublePanelModel(model);
				}
				// Double Slab: one camo per half, split horizontally.
				if (block == ModBlocks.FRAMED_DOUBLE_SLAB) {
					return new FramedDoubleSlabModel(model);
				}
				// Vertical slope: the diagonal plate plus emitted end caps.
				if (block == ModBlocks.FRAMED_VERTICAL_SLOPE) {
					return new FramedVerticalSlopeModel(model);
				}
				// Slope: retexture (super) + emit triangular sides.
				if (block == ModBlocks.FRAMED_SLOPE) {
					return new FramedSlopeModel(model);
				}
				// Pyramid: fully custom geometry (base + 4 triangular sides).
				if (block == ModBlocks.FRAMED_PYRAMID) {
					return new FramedPyramidModel(model);
				}
				// Inner-corner slope: retexture (super) + emit the 4 triangles.
				if (block == ModBlocks.FRAMED_SLOPE_INNER_CORNER) {
					return new FramedSlopeInnerCornerModel(model);
				}
				// Bits: dynamic per-octant geometry, fully emitted.
				if (block == ModBlocks.FRAMED_BITS) {
					return new FramedBitsModel(model);
				}
				// Small bits: same, on the finer grid.
				if (block == ModBlocks.FRAMED_SMALL_BITS) {
					return new FramedSmallBitsModel(model);
				}
				// All framed blocks (full cube included) retexture their own
				// shape with the camo's sprites, so a slab/stairs camo stretches
				// over the frame instead of rendering its own smaller geometry.
				if (block == ModBlocks.FRAMED_BLOCK
					|| block == ModBlocks.FRAMED_SLAB
					|| block == ModBlocks.FRAMED_STAIRS
					|| block == ModBlocks.FRAMED_VERTICAL_SLAB
					|| block == ModBlocks.FRAMED_FENCE
					|| block == ModBlocks.FRAMED_FENCE_GATE
					|| block == ModBlocks.FRAMED_BUTTON
					|| block == ModBlocks.FRAMED_DOOR
					|| block == ModBlocks.FRAMED_TRAPDOOR
					|| block == ModBlocks.FRAMED_PRESSURE_PLATE
					|| block == ModBlocks.FRAMED_WALL
					|| block == ModBlocks.FRAMED_LEVER
					|| block == ModBlocks.FRAMED_CORNER_PILLAR
					|| block == ModBlocks.FRAMED_PILLAR
					|| block == ModBlocks.FRAMED_HALF_PILLAR
					|| block == ModBlocks.FRAMED_CHEST
					|| block == ModBlocks.FRAMED_GLOWING_CUBE
					|| block == ModBlocks.FRAMED_BOUNCY_CUBE
					|| block == ModBlocks.FRAMED_CUSHION
					|| block == ModBlocks.FRAMED_LADDER
					|| block == ModBlocks.FRAMED_BARS
					|| block == ModBlocks.FRAMED_POST
					|| block == ModBlocks.FRAMED_LARGE_BUTTON
					|| block == ModBlocks.FRAMED_IRON_DOOR
					|| block == ModBlocks.FRAMED_PANEL
					|| block == ModBlocks.FRAMED_FLOOR_BOARD
					|| block == ModBlocks.FRAMED_WALL_BOARD
					|| block == ModBlocks.FRAMED_SLAB_EDGE
					|| block == ModBlocks.FRAMED_SLAB_CORNER
					|| block == ModBlocks.FRAMED_HALF_STAIRS
					|| block == ModBlocks.FRAMED_VERTICAL_STAIRS
					|| block == ModBlocks.FRAMED_BOOKSHELF
					|| block == ModBlocks.FRAMED_STONE_BUTTON
					|| block == ModBlocks.FRAMED_STONE_PRESSURE_PLATE
					|| block == ModBlocks.FRAMED_TORCH
					|| block == ModBlocks.FRAMED_SOUL_TORCH
					|| block == ModBlocks.FRAMED_REDSTONE_TORCH
					|| block == ModBlocks.FRAMED_SECRET_STORAGE
					|| block == ModBlocks.FRAMED_ITEM_FRAME) {
					return new FramedRetextureModel(model);
				}
				return model;
			}));
	}
}
