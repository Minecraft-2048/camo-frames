package com.claude.framedblocks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Armour that takes on the look of any block. The block is applied in an ANVIL
 * (armour piece + block) and stored on the stack in the {@code framedblocks:camo}
 * data component; the worn look comes from FramedArmorRenderer, which draws the
 * vanilla armour model with a texture built from that block.
 */
public class FramedArmorItem extends Item {
	/** Which piece this is: needed to recompute the protection when a block is
	 *  applied, since the values differ per slot. */
	public final ArmorType armorType;

	public FramedArmorItem(ArmorType armorType, Properties properties) {
		super(properties);
		this.armorType = armorType;
	}

	/**
	 * Says which block the piece was combined with, and what that buys.
	 *
	 * <p>Nothing else on the stack shows it: the worn look and the icon are drawn
	 * from the applied block, but a player comparing two pieces in a chest has no
	 * way to tell them apart.
	 */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                            Consumer<Component> adder, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, adder, flag);
		BlockState camo = getCamo(stack);
		if (camo.isAir()) return;
		adder.accept(Component.translatable("item.framedblocks.framed_armor.camo",
				Component.translatable(camo.getBlock().getDescriptionId()))
			.withStyle(ChatFormatting.GRAY));
		if (camo.is(Blocks.BEDROCK)) {
			int percent = Math.round(FramedArmorGrade.coverage(armorType) * 100f);
			adder.accept(Component.translatable("item.framedblocks.framed_armor.bedrock", percent)
				.withStyle(ChatFormatting.AQUA));
		}
	}

	/** The block applied to this piece, or AIR when it is still bare. */
	public static BlockState getCamo(ItemStack stack) {
		String raw = stack.get(ModDataComponents.CAMO);
		if (raw == null) return Blocks.AIR.defaultBlockState();
		Identifier id = Identifier.tryParse(raw);
		if (id == null) return Blocks.AIR.defaultBlockState();
		Block block = BuiltInRegistries.BLOCK.getValue(id);
		return block == Blocks.AIR ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
	}

	public static void setCamo(ItemStack stack, Block block) {
		stack.set(ModDataComponents.CAMO, BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	/** Same rule as the blocks: anything but air and our own framed blocks. */
	public static boolean isValidCamo(Block block) {
		if (block == Blocks.AIR || block.defaultBlockState().isAir()) return false;
		return !"framedblocks".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
	}
}
