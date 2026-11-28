package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Venossy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class VenossyEffects {
    private VenossyEffects() {}

    public static final int AURA_MANA = 50, AURA_COOLDOWN = 1200, AURA_TICKS = 200, AURA_RADIUS = 3;
    public static final int TOXIN_MANA = 90, TOXIN_COOLDOWN = 3600;
    public static final int PARALYZE_MANA = 80, PARALYZE_COOLDOWN = 2400;
    public static final int BLOOD_MANA = 300, BLOOD_COOLDOWN = 18000;

    private static VenossyItem item() { return ModItems.VENOSSY.get(); }
    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }
    private static boolean carrier(Player p) { return !item().findIn(p).isEmpty() && item().isUnlocked(p, 0); }

    // ---------- Nv 1 — Mestre dos Venenos ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void immune(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p) || !carrier(p)) return;
        var e = event.getEffectInstance().getEffect();
        if (e.is(MobEffects.POISON) || e.is(ModEffects.VENENO_ASSASSINO)) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    private static final Map<UUID, Long> AURA = new HashMap<>();
    private static final Set<UUID> BLOOD_ARMED = new HashSet<>();

    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect() || !wielding(player)) return;
        LivingEntity target = event.getEntity();
        if (!RelicTargets.canHit(player, target) && target.isAlive()) return;
        int lvl = item().masteryLevel(player);
        if (item().isUnlocked(player, 0)) target.addEffect(new MobEffectInstance(ModEffects.VENENO_ASSASSINO, 100, lvl >= 20 ? 2 : lvl >= 10 ? 1 : 0), player);
        if (BLOOD_ARMED.remove(player.getUUID()) && target.isAlive()) {
            boolean boss = RelicUtil.isBoss(target);
            target.addEffect(new MobEffectInstance(ModEffects.TOXINA_AMPLIFICADA, boss ? 2400 : MobEffectInstance.INFINITE_DURATION, 0), player);
            target.addEffect(new MobEffectInstance(ModEffects.PARALISIA, boss ? 140 : 300, 0), player);
            RelicUtil.say(player, Component.translatable("relic.nexus.venossy.sangue_toxico"));
        }
    }

    /** Aura: dobra o dano dos golpes e do veneno do portador. */
    @SubscribeEvent
    public static void aura(LivingIncomingDamageEvent event) {
        if (AURA.isEmpty()) return;
        LivingEntity victim = event.getEntity();
        long now = victim.level().getGameTime();
        boolean poison = event.getSource().is(RelicUtil.damageType("veneno_assassino"));
        for (var e : AURA.entrySet()) {
            if (e.getValue() <= now) continue;
            ServerPlayer owner = victim.level().getServer().getPlayerList().getPlayer(e.getKey());
            if (owner == null || owner.level() != victim.level() || owner.distanceTo(victim) > AURA_RADIUS || !RelicTargets.canHit(owner, victim)) continue;
            if (poison || (event.getSource().getEntity() == owner && event.getSource().isDirect())) { event.setAmount(event.getAmount() * 2); return; }
        }
    }

    public static boolean aura(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        AURA.put(player.getUUID(), player.level().getGameTime() + AURA_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITCH_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    // ---------- Nv 10 e 15 — adaga arremessada ----------

    private static final class Blade {
        final ServerPlayer owner; final Display.ItemDisplay display; Vec3 pos, vel; boolean returning; final boolean toxin;
        Blade(ServerPlayer o, Display.ItemDisplay d, Vec3 p, Vec3 v, boolean t) { owner = o; display = d; pos = p; vel = v; toxin = t; }
    }
    private static final Map<UUID, Blade> BLADES = new HashMap<>();

    public static boolean thrown(Player p) { return BLADES.containsKey(p.getUUID()); }

    private static boolean throwBlade(ServerPlayer player, ItemStack stack, boolean toxin) {
        if (thrown(player)) return false;
        Vec3 start = player.getEyePosition().subtract(0, 0.2, 0);
        Display.ItemDisplay display = Displays.item((ServerLevel) player.level(), stack, start, 0.8f);
        BLADES.put(player.getUUID(), new Blade(player, display, start, player.getLookAngle().scale(1.6), toxin));
        item().refreshModelData(player, stack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    public static boolean throwToxin(ServerPlayer player, ItemStack stack, RelicAbilityContext c) { return throwBlade(player, stack, true); }
    public static boolean throwParalyze(ServerPlayer player, ItemStack stack, RelicAbilityContext c) { return throwBlade(player, stack, false); }

    private static void strike(Blade b, LivingEntity target) {
        if (b.toxin) {
            if (!target.hasEffect(ModEffects.TOXINA)) target.addEffect(new MobEffectInstance(ModEffects.TOXINA, RelicUtil.isBoss(target) ? 1200 : MobEffectInstance.INFINITE_DURATION, 0), b.owner);
        } else target.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 100, 0), b.owner);
        ((ServerLevel) target.level()).sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        item().addMasteryXp(b.owner, 2);
    }

    private static boolean tickBlade(Blade b, ServerLevel level) {
        if (b.owner.isRemoved() || b.owner.level() != level) { endBlade(b); return true; }
        Vec3 next;
        if (!b.returning) {
            next = b.pos.add(b.vel);
            var hit = level.clip(new ClipContext(b.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b.owner));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, next).inflate(0.6), e -> RelicTargets.canHit(b.owner, e))) {
                if (e.getBoundingBox().inflate(0.4).clip(b.pos, next).isPresent()) { strike(b, e); b.returning = true; break; }
            }
            if (hit.getType() != HitResult.Type.MISS || b.pos.distanceTo(b.owner.position()) > 30) { b.returning = true; next = hit.getLocation(); }
            b.vel = b.vel.add(0, -0.03, 0);
        } else {
            Vec3 to = b.owner.getEyePosition().subtract(b.pos);
            if (to.length() < 1.5) { endBlade(b); return true; }
            next = b.pos.add(to.normalize().scale(Math.min(1.6, to.length())));
        }
        b.pos = next;
        b.display.teleportTo(next.x, next.y, next.z);
        b.display.setYRot(b.display.getYRot() + 40);
        return false;
    }

    private static void endBlade(Blade b) {
        b.display.discard();
        BLADES.remove(b.owner.getUUID());
        ItemStack relic = item().findIn(b.owner);
        if (!relic.isEmpty()) item().refreshModelData(b.owner, relic);
    }

    // ---------- Nv 20 — Sangue Toxico ----------

    public static boolean canToxicBlood(ServerPlayer player, ItemStack stack) {
        return player.hasEffect(ModEffects.TOXINA) && player.hasEffect(ModEffects.PARALISIA);
    }

    public static boolean toxicBlood(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicEffectRules.allowRemoval(() -> { player.removeEffect(ModEffects.TOXINA); player.removeEffect(ModEffects.PARALISIA); });
        BLOOD_ARMED.add(player.getUUID());
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 0.5f);
        RelicUtil.say(player, Component.translatable("relic.nexus.venossy.sangue_toxico_pronto_acerte_alguem"));
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (Blade b : new ArrayList<>(BLADES.values())) if (b.display.level() == level) tickBlade(b, level);
        if (now % 5 == 0) {
            AURA.entrySet().removeIf(e -> e.getValue() <= now);
            for (var e : AURA.entrySet()) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (owner != null && owner.level() == level) EternyEffects.ring(level, owner, AURA_RADIUS, ParticleTypes.ITEM_SLIME);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        Blade b = BLADES.get(event.getEntity().getUUID());
        if (b != null) endBlade(b);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        new ArrayList<>(BLADES.values()).forEach(VenossyEffects::endBlade);
    }
}
