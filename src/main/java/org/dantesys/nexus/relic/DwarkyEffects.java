package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Dwarky (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class DwarkyEffects {
    private DwarkyEffects() {}

    public static final int CHAIN_MANA = 50, CHAIN_COOLDOWN = 600, CHAIN_TICKS = 200;
    public static final int LEAP_MANA = 60, LEAP_COOLDOWN = 400;
    public static final int HOMERUN_MANA = 50, HOMERUN_COOLDOWN = 600, HOMERUN_ARM_TICKS = 300, HOMERUN_FULL = 40;
    public static final int MOUNTAIN_MANA = 200, MOUNTAIN_COOLDOWN = 6000, MOUNTAIN_STONES = 64;
    public static final TagKey<Item> STONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Nexus.MODID, "pedras_quebra_montanha"));

    private static final Identifier DWARF_SCALE = id("dwarky_escala"), DWARF_FALL = id("dwarky_queda"), CHAIN_REACH = id("dwarky_corrente"),
            SHRINK = id("quebra_montanha");
    private static final String HOMERUN_TAG = "DwarkyHomeRunAte";

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Nexus.MODID, path); }
    private static DwarkyItem item() { return ModItems.DWARKY.get(); }

    private static final class State { long chainUntil, leapStart; boolean leaping; float homeRunCharge; }
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }

    public static boolean chainActive(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.chainUntil > player.level().getGameTime();
    }

    private static void refresh(ServerPlayer player) {
        ItemStack stack = RelicUtil.find(player, item());
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) { if (current != null) instance.removeModifier(id); return; }
        if (current == null || current.amount() != amount) instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    // ---------- Nv 1 — Sangue de Anao ----------

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean holding = player.getMainHandItem().getItem() instanceof DwarkyItem && item().isUnlocked(player, 0);
        double fall = holding ? -(0.4 + 0.6 * (item().masteryLevel(player) - 1) / 19.0) : 0;
        setModifier(player, Attributes.SCALE, DWARF_SCALE, holding ? -0.25 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, DWARF_FALL, fall, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        long now = player.level().getGameTime();
        boolean chain = s.chainUntil > now;
        setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, CHAIN_REACH, chain && holding ? 1.0 : 0, AttributeModifier.Operation.ADD_VALUE);
        if (s.chainUntil > 0 && !chain) {
            s.chainUntil = 0;
            refresh(player);
        }
        if (s.leaping && player.onGround() && now - s.leapStart > 5) land(player, s);
        else if (s.leaping && now - s.leapStart > 80) s.leaping = false;
    }

    // ---------- Nv 5 — Corrente ----------

    public static boolean chain(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).chainUntil = player.level().getGameTime() + CHAIN_TICKS;
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        refresh(player);
        return true;
    }

    /** Golpe do mangual: 50% nos outros alvos em 3x3x3. Nao vale para o golpe de queda. */
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        if (!(player.getMainHandItem().getItem() instanceof DwarkyItem) || !chainActive(player) || MaceItem.canSmashAttack(player)) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) return;
        LivingEntity main = event.getEntity();
        ServerLevel level = (ServerLevel) player.level();
        float splash = event.getHealthDamage() * 0.5f;
        if (splash <= 0) return;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, main.getBoundingBox().inflate(1.5),
                e -> e != main && RelicTargets.canHit(player, e))) {
            other.hurtServer(level, level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK, null, player), splash);
        }
        level.sendParticles(ParticleTypes.CRIT, main.getX(), main.getY() + 1, main.getZ(), 10, 1, 0.5, 1, 0.1);
    }

    // ---------- Nv 10 — Pulo Explosivo ----------

    public static boolean leap(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        boolean targets = !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= 5).isEmpty();
        if (!targets) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 2));
            RelicUtil.say(player, Component.translatable("relic.nexus.dwarky.ninguem_por_perto_velocidade_e"));
            return true;
        }
        State s = state(player);
        if (s.chainUntil > 0) { s.chainUntil = 0; refresh(player); }
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.6, 1.3, look.z * 0.6);
        RelicUtil.syncMotion(player);
        s.leaping = true;
        s.leapStart = player.level().getGameTime();
        player.level().playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    private static void land(ServerPlayer player, State s) {
        s.leaping = false;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 center = player.position().add(flat.scale(2.0));
        float damage = (float) (10 * (1 + RelicUtil.magic(player).getAfinidade() / 1000.0));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(3, 2, 3),
                e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= 5)) {
            target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            target.setDeltaMovement(away.x * 1.0, 0.8, away.z * 1.0);
            RelicUtil.syncMotion(target);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), player);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 3, 1, 0.2, 1, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), center.x, center.y + 0.2, center.z, 40, 2, 0.1, 2, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State s = STATES.get(player.getUUID());
            if (s != null && s.leaping) event.setCanceled(true);
        }
    }

    // ---------- Nv 15 — Home Run ----------

    public static boolean homeRunArmed(ItemStack stack, LivingEntity user) {
        long until = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(HOMERUN_TAG, 0L);
        return until > user.level().getGameTime();
    }

    public static boolean homeRun(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        long until = player.level().getGameTime() + HOMERUN_ARM_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(HOMERUN_TAG, until));
        state(player).homeRunCharge = 0;
        RelicUtil.say(player, Component.translatable("relic.nexus.dwarky.home_run_segure_o_clique"));
        return true;
    }

    public static void chargeHomeRun(ServerPlayer player, ItemStack stack, int held) {
        float charge = Math.min(1f, held / (float) HOMERUN_FULL);
        state(player).homeRunCharge = charge;
        RelicUtil.say(player, Component.translatable("relic.nexus.dwarky.home_run_carregado", Math.round(charge * 100)));
    }

    private record Flight(LivingEntity target, ServerPlayer owner, long start, float charge) {}
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private record Stun(Mob mob, long until) {}
    private static final List<Stun> STUNS = new ArrayList<>();

    @SubscribeEvent
    public static void homeRunHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DwarkyItem) || !homeRunArmed(stack, player)) return;
        State s = state(player);
        if (s.homeRunCharge <= 0) return;
        float charge = s.homeRunCharge;
        s.homeRunCharge = 0;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(HOMERUN_TAG));
        LivingEntity target = event.getEntity();
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, target.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 1.4f);
        if (RelicUtil.isBoss(target)) {
            stun(target, player);
            return;
        }
        Vec3 dir = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        double speed = 0.8 + 2.2 * charge;
        target.setDeltaMovement(dir.x * speed, 0.5 + 0.3 * charge, dir.z * speed);
        RelicUtil.syncMotion(target);
        FLIGHTS.add(new Flight(target, player, level.getGameTime(), charge));
    }

    private static void stun(LivingEntity target, ServerPlayer owner) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 9), owner);
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            STUNS.add(new Stun(mob, mob.level().getGameTime() + 20));
        }
        ((ServerLevel) target.level()).sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 8, 0.3, 0.1, 0.3, 0);
    }

    // ---------- Nv 20 — Quebra-Montanha ----------

    public static boolean canMountain(ServerPlayer player, ItemStack stack) {
        return RelicUtil.count(player, s -> s.is(STONES)) >= MOUNTAIN_STONES;
    }

    private record Smash(ServerPlayer owner, Display.ItemDisplay display, Vec3 center, Vec3 flat, long at, double power) {}
    private static final List<Smash> SMASHES = new ArrayList<>();
    private record Shrunk(LivingEntity entity, long until) {}
    private static final List<Shrunk> SHRUNK = new ArrayList<>();

    public static boolean mountain(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicUtil.consume(player, s -> s.is(STONES), MOUNTAIN_STONES);
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 center = player.position().add(flat.scale(3.5));
        Display.ItemDisplay display = Displays.item(level, stack, player.position().add(flat.scale(1.5)).add(0, 4, 0), 4.0f);
        SMASHES.add(new Smash(player, display, center, flat, level.getGameTime() + 12, context.powerMultiplier()));
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    private static void smash(Smash smash) {
        ServerPlayer player = smash.owner();
        ServerLevel level = (ServerLevel) smash.display().level();
        smash.display().discard();
        Vec3 side = new Vec3(-smash.flat().z, 0, smash.flat().x);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(smash.center(), smash.center()).inflate(3, 2.5, 3),
                e -> RelicTargets.canHit(player, e))) {
            double offset = Math.abs(target.position().subtract(smash.center()).dot(side));
            boolean full = offset < 1.5;
            target.hurtServer(level, level.damageSources().playerAttack(player), (float) ((full ? 24 : 14) * smash.power()));
            double shrink = RelicUtil.isBoss(target) ? -0.25 : -0.5;
            setModifier(target, Attributes.SCALE, SHRINK, shrink, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            SHRUNK.removeIf(s -> s.entity() == target);
            SHRUNK.add(new Shrunk(target, level.getGameTime() + (full ? 400 : 300)));
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, smash.center().x, smash.center().y, smash.center().z, 1, 0, 0, 0, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), smash.center().x, smash.center().y + 0.3,
                smash.center().z, 80, 2.5, 0.2, 2.5, 0.2);
        level.playSound(null, BlockPosOf(smash.center()), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 2.0f, 0.5f);
        if (player.isAlive()) item().addMasteryXp(player, 2);
    }

    private static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        FLIGHTS.removeIf(f -> {
            if (f.target().level() != level) return false;
            if (!f.target().isAlive() || now - f.start() > 100) return true;
            if (f.target().horizontalCollision && now - f.start() > 1) {
                float extra = (float) (f.target().getDeltaMovement().length() * 10 + 6 * f.charge());
                f.target().hurtServer(level, level.damageSources().flyIntoWall(), Math.max(4, extra));
                stun(f.target(), f.owner());
                return true;
            }
            if (f.target().onGround() && now - f.start() > 5) {
                stun(f.target(), f.owner());
                return true;
            }
            return false;
        });
        STUNS.removeIf(s -> {
            if (s.mob().level() != level || now < s.until()) return false;
            s.mob().setNoAi(false);
            return true;
        });
        SMASHES.removeIf(s -> {
            if (s.display().level() != level) return false;
            if (now >= s.at()) { smash(s); return true; }
            Vec3 pos = s.display().position();
            s.display().teleportTo(pos.x + s.flat().x * 0.15, pos.y - 0.3, pos.z + s.flat().z * 0.15);
            return false;
        });
        SHRUNK.removeIf(s -> {
            if (s.entity().level() != level) return false;
            if (s.entity().isAlive() && now < s.until()) return false;
            setModifier(s.entity(), Attributes.SCALE, SHRINK, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return true;
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { STATES.remove(event.getEntity().getUUID()); }

    /** Desfaz o que e temporario antes de salvar o mundo. */
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        STUNS.forEach(s -> s.mob().setNoAi(false));
        SHRUNK.forEach(s -> setModifier(s.entity(), Attributes.SCALE, SHRINK, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        SMASHES.forEach(s -> s.display().discard());
        STUNS.clear(); SHRUNK.clear(); SMASHES.clear(); FLIGHTS.clear(); STATES.clear();
    }
}
