package org.dantesys.nexus.relic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.entity.BocaTubarao;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Trindy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class TrindyEffects {
    private TrindyEffects() {}

    public static final int VORTEX_MANA = 50, VORTEX_COOLDOWN = 600;
    public static final int WAVE_MANA = 60, WAVE_COOLDOWN = 900;
    public static final int HUNT_MANA = 90, HUNT_COOLDOWN = 1800;
    public static final int TEMPEST_MANA = 300, TEMPEST_COOLDOWN = 18000;
    private static final String TAG = "NexusTrindy", TEMPEST_MARK = "NexusMarcaTempest";
    private static final Identifier WAVE_STEP = Identifier.fromNamespaceAndPath(Nexus.MODID, "trindy_onda");

    private static TrindyItem item() { return ModItems.TRINDY.get(); }

    private enum Mode { NONE, VORTEX, HUNT, TEMPEST }
    private static final class State { ThrownTrident trident; Mode armed = Mode.NONE; long waveUntil; Vec3 waveDir; final Set<UUID> waveHit = new HashSet<>(); }
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }

    public static boolean flying(Player player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.trident != null && !s.trident.isRemoved();
    }

    // ---------- Arremesso (Lealdade) ----------

    public static void throwTrident(ServerPlayer player, ItemStack relic) {
        ServerLevel level = (ServerLevel) player.level();
        ItemStack visual = new ItemStack(Items.TRIDENT);
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LOYALTY).ifPresent(e -> visual.enchant(e, 3));
        ThrownTrident trident = new ThrownTrident(level, player, visual);
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
        trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        trident.getPersistentData().putBoolean(TAG, true);
        level.addFreshEntity(trident);
        level.playSound(null, trident, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        state(player).trident = trident;
        item().refreshModelData(player, relic);
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownTrident trident) || !trident.getPersistentData().getBooleanOr(TAG, false)) return;
        if (!(trident.getOwner() instanceof ServerPlayer owner) || !(trident.level() instanceof ServerLevel level)) return;
        State s = state(owner);
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target && RelicTargets.canHit(owner, target)) {
            // Captura: puxa o alvo ate o portador.
            Vec3 pull = owner.position().subtract(target.position());
            double strength = target instanceof Player || RelicUtil.isBoss(target) ? 0.5 : 1.0;
            Vec3 v = pull.normalize().scale(Math.min(2.5, pull.length() * 0.18) * strength).add(0, 0.35, 0);
            trident.level().getServer().execute(() -> RelicUtil.push(target, v));
            item().addMasteryXp(owner, 1);
        }
        Vec3 at = event.getRayTraceResult().getLocation();
        switch (s.armed) {
            case VORTEX -> startArea(owner, level, at, Mode.VORTEX);
            case HUNT -> startArea(owner, level, at, Mode.HUNT);
            case TEMPEST -> startArea(owner, level, at, Mode.TEMPEST);
            default -> {}
        }
        s.armed = Mode.NONE;
    }

    public static boolean armVortex(ServerPlayer player, ItemStack stack, RelicAbilityContext c) { return arm(player, Mode.VORTEX, "Vortex"); }
    public static boolean armHunt(ServerPlayer player, ItemStack stack, RelicAbilityContext c) { return arm(player, Mode.HUNT, "Área de Caça"); }
    public static boolean armTempest(ServerPlayer player, ItemStack stack, RelicAbilityContext c) { return arm(player, Mode.TEMPEST, "Mega Tempest"); }

    private static boolean arm(ServerPlayer player, Mode mode, String name) {
        state(player).armed = mode;
        RelicUtil.say(player, name + " pronto: arremesse o tridente.");
        return true;
    }

    public static boolean canTempest(ServerPlayer player, ItemStack stack) {
        return !item().ready(stack, player.level().getGameTime(), 3);
    }

    // ---------- Areas (Vortex, Area de Caca, Mega Tempest) ----------

    private static final class Area {
        final ServerPlayer owner; final ServerLevel level; final Vec3 center; final Mode mode; final long until; final double radius; final double power;
        final Map<UUID, Float> damage = new HashMap<>();
        Area(ServerPlayer owner, ServerLevel level, Vec3 center, Mode mode, long until, double radius, double power) {
            this.owner = owner; this.level = level; this.center = center; this.mode = mode; this.until = until; this.radius = radius; this.power = power;
        }
    }
    private static final List<Area> AREAS = new ArrayList<>();
    private record Bite(ServerPlayer owner, ServerLevel level, UUID target, Vec3 center, double radius, long at, boolean guaranteed) {}
    private static final List<Bite> BITES = new ArrayList<>();

    private static void startArea(ServerPlayer owner, ServerLevel level, Vec3 at, Mode mode) {
        var magic = RelicUtil.magic(owner);
        double res = magic.getRessonancia() / 1000.0, power = 1 + magic.getAfinidade() / 1000.0;
        long now = level.getGameTime();
        Area area = switch (mode) {
            case VORTEX -> new Area(owner, level, at, mode, now + 100 + (long) (60 * res), 6 + 3 * res, power);
            case HUNT -> new Area(owner, level, at, mode, now + 200 + (long) (100 * res), 10 + 4 * res, power);
            default -> new Area(owner, level, at, mode, now + 200, 15, power);
        };
        AREAS.add(area);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), mode == Mode.TEMPEST ? SoundEvents.TRIDENT_THUNDER.value() : SoundEvents.GENERIC_SPLASH,
                SoundSource.PLAYERS, 1.5f, mode == Mode.VORTEX ? 0.6f : 1.0f);
    }

    private static List<LivingEntity> victims(Area a) {
        return a.level.getEntitiesOfClass(LivingEntity.class, new AABB(a.center, a.center).inflate(a.radius),
                e -> RelicTargets.canHit(a.owner, e) && e.position().distanceTo(a.center) <= a.radius);
    }

    private static void tickArea(Area a, long now) {
        ServerLevel level = a.level;
        switch (a.mode) {
            case VORTEX -> {
                for (LivingEntity e : victims(a)) {
                    Vec3 to = a.center.subtract(e.position());
                    double strength = e instanceof Player || RelicUtil.isBoss(e) ? 0.15 : 0.35;
                    RelicUtil.push(e, e.getDeltaMovement().scale(0.5).add(to.normalize().scale(strength)));
                    if (now % 20 == 0) e.hurtServer(level, level.damageSources().trident(a.owner, a.owner), RelicUtil.reduced(e, (float) (2 * a.power)));
                }
                if (now % 2 == 0) for (int i = 0; i < 12; i++) {
                    double ang = (now * 0.3 + i * Math.PI / 6), r = a.radius * ((i % 3) + 1) / 3.0;
                    level.sendParticles(ParticleTypes.SPLASH, a.center.x + Math.cos(ang) * r, a.center.y + 0.3, a.center.z + Math.sin(ang) * r, 2, 0.05, 0.1, 0.05, 0);
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, a.center.x + Math.cos(ang) * r, a.center.y + 0.5, a.center.z + Math.sin(ang) * r, 1, 0, 0, 0, 0);
                }
            }
            case HUNT -> {
                if (now % 20 != 0) return;
                for (LivingEntity e : victims(a)) BocaTubarao.spawn(level, a.owner, e.getX(), e.getY(), e.getZ(), a.owner.getYRot(), (float) (4 * a.power));
                EternyEffects.ring(level, a.owner, 0, ParticleTypes.SPLASH);
            }
            case TEMPEST -> {
                if (now % 10 == 0) level.sendParticles(ParticleTypes.CLOUD, a.center.x, a.center.y + 12, a.center.z, 30, a.radius / 2, 0.5, a.radius / 2, 0.01);
                if (now % 20 == 0) {
                    List<LivingEntity> list = victims(a);
                    if (!list.isEmpty()) {
                        LivingEntity t = list.get(level.getRandom().nextInt(list.size()));
                        RelicUtil.lightningStrike(level, t, a.owner, 6);
                        a.damage.merge(t.getUUID(), 6f, Float::sum);
                    }
                }
                UUID top = topTarget(a);
                if (top != null && now % 5 == 0 && level.getEntity(top) instanceof LivingEntity t) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                }
            }
            default -> {}
        }
    }

    private static UUID topTarget(Area a) {
        // Quem tem a marca permanente vira o alvo automaticamente.
        for (LivingEntity e : victims(a)) if (e.getPersistentData().getBooleanOr(TEMPEST_MARK, false)) return e.getUUID();
        return a.damage.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static void endArea(Area a) {
        if (a.mode != Mode.TEMPEST) return;
        UUID top = topTarget(a);
        if (top == null || !(a.level.getEntity(top) instanceof LivingEntity target)) return;
        if (target.position().distanceTo(a.center) > a.radius) {
            target.getPersistentData().putBoolean(TEMPEST_MARK, true);
            RelicUtil.say(a.owner, "A presa escapou... mas ficou marcada.");
            return;
        }
        boolean guaranteed = target.getPersistentData().getBooleanOr(TEMPEST_MARK, false);
        BocaTubarao.spawn(a.level, a.owner, target.getX(), target.getY() - 0.5, target.getZ(), a.owner.getYRot(), 0, 4.5F);
        BITES.add(new Bite(a.owner, a.level, target.getUUID(), a.center, a.radius, a.level.getGameTime() + BocaTubarao.BITE_TICK, guaranteed));
    }

    private static void bite(Bite b) {
        if (!(b.level.getEntity(b.target) instanceof LivingEntity target) || !target.isAlive()) return;
        if (b.guaranteed || !(target instanceof Player || RelicUtil.isBoss(target))) {
            RelicUtil.guaranteedKill(b.level, target, b.owner);
            item().addMasteryXp(b.owner, 10);
            return;
        }
        float pct = target instanceof Player ? 0.75f : 0.5f;
        target.setInvulnerableTime(0);
        target.hurtServer(b.level, RelicUtil.source(b.level, RelicUtil.damageType("trindy_mordida"), b.owner), target.getMaxHealth() * pct);
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2), b.owner);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1), b.owner);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1), b.owner);
            target.getPersistentData().putBoolean(TEMPEST_MARK, true);
        }
    }

    // ---------- Nv 10 — Olha a Onda ----------

    public static boolean wave(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        Vec3 look = player.getLookAngle();
        s.waveDir = new Vec3(look.x, 0, look.z).normalize();
        s.waveUntil = player.level().getGameTime() + 15 + context.resonance() / 100;
        s.waveHit.clear();
        var step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) step.addOrUpdateTransientModifier(new AttributeModifier(WAVE_STEP, 0.5, AttributeModifier.Operation.ADD_VALUE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }

    private static void tickWave(ServerPlayer player, State s, long now) {
        ServerLevel level = (ServerLevel) player.level();
        if (now >= s.waveUntil || (player.horizontalCollision && now % 2 == 0 && s.waveUntil - now < 14)) {
            s.waveUntil = 0;
            var step = player.getAttribute(Attributes.STEP_HEIGHT);
            if (step != null) step.removeModifier(WAVE_STEP);
            return;
        }
        RelicUtil.push(player, new Vec3(s.waveDir.x * 1.0, Math.max(-0.2, player.getDeltaMovement().y), s.waveDir.z * 1.0));
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.8, 0.2, 0.8, 0.2);
        level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 0.5, player.getZ(), 6, 0.8, 0.4, 0.8, 0.05);
        float damage = (float) (6 * (1 + RelicUtil.magic(player).getAfinidade() / 1000.0));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.5), e -> RelicTargets.canHit(player, e))) {
            double strength = e instanceof Player || RelicUtil.isBoss(e) ? 0.6 : 1.4;
            RelicUtil.push(e, s.waveDir.scale(strength).add(0, 0.4, 0));
            if (s.waveHit.add(e.getUUID())) e.hurtServer(level, level.damageSources().playerAttack(player), RelicUtil.reduced(e, damage));
        }
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            State s = STATES.get(player.getUUID());
            if (s == null) continue;
            if (s.waveUntil > 0) tickWave(player, s, now);
            ThrownTrident t = s.trident;
            if (t != null && (t.isRemoved() || t.level() != level || t.distanceTo(player) < 1.8 && t.tickCount > 5 || t.tickCount > 600)) {
                if (!t.isRemoved()) t.discard();
                s.trident = null;
                ItemStack relic = item().findIn(player);
                if (!relic.isEmpty()) item().refreshModelData(player, relic);
            }
        }
        AREAS.removeIf(a -> {
            if (a.level != level) return false;
            if (now >= a.until) { endArea(a); return true; }
            tickArea(a, now);
            return false;
        });
        BITES.removeIf(b -> {
            if (b.level != level || now < b.at) return false;
            bite(b);
            return true;
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        State s = STATES.remove(event.getEntity().getUUID());
        if (s != null && s.trident != null) s.trident.discard();
        var step = event.getEntity().getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) step.removeModifier(WAVE_STEP);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        STATES.values().forEach(s -> { if (s.trident != null) s.trident.discard(); });
        STATES.clear();
        AREAS.clear();
        BITES.clear();
    }
}
