package org.dantesys.nexus.relic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da Eterny (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class EternyEffects {
    private EternyEffects() {}

    public static final int INFINITY_COOLDOWN = 2400;
    public static final int FIELD_MANA = 60, FIELD_COOLDOWN = 900, FIELD_TICKS = 200, FIELD_RADIUS = 5;
    public static final int NODAMAGE_MANA = 90, NODAMAGE_COOLDOWN = 2400, NODAMAGE_RADIUS = 3;
    public static final int EXTRA_MANA = 200, EXTRA_COOLDOWN = 36000, EXTRA_TICKS = 12000, EXTRA_RADIUS = 10;
    public static final int BURST_MANA = 250, BURST_COOLDOWN = 18000;
    private static final Identifier SMALL = Identifier.fromNamespaceAndPath(Nexus.MODID, "eterny_tamanho");
    private static final Identifier HALF_LIFE = Identifier.fromNamespaceAndPath(Nexus.MODID, "eterny_vida");

    private static EternyItem item() { return ModItems.ETERNY.get(); }

    private static boolean friend(ServerPlayer owner, LivingEntity e) {
        return RelicTargets.isAlly(owner, e) || (e instanceof net.minecraft.world.entity.player.Player p && p.getTeam() != null && p.getTeam() == owner.getTeam());
    }

    // ---------- Nv 1 — Infinidade ----------

    /** Chamado por RelicDeathRules. */
    public static boolean infinity(LivingEntity victim) {
        if (!(victim instanceof ServerPlayer player) || !item().heldBy(player) || !item().isUnlocked(player, 0)) return false;
        ItemStack stack = player.getMainHandItem().is(item()) ? player.getMainHandItem() : player.getOffhandItem();
        if (!item().ready(stack, player.level().getGameTime(), 0)) return false;
        RelicUtil.totemSave(player);
        item().setCooldownTicks(player, stack, 0, INFINITY_COOLDOWN);
        item().addMasteryXp(player, 5);
        return true;
    }

    /** Vida Extra: guardada se a Infinidade estiver disponivel (ela dispara antes). */
    public static boolean extraLife(LivingEntity victim) {
        if (!victim.hasEffect(ModEffects.VIDA_EXTRA)) return false;
        RelicUtil.totemSave(victim);
        return true;
    }

    // ---------- Nv 5 e 10 — campos que acompanham ----------

    private static final class Field { long fieldUntil, shieldUntil; float shieldCap; float absorbed; }
    private static final Map<UUID, Field> FIELDS = new HashMap<>();
    private static Field field(ServerPlayer p) { return FIELDS.computeIfAbsent(p.getUUID(), id -> new Field()); }

    public static boolean lifeField(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        field(player).fieldUntil = player.level().getGameTime() + FIELD_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    public static boolean noDamage(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        Field f = field(player);
        f.shieldUntil = player.level().getGameTime() + Math.min(200, 100 + context.resonance() / 10);
        f.shieldCap = 8 + context.affinity() / 100f;
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        return true;
    }

    /** Dano de mobs e jogadores em aliados dentro do !Dano vira absorcao (ate o teto). */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity) || victim.level().isClientSide()) return;
        long now = victim.level().getGameTime();
        for (var entry : FIELDS.entrySet()) {
            if (entry.getValue().shieldUntil <= now) continue;
            ServerPlayer owner = victim.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || owner.level() != victim.level() || !friend(owner, victim) || owner.distanceTo(victim) > NODAMAGE_RADIUS) continue;
            if (victim.getAbsorptionAmount() >= entry.getValue().shieldCap) continue;
            victim.setAbsorptionAmount(Math.min(entry.getValue().shieldCap, victim.getAbsorptionAmount() + event.getAmount()));
            ((ServerLevel) victim.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1, victim.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
            event.setCanceled(true);
            return;
        }
    }

    // ---------- Nv 15 — Vida Extra ----------

    public static boolean extraLifeCast(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int count = 0;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(EXTRA_RADIUS),
                e -> friend(player, e) && e.distanceTo(player) <= EXTRA_RADIUS)) {
            e.addEffect(new MobEffectInstance(ModEffects.VIDA_EXTRA, EXTRA_TICKS, 0, false, true, true));
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.6, 0.3, 0.2);
            count++;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
        RelicUtil.say(player, "Vida Extra em " + count + (count == 1 ? " alvo." : " alvos."));
        return true;
    }

    // ---------- Nv 20 — Explosao Vital ----------

    private static final Map<UUID, Long> BURSTS = new HashMap<>();

    public static boolean canBurst(ServerPlayer player, ItemStack stack) {
        return !item().ready(stack, player.level().getGameTime(), 0) && !BURSTS.containsKey(player.getUUID());
    }

    public static boolean burst(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6), e -> RelicTargets.canHit(player, e))) {
            target.hurtServer(level, level.damageSources().playerAttack(player), RelicUtil.reduced(target, (float) (10 * context.powerMultiplier())));
            Vec3 away = target.position().subtract(player.position()).normalize();
            RelicUtil.push(target, new Vec3(away.x * 2.0, 0.7, away.z * 2.0));
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 120, 1.5, 1.0, 1.5, 0.6);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
        int ticks = Math.max(600, 1200 - context.resonance() * 6 / 10);
        BURSTS.put(player.getUUID(), level.getGameTime() + ticks);
        var scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) scale.addOrUpdateTransientModifier(new AttributeModifier(SMALL, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.addOrUpdateTransientModifier(new AttributeModifier(HALF_LIFE, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, ticks, 0, false, false, true));
        return true;
    }

    private static void endBurst(ServerPlayer player) {
        if (BURSTS.remove(player.getUUID()) == null) return;
        var scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) scale.removeModifier(SMALL);
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(HALF_LIFE);
        RelicUtil.pushToFreeSpace(player);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Long burst = BURSTS.get(player.getUUID());
        if (burst != null && now >= burst) endBurst(player);
        Field f = FIELDS.get(player.getUUID());
        if (f == null) return;
        ServerLevel level = (ServerLevel) player.level();
        if (f.fieldUntil > now && now % 20 == 0) {
            float heal = 1.0F + RelicUtil.magic(player).getAfinidade() / 250f;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FIELD_RADIUS),
                    e -> friend(player, e) && e.distanceTo(player) <= FIELD_RADIUS)) {
                if (e.getHealth() >= e.getMaxHealth()) e.setAbsorptionAmount(Math.min(8, e.getAbsorptionAmount() + heal));
                else e.heal(heal);
                level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
        if (f.fieldUntil > now && now % 5 == 0) ring(level, player, FIELD_RADIUS, ParticleTypes.HAPPY_VILLAGER);
        if (f.shieldUntil > now && now % 5 == 0) ring(level, player, NODAMAGE_RADIUS, ParticleTypes.END_ROD);
        if (f.fieldUntil <= now && f.shieldUntil <= now) FIELDS.remove(player.getUUID());
    }

    static void ring(ServerLevel level, LivingEntity center, double radius, net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            level.sendParticles(particle, center.getX() + Math.cos(a) * radius, center.getY() + 0.2, center.getZ() + Math.sin(a) * radius, 1, 0, 0.05, 0, 0);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) endBurst(player);
        FIELDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) endBurst(player);
        FIELDS.clear();
    }
}
