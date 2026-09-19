package com.claude.framedblocks.mixin;

import com.claude.framedblocks.item.FramedArmorGrade;
import com.claude.framedblocks.item.FramedArmorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applying a block to framed armour, in an anvil: armour piece + any block, and
 * the result wears that block.
 *
 * <p>Runs before vanilla builds its own result and cancels it, so the usual
 * repair/rename rules never see this combination.
 *
 * <p>The slots are reached through {@code getSlot(int)} rather than the
 * {@code inputSlots} / {@code resultSlots} fields: those are declared on the
 * parent {@code ItemCombinerMenu}, and a mixin can only shadow members of the
 * class it targets. Shadowing them crashed the game at bootstrap.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilCamoMixin {
	@Shadow @Final private DataSlot cost;
	/** How many items vanilla takes from the right slot; 1 = a single block. */
	@Shadow private int repairItemCountCost;

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void framedblocks$applyArmorCamo(CallbackInfo ci) {
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
		ItemStack a = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack b = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();

		// Accept the armour in either slot: players put the "material" on
		// whichever side feels natural.
		ItemStack armor = null, material = null;
		if (a.getItem() instanceof FramedArmorItem) { armor = a; material = b; }
		else if (b.getItem() instanceof FramedArmorItem) { armor = b; material = a; }
		if (armor == null || !(material.getItem() instanceof BlockItem blockItem)) return;

		Block block = blockItem.getBlock();
		if (!FramedArmorItem.isValidCamo(block)) return;

		ItemStack result = armor.copy();
		result.setCount(1);
		FramedArmorItem.setCamo(result, block);
		// The worn armour is drawn from the block texture, but the INVENTORY icon
		// is a flat sprite: it takes the colour of the block through the vanilla
		// custom_model_data tint, the same mechanism dyed leather armour uses.
		result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
			List.of(), List.of(), List.of(), List.of(block.defaultMapColor().col)));

		// The piece takes the strength of the block it wears. The grade follows the
		// tool tier the block needs, so a block of emerald (iron pickaxe) gives
		// iron-grade armour and obsidian (diamond pickaxe) gives diamond-grade.
		ArmorType type = ((FramedArmorItem) armor.getItem()).armorType;
		FramedArmorGrade grade = FramedArmorGrade.of(block);
		result.set(DataComponents.ATTRIBUTE_MODIFIERS, grade.material().createAttributes(type));
		int maxDamage = grade.durabilityFor(type);
		result.set(DataComponents.MAX_DAMAGE, maxDamage);
		// Bedrock is the top of the ladder: the piece stops wearing out entirely.
		// Re-combining with anything else takes that back off.
		if (grade == FramedArmorGrade.BEDROCK) {
			result.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		} else {
			result.remove(DataComponents.UNBREAKABLE);
		}
		// A piece already worn down must not end up damaged past its new maximum.
		Integer damage = result.get(DataComponents.DAMAGE);
		if (damage != null && damage > maxDamage) {
			result.set(DataComponents.DAMAGE, maxDamage);
		}

		this.cost.set(1);
		this.repairItemCountCost = 1;
		self.getSlot(AnvilMenu.RESULT_SLOT).set(result);
		self.broadcastChanges();
		ci.cancel();
	}
}
