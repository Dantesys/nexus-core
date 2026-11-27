package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Necrix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class NecrixEffects {
    private NecrixEffects() {}

    public static final int WARRIORS_MANA = 80, WARRIORS_COOLDOWN = 1200, WARRIORS = 5;
    public static final int ZONE_MANA = 90, ZONE_COOLDOWN = 2400, ZONE_TICKS = 200, ZONE_RADIUS = 10;
    public static final int ELITE_MANA = 150, ELITE_COOLDOWN = 6000, ELITE_TICKS = 400;
    public static final int ARMY_MANA = 300, ARMY_COOLDOWN = 18000, ARMY_TICKS = 600, ARMY_SIZE = 20;
    public static final String ELITE_TAG = "NecrixElite";
    private static final String WARRIOR = "NexusGuerreiroOsseo", GUARD = "NexusGuardaOsseo", MINI_WITHER = "NexusMiniWither", ARMY = "NexusExercito",
            PROVOKED = "NexusProvocadoPor", SKULL = "NexusCaveira";
    private static final Identifier MINI_SCALE = Identifier.fromNamespaceAndPath(Nexus.MODID, "necrix_mini");

    private static NecrixItem item() { return ModItems.NECRIX.get(); }

    // ---------- Nv 1 — Aliado dos Mortos ----------

    private static boolean undead(Entity e) { return RelicUtil.isUndeadMinion(e) && !(e instanceof WitherBoss); }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || !undead(event.getEntity())) return;
        if (!item().isUnlocked(player, 0) || item().findIn(player).isEmpty()) return;
        var data = event.getEntity().getPersistentData();
        if (data.getStringOr(PROVOKED, "").equals(player.getUUID().toString())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void provoked(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player p && undead(event.getEntity()) && !RelicMinions.isMinion(event.getEntity())) {
            event.getEntity().getPersistentData().putString(PROVOKED, p.getUUID().toString());
        }
    }

    // ---------- Esqueletos ----------

    private static void gear(Mob mob, Item weapon, Item helmet, Item chest, Item legs, Item boots) {
        Item[] items = {weapon, helmet, chest, legs, boots};
        EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 5; i++) {
            ItemStack s = new ItemStack(items[i]);
            s.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
            mob.setItemSlot(slots[i], s);
            mob.setDropChance(slots[i], 0f);
        }
    }

    private static Mob raise(ServerPlayer owner, ServerLevel level, EntityType<? extends Mob> type, Vec3 pos, String role) {
        Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        mob.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360, 0);
        RelicTargets.setMinion(mob, owner);
        mob.getPersistentData().putBoolean(role, true);
        mob.getPersistentData().putLong("NexusDesde", level.getGameTime());
        mob.setPersistenceRequired();
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        level.addFreshEntity(mob);
        RelicUtil.pushToFreeSpace(mob);
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        return mob;
    }

    private static List<Mob> minions(ServerPlayer owner, String role) {
        List<Mob> list = new ArrayList<>();
        for (ServerLevel level : owner.level().getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof Mob m && m.isAlive() && m.getPersistentData().getBooleanOr(role, false) && RelicTargets.isMinionOf(owner, m)) list.add(m);
            }
        }
        return list;
    }

    // ---------- Nv 5 — Guerreiros Osseos ----------

    public static boolean warriors(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        minions(player, WARRIOR).forEach(Entity::discard);
        ServerLevel level = (ServerLevel) player.level();
        int lvl = item().masteryLevel(player);
        for (int i = 0; i < WARRIORS; i++) {
            double a = i * Math.PI * 2 / WARRIORS;
            Mob s = raise(player, level, EntityTypes.SKELETON, player.position().add(Math.cos(a) * 2, 0, Math.sin(a) * 2), WARRIOR);
            if (s == null) continue;
            if (lvl >= 20) gear(s, Items.NETHERITE_SWORD, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
            else if (lvl >= 15) gear(s, Items.DIAMOND_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            else if (lvl >= 10) gear(s, Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            else gear(s, Items.STONE_SWORD, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.6f);
        return true;
    }

    // ---------- Nv 10 — Zona Ossea ----------

    private static final class Zone {
        final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long until;
        final Map<UUID, Float> dealt = new HashMap<>(); final Set<LivingEntity> inside = new HashSet<>();
        Zone(ServerPlayer o, ServerLevel l, Vec3 c, long u) { owner = o; level = l; center = c; until = u; }
    }
    private static final List<Zone> ZONES = new ArrayList<>();

    public static boolean boneZone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ZONES.add(new Zone(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + ZONE_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    private static int guardLimit(ServerPlayer owner) { return 10 + Math.clamp(item().masteryLevel(owner) - 10, 0, 10); }

    private static void raiseGuard(ServerPlayer owner, ServerLevel level, Vec3 pos, EntityType<? extends Mob> type, int limit) {
        List<Mob> guards = minions(owner, GUARD).stream().filter(m -> m.getType() == type).sorted(Comparator.comparingLong(m -> m.getPersistentData().getLongOr("NexusDesde", 0))).toList();
        for (int i = 0; i <= guards.size() - limit; i++) guards.get(i).discard();
        Mob guard = raise(owner, level, type, pos, GUARD);
        if (guard != null) {
            if (type == EntityTypes.WITHER_SKELETON) gear(guard, Items.STONE_SWORD, Items.AIR, Items.AIR, Items.AIR, Items.AIR);
            else gear(guard, Items.IRON_SWORD, Items.IRON_HELMET, Items.AIR, Items.AIR, Items.AIR);
        }
    }

    private static boolean tickZone(Zone z, long now) {
        if (now >= z.until || z.owner.isRemoved()) {
            for (LivingEntity e : z.inside) if (e.isAlive()) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), z.owner);
            return true;
        }
        ServerLevel level = z.level;
        if (now % 5 == 0) for (int i = 0; i < 20; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(level.getRandom().nextDouble()) * ZONE_RADIUS;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, z.center.x + Math.cos(a) * r, z.center.y + 0.2, z.center.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
        }
        Set<LivingEntity> now_inside = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(ZONE_RADIUS, 4, ZONE_RADIUS),
                e -> RelicTargets.canHit(z.owner, e) && e.position().distanceTo(z.center) <= ZONE_RADIUS));
        for (LivingEntity e : z.inside) if (!now_inside.contains(e) && e.isAlive()) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), z.owner);
        z.inside.clear();
        z.inside.addAll(now_inside);
        if (now % 20 == 0) {
            for (LivingEntity e : now_inside) {
                float dealt = z.dealt.getOrDefault(e.getUUID(), 0f);
                if (dealt >= 20) continue;
                e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("necrix_ossos"), z.owner), 2);
                z.dealt.put(e.getUUID(), dealt + 2);
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || RelicMinions.isMinion(victim) || event.isCanceled()) return;
        ServerLevel level = (ServerLevel) victim.level();
        for (Zone z : ZONES) {
            if (z.level == level && victim.position().distanceTo(z.center) <= ZONE_RADIUS && RelicTargets.canHit(z.owner, victim)) {
                raiseGuard(z.owner, level, victim.position(), EntityTypes.SKELETON, guardLimit(z.owner));
                return;
            }
        }
        if (event.getSource().getDirectEntity() instanceof WitherSkull skull && skull.getPersistentData().getBooleanOr(SKULL, false)
                && skull.getOwner() instanceof WitherBoss w && !w.getPersistentData().getBooleanOr(ARMY, false)) {
            ServerPlayer owner = RelicMinions.owner(w);
            if (owner != null) raiseGuard(owner, level, victim.position(), EntityTypes.WITHER_SKELETON, 2);
        }
    }

    // ---------- Nv 15 — A Elite Ossea (mini Wither) ----------

    private static WitherBoss spawnMiniWither(ServerPlayer owner, ServerLevel level, boolean army) {
        WitherBoss w = EntityTypes.WITHER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (w == null) return null;
        Vec3 pos = owner.position().add(0, 2.5, 0);
        w.snapTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
        w.setNoAi(true);
        w.setInvulnerableTicks(0);
        RelicTargets.setMinion(w, owner);
        w.getPersistentData().putBoolean(MINI_WITHER, true);
        if (army) w.getPersistentData().putBoolean(ARMY, true);
        var scale = w.getAttribute(Attributes.SCALE);
        if (scale != null) scale.addOrReplacePermanentModifier(new AttributeModifier(MINI_SCALE, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var health = w.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(20);
        w.setHealth(20);
        level.addFreshEntity(w);
        return w;
    }

    private record Elite(WitherBoss wither, ServerPlayer owner, long until, boolean decays) {}
    private static final List<Elite> ELITES = new ArrayList<>();

    public static boolean canElite(ServerPlayer player, ItemStack stack) {
        return ELITES.stream().noneMatch(e -> e.owner == player && e.decays && e.wither.isAlive());
    }

    public static boolean elite(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        WitherBoss w = spawnMiniWither(player, (ServerLevel) player.level(), false);
        if (w == null) return false;
        ELITES.add(new Elite(w, player, player.level().getGameTime() + ELITE_TICKS, true));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(ELITE_TAG, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.6f);
        return true;
    }

    private static void eliteGone(Elite e) {
        if (!e.decays) return;
        ItemStack stack = item().findIn(e.owner);
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(ELITE_TAG));
        item().startCooldown(e.owner, stack, 3, ELITE_COOLDOWN);
    }

    private static boolean tickElite(Elite e, long now) {
        WitherBoss w = e.wither;
        if (!w.isAlive() || e.owner.isRemoved() || (e.decays && now >= e.until)) {
            if (w.isAlive()) w.discard();
            eliteGone(e);
            return true;
        }
        Vec3 target = e.owner.position().add(Math.cos(now * 0.05) * 1.5, 2.6, Math.sin(now * 0.05) * 1.5);
        Vec3 step = target.subtract(w.position());
        w.setPos(w.position().add(step.scale(step.length() > 8 ? 1 : 0.25)));
        if (e.decays && now % 20 == 0) w.setHealth(w.getHealth() - 1);
        if (now % 30 == 0) {
            LivingEntity foe = w.level().getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(16), x -> RelicTargets.canHit(e.owner, x) && x != w)
                    .stream().min(Comparator.comparingDouble(x -> x.distanceToSqr(w))).orElse(null);
            if (foe != null) {
                Vec3 dir = foe.getEyePosition().subtract(w.getEyePosition()).normalize();
                WitherSkull skull = new WitherSkull(w.level(), w, dir);
                skull.setPos(w.getEyePosition().add(dir));
                skull.getPersistentData().putBoolean(SKULL, true);
                w.level().addFreshEntity(skull);
                w.level().playSound(null, w.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.6f, 1.4f);
            }
        }
        return false;
    }

    /** Caveiras do mini Wither: explodem sem quebrar blocos. */
    @SubscribeEvent
    public static void skullHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof WitherSkull skull) || !skull.getPersistentData().getBooleanOr(SKULL, false)) return;
        event.setCanceled(true);
        if (!(skull.level() instanceof ServerLevel level) || !(skull.getOwner() instanceof WitherBoss w)) { skull.discard(); return; }
        ServerPlayer owner = RelicMinions.owner(w);
        Vec3 pos = event.getRayTraceResult().getLocation();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(2), x -> x != w && (owner == null || RelicTargets.canHit(owner, x)))) {
            e.hurtServer(level, level.damageSources().witherSkull(skull, w), RelicUtil.reduced(e, 8));
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1), w);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.3, 0.3, 0.3, 0);
        level.playSound(null, net.minecraft.core.BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.3f);
        skull.discard();
    }

    // ---------- Nv 20 — Exercito de Ossos ----------

    public static boolean canArmy(ServerPlayer player, ItemStack stack) {
        return RelicUtil.count(player, s -> s.is(Items.BONE)) >= 64 && RelicUtil.count(player, s -> s.is(Items.WITHER_SKELETON_SKULL)) >= 3;
    }

    private record Loadout(Item weapon, Item helmet, Item chest, Item legs, Item boots) {}
    private record Soldier(Mob mob, Loadout loadout) {}
    private static final class Army { final ServerPlayer owner; final long until; final List<Soldier> soldiers = new ArrayList<>(); WitherBoss wither; Army(ServerPlayer o, long u) { owner = o; until = u; } }
    private static final List<Army> ARMIES = new ArrayList<>();

    private static Loadout randomLoadout(net.minecraft.util.RandomSource r) {
        Item[] weapons = {Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.IRON_AXE, Items.DIAMOND_AXE, Items.BOW};
        boolean diamond = r.nextBoolean();
        return new Loadout(weapons[r.nextInt(weapons.length)], diamond ? Items.DIAMOND_HELMET : Items.IRON_HELMET, diamond ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE,
                r.nextBoolean() ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS, r.nextBoolean() ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS);
    }

    private static Mob soldier(ServerPlayer owner, ServerLevel level, Loadout l) {
        double a = level.getRandom().nextDouble() * Math.PI * 2, r = 2 + level.getRandom().nextDouble() * 4;
        Mob s = raise(owner, level, EntityTypes.SKELETON, owner.position().add(Math.cos(a) * r, 0, Math.sin(a) * r), ARMY);
        if (s != null) gear(s, l.weapon, l.helmet, l.chest, l.legs, l.boots);
        return s;
    }

    public static boolean army(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicUtil.consume(player, s -> s.is(Items.BONE), 64);
        RelicUtil.consume(player, s -> s.is(Items.WITHER_SKELETON_SKULL), 3);
        ServerLevel level = (ServerLevel) player.level();
        Army army = new Army(player, level.getGameTime() + ARMY_TICKS);
        for (int i = 0; i < ARMY_SIZE; i++) {
            Loadout l = randomLoadout(level.getRandom());
            Mob s = soldier(player, level, l);
            if (s != null) army.soldiers.add(new Soldier(s, l));
        }
        army.wither = spawnMiniWither(player, level, true);
        if (army.wither != null) ELITES.add(new Elite(army.wither, player, army.until, false));
        ARMIES.add(army);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    private static boolean tickArmy(Army a, ServerLevel level, long now) {
        if (now >= a.until || a.owner.isRemoved()) {
            a.soldiers.forEach(s -> s.mob.discard());
            if (a.wither != null) a.wither.discard();
            return true;
        }
        if (a.owner.level() != level) return false;
        for (int i = 0; i < a.soldiers.size(); i++) {
            Soldier s = a.soldiers.get(i);
            if (s.mob.isAlive()) continue;
            Mob fresh = soldier(a.owner, level, s.loadout);
            if (fresh != null) a.soldiers.set(i, new Soldier(fresh, s.loadout));
        }
        return false;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        ZONES.removeIf(z -> z.level == level && tickZone(z, now));
        ELITES.removeIf(e -> e.wither.level() == level && tickElite(e, now));
        ARMIES.removeIf(a -> tickArmy(a, level, now));
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        ELITES.forEach(e -> e.wither.discard());
        ARMIES.forEach(a -> a.soldiers.forEach(s -> s.mob.discard()));
        ELITES.clear(); ARMIES.clear(); ZONES.clear();
    }
}
