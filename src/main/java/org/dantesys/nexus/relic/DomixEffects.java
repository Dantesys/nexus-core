package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Domix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class DomixEffects {
    private DomixEffects() {}

    public static final int PACK_MANA = 80, PACK_COOLDOWN = 1800, PACK_SIZE = 5;
    public static final int IMMUNITY_MANA = 60, IMMUNITY_COOLDOWN = 1200, IMMUNITY_TICKS = 200;
    public static final int CRITTERS_MANA = 120, CRITTERS_COOLDOWN = 2400, CRITTERS_TICKS = 400;
    public static final int BORROW_MANA = 100, BORROW_COOLDOWN = 3000;
    public static final String[] POWERS = {"lobo", "gato", "papagaio", "cavalo", "lhama", "camelo", "axolote", "nautilo"};
    public static final String[] POWER_NAMES = {"relic.nexus.domix.poder.lobo", "relic.nexus.domix.poder.gato", "relic.nexus.domix.poder.papagaio",
            "relic.nexus.domix.poder.cavalo", "relic.nexus.domix.poder.lhama", "relic.nexus.domix.poder.camelo", "relic.nexus.domix.poder.axolote",
            "relic.nexus.domix.poder.nautilo"};
    private static final String TEMP_TAG = "NexusPetTemporario", SPIT_TAG = "NexusCuspe";
    private static final Identifier CAT_FALL = Identifier.fromNamespaceAndPath(Nexus.MODID, "poder_gato");
    private static final Identifier PET_HEALTH = Identifier.fromNamespaceAndPath(Nexus.MODID, "domix_vida");
    private static final Identifier PET_DAMAGE = Identifier.fromNamespaceAndPath(Nexus.MODID, "domix_dano");

    private static DomixItem item() { return ModItems.DOMIX.get(); }

    // ---------- Nv 1 — Boop ----------

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof DomixItem)) return;
        Entity target = event.getTarget();
        boolean tamable = target instanceof TamableAnimal animal && !animal.isTame() || target instanceof AbstractHorse horse && !horse.isTamed();
        if (!tamable) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || !item().isUnlocked(player, 0)) return;
        if (target instanceof TamableAnimal animal) {
            animal.tame(player);
            animal.setOrderedToSit(false);
        } else if (target instanceof AbstractHorse horse) {
            horse.tameWithName(player);
        }
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 6, 0.3, 0.2, 0.3, 0);
        item().addMasteryXp(player, 2);
    }

    // ---------- Pets temporarios ----------

    private record Temp(Mob mob, UUID owner, long until) {}
    private static final List<Temp> TEMPS = new ArrayList<>();

    private static Mob summon(ServerPlayer player, EntityType<?> type, long until, int affinity) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof Mob mob)) return null;
        Vec3 pos = player.position().add(level.getRandom().nextDouble() * 4 - 2, 0, level.getRandom().nextDouble() * 4 - 2);
        mob.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360, 0);
        if (mob instanceof TamableAnimal animal) animal.tame(player);
        else if (mob instanceof AbstractHorse horse) { horse.setTamed(true); horse.setOwner(player); }
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.addOrUpdateTransientModifier(new AttributeModifier(PET_HEALTH, affinity / 50.0, AttributeModifier.Operation.ADD_VALUE));
        var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.addOrUpdateTransientModifier(new AttributeModifier(PET_DAMAGE, affinity / 250.0, AttributeModifier.Operation.ADD_VALUE));
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putBoolean(TEMP_TAG, true);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        TEMPS.add(new Temp(mob, player.getUUID(), until));
        return mob;
    }

    public static boolean pack(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int ticks = Math.min(1200, 400 + item().masteryLevel(player) * 20 + context.resonance() / 2);
        long until = player.level().getGameTime() + ticks;
        for (int i = 0; i < PACK_SIZE; i++) summon(player, org.dantesys.nexus.entity.ModEntities.LOBO_ESPECTRAL.get(), until, context.affinity());
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    public static boolean critters(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        List<? extends String> ids = Config.DOMIX_PETS.get();
        if (ids.isEmpty()) return false;
        ServerLevel level = (ServerLevel) player.level();
        int max = 6 + item().masteryLevel(player) / 5;
        int count = 6 + level.getRandom().nextInt(Math.max(1, max - 5));
        long until = level.getGameTime() + CRITTERS_TICKS;
        for (int i = 0; i < count; i++) {
            Identifier id = Identifier.tryParse(ids.get(level.getRandom().nextInt(ids.size())));
            if (id != null) summon(player, BuiltInRegistries.ENTITY_TYPE.getValue(id), until, context.affinity());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    /** Pets temporarios somem sem dropar nada e nao voltam depois de reiniciar. */
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr(TEMP_TAG, false)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void xp(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr(TEMP_TAG, false)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.loadedFromDisk()) return;
        if (event.getEntity().getPersistentData().getBooleanOr(TEMP_TAG, false)) event.setCanceled(true);
    }

    // ---------- Nv 10 — Imunidade ----------

    private static final Map<UUID, Long> IMMUNE = new HashMap<>();

    public static boolean immunity(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        long until = player.level().getGameTime() + IMMUNITY_TICKS;
        int count = 0;
        for (LivingEntity pet : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
                e -> e instanceof OwnableEntity own && own.getOwner() == player && e.distanceTo(player) <= 10)) {
            IMMUNE.put(pet.getUUID(), until);
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pet.getX(), pet.getY() + 1, pet.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
            count++;
        }
        if (count == 0) {
            RelicUtil.say(player, Component.translatable("relic.nexus.domix.nenhum_pet_seu_por_perto"));
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        Long until = IMMUNE.get(event.getEntity().getUUID());
        if (until != null && until > event.getEntity().level().getGameTime() && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    // ---------- Nv 20 — Poder Emprestado ----------

    private static final class Borrowed {
        final int power; final long until; final CompoundTag pet; final EntityType<?> type;
        Borrowed(int power, long until, CompoundTag pet, EntityType<?> type) { this.power = power; this.until = until; this.pet = pet; this.type = type; }
    }
    private static final Map<UUID, Borrowed> BORROWED = new HashMap<>();

    private static int powerOf(Entity pet) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString();
        String power = ConfigLists.pairs(Config.DOMIX_POWERS.get()).get(id);
        return power == null ? -1 : Arrays.asList(POWERS).indexOf(power);
    }

    private static Mob lookedPet(ServerPlayer player) {
        LivingEntity target = RelicUtil.targetEntity(player, 6, e -> e instanceof OwnableEntity own && own.getOwner() == player);
        return target instanceof Mob mob && !mob.getPersistentData().getBooleanOr(TEMP_TAG, false) ? mob : null;
    }

    public static boolean canBorrow(ServerPlayer player, ItemStack stack) {
        if (BORROWED.containsKey(player.getUUID())) return false;
        Mob pet = lookedPet(player);
        if (pet == null || powerOf(pet) < 0 || pet.getHealth() < pet.getMaxHealth() - 0.01f) return false;
        Vec3 toPlayer = player.getEyePosition().subtract(pet.getEyePosition()).normalize();
        return pet.getViewVector(1.0f).dot(toPlayer) > 0.6;
    }

    public static boolean borrow(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        Mob pet = lookedPet(player);
        if (pet == null) return false;
        int power = powerOf(pet);
        ServerLevel level = (ServerLevel) player.level();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        pet.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        int ticks = Math.min(2400, 1200 + context.resonance() * 6 / 5);
        BORROWED.put(player.getUUID(), new Borrowed(power, level.getGameTime() + ticks, data, pet.getType()));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + 0.5, pet.getZ(), 15, 0.4, 0.4, 0.4, 0);
        pet.discard();
        if (power == 2 && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        RelicUtil.say(player, Component.translatable("relic.nexus.domix.poder_emprestado", Component.translatable(POWER_NAMES[power])));
        return true;
    }

    private static void endBorrow(ServerPlayer player) {
        Borrowed b = BORROWED.remove(player.getUUID());
        if (b == null) return;
        ServerLevel level = (ServerLevel) player.level();
        Entity pet = b.type.create(level, EntitySpawnReason.LOAD);
        if (pet != null) {
            pet.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), b.pet));
            pet.setUUID(UUID.randomUUID());
            pet.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            level.addFreshEntity(pet);
        }
        var fall = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        if (fall != null) fall.removeModifier(CAT_FALL);
        if (b.power == 2 && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        }
        RelicUtil.say(player, Component.translatable("relic.nexus.domix.o_pet_voltou"));
    }

    public static int powerIndex(ServerPlayer player) {
        Borrowed b = BORROWED.get(player.getUUID());
        return b == null ? -1 : b.power;
    }

    public static int powerSecondsLeft(ServerPlayer player) {
        Borrowed b = BORROWED.get(player.getUUID());
        return b == null ? 0 : (int) Math.max(0, (b.until - player.level().getGameTime() + 19) / 20);
    }

    private static final Map<UUID, Long> KEY_COOLDOWN = new HashMap<>();

    /** Lhama: cuspe. Camelo e nautilo: dash. */
    public static boolean usePowerKey(ServerPlayer player) {
        Borrowed b = BORROWED.get(player.getUUID());
        if (b == null || (b.power != 4 && b.power != 5 && b.power != 7)) return false;
        long now = player.level().getGameTime();
        if (KEY_COOLDOWN.getOrDefault(player.getUUID(), 0L) > now) return true;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        if (b.power == 4) {
            LlamaSpit spit = EntityTypes.LLAMA_SPIT.create(level, EntitySpawnReason.TRIGGERED);
            if (spit != null) {
                spit.setOwner(player);
                spit.setPos(player.getEyePosition().add(look.scale(0.5)));
                spit.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
                spit.getPersistentData().putBoolean(SPIT_TAG, true);
                level.addFreshEntity(spit);
                level.playSound(null, player.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            KEY_COOLDOWN.put(player.getUUID(), now + 15);
        } else {
            player.setDeltaMovement(look.x * 1.8, Math.max(0.3, look.y * 0.8), look.z * 1.8);
            RelicUtil.syncMotion(player);
            level.playSound(null, player.blockPosition(), SoundEvents.CAMEL_DASH, SoundSource.PLAYERS, 1.0f, 1.0f);
            KEY_COOLDOWN.put(player.getUUID(), now + 40);
        }
        return true;
    }

    @SubscribeEvent
    public static void spitHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof LlamaSpit spit) || !spit.getPersistentData().getBooleanOr(SPIT_TAG, false)) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
                && spit.getOwner() instanceof ServerPlayer owner && RelicTargets.canHit(owner, target) && spit.level() instanceof ServerLevel level) {
            target.hurtServer(level, level.damageSources().spit(spit, owner), 6);
        }
    }

    private static void applyPower(ServerPlayer player, Borrowed b, long now) {
        if (now % 20 != 0) return;
        ServerLevel level = (ServerLevel) player.level();
        switch (b.power) {
            case 0 -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false, true));
                for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e instanceof Enemy)) {
                    PrivateVisuals.glow(player, enemy, 30);
                }
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
                var fall = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
                if (fall != null) fall.addOrUpdateTransientModifier(new AttributeModifier(CAT_FALL, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(8))) {
                    creeper.setTarget(null);
                    creeper.setSwellDir(-1);
                    Vec3 away = creeper.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                    creeper.getNavigation().moveTo(creeper.getX() + away.x * 8, creeper.getY(), creeper.getZ() + away.z * 8, 1.3);
                }
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 1, true, false, true));
            }
            case 5 -> player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false, true));
            case 6 -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 300, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0, true, false, true));
            }
            case 7 -> player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 300, 0, true, false, true));
            default -> {}
        }
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        TEMPS.removeIf(t -> {
            if (t.mob().level() != level) return false;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(t.owner());
            if (!t.mob().isAlive() || now >= t.until() || owner == null) {
                if (t.mob().isAlive()) {
                    level.sendParticles(ParticleTypes.POOF, t.mob().getX(), t.mob().getY() + 0.5, t.mob().getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                    t.mob().discard();
                }
                return true;
            }
            // Tambem atacam mobs hostis por perto.
            if (now % 20 == 0 && t.mob().getTarget() == null) {
                level.getEntitiesOfClass(Mob.class, t.mob().getBoundingBox().inflate(12), e -> e instanceof Enemy && e.isAlive())
                        .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(t.mob()))).ifPresent(t.mob()::setTarget);
            }
            return false;
        });
        IMMUNE.values().removeIf(until -> until <= now);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Borrowed b = BORROWED.get(player.getUUID());
        if (b == null) return;
        long now = player.level().getGameTime();
        if (now >= b.until) endBorrow(player);
        else applyPower(player, b, now);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) endBorrow(player);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) endBorrow(player);
        TEMPS.forEach(t -> t.mob().discard());
        TEMPS.clear();
        IMMUNE.clear();
    }
}
