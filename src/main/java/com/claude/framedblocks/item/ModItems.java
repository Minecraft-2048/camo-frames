package com.claude.framedblocks.item;

import com.claude.framedblocks.FramedBlocksMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;

/** Non-block items: the framed tools and the framed armour. */
public final class ModItems {
	public static Item FRAMED_HAMMER;
	public static Item FRAMED_WRENCH;
	public static Item FRAMED_HELMET;
	public static Item FRAMED_CHESTPLATE;
	public static Item FRAMED_LEGGINGS;
	public static Item FRAMED_BOOTS;

	private ModItems() {}

	public static void register() {
		FRAMED_HAMMER = registerItem("framed_hammer",
			key -> new FramedHammerItem(new Item.Properties().setId(key).stacksTo(1)));
		FRAMED_WRENCH = registerItem("framed_wrench",
			key -> new FramedWrenchItem(new Item.Properties().setId(key).stacksTo(1)));

		// Armour: the block is applied in an anvil (see AnvilCamoMixin).
		FRAMED_HELMET = registerArmor("framed_helmet", ArmorType.HELMET);
		FRAMED_CHESTPLATE = registerArmor("framed_chestplate", ArmorType.CHESTPLATE);
		FRAMED_LEGGINGS = registerArmor("framed_leggings", ArmorType.LEGGINGS);
		FRAMED_BOOTS = registerArmor("framed_boots", ArmorType.BOOTS);
	}

	private static Item registerArmor(String name, ArmorType type) {
		return registerItem(name, key -> new FramedArmorItem(type, new Item.Properties()
			.setId(key)
			.stacksTo(1)
			.humanoidArmor(FramedArmorMaterials.INSTANCE, type)));
	}

	private static Item registerItem(String name, Function<ResourceKey<Item>, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
	}
}
