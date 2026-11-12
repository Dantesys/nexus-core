package org.dantesys.nexus.effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT,Nexus.MODID);

    public static final Holder<MobEffect> PELE_MANA = EFFECTS.register("pele_mana", () -> new PeleMana(
            //Can be either BENEFICIAL, NEUTRAL or HARMFUL. Used to determine the potion tooltip color of this effect.
            MobEffectCategory.BENEFICIAL,
            //The color of the effect particles in RGB format.
            0xffffff
    ));

    public static void register(IEventBus bus){
        EFFECTS.register(bus);
    }
}
