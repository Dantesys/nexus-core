package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Souly (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class SoulyEffects {
    private SoulyEffects() {}

    public static final int FEAR_MANA = 70, FEAR_COOLDOWN = 2400;
    public static final int CONSUME_MANA = 60, CONSUME_COOLDOWN = 2400, CONSUME_TICKS = 600;
    public static final int MARK_TICKS = 6000;
    public static final int EATER_MANA = 300, EATER_COOLDOWN = 18000, EATER_TICKS = 600, BLINK_COOLDOWN = 100;
    private static final Identifier STOLEN = Identifier.fromNamespaceAndPath(Nexus.MODID, "souly_roubada");
    private static final Identifier GAINED = Identifier.fromNamespaceAndPath(Nexus.MODID, "souly_ganha");
    private static final Identifier CONSUMED = Identifier.fromNamespaceAndPath(Nexus.MODID, "souly_consumo");
    private static final Identifier MARK_DRAIN = Identifier.fromNamespaceAndPath(Nexus.MODID, "souly_marca");

    private static SoulyItem item() { return ModItems.SOULY.get(); }

    public static boolean carrier(Player player) {
        return !item().findIn(player).isEmpty() && item().isUnlocked(player, 0);
    }

    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }

    private static int totems(Player p) { return RelicUtil.count(p, s -> s.is(Items.TOTEM_OF_UNDYING)); }

    private static void modify(LivingEntity e, Identifier id, double amount) {
        var attr = e.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        if (amount == 0) attr.removeModifier(id);
        else attr.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    private static double current(LivingEntity e, Identifier id) {
        var attr = e.getAttribute(Attributes.MAX_HEALTH);
        var mod = attr == null ? null : attr.getModifier(id);
        return mod == null ? 0 : mod.amount();
    }

    // ---------- Nv 1 — Totem por Totem ----------

    private static boolean protectedVictim(LivingEntity victim) {
        if (victim.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || victim.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return true;
        if (victim.hasEffect(ModEffects.VIDA_EXTRA)) return true;
        if (victim instanceof Player p) {
            ItemStack eterny = p.getMainHandItem().is(ModItems.ETERNY.get()) ? p.getMainHandItem() : p.getOffhandItem();
            if (eterny.is(ModItems.ETERNY.get()) && ModItems.ETERNY.get().ready(eterny, p.level().getGameTime(), 0)) return true;
            if (carrier(p) && totems(p) > 0) return true;
        }
        return false;
    }

    /** O golpe mortal do portador (com totem no inventario) anula a protecao do alvo, gastando um totem de cada lado. */
    public static boolean annul(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim || !wielding(killer) || !item().isUnlocked(killer, 0)) return false;
        if (totems(killer) == 0 || !protectedVictim(victim)) return false;
        RelicUtil.consume(killer, s -> s.is(Items.TOTEM_OF_UNDYING), 1);
        if (victim.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) victim.getMainHandItem().shrink(1);
        else if (victim.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) victim.getOffhandItem().shrink(1);
        else if (victim instanceof Player p && carrier(p)) RelicUtil.consume(p, s -> s.is(Items.TOTEM_OF_UNDYING), 1);
        ((ServerLevel) victim.level()).sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1, victim.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
        RelicUtil.say(killer, Component.translatable("relic.nexus.souly.totem_por_totem"));
        return true;
    }

    /** Totens do inventario do portador ativam sem precisar segurar. */
    public static boolean inventoryTotem(LivingEntity victim) {
        if (!(victim instanceof ServerPlayer player) || !carrier(player) || totems(player) == 0) return false;
        RelicUtil.consume(player, s -> s.is(Items.TOTEM_OF_UNDYING), 1);
        RelicUtil.totemSave(player);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer killer) || !wielding(killer)) return;
        LivingEntity victim = event.getEntity();
        ServerLevel level = (ServerLevel) victim.level();
        Mark mark = MARKS.get(killer.getUUID());
        boolean marked = mark != null && mark.target.equals(victim.getUUID());
        boolean eligible = victim instanceof Player || (RelicUtil.isBoss(victim) && victim.getType() != EntityTypes.EVOKER);
        if (marked || (item().isUnlocked(killer, 0) && eligible && level.getRandom().nextFloat() < 0.10f)) {
            level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY() + 0.5, victim.getZ(), new ItemStack(Items.TOTEM_OF_UNDYING)));
            if (marked) MARKS.remove(killer.getUUID());
        }
    }

    // ---------- Nv 5 — Medo Mortal / Nv 10 — Consumo de Vida ----------

    private static final class State {
        long fearUntil, consumeUntil, eaterUntil, nextBlink; float stealPerHit, gained, gainCap;
        final Map<UUID, Float> stolen = new HashMap<>();
        final Map<UUID, Integer> eaterMarks = new HashMap<>();
    }
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }

    public static boolean fear(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        double res = context.resonance() / 1000.0;
        double radius = 3 + 2 * res;
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius)) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0), player);
        }
        State s = state(player);
        s.fearUntil = level.getGameTime() + 600 + (long) (600 * res);
        s.stealPerHit = (float) (1 + res);
        s.gainCap = 10 + context.affinity() / 100f;
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, radius / 2, 0.5, radius / 2, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5f, 0.7f);
        return true;
    }

    private static void endFear(ServerPlayer player, State s) {
        var server = player.level().getServer();
        for (var entry : s.stolen.entrySet()) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(entry.getKey()) instanceof LivingEntity victim) modify(victim, STOLEN, 0);
            }
        }
        s.stolen.clear();
        modify(player, GAINED, 0);
        if (s.gained > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, Math.max(0, (int) Math.ceil(s.gained / 4) - 1)));
        }
        s.gained = 0;
        s.fearUntil = 0;
    }

    public static boolean consume(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        s.consumeUntil = player.level().getGameTime() + CONSUME_TICKS;
        modify(player, CONSUMED, -4);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, CONSUME_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, CONSUME_TICKS, 1));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    // ---------- Nv 15 — Soul Coletor ----------

    private record Mark(UUID target, long until, boolean empowered) {}
    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    private static final Map<UUID, Long> DRAIN_UNTIL = new HashMap<>();

    private static boolean markable(LivingEntity e) { return e instanceof Player || RelicUtil.isBoss(e); }

    // ---------- Nv 20 — Soul Eater ----------

    private static List<LivingEntity> eaterTargets(ServerPlayer player) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(200),
                e -> markable(e) && RelicTargets.canHit(player, e) && e.distanceTo(player) <= 200);
    }

    public static boolean canEat(ServerPlayer player, ItemStack stack) {
        return totems(player) > 0 && !eaterTargets(player).isEmpty();
    }

    public static boolean eat(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        List<LivingEntity> targets = eaterTargets(player);
        int count = RelicUtil.consume(player, s -> s.is(Items.TOTEM_OF_UNDYING), 64 * 40);
        State s = state(player);
        s.eaterMarks.clear();
        var random = player.level().getRandom();
        List<LivingEntity> pool = new ArrayList<>(targets);
        for (int i = 0; i < count; i++) {
            LivingEntity t = pool.isEmpty() ? targets.get(random.nextInt(targets.size())) : pool.remove(random.nextInt(pool.size()));
            s.eaterMarks.merge(t.getUUID(), 1, Integer::sum);
            PrivateVisuals.glow(player, t, EATER_TICKS, net.minecraft.world.scores.TeamColor.DARK_PURPLE, true);
        }
        s.eaterUntil = player.level().getGameTime() + EATER_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
        RelicUtil.say(player, Component.translatable("relic.nexus.souly.soul_eater_marcas_em_alvos", count, s.eaterMarks.size()));
        return true;
    }

    private static boolean eaterActive(State s, long now) { return s != null && s.eaterUntil > now; }

    // ---------- Combate ----------

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect() || !wielding(player)) return;
        LivingEntity target = event.getEntity();
        long now = player.level().getGameTime();
        // Soul Coletor: marca o primeiro jogador/boss atacado.
        if (item().isUnlocked(player, 3) && markable(target) && RelicTargets.canHit(player, target)) {
            Mark mark = MARKS.get(player.getUUID());
            if (mark == null || mark.until <= now) {
                boolean empowered = totems(player) > 0;
                if (empowered) RelicUtil.consume(player, s -> s.is(Items.TOTEM_OF_UNDYING), 1);
                mark = new Mark(target.getUUID(), now + MARK_TICKS, empowered);
                MARKS.put(player.getUUID(), mark);
                PrivateVisuals.glow(player, target, MARK_TICKS, net.minecraft.world.scores.TeamColor.DARK_RED, true);
                RelicUtil.say(player, Component.translatable(empowered ? "relic.nexus.souly.alma_marcada_totem" : "relic.nexus.souly.alma_marcada"));
            }
            if (mark.target.equals(target.getUUID()) && mark.empowered) {
                event.setAmount(event.getAmount() * 2);
                modify(target, MARK_DRAIN, current(target, MARK_DRAIN) - 1);
                DRAIN_UNTIL.put(target.getUUID(), now + MARK_TICKS);
            }
        }
        State s = STATES.get(player.getUUID());
        if (eaterActive(s, now)) {
            Integer marks = s.eaterMarks.get(target.getUUID());
            if (marks != null) event.setAmount(event.getAmount() * (1 + 0.25f * (marks - 1)));
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect() || !wielding(player)) return;
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        LivingEntity target = event.getEntity();
        long now = player.level().getGameTime();
        float dealt = event.getHealthDamage();
        if (s.consumeUntil > now) player.heal(dealt * (0.25f + 0.25f * RelicUtil.magic(player).getRessonancia() / 1000f));
        if (eaterActive(s, now)) player.heal(dealt * 0.30f);
        if (s.fearUntil > now && target.isAlive() && RelicTargets.canHit(player, target)) {
            float room = s.gainCap - s.gained;
            float steal = Math.min(s.stealPerHit, Math.min(room, (float) (target.getMaxHealth() - 2)));
            if (steal > 0) {
                s.stolen.merge(target.getUUID(), steal, Float::sum);
                modify(target, STOLEN, current(target, STOLEN) - steal);
                s.gained += steal;
                modify(player, GAINED, s.gained);
                player.heal(steal);
                ((ServerLevel) player.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    /** Durante o Soul Eater, o dano da Souly nos marcados nao se regenera. */
    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        long now = event.getEntity().level().getGameTime();
        for (State s : STATES.values()) {
            if (eaterActive(s, now) && s.eaterMarks.containsKey(event.getEntity().getUUID())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (now % 20 == 0) DRAIN_UNTIL.entrySet().removeIf(e -> {
            if (e.getValue() > now) return false;
            for (ServerLevel level : player.level().getServer().getAllLevels())
                if (level.getEntity(e.getKey()) instanceof LivingEntity victim) modify(victim, MARK_DRAIN, 0);
            return true;
        });
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        if (s.fearUntil > 0 && now >= s.fearUntil) endFear(player, s);
        if (s.consumeUntil > 0 && now >= s.consumeUntil) { s.consumeUntil = 0; modify(player, CONSUMED, 0); }
        if (eaterActive(s, now) && player.isShiftKeyDown() && now >= s.nextBlink) {
            LivingEntity look = RelicUtil.targetEntity(player, 200, e -> s.eaterMarks.containsKey(e.getUUID()));
            if (look != null && look.distanceTo(player) >= 10) {
                Vec3 behind = look.position().subtract(look.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
                player.teleportTo(behind.x, look.getY(), behind.z);
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, look.getEyePosition());
                RelicUtil.pushToFreeSpace(player);
                ((ServerLevel) player.level()).sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                s.nextBlink = now + BLINK_COOLDOWN;
            }
        }
        if (s.eaterUntil > 0 && now >= s.eaterUntil) { s.eaterUntil = 0; s.eaterMarks.clear(); }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.remove(player.getUUID());
        if (s != null) { endFear(player, s); modify(player, CONSUMED, 0); }
        modify(player, STOLEN, 0);
        modify(player, MARK_DRAIN, 0);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            State s = STATES.get(player.getUUID());
            if (s != null) { endFear(player, s); modify(player, CONSUMED, 0); }
        }
        STATES.clear();
        MARKS.clear();
        DRAIN_UNTIL.clear();
    }
}
