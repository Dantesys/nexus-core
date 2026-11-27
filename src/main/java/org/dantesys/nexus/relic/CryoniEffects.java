package org.dantesys.nexus.relic;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Cryoni (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class CryoniEffects {
    private CryoniEffects() {}

    public static final int SLOW_MANA = 60, SLOW_COOLDOWN = 900, SLOW_TICKS = 200, SLOW_RADIUS = 5;
    public static final int REVERSE_MANA = 80, REVERSE_COOLDOWN = 2400, REVERSE_TICKS = 200;
    public static final int FREEZE_MANA = 150, FREEZE_COOLDOWN = 3600, FREEZE_TICKS = 100, FREEZE_RADIUS = 20;
    public static final int ZERO_MANA = 300, ZERO_COOLDOWN = 18000;
    private static final String STATUE = "NexusEstatuaGelo";
    private static final Identifier SLOW_SPEED = id("lento"), SLOW_ATTACK = id("lento_ataque"), SLOW_JUMP = id("lento_pulo");

    private static Identifier id(String p) { return Identifier.fromNamespaceAndPath(Nexus.MODID, "cryoni_" + p); }
    private static CryoniItem item() { return ModItems.CRYONI.get(); }
    private static boolean carrier(Player p, int slot) { return !item().findIn(p).isEmpty() && item().isUnlocked(p, slot); }

    // ---------- Nv 1 — Sangue Criogenico ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void immune(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && carrier(p, 0)
                && (PhoenixyEffects.isFire(event.getSource()) || event.getSource().is(DamageTypeTags.IS_FREEZING))) {
            event.setCanceled(true);
            p.clearFire();
        }
    }

    // ---------- Nv 5 — Tempo Lento ----------

    private static final class Slow { final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long until; final Set<LivingEntity> slowed = new HashSet<>(); final Set<Projectile> projectiles = new HashSet<>();
        Slow(ServerPlayer o, ServerLevel l, Vec3 c, long u) { owner = o; level = l; center = c; until = u; } }
    private static final List<Slow> SLOWS = new ArrayList<>();

    private static void slowMods(LivingEntity e, double amount) {
        var op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        for (var pair : List.of(Map.entry(Attributes.MOVEMENT_SPEED, SLOW_SPEED), Map.entry(Attributes.ATTACK_SPEED, SLOW_ATTACK), Map.entry(Attributes.JUMP_STRENGTH, SLOW_JUMP))) {
            var a = e.getAttribute(pair.getKey());
            if (a == null) continue;
            if (amount == 0) a.removeModifier(pair.getValue());
            else a.addOrUpdateTransientModifier(new AttributeModifier(pair.getValue(), -amount, op));
        }
    }

    public static boolean slowTime(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        SLOWS.add(new Slow(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + SLOW_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    private static boolean tickSlow(Slow s, long now) {
        if (now >= s.until) {
            s.slowed.forEach(e -> slowMods(e, 0));
            s.projectiles.forEach(p -> p.setDeltaMovement(p.getDeltaMovement().scale(2)));
            return true;
        }
        AABB area = new AABB(s.center, s.center).inflate(SLOW_RADIUS);
        Set<LivingEntity> inside = new HashSet<>(s.level.getEntitiesOfClass(LivingEntity.class, area, e -> RelicTargets.canHit(s.owner, e) && e.position().distanceTo(s.center) <= SLOW_RADIUS));
        for (LivingEntity e : inside) {
            if (s.slowed.add(e)) slowMods(e, RelicUtil.isBoss(e) ? 0.3 : 0.5);
            if (e.getDeltaMovement().y < 0) e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0.6, 1));
        }
        s.slowed.removeIf(e -> { if (inside.contains(e)) return false; slowMods(e, 0); return true; });
        for (Projectile p : s.level.getEntitiesOfClass(Projectile.class, area, p -> !(p.getOwner() instanceof LivingEntity o && (o == s.owner || RelicTargets.isAlly(s.owner, o))))) {
            if (s.projectiles.add(p)) p.setDeltaMovement(p.getDeltaMovement().scale(0.5));
        }
        if (now % 5 == 0) for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            s.level.sendParticles(ParticleTypes.SNOWFLAKE, s.center.x + Math.cos(a) * SLOW_RADIUS, s.center.y + 0.5, s.center.z + Math.sin(a) * SLOW_RADIUS, 1, 0, 0.1, 0, 0);
        }
        return false;
    }

    // ---------- Nv 10 — Oh o Reverse ----------

    private record Snapshot(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot, float health, int food, float saturation, List<MobEffectInstance> effects) {}
    private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

    public static boolean canReverse(ServerPlayer player, ItemStack stack) {
        var h = HISTORY.get(player.getUUID());
        return h != null && h.size() >= REVERSE_TICKS && h.stream().allMatch(s -> s.dim == player.level().dimension());
    }

    public static boolean reverse(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        Snapshot s = HISTORY.get(player.getUUID()).peekFirst();
        if (s == null) return false;
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        player.teleportTo(s.pos.x, s.pos.y, s.pos.z);
        player.setYRot(s.yRot); player.setXRot(s.xRot);
        player.setHealth(Math.min(player.getMaxHealth(), s.health));
        player.getFoodData().setFoodLevel(s.food);
        player.getFoodData().setSaturation(s.saturation);
        RelicEffectRules.allowRemoval(player::removeAllEffects);
        for (MobEffectInstance e : s.effects) player.addEffect(new MobEffectInstance(e));
        HISTORY.get(player.getUUID()).clear();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    // ---------- Nv 15 — Zona Congelada ----------

    private static final class Frozen { final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long start, until;
        final Set<Mob> mobs = new HashSet<>(); final Map<Projectile, Vec3> projectiles = new HashMap<>(); final Map<LivingEntity, Float> stored = new HashMap<>();
        Frozen(ServerPlayer o, ServerLevel l, Vec3 c, long s, long u) { owner = o; level = l; center = c; start = s; until = u; } }
    private static final List<Frozen> FROZEN = new ArrayList<>();

    public static boolean frozenZone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        long now = player.level().getGameTime();
        FROZEN.add(new Frozen(player, (ServerLevel) player.level(), player.position(), now, now + FREEZE_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
        return true;
    }

    private static boolean tickFrozen(Frozen f, long now) {
        if (now >= f.until) { releaseFrozen(f); return true; }
        AABB area = new AABB(f.center, f.center).inflate(FREEZE_RADIUS);
        for (LivingEntity e : f.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != f.owner && e.isAlive() && e.position().distanceTo(f.center) <= FREEZE_RADIUS)) {
            if (RelicUtil.isBoss(e) && now - f.start > FREEZE_TICKS / 2) continue;
            e.setDeltaMovement(Vec3.ZERO);
            if (e instanceof Mob m && !m.isNoAi()) { m.setNoAi(true); f.mobs.add(m); }
            if (e instanceof Player) e.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 5, 0, false, false));
        }
        for (Projectile p : f.level.getEntitiesOfClass(Projectile.class, area, p -> !f.projectiles.containsKey(p))) {
            f.projectiles.put(p, p.getDeltaMovement());
            p.setNoGravity(true);
        }
        f.projectiles.keySet().forEach(p -> p.setDeltaMovement(Vec3.ZERO));
        if (now % 5 == 0) f.level.sendParticles(ParticleTypes.SNOWFLAKE, f.center.x, f.center.y + 2, f.center.z, 30, FREEZE_RADIUS / 2.0, 2, FREEZE_RADIUS / 2.0, 0);
        return false;
    }

    private static void releaseFrozen(Frozen f) {
        f.mobs.forEach(m -> m.setNoAi(false));
        f.projectiles.forEach((p, v) -> { p.setNoGravity(false); p.setDeltaMovement(v); });
        f.stored.forEach((e, dmg) -> {
            if (!e.isAlive()) return;
            e.setInvulnerableTime(0);
            e.hurtServer(f.level, f.level.damageSources().playerAttack(f.owner), dmg);
        });
    }

    /** Golpes do portador nos congelados acumulam e caem todos no fim. */
    @SubscribeEvent
    public static void storeHits(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect()) return;
        for (Frozen f : FROZEN) {
            if (f.owner == player && event.getEntity().position().distanceTo(f.center) <= FREEZE_RADIUS) {
                f.stored.merge(event.getEntity(), event.getAmount(), Float::sum);
                event.setCanceled(true);
                return;
            }
        }
    }

    // ---------- Nv 20 — Zero Absoluto ----------

    public static boolean canZero(ServerPlayer player, ItemStack stack) { return RelicUtil.count(player, s -> s.is(Items.ICE)) >= 64; }

    private static final class Zero { final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long start; final Map<LivingEntity, Integer> waves = new HashMap<>();
        Zero(ServerPlayer o, ServerLevel l, Vec3 c, long s) { owner = o; level = l; center = c; start = s; } }
    private static final List<Zero> ZEROS = new ArrayList<>();

    public static boolean absoluteZero(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicUtil.consume(player, s -> s.is(Items.ICE), 64);
        ZEROS.add(new Zero(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 2.0f, 0.5f);
        return true;
    }

    private static boolean tickZero(Zero z, long now) {
        long age = now - z.start;
        ServerLevel level = z.level;
        z.owner.setDeltaMovement(Vec3.ZERO);
        RelicUtil.syncMotion(z.owner);
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(z.center, z.center).inflate(30), p -> p.getOwner() != z.owner)) {
            p.setNoGravity(true); p.setDeltaMovement(Vec3.ZERO);
        }
        if (age % 40 == 0 && age <= 160) {
            int wave = (int) (age / 40) + 1;
            double radius = 6 * wave;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(radius, 6, radius), e -> e != z.owner && e.isAlive() && e.position().distanceTo(z.center) <= radius)) {
                z.waves.merge(e, 1, Integer::sum);
                e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 40));
            }
            for (int i = 0; i < 60; i++) {
                double a = i * Math.PI * 2 / 60;
                level.sendParticles(ParticleTypes.SNOWFLAKE, z.center.x + Math.cos(a) * radius, z.center.y + 0.5, z.center.z + Math.sin(a) * radius, 2, 0, 0.3, 0, 0.02);
            }
            level.playSound(null, z.owner.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.4f + 0.1f * wave);
        }
        if (age < 170) return false;
        for (var entry : z.waves.entrySet()) {
            LivingEntity e = entry.getKey();
            if (!e.isAlive()) continue;
            int count = entry.getValue();
            boolean ally = RelicTargets.isAlly(z.owner, e);
            if (count >= 5 && !ally) statue(level, e, z.owner);
            else if (count >= 5 || (ally && count >= 3)) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            else if (count >= 3) {
                e.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 400, 0), z.owner);
                e.setTicksFrozen(e.getTicksRequiredToFreeze() + 400);
            }
        }
        return true;
    }

    private static void statue(ServerLevel level, LivingEntity victim, ServerPlayer owner) {
        LivingEntity statue = null;
        if (victim instanceof Player p) {
            Mannequin m = Mannequin.create(EntityTypes.MANNEQUIN, level);
            if (m != null) {
                CompoundTag tag = new CompoundTag();
                ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(p.getGameProfile())).result().ifPresent(t -> tag.put("profile", t));
                m.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                statue = m;
            }
        } else if (victim.getType().create(level, EntitySpawnReason.TRIGGERED) instanceof Mob m) {
            m.setNoAi(true);
            statue = m;
        }
        if (statue != null) {
            statue.snapTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
            statue.setYHeadRot(victim.getYHeadRot());
            statue.setSilent(true);
            statue.getPersistentData().putBoolean(STATUE, true);
            var hp = statue.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(1);
            statue.setHealth(1);
            statue.setTicksFrozen(Integer.MAX_VALUE / 2);
            level.addFreshEntity(statue);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), victim.getX(), victim.getY() + 1, victim.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        RelicUtil.guaranteedKill(level, victim, owner);
    }

    @SubscribeEvent
    public static void statueDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr(STATUE, false)) event.setCanceled(true);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (carrier(player, 0)) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.5), e -> RelicTargets.canHit(player, e))) {
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 20, e.getTicksFrozen() + 7));
            }
        }
        if (carrier(player, 2)) {
            var h = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
            List<MobEffectInstance> effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
            h.addLast(new Snapshot(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(), player.getHealth(),
                    player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), effects));
            while (h.size() > REVERSE_TICKS) h.removeFirst();
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        SLOWS.removeIf(s -> s.level == level && tickSlow(s, now));
        FROZEN.removeIf(f -> f.level == level && tickFrozen(f, now));
        ZEROS.removeIf(z -> z.level == level && tickZero(z, now));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        HISTORY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        SLOWS.forEach(s -> s.slowed.forEach(e -> slowMods(e, 0)));
        FROZEN.forEach(CryoniEffects::releaseFrozen);
        SLOWS.clear(); FROZEN.clear(); ZEROS.clear(); HISTORY.clear();
    }
}
