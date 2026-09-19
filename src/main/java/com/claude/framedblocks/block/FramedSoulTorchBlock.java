package com.claude.framedblocks.block;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The framed torch burning soul fire: same body, same placement, but a blue
 * flame and the dimmer soul-torch light level (10 instead of 14, set where the
 * block is registered).
 */
public class FramedSoulTorchBlock extends FramedTorchBlock {
	public FramedSoulTorchBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	protected ParticleOptions flameParticle() {
		return ParticleTypes.SOUL_FIRE_FLAME;
	}
}
