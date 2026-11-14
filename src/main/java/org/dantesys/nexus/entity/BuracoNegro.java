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

/** Visual do Black Hole da Rifty (animacao "girar", 2s em loop). Quem controla a duracao e o RiftyEffects. */
public class BuracoNegro extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BuracoNegro.class, EntityDataSerializers.FLOAT);

    public BuracoNegro(EntityType<? extends BuracoNegro> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** O centro do modelo fica meio bloco (vezes a escala) acima da posicao da entidade. */
    public static BuracoNegro spawn(ServerLevel level, Vec3 center, float size) {
        BuracoNegro hole = new BuracoNegro(ModEntities.BURACO_NEGRO.get(), level);
        hole.setSize(size);
        hole.setPos(center.x, center.y - 0.5 * size, center.z);
        level.addFreshEntity(hole);
        return hole;
    }

    public float size() { return entityData.get(SIZE); }

    public void setSize(float size) {
        if (Math.abs(size() - size) > 0.01f) entityData.set(SIZE, size);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SIZE, 1.5F); }
    @Override public void tick() { super.tick(); }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
}
