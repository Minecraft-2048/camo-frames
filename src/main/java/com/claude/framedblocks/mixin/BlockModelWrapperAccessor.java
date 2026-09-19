package com.claude.framedblocks.mixin;

import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ModelRenderProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the render properties an ordinary item model was baked with.
 *
 * <p>Every layer of an item carries its OWN display transform: the rotation,
 * translation and scale that place it in the hand, on the ground or in an item
 * frame. A layer added by hand gets none, so it renders unrotated and at full
 * block size while the rest of the item is correctly placed. In the inventory
 * that goes unnoticed, because the GUI transform of a generated item model is
 * the identity; held in hand it looked like a slab floating in front of the
 * player.
 *
 * <p>The properties hold exactly the transforms the model was baked with, but
 * the field is private and nothing exposes it. This accessor hands them over so
 * an added layer can be placed like the ones vanilla built.
 */
@Mixin(BlockModelWrapper.class)
public interface BlockModelWrapperAccessor {
	@Accessor("properties")
	ModelRenderProperties framedblocks$properties();
}
