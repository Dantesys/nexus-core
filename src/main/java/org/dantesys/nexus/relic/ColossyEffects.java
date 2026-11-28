package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Colossy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class ColossyEffects {
    private ColossyEffects() {}

    public static final int QUAKE_MANA = 30, QUAKE_COOLDOWN = 200;
    public static final int EAT_COOLDOWN = 1200;
    public static final int STOMP_MANA = 70, STOMP_COOLDOWN = 1200, SHRINK_TICKS = 200;
    public static final int HOOK_MANA = 200, HOOK_COOLDOWN = 18000, HOOK_LIMIT = 6000;
    public static final String HOOK_TAG = "ColossyGancho";
    private static final Identifier[] GIANT = {id("tamanho"), id("vida"), id("dano"), id("armadura"), id("pulo"), id("alcance"), id("alcance_bloco")};
    private static final Identifier S_SCALE = id("encolhido"), S_REACH = id("encolhido_alcance"), S_DAMAGE = id("encolhido_dano");

    private static Identifier id(String p) { return Identifier.fromNamespaceAndPath(Nexus.MODID, "colossy_" + p); }
    private static ColossyItem item() { return ModItems.COLOSSY.get(); }

    private static boolean wearing(Player p, int slot) { return p.getItemBySlot(EquipmentSlot.LEGS).is(item()) && item().isUnlocked(p, slot); }

    private static void set(LivingEntity e, Holder<Attribute> attr, Identifier id, double v, AttributeModifier.Operation op) {
        var a = e.getAttribute(attr);
        if (a == null) return;
        if (v == 0) a.removeModifier(id);
        else a.addOrUpdateTransientModifier(new AttributeModifier(id, v, op));
    }

    // ---------- Nv 1 — Gigantismo ----------

    private static void giant(Player p, boolean on) {
        double prop = on ? 0.10 + 0.20 * Math.clamp(item().masteryLevel(p) - 1, 0, 19) / 19.0 : 0;
        var mult = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        set(p, Attributes.SCALE, GIANT[0], prop, mult);
        set(p, Attributes.MAX_HEALTH, GIANT[1], prop, mult);
        set(p, Attributes.ATTACK_DAMAGE, GIANT[2], prop, mult);
        set(p, Attributes.ARMOR, GIANT[3], prop, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.JUMP_STRENGTH, GIANT[4], prop, mult);
        set(p, Attributes.ENTITY_INTERACTION_RANGE, GIANT[5], on ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.BLOCK_INTERACTION_RANGE, GIANT[6], on ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ---------- Nv 5 — Soco Terremoto ----------

    public static boolean quake(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = 4 + 2 * context.affinity() / 1000.0;
        float damage = (float) (2 * context.powerMultiplier());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2, radius),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius)) {
            e.hurtServer(level, level.damageSources().playerAttack(player), damage);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), player);
            RelicUtil.push(e, e.getDeltaMovement().add(0, 0.35, 0));
        }
        var ground = level.getBlockState(player.blockPosition().below());
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2 / 40;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX() + Math.cos(a) * radius * 0.8, player.getY() + 0.1,
                    player.getZ() + Math.sin(a) * radius * 0.8, 3, 0.2, 0.1, 0.2, 0.1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.7f);
        return true;
    }

    // ---------- Nv 10 — Fome de Gigante ----------

    private static final Map<UUID, LivingEntity> MEAL = new HashMap<>();

    public static boolean eat(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity meal = MEAL.remove(player.getUUID());
        if (meal == null || !meal.isAlive()) return false;
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.POOF, meal.getX(), meal.getY() + 0.5, meal.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
        meal.discard();
        player.heal(player.getMaxHealth() * (0.5f + 0.3f * context.affinity() / 1000f));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2f, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !wearing(player, 2)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || target instanceof Villager || !RelicUtil.isPassiveAnimal(target)) return;
        ItemStack stack = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!item().ready(stack, player.level().getGameTime(), 2)) return;
        MEAL.put(player.getUUID(), target);
        if (item().triggerPassive(player, stack, 2)) event.setCanceled(true);
        MEAL.remove(player.getUUID());
    }

    // ---------- Nv 15 — Pisao Esmagador ----------

    private record Shrunk(LivingEntity entity, long until) {}
    private static final List<Shrunk> SHRUNK = new ArrayList<>();

    public static void shrink(LivingEntity e, long until, double amount) {
        set(e, Attributes.SCALE, S_SCALE, -amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(e, Attributes.ENTITY_INTERACTION_RANGE, S_REACH, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(e, Attributes.ATTACK_DAMAGE, S_DAMAGE, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        SHRUNK.removeIf(s -> s.entity == e);
        SHRUNK.add(new Shrunk(e, until));
    }

    private static void unshrink(LivingEntity e) {
        set(e, Attributes.SCALE, S_SCALE, 0, null);
        set(e, Attributes.ENTITY_INTERACTION_RANGE, S_REACH, 0, null);
        set(e, Attributes.ATTACK_DAMAGE, S_DAMAGE, 0, null);
    }

    public static boolean stomp(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 flat = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 center = player.position().add(flat.scale(2.0));
        float damage = (float) (8 + 4 * context.affinity() / 1000.0);
        long until = level.getGameTime() + SHRINK_TICKS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(1.5, 1.5, 1.5).expandTowards(0, 1, 0),
                e -> RelicTargets.canHit(player, e))) {
            e.hurtServer(level, level.damageSources().playerAttack(player), RelicUtil.reduced(e, damage));
            shrink(e, until, RelicUtil.isBoss(e) ? 0.15 : 0.5);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z, 3, 1, 0.1, 1, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.2f, 0.5f);
        return true;
    }

    // ---------- Nv 20 — Gancho ----------

    private static final Map<UUID, Long> HOOKS = new HashMap<>();
    private record Launched(LivingEntity entity, long since) {}
    private static final List<Launched> LAUNCHED = new ArrayList<>();

    public static boolean canHook(ServerPlayer player, ItemStack stack) {
        return player.getHealth() <= player.getMaxHealth() * 0.6f && !HOOKS.containsKey(player.getUUID());
    }

    public static boolean armHook(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(HOOK_TAG, true));
        HOOKS.put(player.getUUID(), player.level().getGameTime() + HOOK_LIMIT);
        RelicUtil.say(player, Component.translatable("relic.nexus.colossy.gancho_pronto_acerte_alguem"));
        return true;
    }

    private static void finishHook(ServerPlayer player) {
        HOOKS.remove(player.getUUID());
        ItemStack stack = player.getItemBySlot(EquipmentSlot.LEGS).is(item()) ? player.getItemBySlot(EquipmentSlot.LEGS) : item().findIn(player);
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(HOOK_TAG));
        item().startCooldown(player, stack, 4, HOOK_COOLDOWN);
    }

    @SubscribeEvent
    public static void hookHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !event.getSource().isDirect() || !HOOKS.containsKey(player.getUUID())) return;
        LivingEntity target = event.getEntity();
        if (!RelicTargets.canHit(player, target)) return;
        float ratio = player.getHealth() / player.getMaxHealth();
        float damage = 12 + 8 * Math.clamp((0.6f - ratio) / 0.5f, 0f, 1f);
        event.setAmount(event.getAmount() + damage);
        if (!RelicUtil.isBoss(target)) {
            player.level().getServer().execute(() -> RelicUtil.push(target, new Vec3(0, 1.75, 0)));
            LAUNCHED.add(new Launched(target, player.level().getGameTime()));
        }
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 2, 0.2, 0.2, 0.2, 0);
        player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.5f);
        finishHook(player);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (now % 20 == 0) giant(player, wearing(player, 0));
        Long hook = HOOKS.get(player.getUUID());
        if (hook != null && now >= hook) finishHook(player);
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        SHRUNK.removeIf(s -> {
            if (s.entity.level() != level) return false;
            if (now < s.until && s.entity.isAlive()) return false;
            unshrink(s.entity);
            RelicUtil.pushToFreeSpace(s.entity);
            return true;
        });
        LAUNCHED.removeIf(l -> {
            if (l.entity.level() != level) return false;
            if (!l.entity.isAlive() || now - l.since > 400) return true;
            if (now - l.since > 10 && l.entity.onGround()) {
                l.entity.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 40, 0));
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        giant(event.getEntity(), false);
        unshrink(event.getEntity());
        HOOKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        SHRUNK.forEach(s -> unshrink(s.entity));
        SHRUNK.clear();
    }
}
