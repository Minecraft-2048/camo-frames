package com.claude.framedblocks.block.entity;

import com.claude.framedblocks.FramedBlocksMod;
import com.claude.framedblocks.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static BlockEntityType<FramedBlockEntity> FRAMED;
	public static BlockEntityType<FramedDoublePanelBlockEntity> FRAMED_DOUBLE_PANEL;
	public static BlockEntityType<FramedDoubleSlabBlockEntity> FRAMED_DOUBLE_SLAB;
	public static BlockEntityType<FramedBitsBlockEntity> FRAMED_BITS;
	public static BlockEntityType<FramedSmallBitsBlockEntity> FRAMED_SMALL_BITS;
	public static BlockEntityType<FramedChestBlockEntity> FRAMED_CHEST;
	public static BlockEntityType<FramedItemFrameBlockEntity> FRAMED_ITEM_FRAME;
	public static BlockEntityType<FramedSignBlockEntity> FRAMED_SIGN;
	public static BlockEntityType<FramedStorageBlockEntity> FRAMED_STORAGE;

	private ModBlockEntities() {}

	public static void register() {
		FRAMED = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed"),
			FabricBlockEntityTypeBuilder.create(FramedBlockEntity::new,
				ModBlocks.FRAMED_BLOCK,
				ModBlocks.FRAMED_SLAB,
				ModBlocks.FRAMED_STAIRS,
				ModBlocks.FRAMED_FENCE,
				ModBlocks.FRAMED_FENCE_GATE,
				ModBlocks.FRAMED_BUTTON,
				ModBlocks.FRAMED_DOOR,
				ModBlocks.FRAMED_TRAPDOOR,
				ModBlocks.FRAMED_PRESSURE_PLATE,
				ModBlocks.FRAMED_WALL,
				ModBlocks.FRAMED_LEVER,
				ModBlocks.FRAMED_CORNER_PILLAR,
				ModBlocks.FRAMED_PILLAR,
				ModBlocks.FRAMED_HALF_PILLAR,
				ModBlocks.FRAMED_VERTICAL_SLAB,
				ModBlocks.FRAMED_SLOPE,
				ModBlocks.FRAMED_SLOPE_INNER_CORNER,
				ModBlocks.FRAMED_PYRAMID,
				ModBlocks.FRAMED_GLOWING_CUBE,
				ModBlocks.FRAMED_BOUNCY_CUBE,
				ModBlocks.FRAMED_CUSHION,
				ModBlocks.FRAMED_LADDER,
				ModBlocks.FRAMED_BARS,
				ModBlocks.FRAMED_POST,
				ModBlocks.FRAMED_LARGE_BUTTON,
				ModBlocks.FRAMED_IRON_DOOR,
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
				ModBlocks.FRAMED_VERTICAL_SLOPE
			).build()
		);

		// Bits stores 8 octants, so it needs its own BE type.
		FRAMED_BITS = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_bits"),
			FabricBlockEntityTypeBuilder.create(FramedBitsBlockEntity::new,
				ModBlocks.FRAMED_BITS
			).build()
		);

		// Small bits store 64 cells, so it needs its own BE type.
		FRAMED_SMALL_BITS = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_small_bits"),
			FabricBlockEntityTypeBuilder.create(FramedSmallBitsBlockEntity::new,
				ModBlocks.FRAMED_SMALL_BITS
			).build()
		);

		// Double Panel stores two camos, so it needs its own BE type.
		FRAMED_DOUBLE_PANEL = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_double_panel"),
			FabricBlockEntityTypeBuilder.create(FramedDoublePanelBlockEntity::new,
				ModBlocks.FRAMED_DOUBLE_PANEL
			).build()
		);

		// Chest stores a 27-slot inventory, so it needs its own BE type.
		FRAMED_CHEST = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_chest"),
			FabricBlockEntityTypeBuilder.create(FramedChestBlockEntity::new,
				ModBlocks.FRAMED_CHEST
			).build()
		);
		// Double Slab stores one camo per half, so it needs its own BE type.
		FRAMED_DOUBLE_SLAB = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_double_slab"),
			FabricBlockEntityTypeBuilder.create(FramedDoubleSlabBlockEntity::new,
				ModBlocks.FRAMED_DOUBLE_SLAB
			).build()
		);
		// Secret storage keeps 27 slots but no lid, so it gets its own type too.
		FRAMED_STORAGE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_storage"),
			FabricBlockEntityTypeBuilder.create(FramedStorageBlockEntity::new,
				ModBlocks.FRAMED_SECRET_STORAGE
			).build()
		);
		// Sign: extends vanilla SignBlockEntity (text + edit screen), so it needs
		// its own type; the standing and wall blocks share it.
		FRAMED_SIGN = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_sign"),
			FabricBlockEntityTypeBuilder.create(FramedSignBlockEntity::new,
				ModBlocks.FRAMED_SIGN,
				ModBlocks.FRAMED_WALL_SIGN
			).build()
		);
		// Item frame: stores the displayed item on top of the camo, so its own type.
		FRAMED_ITEM_FRAME = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed_item_frame"),
			FabricBlockEntityTypeBuilder.create(FramedItemFrameBlockEntity::new,
				ModBlocks.FRAMED_ITEM_FRAME
			).build()
		);
	}
}
