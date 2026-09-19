package com.claude.framedblocks;

import com.claude.framedblocks.block.ModBlocks;
import com.claude.framedblocks.block.entity.FramedCamoDrops;
import com.claude.framedblocks.block.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FramedBlocksMod implements ModInitializer {
	public static final String MOD_ID = "framedblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		// The armour camo lives in a data component, so the type must exist
		// before the items that use it.
		com.claude.framedblocks.item.ModDataComponents.register();
		com.claude.framedblocks.item.ModItems.register();
		registerCreativeTab();

		// Right-click with a framed tool fires BEFORE the block's own use handler,
		// so the hammer/wrench work on interactive framed blocks (door, lever…) too,
		// no sneaking needed. PASS lets every other right-click through untouched.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			var item = player.getItemInHand(hand).getItem();
			if (item == com.claude.framedblocks.item.ModItems.FRAMED_HAMMER) {
				return com.claude.framedblocks.item.FramedHammerItem.use(level, hit, player);
			}
			if (item == com.claude.framedblocks.item.ModItems.FRAMED_WRENCH) {
				return com.claude.framedblocks.item.FramedWrenchItem.use(level, hit.getBlockPos(), player);
			}
			return InteractionResult.PASS;
		});

		// Breaking a camo'd framed block drops ALL its camo blocks too (on top of
		// the framed block from the loot table): one for a normal block, several
		// for multi-camo blocks (bits, double panel), and both halves for a door.
		// Fires while the block entity still exists; survival player breaks only.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!level.isClientSide() && !player.getAbilities().instabuild) {
				java.util.List<net.minecraft.world.level.block.state.BlockState> camos = new java.util.ArrayList<>();
				if (blockEntity instanceof FramedCamoDrops d) camos.addAll(d.getDroppedCamos());
				// A door is two blocks; the other half is removed without its own
				// break event, so collect its camo here too.
				if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
					net.minecraft.core.BlockPos other =
						state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
							== net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos.above() : pos.below();
					if (level.getBlockEntity(other) instanceof FramedCamoDrops d2) {
						camos.addAll(d2.getDroppedCamos());
					}
				}
				for (net.minecraft.world.level.block.state.BlockState c : camos) {
					Block.popResource(level, pos, new ItemStack(c.getBlock()));
				}
				// Bits has no loot table (its item count is dynamic): drop one
				// framed-bits item per present octant so mining it isn't a loss.
				if (blockEntity instanceof com.claude.framedblocks.block.entity.FramedBitsBlockEntity bits) {
					for (int k = 0; k < bits.count(); k++) {
						Block.popResource(level, pos, new ItemStack(ModBlocks.FRAMED_BITS));
					}
				}
			}
			return true;
		});

		LOGGER.info("Framed Blocks (1.21.11) — registered");
	}

	/**
	 * Vanilla creative tab (the Fabric item-group module was restructured in
	 * 26.1 and isn't reachable here, so we register a plain CreativeModeTab —
	 * always available). Modded tabs registered in the registry show up in the
	 * creative menu.
	 */
	private static void registerCreativeTab() {
		ResourceKey<CreativeModeTab> key = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
			.title(Component.translatable("itemGroup.framedblocks.main"))
			.icon(() -> new ItemStack(ModBlocks.FRAMED_BLOCK))
			.displayItems((params, output) -> {
				output.accept(ModBlocks.FRAMED_BLOCK);
				output.accept(ModBlocks.FRAMED_SLAB);
				output.accept(ModBlocks.FRAMED_VERTICAL_SLAB);
				output.accept(ModBlocks.FRAMED_STAIRS);
				output.accept(ModBlocks.FRAMED_FENCE);
				output.accept(ModBlocks.FRAMED_FENCE_GATE);
				output.accept(ModBlocks.FRAMED_BUTTON);
				output.accept(ModBlocks.FRAMED_DOOR);
				output.accept(ModBlocks.FRAMED_TRAPDOOR);
				output.accept(ModBlocks.FRAMED_PRESSURE_PLATE);
				output.accept(ModBlocks.FRAMED_WALL);
				output.accept(ModBlocks.FRAMED_LEVER);
				output.accept(ModBlocks.FRAMED_CORNER_PILLAR);
				output.accept(ModBlocks.FRAMED_PILLAR);
				output.accept(ModBlocks.FRAMED_HALF_PILLAR);
				output.accept(ModBlocks.FRAMED_SLOPE);
				output.accept(ModBlocks.FRAMED_SLOPE_INNER_CORNER);
				output.accept(ModBlocks.FRAMED_PYRAMID);
				output.accept(ModBlocks.FRAMED_BITS);
				output.accept(ModBlocks.FRAMED_SMALL_BITS);
				output.accept(ModBlocks.FRAMED_DOUBLE_PANEL);
				output.accept(ModBlocks.FRAMED_CHEST);
				output.accept(ModBlocks.FRAMED_GLOWING_CUBE);
				output.accept(ModBlocks.FRAMED_BOUNCY_CUBE);
				output.accept(ModBlocks.FRAMED_CUSHION);
				output.accept(ModBlocks.FRAMED_LADDER);
				output.accept(ModBlocks.FRAMED_BARS);
				output.accept(ModBlocks.FRAMED_POST);
				output.accept(ModBlocks.FRAMED_LARGE_BUTTON);
				output.accept(ModBlocks.FRAMED_IRON_DOOR);
				output.accept(ModBlocks.FRAMED_PANEL);
				output.accept(ModBlocks.FRAMED_FLOOR_BOARD);
				output.accept(ModBlocks.FRAMED_WALL_BOARD);
				output.accept(ModBlocks.FRAMED_SLAB_EDGE);
				output.accept(ModBlocks.FRAMED_SLAB_CORNER);
				output.accept(ModBlocks.FRAMED_HALF_STAIRS);
				output.accept(ModBlocks.FRAMED_VERTICAL_STAIRS);
				output.accept(ModBlocks.FRAMED_BOOKSHELF);
				output.accept(ModBlocks.FRAMED_STONE_BUTTON);
				output.accept(ModBlocks.FRAMED_STONE_PRESSURE_PLATE);
				output.accept(ModBlocks.FRAMED_TORCH);
				output.accept(ModBlocks.FRAMED_SOUL_TORCH);
				output.accept(ModBlocks.FRAMED_REDSTONE_TORCH);
				output.accept(ModBlocks.FRAMED_SECRET_STORAGE);
				output.accept(ModBlocks.FRAMED_VERTICAL_SLOPE);
				output.accept(ModBlocks.FRAMED_ITEM_FRAME);
				output.accept(ModBlocks.FRAMED_SIGN);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_HAMMER);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_WRENCH);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_HELMET);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_CHESTPLATE);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_LEGGINGS);
				output.accept(com.claude.framedblocks.item.ModItems.FRAMED_BOOTS);
			})
			.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, tab);
	}
}
