package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da Phoenixy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class PhoenixyEffects {
    private PhoenixyEffects() {}

    public static final int PROTECT_MANA = 60, PROTECT_COOLDOWN = 1800, PROTECT_TICKS = 600;
    public static final int FURY_MANA = 80, FURY_COOLDOWN = 1200, FURY_TICKS = 200, FURY_RADIUS = 5;
    public static final int REBIRTH_COOLDOWN = 18000, REBIRTH_LOCK = 1200;
    private static final Identifier SMALL = Identifier.fromNamespaceAndPath(Nexus.MODID, "phoenixy_renascida");
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> FIRE = RelicUtil.damageType("fogo_fenix");

    private static PhoenixyItem item() { return ModItems.PHOENIXY.get(); }

    private static boolean carrier(Player p, int slot) { return !item().findIn(p).isEmpty() && item().isUnlocked(p, slot); }

    public static boolean isFire(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(RelicUtil.damageType("fogo_draconico")) || source.is(FIRE)
                || source.is(RelicUtil.damageType("cultry_queimacao"));
    }

    // ---------- Nv 1 — Fogo da Vida (e Protecao) ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void incoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !isFire(event.getSource())) return;
        boolean healed = victim.hasEffect(ModEffects.FOGO_DA_VIDA)
                || (victim instanceof Player p && carrier(p, 0) && !reborn(p));
        if (!healed) return;
        victim.heal(event.getAmount());
        victim.clearFire();
        event.setCanceled(true);
    }

    // ---------- Nv 5 — Protecao da Fenix ----------

    public static boolean protect(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int count = 0;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3),
                e -> RelicTargets.isAlly(player, e) && e != player && e.distanceTo(player) <= 3)) {
            e.addEffect(new MobEffectInstance(ModEffects.FOGO_DA_VIDA, PROTECT_TICKS, 0, false, true, true));
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            count++;
        }
        player.addEffect(new MobEffectInstance(ModEffects.FOGO_DA_VIDA, PROTECT_TICKS, 0, false, true, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.4f);
        RelicUtil.say(player, Component.translatable(count == 1 ? "relic.nexus.phoenixy.protecao_aliado" : "relic.nexus.phoenixy.protecao_aliados", count));
        return true;
    }

    // ---------- Nv 10 — Furia da Fenix ----------

    private static final Map<UUID, Long> FURY = new HashMap<>();

    public static boolean fury(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        FURY.put(player.getUUID(), player.level().getGameTime() + FURY_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    // ---------- Nv 15 — Asas Flamejantes ----------

    private static final Map<UUID, Long> BOOST = new HashMap<>();

    public static boolean flameBoost(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (BOOST.getOrDefault(player.getUUID(), 0L) > now || reborn(player)) return true;
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int cost = Math.max(1, magic.getMaxMana() / 20);
        if (!player.hasInfiniteMaterials()) {
            if (magic.getMana() < cost) { RelicUtil.say(player, Component.translatable("relic.nexus.phoenixy.mana_insuficiente", cost)); return true; }
            magic.useMana(cost);
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
        player.level().addFreshEntity(new FireworkRocketEntity(player.level(), new ItemStack(Items.FIREWORK_ROCKET), player));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 30, 0.4, 0.4, 0.4, 0.15);
        BOOST.put(player.getUUID(), now + 20);
        return true;
    }

    // ---------- Nv 20 — Renascimento ----------

    private static final Map<UUID, Long> REBORN = new HashMap<>();

    public static boolean reborn(Player player) {
        Long until = REBORN.get(player.getUUID());
        return until != null && until > player.level().getGameTime();
    }

    /** Chamado por RelicDeathRules. */
    public static boolean rebirth(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player) || !carrier(player, 4)) return false;
        ItemStack stack = item().findIn(player);
        if (!item().ready(stack, player.level().getGameTime(), 4)) return false;
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5), e -> e != player && e.distanceTo(player) <= 5)) {
            if (RelicTargets.isAlly(player, e)) e.heal(14);
            else if (RelicTargets.canHit(player, e)) e.hurtServer(level, RelicUtil.source(level, FIRE, player), RelicUtil.reduced(e, 14));
        }
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 200, 2, 1.5, 2, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_DEATH, SoundSource.PLAYERS, 1.5f, 0.5f);
        RelicEffectRules.allowRemoval(player::removeAllEffects);
        player.clearFire();
        player.setHealth(player.getMaxHealth() / 2);
        var scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) scale.addOrUpdateTransientModifier(new AttributeModifier(SMALL, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        item().setCooldownTicks(player, stack, 4, REBIRTH_COOLDOWN);
        REBORN.put(player.getUUID(), level.getGameTime() + REBIRTH_LOCK);
        SMALL_UNTIL.put(player.getUUID(), level.getGameTime() + REBIRTH_COOLDOWN);
        RelicUtil.say(player, Component.translatable("relic.nexus.phoenixy.renascimento_da_fenix"));
        return true;
    }

    private static final Map<UUID, Long> SMALL_UNTIL = new HashMap<>();

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        ServerLevel level = (ServerLevel) player.level();
        Long small = SMALL_UNTIL.get(player.getUUID());
        if (small != null && now >= small) {
            SMALL_UNTIL.remove(player.getUUID());
            var scale = player.getAttribute(Attributes.SCALE);
            if (scale != null) scale.removeModifier(SMALL);
        }
        Long fury = FURY.get(player.getUUID());
        if (fury != null) {
            if (now >= fury) FURY.remove(player.getUUID());
            else {
                if (now % 4 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double a = level.getRandom().nextDouble() * Math.PI * 2, r = level.getRandom().nextDouble() * FURY_RADIUS;
                        level.sendParticles(ParticleTypes.FLAME, player.getX() + Math.cos(a) * r, player.getY() + 0.3, player.getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0.01);
                    }
                    level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1, player.getZ(), 4, 2, 0.5, 2, 0);
                }
                if (now % 20 == 0) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FURY_RADIUS), e -> e.distanceTo(player) <= FURY_RADIUS)) {
                        if (e == player || (RelicTargets.isAlly(player, e) && e.hasEffect(ModEffects.FOGO_DA_VIDA))) e.heal(2);
                        else if (RelicTargets.canHit(player, e)) e.addEffect(new MobEffectInstance(ModEffects.FOGO_FENIX, 40, 0), player);
                    }
                }
            }
        }
        // Brasas das asas.
        if (player.isFallFlying() && carrier(player, 3) && now % 10 == 0 && !reborn(player)) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
            level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(), 2, 0.2, 0.1, 0.2, 0);
            var area = player.getBoundingBox().inflate(2.5, 0, 2.5).expandTowards(0, -12, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                if (RelicTargets.isAlly(player, e)) e.addEffect(new MobEffectInstance(ModEffects.FOGO_DA_VIDA, 100, 0, false, true, true));
                else if (RelicTargets.canHit(player, e)) e.addEffect(new MobEffectInstance(ModEffects.FOGO_FENIX, 100, 0), player);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        FURY.remove(event.getEntity().getUUID());
        BOOST.remove(event.getEntity().getUUID());
    }
}
