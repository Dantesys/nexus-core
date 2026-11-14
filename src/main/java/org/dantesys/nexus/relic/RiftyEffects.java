package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.effects.Subespaco;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.magic.MagicStatus;

import java.util.*;

/** Efeitos da Rifty (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RiftyEffects {
    private RiftyEffects() {}

    public static final int STEP_MANA = 5, STEP_COOLDOWN = 100, STEP_DISTANCE = 5, AIM_RANGE = 4;
    public static final int FOLD_MANA = 50, FOLD_COOLDOWN = 1200, FOLD_RANGE = 15;
    public static final int CRASH_MANA = 120, CRASH_COOLDOWN = 3000, CRASH_RANGE = 15;
    public static final int SUBSPACE_MANA = 60, SUBSPACE_COOLDOWN = 2400, SUBSPACE_TICKS = 400;
    public static final int SUBSPACE_MANA_PER_SECOND = 4, SUBSPACE_MANA_PER_BLOCK = 1, SUBSPACE_MANA_PER_DODGE = 5;
    public static final int HOLE_MANA = 300, HOLE_COOLDOWN = 6000, HOLE_TICKS = 300;
    public static final int HOLE_RADIUS = 10, HOLE_RADIUS_PER_CAPTURE = 2, HOLE_MAX_RADIUS = 100, HOLE_EXPLODE_AT = 10;

    public static final ResourceKey<DamageType> MAGIA = RelicUtil.damageType("rifty_magia");
    public static final ResourceKey<DamageType> VAZIO = RelicUtil.damageType("rifty_vazio");

    private static RiftyItem item() { return ModItems.RIFTY.get(); }

    /** Ultima posicao livre andando em linha reta (caixa do jogador), parando antes do primeiro bloco. */
    private static Vec3 stepAlong(ServerPlayer player, Vec3 direction, double distance) {
        Vec3 start = player.position(), best = start;
        for (double d = 0.25; d <= distance; d += 0.25) {
            Vec3 candidate = start.add(direction.scale(d));
            AABB box = player.getBoundingBox().move(candidate.subtract(start));
            if (!player.level().noCollision(player, box)) break;
            best = candidate;
        }
        return best;
    }

    private static void teleport(ServerPlayer player, Vec3 to) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 30, 0.3, 0.6, 0.3, 0.3);
        level.playSound(null, BlockPos.containing(to), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // ---------- Nv 1 — Passo Dimensional ----------

    public static boolean step(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        Vec3 look = player.getLookAngle();
        // Inimigo perto: o golpe e mirado nele em vez de teleportar (sem custo e sem recarga).
        Optional<LivingEntity> near = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(AIM_RANGE),
                        e -> e.isAlive() && RelicTargets.canHit(player, e) && e.distanceTo(player) <= AIM_RANGE
                                && e.position().subtract(player.position()).normalize().dot(look) > 0.3)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (near.isPresent()) {
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, near.get().getEyePosition());
            player.attack(near.get());
            return false;
        }
        Vec3 to = stepAlong(player, look, STEP_DISTANCE);
        if (to.distanceToSqr(player.position()) < 1.0) return false;
        teleport(player, to);
        item().addMasteryXp(player, 1);
        return true;
    }

    // ---------- Nv 5 — Dobra Dimensional ----------

    public static boolean fold(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        BlockHitResult clip = level.clip(new ClipContext(eye, eye.add(look.scale(FOLD_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = clip.getLocation();
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * context.powerMultiplier());
        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1),
                e -> e.isAlive() && RelicTargets.canHit(player, e) && e.getBoundingBox().inflate(0.5).clip(eye, end).isPresent())) {
            target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            hits++;
        }
        // Rachadura no ar ao longo da linha, que se fecha logo depois.
        double length = eye.distanceTo(end);
        org.dantesys.nexus.entity.FendaDimensional.line(level, eye.add(look.scale(0.6)), end);
        for (double d = 0; d <= length; d += 1.0) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        teleport(player, stepAlong(player, look, Math.max(0, length - 0.3)));
        RelicUtil.pushToFreeSpace(player);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.8f);
        if (hits > 0) item().addMasteryXp(player, hits);
        return true;
    }

    // ---------- Nv 10 — Dimensional Crash ----------

    public static boolean crash(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = RelicUtil.targetEntity(player, CRASH_RANGE, e -> e.isAlive() && RelicTargets.canHit(player, e));
        if (target == null) {
            RelicUtil.say(player, "Nenhum alvo na mira.");
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        float total = target.getMaxHealth() * 0.45f;
        target.hurtServer(level, RelicUtil.source(level, MAGIA, player), total / 2);
        target.setInvulnerableTime(0);
        target.hurtServer(level, RelicUtil.source(level, VAZIO, player), total / 2);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 60, 0.4, 0.8, 0.4, 0.4);
        org.dantesys.nexus.entity.FendaDimensional.radial(level, target.position().add(0, target.getBbHeight() / 2, 0),
                Math.max(2.0F, target.getBbHeight() * 1.6F));
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        // Execucao (nunca em bosses).
        float limit = target instanceof Player ? 0.25f : 0.5f;
        if (target.isAlive() && !RelicUtil.isBoss(target) && target.getHealth() <= target.getMaxHealth() * limit) {
            target.setInvulnerableTime(0);
            target.hurtServer(level, RelicUtil.source(level, VAZIO, player), target.getHealth() + 1000f);
        }
        item().addMasteryXp(player, target.isAlive() ? 2 : 4);
        return true;
    }

    // ---------- Nv 15 — Subespaco ----------

    private static final Map<UUID, BlockPos> SUBSPACE = new HashMap<>();

    public static boolean subspace(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (Subespaco.active(player)) return false;
        player.addEffect(new MobEffectInstance(ModEffects.SUBESPACO, SUBSPACE_TICKS, 0, false, false, true));
        SUBSPACE.put(player.getUUID(), player.blockPosition());
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    private static boolean drain(ServerPlayer player, int amount) {
        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        if (magic.getMana() < amount || !magic.useMana(amount)) {
            player.removeEffect(ModEffects.SUBESPACO);
            RelicUtil.say(player, "Subespaço: mana esgotada.");
            return false;
        }
        player.syncData(ModAttachmentType.MAGIC_STATS);
        return true;
    }

    private static boolean physical(DamageSource source) {
        return !(source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypeTags.BYPASSES_RESISTANCE) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(MAGIA) || source.is(VAZIO));
    }

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!Subespaco.active(event.getEntity())) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        } else if (physical(source)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) drain(player, SUBSPACE_MANA_PER_DODGE);
        }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (Subespaco.active(event.getEntity())) event.getEntity().removeEffect(ModEffects.SUBESPACO);
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (Subespaco.active(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (Subespaco.active(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Subespaco.active(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakBlock(BreakBlockEvent event) {
        if (Subespaco.active(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos last = SUBSPACE.get(player.getUUID());
        if (last == null) return;
        if (!Subespaco.active(player)) {
            SUBSPACE.remove(player.getUUID());
            RelicUtil.pushToFreeSpace(player);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 0.8f);
            return;
        }
        long now = player.level().getGameTime();
        if (now % 20 == 0 && !drain(player, SUBSPACE_MANA_PER_SECOND)) return;
        BlockPos pos = player.blockPosition();
        if (!pos.equals(last)) {
            SUBSPACE.put(player.getUUID(), pos);
            if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()
                    || !player.level().getBlockState(pos.above()).getCollisionShape(player.level(), pos.above()).isEmpty()) {
                if (!drain(player, SUBSPACE_MANA_PER_BLOCK)) return;
            }
        }
        if (now % 4 == 0) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.6, 0.3, 0.01);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SUBSPACE.remove(player.getUUID()) != null) {
            player.removeEffect(ModEffects.SUBESPACO);
            RelicUtil.pushToFreeSpace(player);
        }
    }

    // ---------- Nv 20 — Black Hole ----------

    private static final class Hole {
        final UUID owner; final ServerLevel level; final Vec3 center; final long until; final double power;
        final Set<UUID> captured = new HashSet<>();
        final org.dantesys.nexus.entity.BuracoNegro core;
        Hole(UUID owner, ServerLevel level, Vec3 center, long until, double power, org.dantesys.nexus.entity.BuracoNegro core) {
            this.owner = owner; this.level = level; this.center = center; this.until = until; this.power = power; this.core = core;
        }
        double radius() { return Math.min(HOLE_MAX_RADIUS, HOLE_RADIUS + HOLE_RADIUS_PER_CAPTURE * captured.size()); }
    }
    private static final List<Hole> HOLES = new ArrayList<>();

    public static boolean canBlackHole(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        for (int slot = 1; slot <= 3; slot++) if (item().getCooldownRemaining(stack, now, slot) <= 0) return false;
        return HOLES.stream().noneMatch(h -> h.owner.equals(player.getUUID()));
    }

    public static boolean blackHole(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        HitResult hit = player.pick(24, 0.0F, false);
        Vec3 center = hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult block
                ? block.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getUnitVec3i()).scale(1.5))
                : player.getEyePosition().add(player.getLookAngle().scale(8));
        var core = org.dantesys.nexus.entity.BuracoNegro.spawn(level, center, 1.5F);
        HOLES.add(new Hole(player.getUUID(), level, center, level.getGameTime() + HOLE_TICKS, context.powerMultiplier(), core));
        level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 0.5f);
        return true;
    }

    private static void pull(Entity entity, Vec3 center, double speed) {
        Vec3 dir = center.subtract(entity.getBoundingBox().getCenter());
        double dist = dir.length();
        if (dist < 0.01) return;
        entity.setDeltaMovement(dir.scale(Math.min(speed, dist * 0.5) / dist));
        entity.resetFallDistance();
        RelicUtil.syncMotion(entity);
    }

    private static void tickHole(Hole hole, long now) {
        ServerLevel level = hole.level;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(hole.owner);
        Vec3 center = hole.center;
        double radius = hole.radius();
        if (hole.core != null) hole.core.setSize(1.5F + Math.min(2.5F, hole.captured.size() * 0.15F));
        AABB area = new AABB(center, center).inflate(radius);
        if (owner != null && now % 2 == 0) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && RelicTargets.canHit(owner, e) && e.distanceToSqr(center) <= radius * radius)) {
                double dist = target.getBoundingBox().getCenter().distanceTo(center);
                if (dist < 2.5) hole.captured.add(target.getUUID());
                pull(target, center, hole.captured.contains(target.getUUID()) ? 0.3 : 0.9);
                if (dist < 3.5 && now % 10 == 0) {
                    float damage = RelicUtil.reduced(target, (float) (2.0 * hole.power));
                    target.hurtServer(level, RelicUtil.source(level, MAGIA, owner), damage);
                    target.setInvulnerableTime(0);
                    target.hurtServer(level, RelicUtil.source(level, VAZIO, owner), damage);
                }
            }
        }
        // Itens e XP ficam acumulados no centro.
        for (Entity loot : level.getEntitiesOfClass(Entity.class, area,
                e -> (e instanceof ItemEntity || e instanceof ExperienceOrb) && e.distanceToSqr(center) <= radius * radius)) {
            if (loot.distanceToSqr(center) < 1.0) {
                loot.setDeltaMovement(Vec3.ZERO);
                loot.setNoGravity(true);
            } else pull(loot, center, 0.8);
        }
        if (now % 2 == 0) {
            level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 25, 0.6, 0.6, 0.6, 1.2);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 10, 0.3, 0.3, 0.3, 0.05);
        }
        if (now % 40 == 0) level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    private static void endHole(Hole hole) {
        ServerLevel level = hole.level;
        Vec3 center = hole.center;
        if (hole.core != null) hole.core.discard();
        for (Entity loot : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(3),
                e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) loot.setNoGravity(false);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(hole.owner);
        if (owner != null && hole.captured.size() > HOLE_EXPLODE_AT) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 1, 1, 1, 0);
            level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(10),
                    e -> e.isAlive() && RelicTargets.canHit(owner, e))) {
                target.hurtServer(level, RelicUtil.source(level, MAGIA, owner), RelicUtil.reduced(target, (float) (12.0 * hole.power)));
                Vec3 away = target.position().subtract(center).normalize();
                target.setDeltaMovement(away.x * 2.0, 0.8, away.z * 2.0);
                RelicUtil.syncMotion(target);
            }
            item().addMasteryXp(owner, 10);
        } else {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 0.5, 0.5, 0.5, 0.3);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || HOLES.isEmpty()) return;
        long now = level.getGameTime();
        HOLES.removeIf(hole -> {
            if (hole.level != level) return false;
            if (now >= hole.until) {
                endHole(hole);
                return true;
            }
            tickHole(hole, now);
            return false;
        });
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (Hole hole : HOLES) if (hole.core != null) hole.core.discard();
        HOLES.clear();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (SUBSPACE.remove(player.getUUID()) != null) {
                player.removeEffect(ModEffects.SUBESPACO);
                RelicUtil.pushToFreeSpace(player);
            }
        }
        SUBSPACE.clear();
    }
}
