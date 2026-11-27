package org.dantesys.nexus.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.dantesys.nexus.relic.RelicTargets;
import org.dantesys.nexus.relic.RelicUtil;
import org.jspecify.annotations.Nullable;

/** Boca de tubarao da Fisgy: sobe do chao, morde e afunda (animacao "morder", 1,2s). */
public class BocaTubarao extends Entity implements TraceableEntity {
    public static final int LIFE = 24, BITE_TICK = 12;
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BocaTubarao.class, EntityDataSerializers.FLOAT);
    private @Nullable EntityReference<LivingEntity> owner;
    private float damage = 9.0F;

    public BocaTubarao(EntityType<? extends BocaTubarao> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static BocaTubarao spawn(ServerLevel level, LivingEntity owner, double x, double y, double z, float yRot, float damage) {
        return spawn(level, owner, x, y, z, yRot, damage, 1.5F);
    }

    /** size: escala do modelo (1,5 = normal; a Mega Tempest da Trindy usa uma boca gigante). Dano 0 = so visual. */
    public static BocaTubarao spawn(ServerLevel level, LivingEntity owner, double x, double y, double z, float yRot, float damage, float size) {
        BocaTubarao mouth = new BocaTubarao(ModEntities.BOCA_TUBARAO.get(), level);
        mouth.entityData.set(SIZE, size);
        mouth.owner = EntityReference.of(owner);
        mouth.damage = damage;
        mouth.setYRot(yRot);
        mouth.setPos(x, y, z);
        level.addFreshEntity(mouth);
        return mouth;
    }

    @Override public @Nullable LivingEntity getOwner() { return EntityReference.getLivingEntity(owner, level()); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel level)) return;
        if (tickCount == BITE_TICK && damage > 0) bite(level);
        else if (tickCount == BITE_TICK) level.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 3.0f, 0.4f);
        if (tickCount >= LIFE) discard();
    }

    private void bite(ServerLevel level) {
        LivingEntity source = getOwner();
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.2f, 0.6f);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4))) {
            if (!target.isAlive() || target == source) continue;
            if (source instanceof ServerPlayer player && !RelicTargets.canHit(player, target)) continue;
            DamageSource damageSource = damageSources().indirectMagic(this, source);
            target.hurtServer(level, damageSource, RelicUtil.reduced(target, damage));
            if (target.isInWater()) {
                target.setDeltaMovement(0, -1.2, 0);
                RelicUtil.syncMotion(target);
            }
        }
    }

    /** Tempo da animacao em segundos. */
    public float animationTime(float partialTicks) { return (tickCount + partialTicks) / 20.0F; }

    public float size() { return entityData.get(SIZE); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SIZE, 1.5F); }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
}
