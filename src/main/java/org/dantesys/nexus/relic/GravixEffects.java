package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Gravix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class GravixEffects {
    private GravixEffects() {}

    public static final int DRIFT_MANA_PER_SECOND = 2;
    public static final int FIELD_MANA = 60, FIELD_COOLDOWN = 900, FIELD_TICKS = 300, RADIUS = 5;
    public static final int ZONE_MANA = 70, ZONE_COOLDOWN = 1200, ZONE_TICKS = 300;
    public static final int SHIFT_MANA = 80, SHIFT_COOLDOWN = 1800;
    public static final int MAX_MANA = 300, MAX_COOLDOWN = 18000, MAX_TICKS = 200, MAX_RADIUS = 30;
    private static final Identifier HEAVY_JUMP = Identifier.fromNamespaceAndPath(Nexus.MODID, "gravix_pesado");
    private static final Identifier SQUEEZE = Identifier.fromNamespaceAndPath(Nexus.MODID, "gravix_prensado");
    private static final String SLOWED_TAG = "NexusGravixLento";

    private static GravixItem item() { return ModItems.GRAVIX.get(); }
    private static boolean wearing(Player p, int slot) { return p.getItemBySlot(EquipmentSlot.FEET).is(item()) && item().isUnlocked(p, slot); }

    private static final class State {
        boolean drifting, lastCombo; long fieldUntil, zoneUntil, maxUntil; boolean shiftArmed;
        final Set<LivingEntity> floating = new HashSet<>(), heavy = new HashSet<>();
    }
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }

    public static boolean drifting(Player p) { State s = STATES.get(p.getUUID()); return s != null && s.drifting; }

    private static void setDrift(ServerPlayer player, State s, boolean on) {
        s.drifting = on;
        player.setNoGravity(on);
        RelicUtil.say(player, Component.translatable(on ? "relic.nexus.gravix.gravidade_zero_ligada" : "relic.nexus.gravix.gravidade_zero_desligada"));
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.is(item())) item().refreshModelData(player, boots);
    }

    /** Bota calcada: sem dano de queda. */
    @SubscribeEvent
    public static void noFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && p.getItemBySlot(EquipmentSlot.FEET).is(item()) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
        }
    }

    // ---------- Nv 5 — Campo Gravitacional / Nv 10 — Zona 0 ----------

    public static boolean heavyField(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        endZone(s);
        s.fieldUntil = player.level().getGameTime() + FIELD_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    public static boolean zeroZone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        endField(s);
        s.zoneUntil = player.level().getGameTime() + ZONE_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        return true;
    }

    private static void endField(State s) {
        s.fieldUntil = 0;
        for (LivingEntity e : s.heavy) { var a = e.getAttribute(Attributes.JUMP_STRENGTH); if (a != null) a.removeModifier(HEAVY_JUMP); }
        s.heavy.clear();
    }

    private static void endZone(State s) {
        s.zoneUntil = 0;
        for (LivingEntity e : s.floating) e.setNoGravity(false);
        s.floating.clear();
    }

    private static List<LivingEntity> enemies(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius);
    }

    private static List<Projectile> enemyProjectiles(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), p -> {
            Entity owner = p.getOwner();
            return p.distanceTo(player) <= radius && !(owner instanceof LivingEntity l && (l == player || RelicTargets.isAlly(player, l)));
        });
    }

    private static void tickField(ServerPlayer player, State s, long now) {
        ServerLevel level = (ServerLevel) player.level();
        Set<LivingEntity> inside = new HashSet<>(enemies(player, RADIUS));
        for (LivingEntity e : inside) {
            if (now % 10 == 0) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1 + RelicUtil.magic(player).getAfinidade() / 500), player);
            var jump = e.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null && s.heavy.add(e)) jump.addOrUpdateTransientModifier(new AttributeModifier(HEAVY_JUMP, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
        }
        s.heavy.removeIf(e -> {
            if (inside.contains(e)) return false;
            var a = e.getAttribute(Attributes.JUMP_STRENGTH);
            if (a != null) a.removeModifier(HEAVY_JUMP);
            return true;
        });
        for (Projectile p : enemyProjectiles(player, RADIUS)) p.setDeltaMovement(p.getDeltaMovement().add(0, -0.15, 0));
        if (now % 5 == 0) EternyEffects.ring(level, player, RADIUS, ParticleTypes.REVERSE_PORTAL);
    }

    private static void tickZone(ServerPlayer player, State s, long now) {
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity e : enemies(player, RADIUS)) {
            if (s.floating.add(e)) { e.setNoGravity(true); RelicUtil.push(e, e.getDeltaMovement().add(0, 0.15, 0)); }
        }
        s.floating.removeIf(e -> {
            if (e.isAlive() && e.distanceTo(player) <= RADIUS + 1) return false;
            e.setNoGravity(false);
            return true;
        });
        for (Projectile p : enemyProjectiles(player, RADIUS)) {
            if (!p.isNoGravity()) { p.setNoGravity(true); p.setDeltaMovement(p.getDeltaMovement().scale(0.4)); }
        }
        if (now % 5 == 0) EternyEffects.ring(level, player, RADIUS, ParticleTypes.END_ROD);
    }

    // ---------- Nv 15 — Gravidade Manipulada ----------

    private static final class Shifted { final LivingEntity entity; final Vec3 dir; final long until; double travel; Shifted(LivingEntity e, Vec3 d, long u) { entity = e; dir = d; until = u; } }
    private static final List<Shifted> SHIFTED = new ArrayList<>();

    public static boolean armShift(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).shiftArmed = true;
        RelicUtil.say(player, Component.translatable("relic.nexus.gravix.gravidade_manipulada_pronta_acerte_um"));
        return true;
    }

    @SubscribeEvent
    public static void shiftHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect()) return;
        State s = STATES.get(player.getUUID());
        if (s == null || !s.shiftArmed || !RelicTargets.canHit(player, event.getEntity())) return;
        s.shiftArmed = false;
        Vec3 dir = player.getLookAngle().normalize();
        int ticks = dir.y > 0.7 ? 60 : 100;
        if (RelicUtil.isBoss(event.getEntity())) ticks /= 2;
        SHIFTED.removeIf(x -> x.entity == event.getEntity());
        SHIFTED.add(new Shifted(event.getEntity(), dir, player.level().getGameTime() + ticks));
        event.getEntity().setNoGravity(true);
    }

    private static boolean tickShift(Shifted x, ServerLevel level, long now) {
        LivingEntity e = x.entity;
        if (!e.isAlive() || now >= x.until) { e.setNoGravity(false); return true; }
        Vec3 v = e.getDeltaMovement().add(x.dir.scale(0.08));
        if (v.length() > 3) v = v.normalize().scale(3);
        double speed = v.dot(x.dir);
        boolean hit = (Math.abs(x.dir.y) > 0.5 && e.verticalCollision) || (Math.abs(x.dir.y) <= 0.5 && e.horizontalCollision);
        if (hit && x.travel > 3) {
            e.hurtServer(level, level.damageSources().fall(), (float) (x.travel - 3));
            x.travel = 0;
        } else if (!hit) x.travel += Math.max(0, speed);
        RelicUtil.push(e, v);
        e.resetFallDistance();
        return false;
    }

    // ---------- Nv 20 — Gravity Max ----------

    public static boolean canMax(ServerPlayer player, ItemStack stack) {
        return drifting(player) && !item().ready(stack, player.level().getGameTime(), 2);
    }

    public static boolean gravityMax(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).maxUntil = player.level().getGameTime() + MAX_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 2.0f, 0.5f);
        return true;
    }

    private record Squeezed(LivingEntity entity, long until) {}
    private static final List<Squeezed> SQUEEZED = new ArrayList<>();

    private static void tickMax(ServerPlayer player, long now) {
        ServerLevel level = (ServerLevel) player.level();
        player.setDeltaMovement(Vec3.ZERO);
        RelicUtil.syncMotion(player);
        if (now % 4 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 1, 1, 1, 0.5);
        for (Projectile p : enemyProjectiles(player, MAX_RADIUS)) { p.setNoGravity(false); p.setDeltaMovement(0, -0.5, 0); }
        if (now % 20 != 0) return;
        for (LivingEntity e : enemies(player, MAX_RADIUS)) {
            double d = e.distanceTo(player);
            float damage; int slow;
            if (d <= 5) { damage = 20; slow = -1; }
            else if (d <= 10) { damage = 12; slow = 3; }
            else { damage = 6; slow = 1; }
            if (RelicUtil.isBoss(e)) damage /= 2;
            e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("gravix_pressao"), player), RelicUtil.reduced(e, damage) * (RelicUtil.isBoss(e) ? 2 : 1));
            if (slow < 0) e.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 60, 0), player);
            else e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, slow), player);
            var scale = e.getAttribute(Attributes.SCALE);
            if (scale != null) scale.addOrUpdateTransientModifier(new AttributeModifier(SQUEEZE, -0.5 * (1 - d / MAX_RADIUS), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            SQUEEZED.removeIf(x -> x.entity == e);
            SQUEEZED.add(new Squeezed(e, now + 60));
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 0.4f);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        State s = STATES.get(player.getUUID());
        boolean worn = wearing(player, 0);
        var input = player.getLastClientInput();
        boolean combo = input.shift() && input.jump();
        if (worn) {
            if (s == null) s = state(player);
            if (combo && !s.lastCombo) setDrift(player, s, !s.drifting);
            s.lastCombo = combo;
            if (s.drifting && now % 20 == 0 && !player.hasInfiniteMaterials()) {
                var magic = player.getData(ModAttachmentType.MAGIC_STATS);
                if (magic.getMana() < DRIFT_MANA_PER_SECOND) setDrift(player, s, false);
                else { magic.useMana(DRIFT_MANA_PER_SECOND); player.syncData(ModAttachmentType.MAGIC_STATS); }
            }
        } else if (s != null && s.drifting) setDrift(player, s, false);
        if (s == null) return;
        if (s.fieldUntil > 0) { if (now >= s.fieldUntil) endField(s); else tickField(player, s, now); }
        if (s.zoneUntil > 0) { if (now >= s.zoneUntil) endZone(s); else tickZone(player, s, now); }
        if (s.maxUntil > 0) { if (now >= s.maxUntil) s.maxUntil = 0; else tickMax(player, now); }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        SHIFTED.removeIf(x -> x.entity.level() == level && tickShift(x, level, now));
        SQUEEZED.removeIf(x -> {
            if (x.entity.level() != level || now < x.until) return false;
            var scale = x.entity.getAttribute(Attributes.SCALE);
            if (scale != null) scale.removeModifier(SQUEEZE);
            return true;
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        State s = STATES.remove(event.getEntity().getUUID());
        if (s != null) { endField(s); endZone(s); }
        event.getEntity().setNoGravity(false);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (State s : STATES.values()) { endField(s); endZone(s); }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) p.setNoGravity(false);
        SHIFTED.forEach(x -> x.entity.setNoGravity(false));
        STATES.clear(); SHIFTED.clear();
    }
}
