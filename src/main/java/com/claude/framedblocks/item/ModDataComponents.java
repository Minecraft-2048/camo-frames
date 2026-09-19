package com.claude.framedblocks.item;

import com.claude.framedblocks.FramedBlocksMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * Item data components. The 1.20.1 version kept the armour camo in stack NBT;
 * NBT on stacks is gone since 1.20.5, so the block id lives in a component of
 * our own instead.
 */
public final class ModDataComponents {
	/** Registry id of the block applied to a framed armour piece. */
	public static DataComponentType<String> CAMO;

	private ModDataComponents() {}

	public static void register() {
		CAMO = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(FramedBlocksMod.MOD_ID, "camo"),
			DataComponentType.<String>builder()
				.persistent(Codec.STRING)
				.networkSynchronized(ByteBufCodecs.STRING_UTF8)
				.build());
	}
}
