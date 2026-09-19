package com.claude.framedblocks.mixin;

import com.claude.framedblocks.item.FramedArmorGrade;
import com.claude.framedblocks.item.FramedArmorItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Framed armour wearing BEDROCK cancels the damage it covers.
 *
 * <p>Each slot carries the share of a full set's protection it is worth: 15% for
 * the helmet, 40% for the chestplate, 30% for the leggings, 15% for the boots.
 * Wear the four and the shares add up to 1, so nothing gets through at all; wear
 * only the chestplate and 40% of every hit is cancelled.
 *
 * <p>Damage that bypasses invulnerability is left alone on purpose: {@code /kill}
 * and the void still work. Armour that made a player unkillable in a bedrock void
 * would be a trap, not a reward.
 */
@Mixin(LivingEntity.class)
public abstract class BedrockArmorMixin {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float framedblocks$bedrockArmor(float amount, ServerLevel level, DamageSource source, float original) {
		if (amount <= 0.0f || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return amount;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		float covered = 0.0f;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack worn = self.getItemBySlot(slot);
			if (FramedArmorGrade.isBedrockFramed(worn)) {
				covered += FramedArmorGrade.coverage(((FramedArmorItem) worn.getItem()).armorType);
			}
		}
		if (covered <= 0.0f) return amount;
		if (covered >= 0.999f) return 0.0f;
		return amount * (1.0f - covered);
	}
}
