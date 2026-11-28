package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Fisgy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class FisgyEffects {
    private FisgyEffects() {}

    public static final int LIFE_MANA = 50, LIFE_COOLDOWN = 400;
    public static final int RAIN_MANA = 70, RAIN_COOLDOWN = 900;
    public static final int SHARK_MANA = 60, SHARK_COOLDOWN = 600;
    public static final int TORNADO_MANA = 200, TORNADO_COOLDOWN = 6000, TORNADO_FISH = 64;
    public static final int ARM_TICKS = 600, BITE_WINDOW = 40;
    private static final String FISH_TAG = "NexusPeixeChuva";

    private static FisgyItem item() { return ModItems.FISGY.get(); }

    // ---------- Nv 1 — Pesca em Qualquer Lugar ----------

    private static final class Catch { String place; long biteAt; long biteUntil; }
    private static final Map<UUID, Catch> CATCHES = new HashMap<>();

    private static String place(FishingHook hook) {
        Level level = hook.level();
        if (hook.isInWater()) return null;
        if (hook.isInLava()) return "lava";
        if (!hook.onGround()) return null;
        if (level.dimension() == Level.NETHER) return "nether";
        if (level.dimension() == Level.END) return "end";
        BlockState below = level.getBlockState(hook.blockPosition().below());
        if (below.is(BlockTags.SAND)) return "areia";
        if (below.is(BlockTags.SNOW) || below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) return "neve";
        return "terra";
    }

    private static ItemStack roll(String place, ServerLevel level) {
        String table = ConfigLists.pairs(Config.FISGY_TABLES.get()).get(place);
        if (table == null) return ItemStack.EMPTY;
        List<Item> items = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (String entry : table.split(",")) {
            int sep = entry.lastIndexOf(':');
            if (sep <= 0) continue;
            Identifier id = Identifier.tryParse(entry.substring(0, sep).trim());
            int weight;
            try { weight = Integer.parseInt(entry.substring(sep + 1).trim()); } catch (NumberFormatException e) { continue; }
            if (id == null || weight <= 0) continue;
            items.add(BuiltInRegistries.ITEM.getValue(id));
            weights.add(weight);
            total += weight;
        }
        if (total == 0) return ItemStack.EMPTY;
        int r = level.getRandom().nextInt(total);
        for (int i = 0; i < items.size(); i++) {
            r -= weights.get(i);
            if (r < 0) return new ItemStack(items.get(i));
        }
        return ItemStack.EMPTY;
    }

    /**
     * Recolher a linha. Com "mordida" fora da agua, entrega a pesca. Sem mordida, se a boia estiver presa num bloco
     * (chao, parede ou teto), funciona como arpao e puxa o jogador ate ela. Retorna true se tratou.
     */
    public static boolean reelCustom(ServerPlayer player, FishingHook hook) {
        if (hook.getHookedIn() != null || !item().isUnlocked(player, 0)) return false;
        Catch c = CATCHES.remove(hook.getUUID());
        Vec3 anchor = ANCHORS.remove(hook.getUUID());
        long now = player.level().getGameTime();
        ServerLevel level = (ServerLevel) player.level();
        if (c != null && now >= c.biteAt && now <= c.biteUntil) {
            ItemStack loot = roll(c.place, level);
            if (!loot.isEmpty() && c.place.equals("lava")) {
                // Na lava o item iria cair de volta e queimar: vai direto para o inventario.
                level.sendParticles(ParticleTypes.LAVA, hook.getX(), hook.getY() + 0.3, hook.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                Vec3 to = player.getEyePosition().subtract(hook.position());
                for (double d = 0; d < to.length(); d += 0.5) {
                    Vec3 p = hook.position().add(to.normalize().scale(d));
                    level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                RelicUtil.giveOrDrop(player, loot);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.0f);
                item().addMasteryXp(player, 1);
            } else if (!loot.isEmpty()) {
                ItemEntity entity = new ItemEntity(level, hook.getX(), hook.getY() + 0.3, hook.getZ(), loot);
                // Pesca na lava nao queima no caminho ate o jogador.
                entity.getPersistentData().putBoolean(LOOT_TAG, true);
                Vec3 pull = player.position().subtract(hook.position());
                entity.setDeltaMovement(pull.x * 0.1, pull.y * 0.1 + Math.sqrt(pull.length()) * 0.08, pull.z * 0.1);
                level.addFreshEntity(entity);
                item().addMasteryXp(player, 1);
            }
            hook.discard();
            return true;
        }
        if (anchor != null || (hook.onGround() && !hook.isInWater())) {
            harpoon(player, anchor != null ? anchor : hook.position());
            hook.discard();
            return true;
        }
        if (c != null) {
            hook.discard();
            return true;
        }
        return false;
    }

    // ---------- Nv 1 — Arpao ----------

    private static final String LOOT_TAG = "NexusPescaFisgy";
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();
    private static final Map<UUID, Long> NO_FALL = new HashMap<>();

    private static void harpoon(ServerPlayer player, Vec3 target) {
        Vec3 to = target.subtract(player.position());
        double dist = to.length();
        if (dist < 1.5) return;
        double speed = Math.min(2.6, 0.6 + dist * 0.18);
        Vec3 velocity = to.normalize().scale(speed).add(0, 0.25 + Math.min(0.4, Math.max(0, to.y) * 0.05), 0);
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        RelicUtil.syncMotion(player);
        NO_FALL.put(player.getUUID(), player.level().getGameTime() + 60);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 0.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** Boia que bate numa parede ou no teto fica presa ali (arpao). */
    private static void anchorHook(FishingHook hook) {
        Vec3 anchor = ANCHORS.get(hook.getUUID());
        if (anchor == null) {
            boolean ceiling = hook.verticalCollision && !hook.onGround();
            if (hook.isInWater() || hook.isInLava() || !(hook.horizontalCollision || ceiling)) return;
            anchor = hook.position();
            ANCHORS.put(hook.getUUID(), anchor);
        }
        hook.setPos(anchor);
        hook.setDeltaMovement(Vec3.ZERO);
        hook.needsSync = true;
    }

    /** Itens pescados pela Fisgy nao queimam (pesca na lava). */
    @SubscribeEvent
    public static void lootFireproof(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ItemEntity && event.getEntity().getPersistentData().getBooleanOr(LOOT_TAG, false)
                && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            event.setInvulnerable(true);
        }
    }

    /** Sem dano de queda logo depois do arpao. */
    @SubscribeEvent
    public static void noFall(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        Long until = NO_FALL.get(event.getEntity().getUUID());
        if (until == null) return;
        if (until < event.getEntity().level().getGameTime()) { NO_FALL.remove(event.getEntity().getUUID()); return; }
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            NO_FALL.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getMainHandItem().getItem() instanceof FisgyItem) {
            item().addMasteryXp(player, 1);
        }
    }

    // ---------- Nv 5 e 15 — efeitos ao fisgar ----------

    private static final class Armed { long life, shark; final Set<UUID> handled = new HashSet<>(); }
    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    private static Armed armed(ServerPlayer player) { return ARMED.computeIfAbsent(player.getUUID(), id -> new Armed()); }

    private static LivingEntity hooked(ServerPlayer player) {
        return player.fishing != null && player.fishing.getHookedIn() instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public static boolean lifeFish(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = hooked(player);
        if (target != null) return applyLife(player, target);
        armed(player).life = player.level().getGameTime() + ARM_TICKS;
        RelicUtil.say(player, Component.translatable("relic.nexus.fisgy.peixe_da_vida_fisgue_um"));
        return true;
    }

    public static boolean sharkBait(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = hooked(player);
        if (target != null) return applyShark(player, target);
        armed(player).shark = player.level().getGameTime() + ARM_TICKS;
        RelicUtil.say(player, Component.translatable("relic.nexus.fisgy.isca_de_tubarao_fisgue_um"));
        return true;
    }

    private static boolean applyLife(ServerPlayer player, LivingEntity target) {
        if (target.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.AQUATIC)) {
            RelicUtil.say(player, Component.translatable("relic.nexus.fisgy.peixe_da_vida_nao_pega"));
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        List<? extends String> ids = Config.FISGY_AQUATIC.get();
        if (ids.isEmpty()) return false;
        Identifier id = Identifier.tryParse(ids.get(level.getRandom().nextInt(ids.size())));
        if (id == null) return false;
        Entity fish = BuiltInRegistries.ENTITY_TYPE.getValue(id).create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(fish instanceof LivingEntity aquatic)) return false;
        float drain = RelicUtil.reduced(target, aquatic.getMaxHealth());
        boolean dies = target.getHealth() <= drain;
        target.hurtServer(level, level.damageSources().indirectMagic(player, player), dies ? Float.MAX_VALUE : drain);
        Vec3 side = target.position().add(level.getRandom().nextDouble() * 2 - 1, 0.3, level.getRandom().nextDouble() * 2 - 1);
        aquatic.snapTo(side.x, side.y, side.z, level.getRandom().nextFloat() * 360, 0);
        level.addFreshEntity(aquatic);
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1, target.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, target.blockPosition(), SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (dies) RelicUtil.say(player, Component.translatable("relic.nexus.fisgy.virou_peixe"));
        item().addMasteryXp(player, 2);
        return true;
    }

    private static boolean applyShark(ServerPlayer player, LivingEntity target) {
        if (!RelicTargets.canHit(player, target)) return false;
        ServerLevel level = (ServerLevel) player.level();
        org.dantesys.nexus.entity.BocaTubarao.spawn(level, player, target.getX(), target.getY(), target.getZ(), player.getYRot(), 9.0F);
        item().addMasteryXp(player, 2);
        return true;
    }

    // ---------- Nv 10 — O Mar Nao Esta pra Peixe ----------

    private static final class Rain { final ServerPlayer owner; final long until; final double radius; Rain(ServerPlayer o, long u, double r) { owner = o; until = u; radius = r; } }
    private static final List<Rain> RAINS = new ArrayList<>();
    /** Mob caindo da chuva. Peixes somem pouco depois de cair (sem drops); golfinhos ficam. */
    private static final class Falling {
        final Entity entity; final ServerPlayer owner; final boolean fish; long landedAt = -1;
        Falling(Entity entity, ServerPlayer owner, boolean fish) { this.entity = entity; this.owner = owner; this.fish = fish; }
        Entity entity() { return entity; }
        ServerPlayer owner() { return owner; }
    }
    private static final String RAIN_FISH_TAG = "NexusPeixeDaChuva";
    private static final List<Falling> DOLPHINS = new ArrayList<>();

    public static boolean fishRain(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RAINS.add(new Rain(player, player.level().getGameTime() + 120 + context.resonance() / 5, 4 + context.affinity() / 200.0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static void tickRain(Rain rain, ServerLevel level) {
        var random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2, distance = Math.sqrt(random.nextDouble()) * rain.radius;
        double x = rain.owner.getX() + Math.cos(angle) * distance, z = rain.owner.getZ() + Math.sin(angle) * distance;
        double y = rain.owner.getY() + 8;
        if (random.nextInt(100) < 2) {
            Entity dolphin = EntityTypes.DOLPHIN.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (dolphin != null) {
                dolphin.snapTo(x, y, z, random.nextFloat() * 360, 0);
                level.addFreshEntity(dolphin);
                DOLPHINS.add(new Falling(dolphin, rain.owner, false));
            }
            return;
        }
        int roll = random.nextInt(98);
        EntityType<?> type = roll < 40 ? EntityTypes.COD : roll < 70 ? EntityTypes.SALMON : roll < 85 ? EntityTypes.TROPICAL_FISH : EntityTypes.PUFFERFISH;
        Entity fish = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (fish == null) return;
        fish.snapTo(x, y, z, random.nextFloat() * 360, 0);
        fish.setDeltaMovement(0, -1.0, 0);
        fish.getPersistentData().putBoolean(RAIN_FISH_TAG, true);
        level.addFreshEntity(fish);
        DOLPHINS.add(new Falling(fish, rain.owner, true));
    }

    /** Peixes da chuva nao dropam nada. */
    @SubscribeEvent
    public static void rainFishDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr(RAIN_FISH_TAG, false)) event.setCanceled(true);
    }

    // ---------- Nv 20 — Tornado de Peixes ----------

    private static final class Tornado {
        final ServerPlayer owner; final ServerLevel level; Vec3 center; final Vec3 direction; final long until;
        final double radius; final boolean poison; final Set<LivingEntity> caught = new HashSet<>();
        Tornado(ServerPlayer owner, Vec3 center, Vec3 direction, long until, double radius, boolean poison) {
            this.owner = owner; this.level = (ServerLevel) owner.level(); this.center = center; this.direction = direction;
            this.until = until; this.radius = radius; this.poison = poison;
        }
    }
    private static final List<Tornado> TORNADOS = new ArrayList<>();

    public static boolean canTornado(ServerPlayer player, ItemStack stack) {
        return RelicUtil.count(player, s -> s.is(ItemTags.FISHES)) >= TORNADO_FISH;
    }

    public static boolean tornado(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        boolean poison = RelicUtil.count(player, s -> s.is(Items.PUFFERFISH)) > 0;
        // Baiacu primeiro: os tipos de peixe gastos mudam o efeito.
        int used = RelicUtil.consume(player, s -> s.is(Items.PUFFERFISH), TORNADO_FISH);
        RelicUtil.consume(player, s -> s.is(ItemTags.FISHES), TORNADO_FISH - used);
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z).normalize();
        TORNADOS.add(new Tornado(player, player.position().add(dir.scale(2)), dir,
                player.level().getGameTime() + 120 + context.resonance() / 5, 2 + context.affinity() / 333.0, poison));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    private static boolean tickTornado(Tornado t, long now) {
        if (now >= t.until || !t.owner.isAlive()) {
            for (LivingEntity target : t.caught) {
                if (!target.isAlive()) continue;
                Vec3 out = target.position().subtract(t.center).multiply(1, 0, 1).normalize();
                target.setDeltaMovement(out.x * 1.6, 0.9, out.z * 1.6);
                RelicUtil.syncMotion(target);
            }
            return true;
        }
        t.center = t.center.add(t.direction.scale(0.35));
        for (int i = 0; i < 12; i++) {
            double h = (now * 0.3 + i) % 5, a = now * 0.5 + i * Math.PI / 6, r = t.radius * (0.4 + h / 8);
            t.level.sendParticles(i % 3 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FISHING, t.center.x + Math.cos(a) * r,
                    t.center.y + h, t.center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        for (LivingEntity target : t.level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(t.center, t.center).inflate(t.radius, 5, t.radius),
                e -> RelicTargets.canHit(t.owner, e))) {
            boolean boss = RelicUtil.isBoss(target);
            if (!boss) {
                t.caught.add(target);
                Vec3 toCenter = t.center.subtract(target.position()).multiply(1, 0, 1);
                Vec3 spin = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.4);
                target.setDeltaMovement(toCenter.scale(0.25).add(spin).add(t.direction.scale(0.35)).add(0, 0.25, 0));
                target.fallDistance = 0;
                RelicUtil.syncMotion(target);
            }
            if (now % 10 == 0) {
                target.hurtServer(t.level, t.level.damageSources().indirectMagic(t.owner, t.owner), RelicUtil.reduced(target, 3));
                if (t.poison) target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), t.owner);
            }
        }
        return false;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            FishingHook hook = player.fishing;
            if (hook == null || !(player.getMainHandItem().getItem() instanceof FisgyItem || player.getOffhandItem().getItem() instanceof FisgyItem)) continue;
            // Efeitos armados ao fisgar
            Armed a = ARMED.get(player.getUUID());
            LivingEntity target = hooked(player);
            if (a != null && target != null && a.handled.add(hook.getUUID())) {
                if (a.life > now) { a.life = 0; applyLife(player, target); }
                if (a.shark > now) { a.shark = 0; applyShark(player, target); }
            }
            // Pesca fora da agua e arpao
            if (!item().isUnlocked(player, 0) || hook.getHookedIn() != null) continue;
            anchorHook(hook);
            String place = place(hook);
            Catch c = CATCHES.get(hook.getUUID());
            if (place == null) { CATCHES.remove(hook.getUUID()); continue; }
            if (c == null || !c.place.equals(place)) {
                c = new Catch();
                c.place = place;
                c.biteAt = now + 100 + level.getRandom().nextInt(200) - item().masteryLevel(player) * 3;
                c.biteUntil = c.biteAt + BITE_WINDOW;
                CATCHES.put(hook.getUUID(), c);
            }
            if (now >= c.biteAt && now <= c.biteUntil) {
                if (now == c.biteAt) {
                    // A boia "pula" e o jogador e avisado: hora de recolher.
                    hook.setDeltaMovement(0, 0.35, 0);
                    hook.needsSync = true;
                    level.playSound(null, hook.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    RelicUtil.say(player, Component.translatable("relic.nexus.fisgy.fisgou_recolha_a_linha"));
                }
                if (now % 3 == 0) {
                    BlockState ground = level.getBlockState(hook.blockPosition().below());
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground), hook.getX(), hook.getY() + 0.1, hook.getZ(), 6, 0.15, 0.05, 0.15, 0.1);
                    level.sendParticles(place.equals("lava") ? ParticleTypes.LAVA : ParticleTypes.FISHING, hook.getX(), hook.getY() + 0.2, hook.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
                }
            } else if (now > c.biteUntil) {
                c.biteAt = now + 100 + level.getRandom().nextInt(200);
                c.biteUntil = c.biteAt + BITE_WINDOW;
            }
        }
        RAINS.removeIf(r -> r.owner.level() != level ? false : now >= r.until || !r.owner.isAlive());
        for (Rain rain : RAINS) if (rain.owner.level() == level && now % 3 == 0) tickRain(rain, level);
        DOLPHINS.removeIf(f -> {
            Entity e = f.entity();
            if (e.level() != level) return false;
            if (!e.isAlive()) return true;
            if (f.landedAt >= 0) {
                // Peixe se debate um pouco no chao e some.
                if (f.fish && now - f.landedAt >= 60) {
                    level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.2, e.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                    e.discard();
                    return true;
                }
                return !f.fish;
            }
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(f.fish ? 0.4 : 1.5),
                    t -> t != e && RelicTargets.canHit(f.owner(), t));
            if (!e.onGround() && !e.isInWater() && hit.isEmpty()) return false;
            boolean puffer = e.getType() == EntityTypes.PUFFERFISH;
            float damage = f.fish ? (puffer ? 2 : 3) : 8;
            for (LivingEntity target : hit) {
                target.hurtServer(level, level.damageSources().indirectMagic(f.owner(), f.owner()), RelicUtil.reduced(target, damage));
                if (puffer) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), f.owner());
            }
            level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(), e.getZ(), f.fish ? 8 : 30, f.fish ? 0.3 : 0.8, 0.3, f.fish ? 0.3 : 0.8, 0.1);
            f.landedAt = now;
            return false;
        });
        TORNADOS.removeIf(t -> t.level == level && tickTornado(t, now));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        ARMED.remove(event.getEntity().getUUID());
        RAINS.removeIf(r -> r.owner == event.getEntity());
        TORNADOS.removeIf(t -> t.owner == event.getEntity());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CATCHES.clear(); ANCHORS.clear(); NO_FALL.clear(); ARMED.clear(); RAINS.clear(); DOLPHINS.clear(); TORNADOS.clear();
    }
}
