package org.dantesys.nexus.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.util.Elementos;

import java.util.Comparator;

public final class SimpleElementalSpells {
    private static final int MAX_AFFINITY = 1000;
    private static final int SECONDARY_EFFECT_AFFINITY = 600;

    private SimpleElementalSpells() {
    }

    public static boolean cast(ServerPlayer player, Elementos elemento, int affinity) {
        int clampedAffinity = Mth.clamp(affinity, 0, MAX_AFFINITY);
        return switch (elemento) {
            case IGNEA -> castFlameBurst(player, clampedAffinity);
            case VIDA -> castRecovery(player, clampedAffinity);
            case LUZ -> castIlluminatedVision(player, clampedAffinity);
            case SOMBRA -> castShadowStep(player, clampedAffinity);
            case METAL -> castSteelSkin(player, clampedAffinity);
            case TEMPESTADE -> castElectricStep(player, clampedAffinity);
            case NAO_ELEMENTAL -> false;
        };
    }

    private static boolean castFlameBurst(ServerPlayer player, int affinity) {
        double range = scale(5.0D, affinity);
        particleLine(player, ParticleTypes.FLAME, range, 0.25D);
        LivingEntity target = findLookTarget(player, range);
        if (target != null) {
            float damage = (float) scale(2.0D, affinity);
            target.hurt(player.level().damageSources().playerAttack(player), damage);
            target.igniteForSeconds((float) scale(2.0D, affinity));
        }
        return true;
    }

    private static boolean castRecovery(ServerPlayer player, int affinity) {
        particleBurst(player, ParticleTypes.HEART, 18, 0.8D);
        particleBurst(player, ParticleTypes.HAPPY_VILLAGER, 22, 1.0D);
        player.addEffect(effect(MobEffects.REGENERATION, scaledTicks(2, affinity), effectAmplifier(affinity)));
        if (affinity >= SECONDARY_EFFECT_AFFINITY) {
            player.addEffect(effect(MobEffects.SATURATION, scaledTicks(2, affinity), effectAmplifier(affinity)));
        }
        return true;
    }

    private static boolean castIlluminatedVision(ServerPlayer player, int affinity) {
        particleBurst(player, ParticleTypes.END_ROD, 32, scale(1.0D, affinity));
        player.addEffect(effect(MobEffects.NIGHT_VISION, scaledTicks(10, affinity), 0));
        if (affinity >= SECONDARY_EFFECT_AFFINITY) {
            double range = scale(5.0D, affinity);
            int duration = scaledTicks(2, affinity);
            AABB area = player.getBoundingBox().inflate(range);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive())) {
                target.addEffect(effect(MobEffects.BLINDNESS, duration, effectAmplifier(affinity)));
            }
        }
        return true;
    }

    private static boolean castShadowStep(ServerPlayer player, int affinity) {
        particleBurst(player, ParticleTypes.LARGE_SMOKE, 28, scale(0.8D, affinity));
        particleBurst(player, ParticleTypes.PORTAL, 35, scale(0.9D, affinity));
        int duration = scaledTicks(2, affinity);
        player.addEffect(effect(MobEffects.INVISIBILITY, duration, effectAmplifier(affinity)));
        if (affinity >= SECONDARY_EFFECT_AFFINITY) {
            player.addEffect(effect(MobEffects.SPEED, duration, effectAmplifier(affinity)));
        }
        return true;
    }

    private static boolean castSteelSkin(ServerPlayer player, int affinity) {
        particleBurst(player, ParticleTypes.CRIT, 30, scale(0.8D, affinity));
        particleBurst(player, ParticleTypes.ASH, 24, scale(0.7D, affinity));
        int duration = scaledTicks(2, affinity);
        player.addEffect(effect(MobEffects.RESISTANCE, duration, effectAmplifier(affinity)));
        if (affinity >= SECONDARY_EFFECT_AFFINITY) {
            player.addEffect(effect(MobEffects.STRENGTH, duration, effectAmplifier(affinity)));
        }
        return true;
    }

    private static boolean castElectricStep(ServerPlayer player, int affinity) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        double range = scale(5.0D, affinity);
        particleLine(player, ParticleTypes.ELECTRIC_SPARK, range, 0.2D);
        particleBurst(player, ParticleTypes.ELECTRIC_SPARK, 35, 0.8D);
        Vec3 destination = findTeleportDestination(player, direction, range);
        if (destination == null) {
            return false;
        }

        player.teleportTo(destination.x, destination.y, destination.z);
        particleBurst(player, ParticleTypes.ELECTRIC_SPARK, 35, 0.8D);
        if (affinity >= SECONDARY_EFFECT_AFFINITY) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.setPos(origin.x, origin.y, origin.z);
                level.addFreshEntity(lightning);
            }
        }
        return true;
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchArea = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                        entity -> entity != player && entity.isAlive() && player.hasLineOfSight(entity))
                .stream()
                .filter(entity -> {
                    Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(eye);
                    return toTarget.lengthSqr() <= range * range
                            && look.dot(toTarget.normalize()) >= 0.94D;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static Vec3 findTeleportDestination(ServerPlayer player, Vec3 direction, double range) {
        Vec3 origin = player.position();
        for (double distance = range; distance >= 0.5D; distance -= 0.5D) {
            Vec3 destination = origin.add(direction.scale(distance));
            AABB movedBox = player.getBoundingBox().move(destination.subtract(origin));
            if (player.level().noCollision(player, movedBox)) {
                return destination;
            }
        }
        return null;
    }

    private static MobEffectInstance effect(
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            int duration,
            int amplifier
    ) {
        return new MobEffectInstance(effect, duration, amplifier, true, true, true);
    }

    private static void particleLine(
            ServerPlayer player,
            ParticleOptions particle,
            double range,
            double spacing
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        for (double distance = 0.25D; distance <= range; distance += spacing) {
            Vec3 position = origin.add(direction.scale(distance));
            level.sendParticles(particle, position.x, position.y, position.z,
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void particleBurst(
            ServerPlayer player,
            ParticleOptions particle,
            int count,
            double spread
    ) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    particle,
                    player.getX(),
                    player.getY() + player.getBbHeight() * 0.5D,
                    player.getZ(),
                    count,
                    spread,
                    player.getBbHeight() * 0.5D,
                    spread,
                    0.05D
            );
        }
    }

    private static int scaledTicks(int baseSeconds, int affinity) {
        return Math.max(1, (int) Math.round(scale(baseSeconds * 20.0D, affinity)));
    }

    private static int effectAmplifier(int affinity) {
        return affinity / 250;
    }

    private static double scale(double base, int affinity) {
        return base * (1.0D + affinity / (double) MAX_AFFINITY);
    }
}
