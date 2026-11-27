package org.dantesys.nexus.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Palma dourada gigante da Palma da Lotus (Cultry): desce do ceu, bate no chao e afunda. So visual. */
public class PalmaLotus extends Entity {
    public static final int LIFE = 14, SLAM_TICK = 4;
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(PalmaLotus.class, EntityDataSerializers.FLOAT);

    public PalmaLotus(EntityType<? extends PalmaLotus> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static PalmaLotus spawn(ServerLevel level, double x, double y, double z, float yRot, float size) {
        PalmaLotus palm = new PalmaLotus(ModEntities.PALMA_LOTUS.get(), level);
        palm.entityData.set(SIZE, size);
        palm.setYRot(yRot);
        palm.setPos(x, y, z);
        level.addFreshEntity(palm);
        return palm;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount >= LIFE) discard();
    }

    public float size() { return entityData.get(SIZE); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SIZE, 4.0F); }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
}
