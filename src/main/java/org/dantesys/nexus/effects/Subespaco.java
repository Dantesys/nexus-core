package org.dantesys.nexus.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Subespaco (Rifty): fora da realidade. Os mixins de colisao e escalada leem este efeito nos dois lados. */
public class Subespaco extends MobEffect {
    public Subespaco(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static boolean active(LivingEntity entity) {
        return entity.hasEffect(ModEffects.SUBESPACO);
    }
}
