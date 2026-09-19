package com.claude.framedblocks.client;

import com.claude.framedblocks.item.FramedArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws worn framed armour.
 *
 * <p>Renders the vanilla armour models against a texture built by
 * {@link FramedArmorTextures}: the vanilla iron layer recoloured with the applied
 * block. Keeping a real armour sheet is what makes it read as armour, since its
 * alpha cuts the silhouette (so skin shows where it should) and its light/dark
 * pixels give the plates their relief; only the colour comes from the block.
 *
 * <p>Vanilla draws nothing of its own here: the equipment asset
 * {@code framedblocks:framed} declares no layers, so this renderer is the only
 * thing that paints the armour.
 */
public class FramedArmorRenderer implements ArmorRenderer {
	/**
	 * Un modele par emplacement : le jeu en definit un pour chacun, avec sa propre
	 * epaisseur. Partager celui du torse pour le casque, le plastron et les bottes
	 * les rendait invisibles — la geometrie ne correspondait pas a la piece.
	 */
	private final java.util.Map<EquipmentSlot, HumanoidModel<HumanoidRenderState>> models =
		new java.util.EnumMap<>(EquipmentSlot.class);

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
	                   HumanoidRenderState state, EquipmentSlot slot, int light,
	                   HumanoidModel<HumanoidRenderState> contextModel) {
		Minecraft mc = Minecraft.getInstance();
		HumanoidModel<HumanoidRenderState> model = models.computeIfAbsent(slot,
			s -> new HumanoidModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_ARMOR.get(s))));
		// Leggings sit on the thin inner sheet (texture layer 2), the rest on the
		// outer one (layer 1): the same split as vanilla armour.
		boolean legs = slot == EquipmentSlot.LEGS;
		setVisible(model, slot);

		BlockState camo = FramedArmorItem.getCamo(stack);
		Identifier texture = FramedArmorTextures.get(mc, camo, legs ? 2 : 1);
		if (texture == null) {
			texture = Identifier.withDefaultNamespace("textures/entity/equipment/humanoid"
				+ (legs ? "_leggings" : "") + "/iron.png");
		}

		// Copies the pose of the entity model onto our armour model, then submits it.
		ArmorRenderer.submitTransformCopyingModel(
			contextModel, state, model, state, true,
			collector, poseStack, RenderTypes.armorCutoutNoCull(texture),
			light, OverlayTexture.NO_OVERLAY, 0, null);
	}

	/** Only the parts belonging to this slot, exactly like vanilla armour. */
	private static void setVisible(HumanoidModel<HumanoidRenderState> model, EquipmentSlot slot) {
		model.setAllVisible(false);
		switch (slot) {
			case HEAD -> {
				model.head.visible = true;
				model.hat.visible = true;
			}
			case CHEST -> {
				model.body.visible = true;
				model.rightArm.visible = true;
				model.leftArm.visible = true;
			}
			case LEGS -> {
				model.body.visible = true;
				model.rightLeg.visible = true;
				model.leftLeg.visible = true;
			}
			case FEET -> {
				model.rightLeg.visible = true;
				model.leftLeg.visible = true;
			}
			default -> { }
		}
	}
}
