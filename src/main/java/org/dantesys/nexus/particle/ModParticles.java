package org.dantesys.nexus.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

/** Particulas do mod. */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Nexus.MODID);

    /** Cultry — Palma da Lotus. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PETALA_LOTUS = PARTICLES.register("petala_lotus", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) { PARTICLES.register(bus); }
}
