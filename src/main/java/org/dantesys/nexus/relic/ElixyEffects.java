package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Elixy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class ElixyEffects {
    private ElixyEffects() {}

    public static final int MAD_MANA = 40;
    public static final int CREATE_MANA = 60, CREATE_COOLDOWN = 6000;
    public static final int GOLD_MANA = 70, GOLD_COOLDOWN = 1800;
    public static final int TRANSMUTE_MANA = 250, TRANSMUTE_COOLDOWN = 18000, TRANSMUTE_TICKS = 600, TRANSMUTE_RADIUS = 10;
    public static final String COUNT_TAG = "ElixyEfeitos";
    private static final String GOLD_TAG = "NexusOuroDosTolos";

    private static ElixyItem item() { return ModItems.ELIXY.get(); }

    private static final List<Holder<MobEffect>> NEGATIVE = List.of(MobEffects.SLOWNESS, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER,
            MobEffects.BLINDNESS, MobEffects.NAUSEA, MobEffects.HUNGER, MobEffects.MINING_FATIGUE, MobEffects.LEVITATION, MobEffects.INSTANT_DAMAGE);
    private static final List<Holder<MobEffect>> POSITIVE = List.of(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.REGENERATION, MobEffects.RESISTANCE,
            MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.NIGHT_VISION, MobEffects.JUMP_BOOST, MobEffects.ABSORPTION,
            MobEffects.HASTE, MobEffects.LUCK, MobEffects.SLOW_FALLING);

    // ---------- Nv 1 — Negacao ----------

    private static Optional<Holder<MobEffect>> opposite(Holder<MobEffect> e) {
        if (e.is(MobEffects.SLOWNESS)) return Optional.of(MobEffects.SPEED);
        if (e.is(MobEffects.WEAKNESS)) return Optional.of(MobEffects.STRENGTH);
        if (e.is(MobEffects.POISON) || e.is(MobEffects.WITHER) || e.is(ModEffects.VENENO_ASSASSINO)) return Optional.of(MobEffects.REGENERATION);
        if (e.is(MobEffects.BLINDNESS) || e.is(MobEffects.DARKNESS)) return Optional.of(MobEffects.NIGHT_VISION);
        if (e.is(MobEffects.HUNGER)) return Optional.of(MobEffects.SATURATION);
        if (e.is(MobEffects.LEVITATION)) return Optional.of(MobEffects.SLOW_FALLING);
        if (e.is(MobEffects.MINING_FATIGUE)) return Optional.of(MobEffects.HASTE);
        if (e.is(MobEffects.UNLUCK)) return Optional.of(MobEffects.LUCK);
        return null == e ? Optional.empty() : (e.is(MobEffects.NAUSEA) ? Optional.empty() : null);
    }

    public static boolean negates(LivingEntity e) {
        return e instanceof Player p && !item().findIn(p).isEmpty() && item().isUnlocked(p, 0);
    }

    private static boolean converting = false;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void applicable(MobEffectEvent.Applicable event) {
        if (converting || event.getEntity().level().isClientSide() || !negates(event.getEntity())) return;
        MobEffectInstance instance = event.getEffectInstance();
        Optional<Holder<MobEffect>> opposite = opposite(instance.getEffect());
        if (opposite == null) return;
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        opposite.ifPresent(o -> {
            converting = true;
            try { event.getEntity().addEffect(new MobEffectInstance(o, instance.getDuration(), instance.getAmplifier())); }
            finally { converting = false; }
        });
    }

    /** Dano Instantaneo (pocao) vira Cura Instantanea. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void instant(LivingIncomingDamageEvent event) {
        var src = event.getSource();
        if (!(src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) || !negates(event.getEntity())) return;
        event.getEntity().heal(event.getAmount());
        event.setCanceled(true);
    }

    // ---------- Nv 5 — Pocao do Louco ----------

    private static ItemStack splash(List<MobEffectInstance> effects, int color, String name) {
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color), effects, Optional.of(name)));
        return potion;
    }

    private static ThrownSplashPotion throwPotion(ServerPlayer player, ItemStack potion) {
        ThrownSplashPotion thrown = new ThrownSplashPotion(player.level(), player, potion);
        thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.6F, 1.0F);
        player.level().addFreshEntity(thrown);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
        return thrown;
    }

    public static boolean madPotion(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        var random = player.level().getRandom();
        int count = 1 + random.nextInt(5);
        int duration = 100 + 300 * context.resonance() / 1000;
        int amp = Math.min(2, context.affinity() * 3 / 1001);
        List<Holder<MobEffect>> pool = new ArrayList<>(NEGATIVE);
        Collections.shuffle(pool, new Random(random.nextLong()));
        List<MobEffectInstance> effects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Holder<MobEffect> e = pool.get(i);
            effects.add(e.is(MobEffects.INSTANT_DAMAGE) ? new MobEffectInstance(e, 1, 0) : new MobEffectInstance(e, duration, amp));
        }
        throwPotion(player, splash(effects, 0x6b2d8a, "Poção do Louco"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(COUNT_TAG, count));
        RelicUtil.say(player, "Poção do Louco: " + count + (count == 1 ? " efeito." : " efeitos."));
        return true;
    }

    // ---------- Nv 10 — Criador de Pocao ----------

    public static boolean createPotion(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        var random = player.level().getRandom();
        Holder<MobEffect> effect = POSITIVE.get(random.nextInt(POSITIVE.size()));
        int amp = 2 + random.nextInt(1 + context.affinity() * 3 / 1001);
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(effect.value().getColor()),
                List.of(new MobEffectInstance(effect, 1200, amp)), Optional.of("Poção da Elixy")));
        RelicUtil.giveOrDrop(player, potion);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    // ---------- Nv 15 — Ouro dos Tolos ----------

    public static boolean foolsGold(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ThrownSplashPotion thrown = throwPotion(player, splash(List.of(), 0xffcc33, "Ouro dos Tolos"));
        thrown.getPersistentData().putBoolean(GOLD_TAG, true);
        return true;
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownSplashPotion potion) || !potion.getPersistentData().getBooleanOr(GOLD_TAG, false)) return;
        if (!(potion.level() instanceof ServerLevel level) || !(potion.getOwner() instanceof ServerPlayer owner)) return;
        Vec3 pos = event.getRayTraceResult().getLocation();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(4, 2, 4),
                e -> RelicTargets.canHit(owner, e) || (e instanceof Mob && !(e instanceof OwnableEntity own && own.getOwnerReference() != null)))) {
            if (target instanceof Player || RelicUtil.isBoss(target)) {
                target.hurtServer(level, level.damageSources().indirectMagic(potion, owner), 6.0F);
                continue;
            }
            if (!(target instanceof Mob) || (target instanceof OwnableEntity own && own.getOwnerReference() != null)) continue;
            int nuggets = 3 + level.getRandom().nextInt(7), ingots = level.getRandom().nextInt(3);
            level.addFreshEntity(new ItemEntity(level, target.getX(), target.getY() + 0.3, target.getZ(), new ItemStack(Items.GOLD_NUGGET, nuggets)));
            if (ingots > 0) level.addFreshEntity(new ItemEntity(level, target.getX(), target.getY() + 0.3, target.getZ(), new ItemStack(Items.GOLD_INGOT, ingots)));
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 30, 0.3, 0.4, 0.3, 0.1);
            target.discard();
            ModItems.ELIXY.get().addMasteryXp(owner, 1);
        }
        level.playSound(null, BlockPos.containing(pos), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    // ---------- Nv 20 — Transmutacao ----------

    private static Set<Holder<MobEffect>> potionEffects(ItemStack s) {
        PotionContents contents = s.get(DataComponents.POTION_CONTENTS);
        Set<Holder<MobEffect>> set = new HashSet<>();
        if (contents != null) contents.getAllEffects().forEach(e -> set.add(e.getEffect()));
        return set;
    }

    private static List<Integer> potionSlots(Player player) {
        var inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        Set<Holder<MobEffect>> seen = new HashSet<>();
        for (int i = 0; i < inv.getContainerSize() && slots.size() < 2; i++) {
            Set<Holder<MobEffect>> effects = potionEffects(inv.getItem(i));
            if (effects.isEmpty()) continue;
            if (slots.isEmpty() || !seen.containsAll(effects)) { slots.add(i); seen.addAll(effects); }
        }
        return slots;
    }

    public static boolean canTransmute(ServerPlayer player, ItemStack stack) { return potionSlots(player).size() >= 2; }

    private record Field(ServerPlayer owner, ServerLevel level, Vec3 center, long until) {}
    private static final List<Field> FIELDS = new ArrayList<>();

    public static boolean transmute(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        for (int slot : potionSlots(player)) player.getInventory().getItem(slot).shrink(1);
        FIELDS.add(new Field(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + TRANSMUTE_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }

    private static final List<EntityType<?>> PEACEFUL = List.of(EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.RABBIT);

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FIELDS.isEmpty()) return;
        long now = level.getGameTime();
        FIELDS.removeIf(f -> {
            if (f.level != level) return false;
            if (now >= f.until || f.owner.isRemoved()) return true;
            if (now % 5 == 0) {
                for (int i = 0; i < 20; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(level.getRandom().nextDouble()) * TRANSMUTE_RADIUS;
                    level.sendParticles(ParticleTypes.WITCH, f.center.x + Math.cos(a) * r, f.center.y + 0.3, f.center.z + Math.sin(a) * r, 1, 0, 0.2, 0, 0);
                }
            }
            if (now % 20 != 0) return false;
            var random = level.getRandom();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(f.center, f.center).inflate(TRANSMUTE_RADIUS),
                    e -> e.position().distanceTo(f.center) <= TRANSMUTE_RADIUS)) {
                boolean self = e == f.owner;
                if (!self && RelicTargets.isAlly(f.owner, e)) continue;
                if (e instanceof Enemy && e instanceof Mob && !RelicUtil.isBoss(e)) {
                    Entity animal = PEACEFUL.get(random.nextInt(PEACEFUL.size())).create(level, EntitySpawnReason.CONVERSION);
                    if (animal != null) {
                        animal.snapTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), 0);
                        level.addFreshEntity(animal);
                    }
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 15, 0.4, 0.5, 0.4, 0);
                    e.discard();
                    continue;
                }
                if (!(self || e instanceof Player || RelicUtil.isBoss(e))) continue;
                if (!self) {
                    for (MobEffectInstance active : List.copyOf(e.getActiveEffects())) {
                        if (active.getEffect().value().isBeneficial()) e.removeEffect(active.getEffect());
                    }
                }
                Holder<MobEffect> bad = NEGATIVE.get(random.nextInt(NEGATIVE.size() - 1));
                MobEffectInstance current = e.getEffect(bad);
                int amp = current == null ? 0 : Math.min(4, current.getAmplifier() + 1);
                e.addEffect(new MobEffectInstance(bad, 100, amp), f.owner);
            }
            return false;
        });
    }
}
