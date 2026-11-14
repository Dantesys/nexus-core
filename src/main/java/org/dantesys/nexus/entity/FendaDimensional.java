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
import net.minecraft.world.phys.Vec3;

/**
 * Fenda no ar da Rifty. LINEAR: rachadura ao longo de uma linha (Dobra Dimensional), da posicao
 * ate LENGTH blocos na direcao (yRot/xRot), que se fecha. RADIAL: fenda que se abre em cima do alvo (Dimensional Crash).
 */
public class FendaDimensional extends Entity {
    public static final int LINEAR = 0, RADIAL = 1;
    public static final int LINEAR_LIFE = 20, RADIAL_LIFE = 16;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(FendaDimensional.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(FendaDimensional.class, EntityDataSerializers.FLOAT);

    public FendaDimensional(EntityType<? extends FendaDimensional> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FendaDimensional line(ServerLevel level, Vec3 from, Vec3 to) {
        FendaDimensional crack = new FendaDimensional(ModEntities.FENDA.get(), level);
        Vec3 dir = to.subtract(from);
        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        crack.entityData.set(KIND, LINEAR);
        crack.entityData.set(LENGTH, (float) dir.length());
        crack.setYRot((float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0));
        crack.setXRot((float) -Math.toDegrees(Math.atan2(dir.y, horizontal)));
        crack.setPos(from);
        level.addFreshEntity(crack);
        return crack;
    }

    public static FendaDimensional radial(ServerLevel level, Vec3 center, float size) {
        FendaDimensional crack = new FendaDimensional(ModEntities.FENDA.get(), level);
        crack.entityData.set(KIND, RADIAL);
        crack.entityData.set(LENGTH, size);
        crack.setPos(center);
        level.addFreshEntity(crack);
        return crack;
    }

    public int kind() { return entityData.get(KIND); }
    public float length() { return entityData.get(LENGTH); }
    public int life() { return kind() == RADIAL ? RADIAL_LIFE : LINEAR_LIFE; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount >= life()) discard();
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, LINEAR);
        builder.define(LENGTH, 1.0F);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
}
