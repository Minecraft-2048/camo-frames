package com.claude.framedblocks.client;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Delegating {@link BlockStateModel} base — replicates fabric-api's old
 * {@code WrapperBlockStateModel}, which was removed from model-loading v6+
 * (1.21.11's fabric-api only ships Wrapper*Unbaked* wrappers). Subclasses
 * override {@link #emitQuads} and call {@code super.emitQuads} for the wrapped
 * (frame) geometry. createGeometryKey is intentionally NOT overridden: the
 * default (null) keeps camo'd models uncacheable so per-position camo works.
 */
public abstract class WrapperBlockStateModel implements BlockStateModel {
	protected final BlockStateModel wrapped;

	protected WrapperBlockStateModel(BlockStateModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> out) {
		wrapped.collectParts(random, out);
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return wrapped.particleIcon();
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos,
	                      BlockState state, RandomSource random, Predicate<Direction> cull) {
		wrapped.emitQuads(emitter, blockView, pos, state, random, cull);
	}
}
