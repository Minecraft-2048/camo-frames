package com.claude.framedblocks.client;

import com.claude.framedblocks.block.FramedItemFrameBlock;
import com.claude.framedblocks.block.entity.FramedItemFrameBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the ITEM displayed by the framed item frame. The frame body itself is a
 * chunk model (retextured with the camo like every other framed block); only the
 * floating item lives here. The transforms match the vanilla item-frame entity
 * renderer: item plane 0.4375 from the block centre toward the wall, eight
 * 45-degree steps of spin, scale 0.5, FIXED display mode.
 *
 * <p>Built on the render-state pipeline: what to draw is resolved once, while the
 * block entity is still in reach ({@link #extractRenderState}), and handed to the
 * collector later ({@link #submit}). The item itself is resolved into an
 * {@link ItemStackRenderState}, which is how every item is drawn now.
 */
public class FramedItemFrameRenderer
		implements BlockEntityRenderer<FramedItemFrameBlockEntity, FramedItemFrameRenderer.FrameState> {

	public static class FrameState extends BlockEntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		Direction facing = Direction.NORTH;
		int rotation = 0;
		boolean empty = true;
	}

	public FramedItemFrameRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public FrameState createRenderState() {
		return new FrameState();
	}

	@Override
	public void extractRenderState(FramedItemFrameBlockEntity be, FrameState st, float partialTick,
	                               Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, st, crumbling);
		ItemStack stack = be.getDisplayedItem();
		st.empty = stack.isEmpty();
		st.rotation = be.getRotation();
		BlockState bs = be.getBlockState();
		st.facing = bs.hasProperty(FramedItemFrameBlock.FACING)
			? bs.getValue(FramedItemFrameBlock.FACING) : Direction.NORTH;
		if (st.empty) {
			st.item.clear();
		} else {
			Minecraft.getInstance().getItemModelResolver().updateForTopItem(
				st.item, stack, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
		}
	}

	@Override
	public void submit(FrameState st, PoseStack poseStack, SubmitNodeCollector collector,
	                   CameraRenderState cameraState) {
		if (st.empty) return;

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		// Orient local +z to the FACING direction (rotY(90) maps +z to east).
		switch (st.facing) {
			case SOUTH -> {}
			case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90f));
			case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180f));
			case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(270f));
			case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
			case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(90f));
		}
		// Push the item from the centre toward the frame plane (local -z = the wall).
		poseStack.translate(0f, 0f, -0.4375f);
		// Eight-step spin like vanilla (negative: clockwise as seen by the viewer).
		poseStack.mulPose(Axis.ZP.rotationDegrees(-st.rotation * 45f));
		poseStack.scale(0.5f, 0.5f, 0.5f);

		st.item.submit(poseStack, collector, st.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
