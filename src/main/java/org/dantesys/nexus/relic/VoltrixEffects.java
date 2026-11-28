package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Voltrix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class VoltrixEffects {
    private VoltrixEffects() {}

    public static final int CHAIN_MANA = 50, CHAIN_COOLDOWN = 400;
    public static final int ELECTRIFY_MANA = 70, ELECTRIFY_COOLDOWN = 1800, ELECTRIFY_TICKS = 600;
    public static final int STORM_MANA = 150, STORM_COOLDOWN = 3600, STORM_TICKS = 400, STORM_RADIUS = 10;
    public static final int FURY_MANA = 300, FURY_COOLDOWN = 18000, FURY_TICKS = 600;
    private static final float BOLT_DAMAGE = 5;

    private static VoltrixItem item() { return ModItems.VOLTRIX.get(); }
    private static boolean carrier(Player p) { return !item().findIn(p).isEmpty() && item().isUnlocked(p, 0); }
    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }

    private static final class State { long struck, electrifyUntil, furyUntil; boolean chainArmed; final Map<UUID, Long> shockCooldown = new HashMap<>(); }
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }

    // ---------- Nv 1 — Alta Voltagem ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void lightning(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player p) || !event.getSource().is(DamageTypes.LIGHTNING_BOLT) || !carrier(p)) return;
        event.setCanceled(true);
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1));
        state(p).struck = p.level().getGameTime();
    }

    // ---------- Nv 5 — Golpe em Cadeia ----------

    private static final class Chain { final ServerPlayer owner; final ServerLevel level; LivingEntity last; final Set<UUID> hit = new HashSet<>(); long next; int jumps;
        Chain(ServerPlayer o, ServerLevel l, LivingEntity first, long n) { owner = o; level = l; last = first; next = n; hit.add(first.getUUID()); } }
    private static final List<Chain> CHAINS = new ArrayList<>();

    public static boolean armChain(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).chainArmed = true;
        RelicUtil.say(player, Component.translatable("relic.nexus.voltrix.golpe_em_cadeia_pronto"));
        return true;
    }

    private static void shock(ServerLevel level, ServerPlayer owner, LivingEntity target, int paralysis, float damage) {
        target.setInvulnerableTime(0);
        target.hurtServer(level, level.damageSources().lightningBolt(), damage);
        target.addEffect(new MobEffectInstance(ModEffects.PARALISIA, paralysis, 0), owner);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.6, 0.3, 0.2);
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        for (double t = 0; t <= 1; t += 0.1) {
            Vec3 p = from.add(d.scale(t));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private static boolean tickChain(Chain c, long now) {
        if (now < c.next) return false;
        LivingEntity from = c.last;
        LivingEntity next = c.level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(3),
                e -> e != from && !c.hit.contains(e.getUUID()) && RelicTargets.canHit(c.owner, e) && e.distanceTo(from) <= 3)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
        if (next == null) {
            if (c.jumps == 0) from.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 40, 0), c.owner);
            return true;
        }
        arc(c.level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
        shock(c.level, c.owner, next, 20, 4);
        c.hit.add(next.getUUID());
        c.last = next;
        c.jumps++;
        c.next = now + 20;
        return false;
    }

    // ---------- Combate (cadeia, eletrificado, furia) ----------

    @SubscribeEvent
    public static void melee(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().isDirect() && wielding(player)) {
            State s = STATES.get(player.getUUID());
            if (s != null && RelicTargets.canHit(player, victim)) {
                if (s.chainArmed) {
                    s.chainArmed = false;
                    shock(level, player, victim, 20, 4);
                    CHAINS.add(new Chain(player, level, victim, now + 20));
                }
                if (s.electrifyUntil > now && s.shockCooldown.getOrDefault(victim.getUUID(), 0L) <= now) {
                    victim.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 20, 0), player);
                    s.shockCooldown.put(victim.getUUID(), now + 40);
                }
                if (s.furyUntil > now) RelicUtil.lightningStrike(level, victim, player, BOLT_DAMAGE);
            }
        }
        // Eletrificado: quem bate no portador tambem e paralisado.
        if (victim instanceof ServerPlayer holder && event.getSource().isDirect() && event.getSource().getEntity() instanceof LivingEntity attacker) {
            State s = STATES.get(holder.getUUID());
            if (s != null && s.electrifyUntil > now && s.shockCooldown.getOrDefault(attacker.getUUID(), 0L) <= now) {
                attacker.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 20, 0), holder);
                s.shockCooldown.put(attacker.getUUID(), now + 40);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
            }
        }
    }

    // ---------- Nv 10 — Eletrificar ----------

    public static boolean electrify(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).electrifyUntil = player.level().getGameTime() + ELECTRIFY_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
        return true;
    }

    // ---------- Nv 15 — Chamado da Tempestade ----------

    private record Storm(ServerPlayer owner, ServerLevel level, Vec3 center, long until) {}
    private static final List<Storm> STORMS = new ArrayList<>();

    public static boolean storm(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        STORMS.add(new Storm(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + STORM_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.8f);
        return true;
    }

    private static boolean tickStorm(Storm s, long now) {
        if (now >= s.until) return true;
        if (now % 10 != 0) return false;
        var r = s.level.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * STORM_RADIUS;
        double x = s.center.x + Math.cos(a) * d, z = s.center.z + Math.sin(a) * d;
        int y = s.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        Vec3 strike = new Vec3(x, y, z);
        RelicUtil.visualLightning(s.level, strike);
        for (LivingEntity e : s.level.getEntitiesOfClass(LivingEntity.class, new AABB(strike, strike).inflate(1.5, 3, 1.5), LivingEntity::isAlive)) {
            e.setInvulnerableTime(0);
            e.hurtServer(s.level, s.level.damageSources().lightningBolt(), BOLT_DAMAGE);
        }
        return false;
    }

    // ---------- Nv 20 — Furia do Trovao ----------

    public static boolean canFury(ServerPlayer player, ItemStack stack) {
        State s = STATES.get(player.getUUID());
        return s != null && player.hasEffect(MobEffects.STRENGTH) && player.level().getGameTime() - s.struck <= 100;
    }

    public static boolean fury(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).furyUntil = player.level().getGameTime() + FURY_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.2f);
        RelicUtil.say(player, Component.translatable("relic.nexus.voltrix.furia_do_trovao"));
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.get(player.getUUID());
        long now = player.level().getGameTime();
        if (s != null && (s.electrifyUntil > now || s.furyUntil > now) && now % 4 == 0) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 4, 0.4, 0.6, 0.4, 0.05);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        CHAINS.removeIf(c -> c.level == level && tickChain(c, now));
        STORMS.removeIf(s -> s.level == level && tickStorm(s, now));
    }
}
