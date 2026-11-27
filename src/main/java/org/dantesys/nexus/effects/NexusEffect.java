package org.dantesys.nexus.effects;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.dantesys.nexus.relic.RelicUtil;
import org.jspecify.annotations.Nullable;

/**
 * Efeito das reliquias. Com damageType, causa dano a cada intervalo (pode matar, diferente do veneno vanilla).
 * Quem nao pode ser curado com leite/Negacao e controlado em RelicEffectRules.
 */
public class NexusEffect extends MobEffect {
    private final @Nullable ResourceKey<DamageType> damageType;
    private final int baseInterval;
    private final float damage;
    private final boolean intervalByLevel;

    public NexusEffect(MobEffectCategory category, int color) {
        this(category, color, null, 0, 0, false);
    }

    public NexusEffect(MobEffectCategory category, int color, @Nullable ResourceKey<DamageType> damageType, int interval, float damage, boolean intervalByLevel) {
        super(category, color);
        this.damageType = damageType;
        this.baseInterval = interval;
        this.damage = damage;
        this.intervalByLevel = intervalByLevel;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        if (damageType == null) return false;
        int interval = intervalByLevel ? Math.max(5, baseInterval >> amplifier) : baseInterval;
        return tickCount % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (damageType != null) entity.hurtServer(level, RelicUtil.source(level, damageType, null), damage);
        return true;
    }
}
