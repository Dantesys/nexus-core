package org.dantesys.nexus.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.relic.DraconixEffects;
import org.dantesys.nexus.relic.RelicTargets;
import org.dantesys.nexus.relic.RelicUtil;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Dragao da Draconix (O Verdadeiro Dragao). Voa sempre; o dono monta com clique direito.
 * Sem montaria: segue o dono e morde o alvo dele. Imune a fogo, lava e magia (Pele Escamosa do dragao).
 */
public class DragaoDraconix extends PathfinderMob {
    private static final EntityDataAccessor<Integer> BREATH = SynchedEntityData.defineId(DragaoDraconix.class, EntityDataSerializers.INT);
    public static final float RIDDEN_SPEED = 0.08F;
    private @Nullable UUID owner;
    private int biteCooldown;

    public DragaoDraconix(EntityType<? extends DragaoDraconix> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.CAMERA_DISTANCE, 7.0);
    }

    public void setOwner(Player player) { this.owner = player.getUUID(); }
    public @Nullable UUID ownerId() { return owner; }
    public @Nullable Player ownerPlayer() { return owner == null ? null : level().getPlayerByUUID(owner); }

    /** Animacao da boca aberta (ticks restantes). */
    public int breathTicks() { return entityData.get(BREATH); }
    public void openMouth(int ticks) { entityData.set(BREATH, ticks); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BREATH, 0);
    }

    // ---------- Montaria ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getUUID().equals(owner) || isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this, true, true);
        return InteractionResult.SUCCESS;
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty() && passenger.getUUID().equals(owner); }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override public boolean isFlyingVehicle() { return true; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height() * 0.82, 0.0);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.6F, forward = 0, up = 0;
        if (controller.zza != 0) {
            float cos = Mth.cos(controller.getXRot() * Mth.DEG_TO_RAD), sin = -Mth.sin(controller.getXRot() * Mth.DEG_TO_RAD);
            float dir = controller.zza > 0 ? 1.0F : -0.4F;
            forward = cos * dir;
            up = sin * dir;
        }
        if (controller.isJumping()) up += 0.6F;
        return new Vec3(strafe, up, forward);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        float yRot = getYRot() + Mth.wrapDegrees(controller.getYRot() - getYRot()) * 0.2F;
        setRot(yRot, controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
    }

    @Override
    public void travel(Vec3 input) {
        travelFlying(input, isVehicle() ? RIDDEN_SPEED : 0.02F);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(getX(), getBoundingBox().maxY, getZ());
    }

    // ---------- IA: segue o dono e morde o alvo dele ----------

    @Override
    public void tick() {
        super.tick();
        if (breathTicks() > 0 && !level().isClientSide()) openMouth(breathTicks() - 1);
        if (level().isClientSide() || !(level() instanceof ServerLevel level)) return;
        Player p = ownerPlayer();
        if (!(p instanceof ServerPlayer player) || !player.isAlive()) { discard(); return; }
        if (isVehicle()) { fallDistance = 0; return; }
        if (biteCooldown > 0) biteCooldown--;

        LivingEntity target = target(player);
        Vec3 goal;
        if (target != null) goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
        else {
            double a = Math.toRadians(player.getYRot() + 140);
            goal = player.position().add(Math.sin(a) * 3, 2.5, -Math.cos(a) * 3);
        }
        if (distanceToSqr(player) > 48 * 48) { teleportTo(player.getX(), player.getY() + 3, player.getZ()); return; }
        Vec3 to = goal.subtract(position());
        double speed = target != null ? 0.7 : Math.min(0.8, to.length() * 0.12);
        Vec3 wanted = to.lengthSqr() < 0.25 ? Vec3.ZERO : to.normalize().scale(speed);
        setDeltaMovement(getDeltaMovement().lerp(wanted, 0.2));
        Vec3 look = target != null ? to : getDeltaMovement();
        if (look.horizontalDistanceSqr() > 0.001) {
            float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3F);
            yBodyRot = yHeadRot = getYRot();
        }
        if (target != null && biteCooldown <= 0 && distanceTo(target) < 2.8) bite(level, player, target);
    }

    private @Nullable LivingEntity target(ServerPlayer player) {
        LivingEntity t = player.getLastHurtMob();
        if (t == null || player.tickCount - player.getLastHurtMobTimestamp() > 200) t = player.getLastHurtByMob();
        if (t == null || !t.isAlive() || t == this || t.distanceTo(player) > 24 || !RelicTargets.canHit(player, t)) return null;
        return t;
    }

    private void bite(ServerLevel level, ServerPlayer player, LivingEntity target) {
        biteCooldown = 20;
        openMouth(8);
        target.hurtServer(level, damageSources().mobAttack(this), RelicUtil.reduced(target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE)));
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    /** Baforada gigante a partir da boca do dragao. */
    public void giantBreath(ServerPlayer rider, double power) {
        if (!(level() instanceof ServerLevel level)) return;
        openMouth(30);
        float yaw = (isVehicle() ? rider.getYRot() : getYRot()) * Mth.DEG_TO_RAD, pitch = rider.getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 mouth = position().add(0, getBbHeight() * 0.8, 0).add(look.scale(2.0));
        DraconixEffects.cone(level, rider, mouth, look, 16, 40, (float) (12 * power), 160, this);
        for (int i = 0; i < 160; i++) {
            double d = random.nextDouble() * 16;
            Vec3 spread = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(d * 0.3);
            Vec3 p = mouth.add(look.scale(d)).add(spread);
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0.02);
        }
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 1.2f);
    }

    // ---------- Dano / morte ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.IS_FALL)) return false;
        if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) return false;
        return super.hurtServer(level, source, damage);
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) { return false; }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && ownerPlayer() instanceof ServerPlayer player) DraconixEffects.dragonDied(player);
    }

    @Override protected @Nullable SoundEvent getAmbientSound() { return random.nextInt(4) == 0 ? SoundEvents.ENDER_DRAGON_AMBIENT : null; }
    @Override protected @Nullable SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected float getSoundVolume() { return 0.6F; }
    @Override public float getVoicePitch() { return 1.4F; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {}
    @Override public boolean shouldDropExperience() { return false; }

    @Override protected void addAdditionalSaveData(ValueOutput output) { super.addAdditionalSaveData(output); }
    @Override protected void readAdditionalSaveData(ValueInput input) { super.readAdditionalSaveData(input); }
}
