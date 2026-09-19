package com.claude.framedblocks.item;

import com.claude.framedblocks.FramedBlocksMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Iron-grade protection, repaired with framed blocks.
 *
 * <p>The 1.20.1 version implemented the old {@code ArmorMaterial} interface.
 * Since 1.20.5 the material is a record, and it carries an EQUIPMENT ASSET key
 * naming the worn-armour texture set. Ours points at
 * {@code assets/framedblocks/equipment/framed.json}, which declares no layers on
 * purpose: the worn look is drawn by FramedArmorRenderer from a texture built at
 * runtime for the applied block, so there is nothing for vanilla to draw.
 */
public final class FramedArmorMaterials {
	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(
		EquipmentAssets.ROOT_ID,
		Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "framed"));

	/** Items that repair the armour in an anvil (the framed blocks). */
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "repairs_framed_armor"));

	public static final ArmorMaterial INSTANCE = new ArmorMaterial(
		15,                                   // durability multiplier, same as iron
		Map.of(
			ArmorType.HELMET, 2,
			ArmorType.CHESTPLATE, 6,
			ArmorType.LEGGINGS, 5,
			ArmorType.BOOTS, 2,
			ArmorType.BODY, 4),
		9,                                    // enchantability
		SoundEvents.ARMOR_EQUIP_IRON,
		0.0f,                                 // toughness
		0.0f,                                 // knockback resistance
		REPAIRS,
		ASSET);

	private FramedArmorMaterials() {}
}
