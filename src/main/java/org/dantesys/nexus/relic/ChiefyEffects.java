package org.dantesys.nexus.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Chiefy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class ChiefyEffects {
    private ChiefyEffects() {}

    public static final int PINATA_MANA = 40, PINATA_COOLDOWN = 600;
    public static final int SEASON_MANA = 50, SEASON_COOLDOWN = 2400, SEASON_MAX = 16;
    public static final int STORM_MANA = 80, STORM_COOLDOWN = 2400, STORM_TICKS = 300;
    public static final int FEAST_MANA = 300, FEAST_COOLDOWN = 18000, FEAST_TICKS = 600, FEAST_FOOD = 128;
    private static final String IMPROVED = "NexusMelhorada", SEASONED = "NexusTempero";
    private static final List<Item> FOODS = List.of(Items.BREAD, Items.APPLE, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP,
            Items.COOKIE, Items.BAKED_POTATO, Items.CARROT, Items.PUMPKIN_PIE, Items.MELON_SLICE, Items.SWEET_BERRIES, Items.COOKED_SALMON);

    private static ChiefyItem item() { return ModItems.CHIEFY.get(); }
    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }
    private static ItemStack randomFood(ServerLevel level) { return new ItemStack(FOODS.get(level.getRandom().nextInt(FOODS.size()))); }

    private static void lore(ItemStack stack, String line) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        stack.set(DataComponents.LORE, lore.withLineAdded(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GOLD)));
    }

    // ---------- Nv 1 — Lamina em Chamas ----------

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !wielding(player) || !item().isUnlocked(player, 0)) return;
        ServerLevel level = (ServerLevel) player.level();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.has(DataComponents.FOOD)) continue;
            ItemStack cooked = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                    .map(r -> r.value().assemble(new SingleRecipeInput(stack))).orElse(stack.copy());
            if (!cooked.has(DataComponents.FOOD)) cooked = stack.copy();
            cooked.setCount(stack.getCount());
            FoodProperties food = cooked.get(DataComponents.FOOD);
            cooked.set(DataComponents.FOOD, new FoodProperties(Math.round(food.nutrition() * 1.5f), food.saturation() * 1.5f, food.canAlwaysEat()));
            CustomData.update(DataComponents.CUSTOM_DATA, cooked, t -> t.putBoolean(IMPROVED, true));
            lore(cooked, "Melhorada pela Chiefy");
            drop.setItem(cooked);
        }
    }

    // ---------- Nv 5 — Pinhata ----------

    public static boolean pinata(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = RelicUtil.targetEntity(player, 5, e -> RelicTargets.canHit(player, e));
        if (target == null) { RelicUtil.say(player, "Nenhum alvo na mira."); return false; }
        ServerLevel level = (ServerLevel) player.level();
        var random = level.getRandom();
        if (target instanceof Player || RelicUtil.isBoss(target) || (target instanceof OwnableEntity own && own.getOwnerReference() != null)) {
            target.hurtServer(level, level.damageSources().playerAttack(player), 4 + random.nextInt(5));
            if (target instanceof Player victim) {
                var food = victim.getFoodData();
                food.setFoodLevel(Math.max(0, food.getFoodLevel() - 1 - random.nextInt(2)));
                food.setSaturation(Math.max(0, food.getSaturationLevel() - 1 - random.nextInt(2)));
            }
            return true;
        }
        int count = 3 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            ItemEntity drop = new ItemEntity(level, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), randomFood(level));
            drop.setDeltaMovement((random.nextDouble() - 0.5) * 0.5, 0.35 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.5);
            level.addFreshEntity(drop);
        }
        level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 1, target.getZ(), 40, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0f, 1.2f);
        target.discard();
        item().addMasteryXp(player, 1);
        return true;
    }

    // ---------- Nv 10 — Mestre Cuca ----------

    private static Holder<MobEffect> seasoning(ItemStack s) {
        if (s.is(ItemTags.FISHES)) return MobEffects.WATER_BREATHING;
        if (s.is(ItemTags.MEAT)) return MobEffects.STRENGTH;
        if (s.is(Items.CARROT) || s.is(Items.GOLDEN_CARROT)) return MobEffects.NIGHT_VISION;
        if (s.is(Items.BREAD)) return MobEffects.RESISTANCE;
        if (s.is(Items.PUMPKIN_PIE) || s.is(Items.CAKE)) return MobEffects.REGENERATION;
        if (s.is(Items.POTATO) || s.is(Items.BAKED_POTATO)) return MobEffects.SPEED;
        if (s.is(Items.APPLE) || s.is(Items.GOLDEN_APPLE) || s.is(Items.MELON_SLICE) || s.is(Items.SWEET_BERRIES) || s.is(Items.GLOW_BERRIES)
                || s.is(Items.CHORUS_FRUIT) || s.is(Items.COOKIE) || s.is(Items.HONEY_BOTTLE)) return MobEffects.JUMP_BOOST;
        return null;
    }

    private static boolean seasoned(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(SEASONED);
    }

    public static boolean season(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        var inv = player.getInventory();
        int left = SEASON_MAX;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack food = inv.getItem(i);
            Holder<MobEffect> effect = seasoning(food);
            if (food.isEmpty() || effect == null || seasoned(food)) continue;
            ItemStack part = food.split(Math.min(left, food.getCount()));
            left -= part.getCount();
            String id = effect.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            CustomData.update(DataComponents.CUSTOM_DATA, part, t -> t.putString(SEASONED, id));
            lore(part, "Temperada pela Chiefy");
            if (food.isEmpty()) inv.setItem(i, part);
            else RelicUtil.giveOrDrop(player, part);
        }
        if (left == SEASON_MAX) { RelicUtil.say(player, "Nenhuma comida para temperar."); return false; }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8f, 1.6f);
        RelicUtil.say(player, (SEASON_MAX - left) + " comidas temperadas.");
        return true;
    }

    @SubscribeEvent
    public static void eaten(LivingEntityUseItemEvent.Finish event) {
        ItemStack food = event.getItem();
        var tag = food.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String id = tag.getStringOr(SEASONED, "");
        if (id.isEmpty()) return;
        var key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null) return;
        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(key).ifPresent(effect ->
                event.getEntity().addEffect(new MobEffectInstance(effect, 600, tag.getBooleanOr(IMPROVED, false) ? 1 : 0)));
    }

    // ---------- Nv 15 — Tempestade Culinaria / Nv 20 — Banquete ----------

    private static final class Storm {
        final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long until; final boolean feast; final double power;
        final Map<UUID, Float> counters = new HashMap<>(); final Map<UUID, Long> lastHit = new HashMap<>();
        Storm(ServerPlayer o, ServerLevel l, Vec3 c, long u, boolean f, double p) { owner = o; level = l; center = c; until = u; feast = f; power = p; }
    }
    private static final List<Storm> STORMS = new ArrayList<>();

    public static boolean foodStorm(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        STORMS.add(new Storm(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + STORM_TICKS, false, context.powerMultiplier()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }

    private static int foodCount(Player p) { return RelicUtil.count(p, s -> s.has(DataComponents.FOOD) && !RelicUtil.isRelic(s)); }

    public static boolean canFeast(ServerPlayer player, ItemStack stack) { return foodCount(player) >= FEAST_FOOD; }

    public static boolean feast(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicUtil.consume(player, s -> s.has(DataComponents.FOOD) && !RelicUtil.isRelic(s), FEAST_FOOD);
        STORMS.add(new Storm(player, (ServerLevel) player.level(), player.position(), player.level().getGameTime() + FEAST_TICKS, true, context.powerMultiplier()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 2.0f, 0.5f);
        return true;
    }

    private static void feed(LivingEntity e) {
        if (e instanceof Player p) {
            p.getFoodData().eat(3, 0.6f);
        } else e.heal(1);
    }

    private static void overeat(Storm s, LivingEntity target, float damage) {
        ServerLevel level = s.level;
        var src = RelicUtil.source(level, RelicUtil.damageType("chiefy_comilanca"), s.owner);
        target.hurtServer(level, src, RelicUtil.isBoss(target) ? damage / 2 : damage);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), e -> e != target && RelicTargets.canHit(s.owner, e))) {
            near.hurtServer(level, src, 4);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 2, 0.3, 0.3, 0.3, 0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.CAKE), target.getX(), target.getY() + 1, target.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private static void tickStorm(Storm s, long now) {
        ServerLevel level = s.level;
        Vec3 center = s.feast ? s.owner.position() : s.center;
        double radius = 5;
        AABB area = new AABB(center, center).inflate(radius, 3, radius);
        if (!s.feast && now % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, randomFood(level).getItem()), e.getX(), e.getY() + e.getBbHeight() + 1, e.getZ(), 4, 0.1, 0.1, 0.1, 0.05);
                level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
                if (RelicTargets.isAlly(s.owner, e)) { feed(e); continue; }
                if (!RelicTargets.canHit(s.owner, e)) continue;
                if (!RelicUtil.isBoss(e)) feed(e);
                Long last = s.lastHit.get(e.getUUID());
                if (last != null && now - last > 200) s.counters.remove(e.getUUID());
                s.lastHit.put(e.getUUID(), now);
                float c = s.counters.merge(e.getUUID(), 1f, Float::sum);
                if (c >= 4) { s.counters.remove(e.getUUID()); overeat(s, e, 12); }
            }
        }
        if (s.feast) {
            if (now % 4 == 0) EternyEffects.ring(level, s.owner, radius, ParticleTypes.HAPPY_VILLAGER);
            Set<UUID> inside = new HashSet<>();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e.distanceTo(s.owner) <= radius)) {
                if (RelicTargets.isAlly(s.owner, e)) {
                    if (e instanceof Player p && now % 20 == 0) { p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(20); }
                    if (now % 40 == 0) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                    continue;
                }
                if (!RelicTargets.canHit(s.owner, e)) continue;
                inside.add(e.getUUID());
                float c = s.counters.merge(e.getUUID(), 1f, Float::sum);
                if (c >= 100) {
                    s.counters.remove(e.getUUID());
                    var src = RelicUtil.source(level, RelicUtil.damageType("chiefy_comilanca"), s.owner);
                    int piles = 2 + level.getRandom().nextInt(4);
                    for (int i = 0; i < piles; i++) level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), randomFood(level)));
                    if (RelicUtil.isBoss(e)) e.hurtServer(level, src, e.getMaxHealth() * 0.3f);
                    else if (e instanceof Player p) e.hurtServer(level, src, p.getFoodData().getFoodLevel() >= 20 ? 1000 : 20);
                    else e.hurtServer(level, src, 1000);
                    level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 1, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                }
            }
            s.counters.replaceAll((id, c) -> inside.contains(id) ? c : Math.max(0, c - 0.5f));
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || STORMS.isEmpty()) return;
        long now = level.getGameTime();
        STORMS.removeIf(s -> {
            if (s.level != level) return false;
            if (now >= s.until || s.owner.isRemoved()) return true;
            tickStorm(s, now);
            return false;
        });
    }
}
