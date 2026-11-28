package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Brutix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class BrutixEffects {
    private BrutixEffects() {}

    public static final int BOOMERANG_MANA = 30, BOOMERANG_COOLDOWN = 100;
    public static final int SPIN_MANA = 50, SPIN_COOLDOWN = 400, SPIN_TICKS = 60;
    public static final int ROAR_MANA = 40, ROAR_COOLDOWN = 600;
    public static final int DISCHARGE_MANA = 150, DISCHARGE_COOLDOWN = 3000;
    /** Dano recebido para encher um estagio de furia. */
    public static final float FURY_PER_STAGE = 8;
    public static final int DECAY_DELAY = 200, DECAY_INTERVAL = 60;
    public static final float DISCHARGE_BASE = 16, DISCHARGE_PER_STAGE = 8;

    private static final class State {
        float points; int stages; long lastHit, nextDecay, disabledUntil, spinUntil; int spinStages;
        Boomerang boomerang;
    }
    private static final class Boomerang {
        final Display.ItemDisplay display; final Vec3 start; final Vec3 direction; final double range; final float damage;
        Vec3 pos; boolean returning; final Set<UUID> hitOut = new HashSet<>(), hitBack = new HashSet<>();
        Boomerang(Display.ItemDisplay display, Vec3 start, Vec3 direction, double range, float damage) {
            this.display = display; this.start = start; this.direction = direction; this.range = range; this.damage = damage; this.pos = start;
        }
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static BrutixItem item() { return ModItems.BRUTIX.get(); }
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }
    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    public static int maxStages(ServerPlayer player) { return Math.min(5, 3 + item().masteryLevel(player) / 10); }
    public static int stages(ServerPlayer player) { State s = STATES.get(player.getUUID()); return s == null ? 0 : s.stages; }
    public static double barPercent(ServerPlayer player) { State s = STATES.get(player.getUUID()); return s == null ? 0 : 100.0 * s.points / FURY_PER_STAGE; }
    public static boolean flying(ServerPlayer player) { State s = STATES.get(player.getUUID()); return s != null && s.boomerang != null; }

    private static void refresh(ServerPlayer player) {
        ItemStack stack = RelicUtil.find(player, item());
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    private static void applyStrength(ServerPlayer player, State s) {
        if (s.stages > 0) player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, s.stages - 1, true, false, true));
        else player.removeEffect(MobEffects.STRENGTH);
    }

    private static void spend(ServerPlayer player, State s) {
        s.stages = 0;
        s.points = 0;
        player.removeEffect(MobEffects.STRENGTH);
        refresh(player);
    }

    // ---------- Nv 1 — Furia ----------

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof LivingEntity)) return;
        if (event.getHealthDamage() <= 0 || RelicUtil.find(player, item()).isEmpty() || !item().isUnlocked(player, 0)) return;
        State s = state(player);
        long now = now(player);
        if (s.disabledUntil > now) return;
        s.lastHit = now;
        int max = maxStages(player);
        if (s.stages >= max) return;
        s.points += event.getHealthDamage();
        while (s.points >= FURY_PER_STAGE && s.stages < max) {
            s.points -= FURY_PER_STAGE;
            s.stages++;
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2, player.getZ(), 4 + s.stages, 0.4, 0.3, 0.4, 0);
            player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.3f, 1.6f);
            item().addMasteryXp(player, 1);
        }
        if (s.stages >= max) s.points = 0;
        applyStrength(player, s);
        refresh(player);
    }

    // ---------- Nv 5 — Boomerang ----------

    public static boolean boomerang(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        if (s.boomerang != null) return false;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition().subtract(0, 0.3, 0), dir = player.getLookAngle();
        double range = 10 + s.stages * 2;
        var clip = level.clip(new ClipContext(start, start.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (clip.getType() != HitResult.Type.MISS) range = Math.max(1, clip.getLocation().distanceTo(start) - 0.5);
        // Machado deitado (90 graus no eixo X), girando no plano horizontal como um boomerang.
        float h = (float) Math.sqrt(0.5);
        Display.ItemDisplay display = Displays.item(level, stack, start, 1.0f, new float[]{h, 0, 0, h});
        display.setYRot(player.getYRot());
        s.boomerang = new Boomerang(display, start, dir, range, (float) (8 * context.powerMultiplier()));
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        refresh(player);
        return true;
    }

    private static void tickBoomerang(ServerPlayer player, State s) {
        Boomerang b = s.boomerang;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 next;
        if (!b.returning) {
            next = b.pos.add(b.direction.scale(1.0));
            if (next.distanceTo(b.start) >= b.range) b.returning = true;
        } else {
            Vec3 target = player.getEyePosition().subtract(0, 0.3, 0);
            Vec3 to = target.subtract(b.pos);
            if (to.length() < 1.5 || b.display.level() != level) { endBoomerang(player, s); return; }
            next = b.pos.add(to.normalize().scale(Math.min(1.3, to.length())));
        }
        Set<UUID> hits = b.returning ? b.hitBack : b.hitOut;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, next).inflate(1.0),
                e -> RelicTargets.canHit(player, e) && !hits.contains(e.getUUID()))) {
            hits.add(target.getUUID());
            // Fonte "arremessado" (direta = o machado): nao e bloqueada pelo cancelamento do golpe sem machado.
            target.hurtServer(level, level.damageSources().thrown(b.display, player), b.damage);
            item().addMasteryXp(player, 1);
        }
        b.pos = next;
        b.display.teleportTo(next.x, next.y, next.z);
        b.display.setYRot(b.display.getYRot() + 45);
        if (level.getGameTime() % 4 == 0) level.playSound(null, b.display.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    private static void endBoomerang(ServerPlayer player, State s) {
        if (s.boomerang == null) return;
        s.boomerang.display.discard();
        s.boomerang = null;
        refresh(player);
    }

    /** Sem o machado na mao enquanto ele voa. */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer player && source.isDirect() && player.getMainHandItem().getItem() instanceof BrutixItem
                && flying(player) && source.getDirectEntity() == player) event.setCanceled(true);
    }

    // ---------- Nv 10 — Giro Destruidor ----------

    public static boolean spin(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        s.spinStages = s.stages;
        s.spinUntil = now(player) + SPIN_TICKS;
        spend(player, s);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    private static void tickSpin(ServerPlayer player, State s, long now) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = 2.5 + 0.5 * s.spinStages;
        for (int i = 0; i < 6; i++) {
            double a = now * 0.8 + i * Math.PI / 3;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(a) * radius * 0.7, player.getY() + 1,
                    player.getZ() + Math.sin(a) * radius * 0.7, 1, 0, 0, 0, 0);
        }
        if ((s.spinUntil - now) % 10 != 0) return;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1, radius),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius + 0.5)) {
            target.hurtServer(level, level.damageSources().playerAttack(player), 4 + 3 * s.spinStages);
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            target.setDeltaMovement(away.x * 0.8, 0.3, away.z * 0.8);
            RelicUtil.syncMotion(target);
        }
    }

    // ---------- Nv 15 — Grito de Guerra ----------

    public static boolean roar(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        ServerLevel level = (ServerLevel) player.level();
        double radius = 6 + context.affinity() / 100.0;
        int amplifier = Math.min(2, s.stages / 2);
        int ticks = 100 + s.stages * 40 + context.resonance() / 10;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, amplifier), player);
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            target.setDeltaMovement(away.x * 1.2, 0.35, away.z * 1.2);
            RelicUtil.syncMotion(target);
            if (target instanceof Mob mob) mob.setTarget(null);
            if (target instanceof PathfinderMob pathfinder) {
                Vec3 flee = target.position().add(away.scale(8));
                pathfinder.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.4);
            }
        }
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16;
            level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX() + Math.cos(a) * 2, player.getY() + 1, player.getZ() + Math.sin(a) * 2, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5f, 0.7f);
        return true;
    }

    // ---------- Nv 20 — Descarga de Furia ----------

    public static boolean canDischarge(ServerPlayer player, ItemStack stack) {
        return stages(player) >= (int) Math.ceil(maxStages(player) / 2.0);
    }

    public static boolean discharge(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        int spent = s.stages;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 center = player.position().add(flat.scale(2.0)).add(0, 1, 0);
        float damage = (float) ((DISCHARGE_BASE + DISCHARGE_PER_STAGE * spent) * context.powerMultiplier());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(1.8, 1.5, 1.8),
                e -> RelicTargets.canHit(player, e))) {
            target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            item().addMasteryXp(player, 1);
        }
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        for (double t = -1.5; t <= 1.5; t += 0.25) {
            Vec3 p = center.add(side.scale(t)).add(0, t * 0.8, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.2);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.5f);
        spend(player, s);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        s.disabledUntil = now(player) + 200;
        RelicUtil.say(player, Component.translatable("relic.nexus.brutix.descarga_de_furia_estagios", spent));
        return true;
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        long now = now(player);
        if (s.boomerang != null) tickBoomerang(player, s);
        if (s.spinUntil > now) tickSpin(player, s, now);
        else if (s.spinUntil > 0 && s.spinUntil <= now) {
            s.spinUntil = 0;
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
        }
        if (s.stages > 0 && now - s.lastHit > DECAY_DELAY && now >= s.nextDecay) {
            s.stages--;
            s.points = 0;
            s.nextDecay = now + DECAY_INTERVAL;
            applyStrength(player, s);
            refresh(player);
        }
        if (s.stages > 0 && now % 40 == 0) applyStrength(player, s);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State s = STATES.remove(player.getUUID());
            if (s != null && s.boomerang != null) s.boomerang.display.discard();
        }
    }

    /** O machado em voo e so visual: some antes de o mundo ser salvo. */
    @SubscribeEvent
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        STATES.values().forEach(s -> { if (s.boomerang != null) s.boomerang.display.discard(); });
        STATES.clear();
    }
}
