package org.dantesys.nexus.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Bastry (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class BastryEffects {
    private BastryEffects() {}

    public static final int FIELD_MANA = 60, FIELD_COOLDOWN = 1200, FIELD_TICKS = 200;
    public static final double FIELD_RADIUS = 2.0;
    public static final int COUNTER_MANA = 60, COUNTER_COOLDOWN = 1200, COUNTER_TICKS = 200;
    public static final int STEEL_MANA = 40, STEEL_COOLDOWN = 1200;
    public static final int UNTOUCHABLE_MANA = 200, UNTOUCHABLE_COOLDOWN = 12000, UNTOUCHABLE_TICKS = 600;
    /** A carga comeca a cair depois de 5 s sem bloquear: 2% do limite por segundo. */
    public static final int DECAY_DELAY = 100;
    public static final double DECAY_PER_SECOND = 0.02;

    private static final ResourceKey<DamageType> COUNTER = RelicUtil.damageType("bastry_counter");
    private static final Identifier STEEL_KNOCKBACK = Identifier.fromNamespaceAndPath(Nexus.MODID, "pele_de_aco");
    private static final String INSIDE_TAG = "NexusCampoDentro", REFLECTED_TAG = "NexusCampoRefletido";
    private static final DustParticleOptions FIELD_COLOR = new DustParticleOptions(0xB0B0FF, 0.8f);

    private static final class State {
        double charge;
        long lastBlock, fieldUntil, counterUntil, steelUntil, blastUntil, untouchableUntil;
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static BastryItem item() { return ModItems.BASTRY.get(); }
    private static State state(LivingEntity player) { return STATES.computeIfAbsent(player.getUUID(), id -> new State()); }
    private static long now(Entity entity) { return entity.level().getGameTime(); }

    /** Limite da carga: maestria e Capacidade (mana maxima). */
    public static double cap(ServerPlayer player) {
        MagicStatus magic = RelicUtil.magic(player);
        return (20 + item().masteryLevel(player) * 4) * (0.5 + Math.clamp(magic.getMaxMana(), 0, MagicStatus.MAX_STATUS) / 1000.0);
    }
    public static double charge(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s == null ? 0 : s.charge;
    }

    private static void refresh(ServerPlayer player) {
        ItemStack stack = RelicUtil.find(player, item());
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    // ---------- Consultas para outras reliquias ----------

    private static boolean fieldActive(State s, Entity owner) { return s.fieldUntil > now(owner); }

    /** Dono do campo que contem o ponto, se quem atirou estiver fora dele; senao null. */
    public static ServerPlayer reflectingField(ServerLevel level, Vec3 point, Entity shooter) {
        for (var entry : STATES.entrySet()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || owner.level() != level || !fieldActive(entry.getValue(), owner) || owner == shooter) continue;
            Vec3 center = owner.position().add(0, 1, 0);
            if (point.distanceTo(center) > FIELD_RADIUS) continue;
            if (shooter != null && shooter.position().add(0, 1, 0).distanceTo(center) <= FIELD_RADIUS) continue;
            return owner;
        }
        return null;
    }

    /** Alvo protegido por algum Campo Reflexivo (fragmentos da Rupty nao o marcam). */
    public static boolean isProtected(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        return reflectingField(level, target.position().add(0, target.getBbHeight() / 2, 0), null) != null;
    }

    public static boolean isUntouchable(LivingEntity entity) {
        State s = STATES.get(entity.getUUID());
        return s != null && s.untouchableUntil > now(entity);
    }

    // ---------- Nv 1 — Carga ----------

    @SubscribeEvent
    public static void shieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getBlocked()) return;
        if (!(player.getUseItem().getItem() instanceof BastryItem)) return;
        State s = state(player);
        if (s.counterUntil > now(player)) {
            event.setBlocked(false);
            return;
        }
        if (!item().isUnlocked(player, 0)) return;
        double cap = cap(player);
        if (s.charge >= cap) {
            // Carga cheia: o dano bloqueado passa.
            event.setBlocked(false);
            return;
        }
        s.charge = Math.min(cap, s.charge + event.getBlockedDamage());
        s.lastBlock = now(player);
        item().addMasteryXp(player, 1);
        refresh(player);
    }

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        // Gasta a carga no proximo golpe do dono (qualquer item): vale o maior valor, nunca a soma.
        if (source.getEntity() instanceof ServerPlayer attacker && source.isDirect() && !source.is(COUNTER)
                && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            State s = STATES.get(attacker.getUUID());
            if (s != null && s.charge > 0 && target != attacker) {
                event.setAmount((float) Math.max(event.getAmount(), s.charge));
                s.charge = 0;
                refresh(attacker);
            }
        }

        State s = STATES.get(target.getUUID());
        if (s == null) return;
        long now = now(target);
        if (s.untouchableUntil > now && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            return;
        }
        if (s.blastUntil > now && source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
            return;
        }
        // Counter: so corpo a corpo; dano devolvido nunca volta de novo.
        if (s.counterUntil > now && source.isDirect() && source.getEntity() instanceof LivingEntity attacker
                && attacker != target && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(COUNTER)
                && !source.is(DamageTypes.THORNS) && target instanceof ServerPlayer owner) {
            float received = event.getAmount() / 2;
            event.setAmount(received);
            ServerLevel level = (ServerLevel) owner.level();
            attacker.hurtServer(level, RelicUtil.source(level, COUNTER, owner), received * 2);
            level.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
            item().addMasteryXp(owner, 1);
        }
    }

    // ---------- Nv 5 — Campo Reflexivo ----------

    public static boolean field(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).fieldUntil = now(player) + FIELD_TICKS + context.resonance() / 5;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
        return true;
    }

    /** Projeteis criados dentro de um campo nao sao refletidos; os do dono do campo somem. */
    @SubscribeEvent
    public static void projectileJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity shooter = projectile.getOwner();
        if (shooter != null) {
            State s = STATES.get(shooter.getUUID());
            if (s != null && fieldActive(s, shooter)) {
                event.setCanceled(true);
                return;
            }
        }
        for (var entry : STATES.entrySet()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || owner.level() != level || !fieldActive(entry.getValue(), owner)) continue;
            if (projectile.position().distanceTo(owner.position().add(0, 1, 0)) <= FIELD_RADIUS + 0.5) {
                projectile.getPersistentData().putBoolean(INSIDE_TAG, true);
            }
        }
    }

    private static void tickField(ServerPlayer owner, State s) {
        ServerLevel level = (ServerLevel) owner.level();
        Vec3 center = owner.position().add(0, 1, 0);
        if (now(owner) % 4 == 0) {
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + now(owner) * 0.1;
                level.sendParticles(FIELD_COLOR, center.x + Math.cos(a) * FIELD_RADIUS, center.y + Math.sin(a * 2) * 0.8,
                        center.z + Math.sin(a) * FIELD_RADIUS, 1, 0, 0, 0, 0);
            }
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(FIELD_RADIUS + 1))) {
            var data = projectile.getPersistentData();
            if (data.getBooleanOr(INSIDE_TAG, false) || data.getBooleanOr(REFLECTED_TAG, false)) continue;
            if (projectile.position().distanceTo(center) > FIELD_RADIUS) continue;
            Entity shooter = projectile.getOwner();
            Vec3 velocity = projectile.getDeltaMovement();
            double speed = Math.max(0.5, velocity.length());
            Vec3 direction = shooter != null && shooter != owner
                    ? shooter.position().add(0, shooter.getBbHeight() / 2, 0).subtract(projectile.position()).normalize()
                    : velocity.scale(-1).normalize();
            projectile.setDeltaMovement(direction.scale(speed));
            projectile.needsSync = true;
            projectile.getPersistentData().putBoolean(REFLECTED_TAG, true);
            // A flecha volta como sendo do dono do campo, para poder acertar quem atirou.
            if (!(projectile instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl)) projectile.setOwner(owner);
            level.playSound(null, projectile.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
            item().addMasteryXp(owner, 1);
        }
    }

    // ---------- Nv 10 — Counter ----------

    public static boolean counter(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).counterUntil = now(player) + COUNTER_TICKS;
        if (player.getUseItem().getItem() instanceof BastryItem) player.stopUsingItem();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
        return true;
    }

    // ---------- Nv 15 — Pele de Aco ----------

    public static boolean steelSkin(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        double fraction = Math.min(1.0, s.charge / Math.max(1, cap(player)));
        if (fraction < 0.25) {
            RelicUtil.say(player, "Carga insuficiente (mínimo 25%).");
            return false;
        }
        int ticks = 100 + (int) (300 * fraction) + context.resonance() / 5;
        s.steelUntil = now(player) + ticks;
        var knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.addOrUpdateTransientModifier(new AttributeModifier(STEEL_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE));
        if (fraction >= 0.5) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0));
        if (fraction >= 0.999) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 1));
            s.blastUntil = now(player) + ticks;
        } else if (fraction >= 0.75) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0));
        }
        s.charge = 0;
        refresh(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        RelicUtil.say(player, "Pele de Aço: " + (int) Math.round(fraction * 100) + "% da carga.");
        return true;
    }

    // ---------- Nv 20 — Intocavel ----------

    public static boolean canUntouchable(ServerPlayer player, ItemStack stack) {
        if (!RelicUtil.hasRank(RelicUtil.magic(player).getMaxMana(), Ranks.A)) return false;
        boolean lowHealth = player.getHealth() <= player.getMaxHealth() * 0.75f;
        boolean charged = charge(player) >= cap(player) * 0.75;
        if (!lowHealth && !charged) return false;
        long now = now(player);
        for (int slot = 1; slot <= 3; slot++) {
            if (item().isUnlocked(player, slot) && item().getCooldownRemaining(stack, now, slot) <= 0) return false;
        }
        return true;
    }

    public static boolean untouchable(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        boolean convert = s.charge > 0;
        Map<String, String> conversions = ConfigLists.pairs(Config.BASTRY_CONVERSIONS.get());
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            Holder<MobEffect> holder = effect.getEffect();
            if (holder.value().getCategory() != MobEffectCategory.HARMFUL) continue;
            player.removeEffect(holder);
            if (!convert) continue;
            String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            String to = conversions.get(id);
            if (to != null) ConfigLists.effect(to).ifPresent(good ->
                    player.addEffect(new MobEffectInstance(good, effect.getDuration(), effect.getAmplifier())));
        }
        if (convert) s.charge = 0;
        s.untouchableUntil = now(player) + UNTOUCHABLE_TICKS;
        refresh(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        RelicUtil.say(player, "Intocável por 30s!");
        return true;
    }

    @SubscribeEvent
    public static void effectApplicable(MobEffectEvent.Applicable event) {
        if (isUntouchable(event.getEntity()) && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        long now = now(player);
        if (fieldActive(s, player)) tickField(player, s);
        if (s.counterUntil > now && player.getUseItem().getItem() instanceof BastryItem) player.stopUsingItem();
        if (s.steelUntil > 0 && s.steelUntil <= now) {
            var knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) knockback.removeModifier(STEEL_KNOCKBACK);
            s.steelUntil = 0;
        }
        if (s.untouchableUntil > now && now % 5 == 0) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 2, 0.4, 0.6, 0.4, 0.01);
        }
        if (s.charge > 0 && now - s.lastBlock > DECAY_DELAY && now % 20 == 0) {
            s.charge = Math.max(0, s.charge - cap(player) * DECAY_PER_SECOND);
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var knockback = event.getEntity().getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.removeModifier(STEEL_KNOCKBACK);
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { STATES.clear(); }
}
