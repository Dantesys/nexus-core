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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da Steely (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class SteelyEffects {
    private SteelyEffects() {}

    public static final int BODY_MANA = 60, BODY_COOLDOWN = 1800, BODY_TICKS = 400;
    public static final int GUARD_MANA = 120, GUARD_COOLDOWN = 3600;
    public static final int GOLEM_MANA = 300, GOLEM_COOLDOWN = 18000, GOLEM_TICKS = 3600;
    private static final String GUARD_TAG = "NexusGuardaCostas";
    private static final Identifier G_SCALE = id("golem_tamanho"), G_HEALTH = id("golem_vida"), G_DAMAGE = id("golem_dano"), G_STEP = id("golem_degrau");
    private static final Identifier T_DAMAGE = id("transformado_dano"), T_KB = id("transformado_repulsao"), T_STEP = id("transformado_degrau"), T_SCALE = id("transformado_tamanho");

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Nexus.MODID, "steely_" + path); }
    private static SteelyItem item() { return ModItems.STEELY.get(); }

    private static boolean wearing(Player p, int slot) {
        return p.getItemBySlot(EquipmentSlot.CHEST).is(item()) && item().isUnlocked(p, slot);
    }

    private static boolean nexusType(DamageSource source) {
        return source.typeHolder().unwrapKey().map(k -> k.identifier().getNamespace().equals(Nexus.MODID)).orElse(false);
    }

    // ---------- Nv 1 — Aco Arcano ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void reduce(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || !wearing(player, 0) && !transformed(player)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(RelicUtil.MORTE_GARANTIDA)) return;
        int level = item().masteryLevel(player);
        double reduction = 0.2 + 0.3 * Math.clamp(level - 1, 0, 19) / 19.0;
        float floor = event.getOriginalDamage() * 0.15f;
        float now = event.getNewDamage();
        if (now <= floor) return;
        event.setNewDamage(Math.max(floor, (float) (now * (1 - reduction))));
    }

    // ---------- Nv 5 — Corpo de Aco / Nv 10 — Aco Duro ----------

    private static final Map<UUID, Long> BODY = new HashMap<>();
    private static int reflectDepth = 0;

    public static boolean steelBody(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        BODY.put(player.getUUID(), player.level().getGameTime() + BODY_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, BODY_TICKS, 1));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
        return true;
    }

    private static boolean magic(DamageSource source) {
        return source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL) || nexusType(source);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        Long body = BODY.get(player.getUUID());
        if (body != null && body > player.level().getGameTime() && magic(source)
                && !source.is(RelicUtil.MORTE_GARANTIDA) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            return;
        }
        // Aco Duro: devolve parte do dano original (no maximo 2 reflexoes em cadeia).
        if ((wearing(player, 2) || transformed(player)) && source.getEntity() instanceof LivingEntity attacker && attacker != player && reflectDepth < 2) {
            ServerLevel level = (ServerLevel) player.level();
            float amount = event.getAmount() * (0.15f + 0.15f * RelicUtil.magic(player).getAfinidade() / 1000f);
            reflectDepth++;
            try { attacker.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("steely_reflexo"), player), amount); }
            finally { reflectDepth--; }
        }
    }

    // ---------- Nv 15 — Guarda-Costas ----------

    private static IronGolem golem(ServerPlayer player) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (var e : level.getEntities(EntityTypes.IRON_GOLEM, g -> g.getPersistentData().getBooleanOr(GUARD_TAG, false) && RelicTargets.isMinionOf(player, g))) {
                if (e.isAlive()) return e;
            }
        }
        return null;
    }

    public static boolean canGuard(ServerPlayer player, ItemStack stack) { return golem(player) == null && !transformed(player); }

    public static boolean guard(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        IronGolem g = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (g == null) return false;
        Vec3 pos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        g.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0);
        g.setPlayerCreated(true);
        g.setPersistenceRequired();
        RelicTargets.setMinion(g, player);
        g.getPersistentData().putBoolean(GUARD_TAG, true);
        mod(g, Attributes.SCALE, G_SCALE, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(g, Attributes.MAX_HEALTH, G_HEALTH, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(g, Attributes.ATTACK_DAMAGE, G_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(g, Attributes.STEP_HEIGHT, G_STEP, 2.0, AttributeModifier.Operation.ADD_VALUE);
        g.setHealth(g.getMaxHealth());
        level.addFreshEntity(g);
        RelicUtil.pushToFreeSpace(g);
        level.playSound(null, g.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static void mod(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, Identifier id, double v, AttributeModifier.Operation op) {
        var a = e.getAttribute(attr);
        if (a != null) a.addOrReplacePermanentModifier(new AttributeModifier(id, v, op));
    }

    @SubscribeEvent
    public static void golemDied(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem g) || !g.getPersistentData().getBooleanOr(GUARD_TAG, false)) return;
        ServerPlayer owner = RelicMinions.owner(g);
        if (owner == null) return;
        ItemStack stack = item().findIn(owner);
        if (!stack.isEmpty()) item().startCooldown(owner, stack, 3, GUARD_COOLDOWN);
        RelicUtil.say(owner, Component.translatable("relic.nexus.steely.seu_guarda_costas_caiu"));
    }

    // ---------- Nv 20 — Eu Sou o Iron Golem ----------

    private static final Map<UUID, Long> GOLEMS = new HashMap<>();
    public static final String GOLEM_FORM = "steely_golem";

    public static boolean transformed(Player p) {
        Long until = GOLEMS.get(p.getUUID());
        return until != null && until > p.level().getGameTime();
    }

    public static boolean canTransform(ServerPlayer player, ItemStack stack) {
        return item().ready(stack, player.level().getGameTime(), 3) && golem(player) == null && !transformed(player);
    }

    public static boolean transform(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        item().startCooldown(player, stack, 3, GUARD_COOLDOWN);
        GOLEMS.put(player.getUUID(), player.level().getGameTime() + GOLEM_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, GOLEM_TICKS, 24, false, false, true));
        player.setAbsorptionAmount(100);
        tmod(player, Attributes.ATTACK_DAMAGE, T_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        tmod(player, Attributes.KNOCKBACK_RESISTANCE, T_KB, 1.0, AttributeModifier.Operation.ADD_VALUE);
        tmod(player, Attributes.STEP_HEIGHT, T_STEP, 2.4, AttributeModifier.Operation.ADD_VALUE);
        tmod(player, Attributes.SCALE, T_SCALE, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.5f, 0.6f);
        player.setData(org.dantesys.nexus.data.ModAttachmentType.FORMA, GOLEM_FORM);
        RelicUtil.say(player, Component.translatable("relic.nexus.steely.eu_sou_o_iron_golem"));
        return true;
    }

    private static void tmod(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, Identifier id, double v, AttributeModifier.Operation op) {
        var a = e.getAttribute(attr);
        if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(id, v, op));
    }

    private static void endTransform(ServerPlayer player) {
        if (GOLEMS.remove(player.getUUID()) == null) return;
        player.setData(org.dantesys.nexus.data.ModAttachmentType.FORMA, "");
        for (var pair : new Object[][]{{Attributes.ATTACK_DAMAGE, T_DAMAGE}, {Attributes.KNOCKBACK_RESISTANCE, T_KB}, {Attributes.STEP_HEIGHT, T_STEP}, {Attributes.SCALE, T_SCALE}}) {
            @SuppressWarnings("unchecked") var a = player.getAttribute((net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>) pair[0]);
            if (a != null) a.removeModifier((Identifier) pair[1]);
        }
        player.removeEffect(MobEffects.ABSORPTION);
        RelicUtil.say(player, Component.translatable("relic.nexus.steely.a_transformacao_acabou"));
    }

    @SubscribeEvent
    public static void punch(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !transformed(player)) return;
        if (!player.getMainHandItem().isEmpty()) { event.setCanceled(true); return; }
        if (event.getTarget() instanceof LivingEntity target) {
            player.level().getServer().execute(() -> RelicUtil.push(target, target.getDeltaMovement().add(0, 1.1, 0)));
        }
    }

    @SubscribeEvent
    public static void noItems(PlayerInteractEvent.RightClickItem event) {
        if (transformed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void noFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && transformed(p) && event.getSource().is(DamageTypeTags.IS_FALL)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long until = GOLEMS.get(player.getUUID());
        if (until == null) return;
        long now = player.level().getGameTime();
        if (now >= until || (now - (until - GOLEM_TICKS) > 10 && player.getAbsorptionAmount() <= 0)) endTransform(player);
        else if (now % 10 == 0) ((ServerLevel) player.level()).sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.4, 0.3, 0.02);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) endTransform(player);
        BODY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) endTransform(player);
    }
}
