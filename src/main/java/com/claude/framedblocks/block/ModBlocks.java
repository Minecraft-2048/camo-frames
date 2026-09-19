package com.claude.framedblocks.block;

import com.claude.framedblocks.FramedBlocksMod;
import com.claude.framedblocks.item.FramedBitsItem;
import com.claude.framedblocks.item.FramedSmallBitsItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/**
 * Block + item registration, ported to Minecraft 26.1.2 (official Mojang
 * names). Key differences vs the 1.20.1 version:
 *   - AbstractBlock.Settings        -> BlockBehaviour.Properties (.of() factory)
 *   - .sounds(BlockSoundGroup)      -> .sound(SoundType)
 *   - .nonOpaque()                  -> .noOcclusion()
 *   - .pistonBehavior(...)          -> .pushReaction(PushReaction...)
 *   - Identifier (now in net.minecraft.resources, .fromNamespaceAndPath)
 *   - Blocks/items must carry a ResourceKey via Properties.setId(key)
 *   - Registries (keys) + BuiltInRegistries (the registries themselves)
 *
 * Slice 1: only FRAMED_BLOCK, as a plain Block, to validate the toolchain +
 * registration API. The camo system, the other 13 blocks, and the rendering
 * are ported in later slices.
 */
public final class ModBlocks {
	public static Block FRAMED_BLOCK;
	public static Block FRAMED_SLAB;
	public static Block FRAMED_VERTICAL_SLAB;
	public static Block FRAMED_STAIRS;
	public static Block FRAMED_FENCE;
	public static Block FRAMED_FENCE_GATE;
	public static Block FRAMED_BUTTON;
	public static Block FRAMED_DOOR;
	public static Block FRAMED_DOUBLE_PANEL;
	public static Block FRAMED_DOUBLE_SLAB;
	public static Block FRAMED_TRAPDOOR;
	public static Block FRAMED_PRESSURE_PLATE;
	public static Block FRAMED_WALL;
	public static Block FRAMED_LEVER;
	public static Block FRAMED_CORNER_PILLAR;
	public static Block FRAMED_PILLAR;
	public static Block FRAMED_HALF_PILLAR;
	public static Block FRAMED_SLOPE;
	public static Block FRAMED_SLOPE_INNER_CORNER;
	public static Block FRAMED_PYRAMID;
	public static Block FRAMED_BITS;
	public static Block FRAMED_SMALL_BITS;
	public static Block FRAMED_CHEST;
	public static Block FRAMED_GLOWING_CUBE;
	public static Block FRAMED_BOUNCY_CUBE;
	public static Block FRAMED_CUSHION;
	public static Block FRAMED_LADDER;
	public static Block FRAMED_BARS;
	public static Block FRAMED_POST;
	public static Block FRAMED_LARGE_BUTTON;
	public static Block FRAMED_IRON_DOOR;
	public static Block FRAMED_PANEL;
	public static Block FRAMED_FLOOR_BOARD;
	public static Block FRAMED_WALL_BOARD;
	public static Block FRAMED_SLAB_EDGE;
	public static Block FRAMED_SLAB_CORNER;
	public static Block FRAMED_HALF_STAIRS;
	public static Block FRAMED_VERTICAL_STAIRS;
	public static Block FRAMED_BOOKSHELF;
	public static Block FRAMED_STONE_BUTTON;
	public static Block FRAMED_STONE_PRESSURE_PLATE;
	public static Block FRAMED_TORCH;
	public static Block FRAMED_SOUL_TORCH;
	public static Block FRAMED_REDSTONE_TORCH;
	public static Block FRAMED_SECRET_STORAGE;
	public static Block FRAMED_VERTICAL_SLOPE;
	public static Block FRAMED_ITEM_FRAME;
	public static Block FRAMED_SIGN;
	public static Block FRAMED_WALL_SIGN;

	private ModBlocks() {}

	public static void register() {
		FRAMED_BLOCK = registerBlock("framed_block",
			key -> new FramedBlock(frameProps(key)));
		FRAMED_SLAB = registerBlock("framed_slab",
			key -> new FramedSlabBlock(frameProps(key)));
		FRAMED_VERTICAL_SLAB = registerBlock("framed_vertical_slab",
			key -> new FramedVerticalSlabBlock(frameProps(key)));
		// Stairs need a base state for inheritance only; FRAMED_BLOCK is fine.
		FRAMED_STAIRS = registerBlock("framed_stairs",
			key -> new FramedStairsBlock(FRAMED_BLOCK.defaultBlockState(), frameProps(key)));
		FRAMED_FENCE = registerBlock("framed_fence",
			key -> new FramedFenceBlock(frameProps(key)));
		FRAMED_FENCE_GATE = registerBlock("framed_fence_gate",
			key -> new FramedFenceGateBlock(WoodType.OAK, frameProps(key)));
		FRAMED_BUTTON = registerBlock("framed_button",
			key -> new FramedButtonBlock(BlockSetType.OAK, 30, frameProps(key)));
		FRAMED_DOOR = registerBlock("framed_door",
			key -> new FramedDoorBlock(BlockSetType.OAK, frameProps(key)));
		FRAMED_DOUBLE_PANEL = registerBlock("framed_double_panel",
			key -> new FramedDoublePanelBlock(frameProps(key)));
		// Formed in world by placing a framed slab into another one, and drops
		// two framed slabs: it needs no item of its own.
		FRAMED_DOUBLE_SLAB = registerBlockNoItem("framed_double_slab",
			key -> new FramedDoubleSlabBlock(frameProps(key)));
		FRAMED_TRAPDOOR = registerBlock("framed_trapdoor",
			key -> new FramedTrapdoorBlock(BlockSetType.OAK, frameProps(key)));
		FRAMED_PRESSURE_PLATE = registerBlock("framed_pressure_plate",
			key -> new FramedPressurePlateBlock(BlockSetType.OAK, frameProps(key)));
		FRAMED_WALL = registerBlock("framed_wall",
			key -> new FramedWallBlock(frameProps(key)));
		FRAMED_LEVER = registerBlock("framed_lever",
			key -> new FramedLeverBlock(frameProps(key)));
		FRAMED_CORNER_PILLAR = registerBlock("framed_corner_pillar",
			key -> new FramedCornerPillarBlock(frameProps(key)));
		FRAMED_PILLAR = registerBlock("framed_pillar",
			key -> new FramedPillarBlock(frameProps(key)));
		FRAMED_HALF_PILLAR = registerBlock("framed_half_pillar",
			key -> new FramedHalfPillarBlock(frameProps(key)));
		FRAMED_SLOPE = registerBlock("framed_slope",
			key -> new FramedSlopeBlock(frameProps(key)));
		FRAMED_SLOPE_INNER_CORNER = registerBlock("framed_slope_inner_corner",
			key -> new FramedSlopeInnerCornerBlock(frameProps(key)));
		FRAMED_PYRAMID = registerBlock("framed_pyramid",
			key -> new FramedPyramidBlock(frameProps(key)));
		FRAMED_CHEST = registerBlock("framed_chest",
			key -> new FramedChestBlock(frameProps(key)));

		FRAMED_GLOWING_CUBE = registerBlock("framed_glowing_cube",
			// Luminance 12, not 15: ice and snow melt when the block light
			// reaching them exceeds 11, and a level-12 emitter gives its
			// neighbours exactly 11 - the brightest level that melts nothing.
			key -> new FramedBlock(frameProps(key).lightLevel(s -> 12)));
		FRAMED_BOUNCY_CUBE = registerBlock("framed_bouncy_cube",
			key -> new FramedBouncyCubeBlock(frameProps(key)));
		// The opposite of the bouncy cube: it absorbs a fall instead of
		// returning it, so it can be a hidden landing pad.
		FRAMED_CUSHION = registerBlock("framed_cushion",
			key -> new FramedCushionBlock(frameProps(key).sound(SoundType.WOOL)));
		FRAMED_LADDER = registerBlock("framed_ladder",
			key -> new FramedLadderBlock(frameProps(key)));
		FRAMED_BARS = registerBlock("framed_bars",
			key -> new FramedBarsBlock(frameProps(key)));
		FRAMED_POST = registerBlock("framed_post",
			key -> new FramedPostBlock(frameProps(key)));
		FRAMED_LARGE_BUTTON = registerBlock("framed_large_button",
			key -> new FramedLargeButtonBlock(BlockSetType.OAK, 30,
				frameProps(key).noCollision().strength(0.5f)));
		// Iron door: the same FramedDoorBlock, but BlockSetType.IRON makes it
		// redstone-only - no hand-open, so right-click only applies the camo.
		FRAMED_IRON_DOOR = registerBlock("framed_iron_door",
			key -> new FramedDoorBlock(BlockSetType.IRON, BlockBehaviour.Properties.of()
				.setId(key)
				.mapColor(MapColor.METAL)
				.strength(5.0f)
				.sound(SoundType.METAL)
				.noOcclusion()
				.pushReaction(PushReaction.DESTROY)));
		FRAMED_PANEL = registerBlock("framed_panel",
			key -> new FramedPanelBlock(frameProps(key)));
		FRAMED_FLOOR_BOARD = registerBlock("framed_floor_board",
			key -> new FramedFloorBoardBlock(frameProps(key)));
		FRAMED_WALL_BOARD = registerBlock("framed_wall_board",
			key -> new FramedWallBoardBlock(frameProps(key)));
		FRAMED_SLAB_EDGE = registerBlock("framed_slab_edge",
			key -> new FramedSlabEdgeBlock(frameProps(key)));
		FRAMED_SLAB_CORNER = registerBlock("framed_slab_corner",
			key -> new FramedSlabCornerBlock(frameProps(key)));
		FRAMED_HALF_STAIRS = registerBlock("framed_half_stairs",
			key -> new FramedHalfStairsBlock(frameProps(key)));
		FRAMED_VERTICAL_STAIRS = registerBlock("framed_vertical_stairs",
			key -> new FramedVerticalStairsBlock(frameProps(key)));
		FRAMED_BOOKSHELF = registerBlock("framed_bookshelf",
			key -> new FramedBlock(frameProps(key)));
		FRAMED_STONE_BUTTON = registerBlock("framed_stone_button",
			key -> new FramedButtonBlock(BlockSetType.STONE, 20, stoneProps(key)));
		FRAMED_STONE_PRESSURE_PLATE = registerBlock("framed_stone_pressure_plate",
			key -> new FramedPressurePlateBlock(BlockSetType.STONE, stoneProps(key)));
		FRAMED_TORCH = registerBlock("framed_torch",
			key -> new FramedTorchBlock(torchProps(key).lightLevel(s -> 14)));
		FRAMED_SOUL_TORCH = registerBlock("framed_soul_torch",
			key -> new FramedSoulTorchBlock(torchProps(key).lightLevel(s -> 10)));
		FRAMED_REDSTONE_TORCH = registerBlock("framed_redstone_torch",
			key -> new FramedRedstoneTorchBlock(redstoneTorchProps(key)));
		FRAMED_SECRET_STORAGE = registerBlock("framed_secret_storage",
			key -> new FramedSecretStorageBlock(frameProps(key)));
		FRAMED_VERTICAL_SLOPE = registerBlock("framed_vertical_slope",
			key -> new FramedVerticalSlopeBlock(frameProps(key)));

		// Item frame: a thin frame that displays one item, on any of the six faces.
		FRAMED_ITEM_FRAME = registerBlock("framed_item_frame",
			key -> new FramedItemFrameBlock(BlockBehaviour.Properties.of()
				.setId(key)
				.mapColor(MapColor.WOOD)
				.noCollision()
				.strength(0.5f)
				.sound(SoundType.WOOD)
				.noOcclusion()
				.pushReaction(PushReaction.DESTROY)));

		// Sign: the standing block and the wall block share one block-entity type
		// and one item - a SignItem places whichever fits the clicked face. The
		// wall variant reuses the loot table of the standing one (the old
		// dropsLike, renamed to overrideLootTable).
		ResourceKey<Block> signKey = ResourceKey.create(Registries.BLOCK, id("framed_sign"));
		FRAMED_SIGN = Registry.register(BuiltInRegistries.BLOCK, signKey,
			new FramedSignBlock(WoodType.OAK, signProps(signKey)));
		ResourceKey<Block> wallSignKey = ResourceKey.create(Registries.BLOCK, id("framed_wall_sign"));
		FRAMED_WALL_SIGN = Registry.register(BuiltInRegistries.BLOCK, wallSignKey,
			new FramedWallSignBlock(WoodType.OAK, signProps(wallSignKey).overrideLootTable(FRAMED_SIGN.getLootTable())));
		ResourceKey<Item> signItemKey = ResourceKey.create(Registries.ITEM, id("framed_sign"));
		Registry.register(BuiltInRegistries.ITEM, signItemKey,
			new SignItem(FRAMED_SIGN, FRAMED_WALL_SIGN,
				new Item.Properties().setId(signItemKey).stacksTo(16).useBlockDescriptionPrefix()));

		// Bits: custom item (FramedBitsItem) for per-octant placement. dynamicShape()
		// is REQUIRED: its collision/outline shape comes from the block ENTITY
		// (the octants), not the block state. Without it MC caches the shape once
		// per state (bits has none) from an empty world with no BE, so every bits
		// block would collide as that stale cached shape regardless of its octants.
		ResourceKey<Block> bitsKey = ResourceKey.create(Registries.BLOCK, id("framed_bits"));
		FRAMED_BITS = Registry.register(BuiltInRegistries.BLOCK, bitsKey, new FramedBitsBlock(frameProps(bitsKey).dynamicShape()));
		ResourceKey<Item> bitsItemKey = ResourceKey.create(Registries.ITEM, id("framed_bits"));
		Registry.register(BuiltInRegistries.ITEM, bitsItemKey,
			new FramedBitsItem(FRAMED_BITS, new Item.Properties().setId(bitsItemKey).useBlockDescriptionPrefix()));

		// Small bits: the same micro-block on a 4x4x4 grid, so each cell is
		// 4px instead of 8. Its own block and block-entity type, which leaves
		// existing framed bits untouched.
		ResourceKey<Block> smallBitsKey = ResourceKey.create(Registries.BLOCK, id("framed_small_bits"));
		FRAMED_SMALL_BITS = Registry.register(BuiltInRegistries.BLOCK, smallBitsKey,
			new FramedSmallBitsBlock(frameProps(smallBitsKey).dynamicShape()));
		ResourceKey<Item> smallBitsItemKey = ResourceKey.create(Registries.ITEM, id("framed_small_bits"));
		Registry.register(BuiltInRegistries.ITEM, smallBitsItemKey,
			new FramedSmallBitsItem(FRAMED_SMALL_BITS,
				new Item.Properties().setId(smallBitsItemKey).useBlockDescriptionPrefix()));
	}

	/** Shared base properties for the framed blocks (wood-like, non-opaque). */
	static BlockBehaviour.Properties frameProps(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.of()
			.setId(key)
			.mapColor(MapColor.WOOD)
			.strength(2.0f, 3.0f)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.pushReaction(PushReaction.BLOCK);
	}

	/** Stone-flavoured variants (button, pressure plate): no collision box, and
	 *  a piston breaks them off like the vanilla ones. */
	static BlockBehaviour.Properties stoneProps(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.of()
			.setId(key)
			.mapColor(MapColor.STONE)
			.noCollision()
			.strength(0.5f)
			.sound(SoundType.STONE)
			.pushReaction(PushReaction.DESTROY);
	}

	/** Sign board: no collision, breaks quickly, wooden. */
	static BlockBehaviour.Properties signProps(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.of()
			.setId(key)
			.mapColor(MapColor.WOOD)
			.noCollision()
			.strength(1.0f)
			.sound(SoundType.WOOD);
	}

	/** Torch bodies: no collision, instantly breakable, a piston knocks them off.
	 *  The light level is chained on by each torch (14, soul 10, redstone 7). */
	static BlockBehaviour.Properties torchProps(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.of()
			.setId(key)
			.mapColor(MapColor.WOOD)
			.noCollision()
			.strength(0.0f)
			.sound(SoundType.WOOD)
			.pushReaction(PushReaction.DESTROY);
	}

	/** The redstone torch is dim while lit and completely dark once its support
	 *  powers it off, so its light level has to read the state. */
	static BlockBehaviour.Properties redstoneTorchProps(ResourceKey<Block> key) {
		return torchProps(key).lightLevel(s ->
			s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT) ? 7 : 0);
	}

	/** A block with no item form: it is only ever created in world. */
	static Block registerBlockNoItem(String name, Function<ResourceKey<Block>, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(key));
	}

	static Block registerBlock(String name, Function<ResourceKey<Block>, Block> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
			new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, path);
	}
}
