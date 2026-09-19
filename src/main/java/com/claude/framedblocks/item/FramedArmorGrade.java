package com.claude.framedblocks.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/**
 * How strong a framed armour piece is, decided by the block applied to it.
 *
 * <p>The rule is vanilla's own: whatever TOOL TIER the block needs to be mined
 * sets the grade. A block of emerald needs an iron pickaxe, so emerald armour
 * comes out one notch above iron; obsidian needs a diamond pickaxe and comes out
 * netherite-grade; dirt or planks need nothing and leave the piece as it was.
 *
 * <p>Applying a block never makes the armour WORSE. {@link #NONE} is exactly the
 * bare piece, and every grade above it raises protection, toughness AND
 * durability together, so combining is always worth doing.
 *
 * <p>Using the tool tags rather than a hand-written block list means modded
 * blocks are graded too, as long as they declare their mining level the normal
 * way. Bedrock is the one hard-coded case: it belongs to no tier at all, being
 * unmineable, and is the deliberate top of the ladder.
 */
public enum FramedArmorGrade {
	/** Bare, or a block that needs no particular tool. Iron, like the base item. */
	NONE(2, 6, 5, 2, 0.0f, 0.0f, 15),
	/** Needs a stone tool: between iron and diamond. */
	STONE(3, 7, 5, 2, 1.0f, 0.0f, 24),
	/** Needs an iron tool (emerald, gold, diamond blocks): diamond-grade. */
	IRON(3, 8, 6, 3, 2.0f, 0.0f, 33),
	/** Needs a diamond tool (obsidian, ancient debris): netherite-grade. */
	DIAMOND(3, 8, 6, 3, 3.0f, 0.1f, 37),
	/** Bedrock. Netherite stats, never wears out, and cancels damage outright. */
	BEDROCK(3, 8, 6, 3, 3.0f, 0.1f, 37);

	private final ArmorMaterial material;
	private final int durabilityMultiplier;

	FramedArmorGrade(int helmet, int chestplate, int leggings, int boots,
	                 float toughness, float knockbackResistance, int durabilityMultiplier) {
		this.durabilityMultiplier = durabilityMultiplier;
		this.material = new ArmorMaterial(
			durabilityMultiplier,
			Map.of(
				ArmorType.HELMET, helmet,
				ArmorType.CHESTPLATE, chestplate,
				ArmorType.LEGGINGS, leggings,
				ArmorType.BOOTS, boots,
				ArmorType.BODY, chestplate - 2),
			9,
			SoundEvents.ARMOR_EQUIP_IRON,
			toughness,
			knockbackResistance,
			FramedArmorMaterials.REPAIRS,
			FramedArmorMaterials.ASSET);
	}

	public ArmorMaterial material() {
		return material;
	}

	public int durabilityFor(ArmorType type) {
		return type.getDurability(durabilityMultiplier);
	}

	/** The grade a block confers, from the tool tier it needs. */
	public static FramedArmorGrade of(Block block) {
		if (block == Blocks.BEDROCK) return BEDROCK;
		var state = block.defaultBlockState();
		if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return DIAMOND;
		if (state.is(BlockTags.NEEDS_IRON_TOOL)) return IRON;
		if (state.is(BlockTags.NEEDS_STONE_TOOL)) return STONE;
		return NONE;
	}

	/**
	 * The share of a full set's protection this slot carries: the armour points of
	 * the slot over the 20 of a whole set (3/8/6/3). A full bedrock set therefore
	 * comes to exactly 1, and each missing piece leaves its own share of the damage
	 * getting through.
	 */
	public static float coverage(ArmorType type) {
		return switch (type) {
			case HELMET -> 0.15f;
			case CHESTPLATE -> 0.40f;
			case LEGGINGS -> 0.30f;
			case BOOTS -> 0.15f;
			default -> 0.0f;
		};
	}

	/** Whether this stack is a framed piece wearing bedrock. */
	public static boolean isBedrockFramed(ItemStack stack) {
		return stack.getItem() instanceof FramedArmorItem
			&& FramedArmorItem.getCamo(stack).is(Blocks.BEDROCK);
	}
}
