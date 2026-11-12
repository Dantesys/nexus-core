package org.dantesys.nexus.events;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.advancement.ManaAdvancements;
import org.dantesys.nexus.blocks.BolaCristal;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.effects.PeleMana;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;
import org.dantesys.nexus.worldgen.ModBiomes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NexusPlayerEvents {
    private static final int LOW_AFFINITY_B_THRESHOLD = 600;
    private static final int LOW_MANA_NAUSEA_THRESHOLD_PERCENT = 10;
    private static final int BIOME_EVENT_COOLDOWN_TICKS = 20 * 60 * 3;
    private static final int BIOME_EVENT_WARNING_TICKS = 20 * 5;
    private static final float BIOME_EVENT_CHANCE = 0.25f;
    private static final int ILLUMINATED_VALLEY_EFFECT_DURATION = 60;
    private static final float LOOKING_FORWARD_PITCH_TOLERANCE = 9.0f;
    private static final String BIOME_EVENT_WARNING_MESSAGE = "message.nexus.biome_event.warning";
    private static final String BIOME_EVENT_WARNING_DETAIL_PREFIX = "message.nexus.biome_event.warning.";
    private static final int TEMPESTADE_LIGHTNING_STORM_TICKS = 20 * 30;
    private static final int TEMPESTADE_LIGHTNING_STORM_RADIUS = 15;
    private static final Map<UUID, ScheduledBiomeEvent> SCHEDULED_BIOME_EVENTS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_TEMPESTADE_STORMS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (Config.GENERATE_MAGIC_STATUS_ON_FIRST_JOIN.get()) {
                BolaCristal.initializeMagicStatus(player, player.getRandom());
            }
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            if (status.isCreated()) {
                ManaAdvancements.check(player, status);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        if (!status.isManaSkinActive()) {
            return;
        }

        status.setManaSkinActive(false);
        player.removeEffect(ModEffects.PELE_MANA);
        PeleMana.clearGlow(player);
        player.syncData(ModAttachmentType.MAGIC_STATS);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        updateManaSkinEffect(player, status);

        if (player.tickCount % 20 != 0) {
            return;
        }

        tickScheduledBiomeEvent(player);
        tickActiveTempestadeStorm(player);

        boolean changed = status.regenerarMana();
        applyIlluminatedValleyLightEffect(player, status);
        changed |= handleNexusBiomeHazards(player, status);
        if (status.isManaSkinActive()) {
            int cost = getManaSkinCost(player, status);
            if (status.useMana(cost)) {
                changed = true;
            } else {
                status.setManaSkinActive(false);
                updateManaSkinEffect(player, status);
                changed = true;
            }
        }
        applyLowManaPenalty(player, status);

        if (changed) {
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
    }

    private static void applyLowManaPenalty(ServerPlayer player, MagicStatus status) {
        if (status.getMaxMana() <= 0) {
            return;
        }

        if (status.getMana() * 100 <= status.getMaxMana() * LOW_MANA_NAUSEA_THRESHOLD_PERCENT) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, true, false, true));
        }
    }

    private static void updateManaSkinEffect(ServerPlayer player, MagicStatus status) {
        if (!status.isManaSkinActive() || status.getMana() <= 0) {
            status.setManaSkinActive(false);
            player.removeEffect(ModEffects.PELE_MANA);
            PeleMana.clearGlow(player);
            return;
        }

        PeleMana.applyGlow(player, status.getSelectedMana());
        player.addEffect(new MobEffectInstance(
                ModEffects.PELE_MANA,
                40,
                status.getSelectedMana().ordinal(),
                true,
                false,
                true
        ));
    }

    private static int getManaSkinCost(ServerPlayer player, MagicStatus status) {
        if (!isInNexusBiome(player)) {
            return 100;
        }

        int resonance = Mth.clamp(status.get(status.getSelectedMana()).getRessonancia(), 0, 1000);
        return Mth.clamp(100 - resonance * 90 / 1000, 10, 100);
    }

    private static boolean isInNexusBiome(ServerPlayer player) {
        var biome = player.level().getBiome(player.blockPosition());
        return biome.is(ModBiomes.IGNEO)
                || biome.is(ModBiomes.VIDA)
                || biome.is(ModBiomes.SOMBRIA)
                || biome.is(ModBiomes.LUZ)
                || biome.is(ModBiomes.METAL)
                || biome.is(ModBiomes.TEMPESTADE);
    }

    private static void applyIlluminatedValleyLightEffect(ServerPlayer player, MagicStatus status) {
        Level level = player.level();
        if (!level.getBiome(player.blockPosition()).is(ModBiomes.LUZ)) {
            return;
        }

        if (isNight(level)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    ILLUMINATED_VALLEY_EFFECT_DURATION*20,
                    0,
                    true,
                    false,
                    true
            ));
            return;
        }

        if (!status.isManaSkinActive()
                && !hasHelmet(player)
                && !hasCoverUpToTwoBlocksAbove(player)
                && !isLookingForwardOrUp(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    ILLUMINATED_VALLEY_EFFECT_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }
    }

    private static boolean isNight(Level level) {
        long dayTime = level.getDefaultClockTime() % 24000L;
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    private static boolean hasHelmet(ServerPlayer player) {
        return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }

    private static boolean hasCoverUpToTwoBlocksAbove(ServerPlayer player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        for (int height = 1; height <= 2; height++) {
            BlockPos coverPos = origin.above(height);
            BlockState state = level.getBlockState(coverPos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLookingForwardOrUp(ServerPlayer player) {
        return player.getXRot() >= LOOKING_FORWARD_PITCH_TOLERANCE;
    }

    private static boolean handleNexusBiomeHazards(ServerPlayer player, MagicStatus status) {
        ServerLevel level = player.level();

        if (status.isManaSkinActive()) {
            return tickInactiveBiomeEvent(status);
        }

        Elementos biomeElement = getNexusBiomeElement(player);
        if (biomeElement == null || status.get(biomeElement).getAfinidade() >= LOW_AFFINITY_B_THRESHOLD) {
            return tickInactiveBiomeEvent(status);
        }

        applyLowAffinityBiomeEffect(player, biomeElement);

        if (status.isNexusBiomeEventPending()) {
            status.tickNexusBiomeEventCooldown(20);
            if (status.getNexusBiomeEventCooldown() <= 0) {
                status.setNexusBiomeEventPending(false);
                status.setNexusBiomeEventCooldown(BIOME_EVENT_COOLDOWN_TICKS);
                triggerNexusBiomeEvent(level, player, biomeElement);
            }
            return true;
        }

        if (status.getNexusBiomeEventCooldown() > 0) {
            status.tickNexusBiomeEventCooldown(20);
            return true;
        }

        if (player.getRandom().nextFloat() < BIOME_EVENT_CHANCE) {
            status.setNexusBiomeEventPending(true);
            status.setNexusBiomeEventCooldown(BIOME_EVENT_WARNING_TICKS);
            sendBiomeEventWarning(player, biomeElement);
        } else {
            status.setNexusBiomeEventCooldown(BIOME_EVENT_COOLDOWN_TICKS);
        }
        return true;
    }

    private static boolean tickInactiveBiomeEvent(MagicStatus status) {
        boolean changed = false;
        if (status.isNexusBiomeEventPending()) {
            status.setNexusBiomeEventPending(false);
            changed = true;
        }
        if (status.getNexusBiomeEventCooldown() > 0) {
            status.tickNexusBiomeEventCooldown(20);
            changed = true;
        }
        return changed;
    }

    public static Elementos getNexusBiomeElement(ServerPlayer player) {
        var biome = player.level().getBiome(player.blockPosition());
        if (biome.is(ModBiomes.IGNEO)) return Elementos.IGNEA;
        if (biome.is(ModBiomes.VIDA)) return Elementos.VIDA;
        if (biome.is(ModBiomes.SOMBRIA)) return Elementos.SOMBRA;
        if (biome.is(ModBiomes.LUZ)) return Elementos.LUZ;
        if (biome.is(ModBiomes.METAL)) return Elementos.METAL;
        if (biome.is(ModBiomes.TEMPESTADE)) return Elementos.TEMPESTADE;
        return null;
    }

    public static void scheduleNexusBiomeEvent(ServerPlayer player, Elementos elemento, int ticks) {
        if (elemento == null || elemento == Elementos.NAO_ELEMENTAL) {
            return;
        }

        ScheduledBiomeEvent scheduled = new ScheduledBiomeEvent(elemento, Math.max(0, ticks));
        SCHEDULED_BIOME_EVENTS.put(player.getUUID(), scheduled);
        if (scheduled.warned && scheduled.remainingTicks > 0) {
            sendBiomeEventWarning(player, scheduled.elemento);
        }
    }

    public static void clearScheduledNexusBiomeEvent(ServerPlayer player) {
        SCHEDULED_BIOME_EVENTS.remove(player.getUUID());
    }

    private static void tickScheduledBiomeEvent(ServerPlayer player) {
        ScheduledBiomeEvent scheduled = SCHEDULED_BIOME_EVENTS.get(player.getUUID());
        if (scheduled == null) {
            return;
        }

        scheduled.remainingTicks -= 20;
        if (!scheduled.warned && scheduled.remainingTicks <= BIOME_EVENT_WARNING_TICKS) {
            scheduled.warned = true;
            sendBiomeEventWarning(player, scheduled.elemento);
        }

        if (scheduled.remainingTicks > 0) {
            return;
        }

        SCHEDULED_BIOME_EVENTS.remove(player.getUUID());
        if (player.level() instanceof ServerLevel level) {
            triggerNexusBiomeEvent(level, player, scheduled.elemento);
        }
    }

    private static void applyLowAffinityBiomeEffect(ServerPlayer player, Elementos elemento) {
        switch (elemento) {
            case IGNEA -> player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 80));
            case VIDA -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false, true));
            case SOMBRA -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, true));
            case LUZ -> {
            }
            case METAL -> player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 0, true, false, true));
            case TEMPESTADE -> applyRandomTempestadeEffect(player);
            case NAO_ELEMENTAL -> {
            }
        }
    }

    public static void triggerNexusBiomeEvent(ServerLevel level, ServerPlayer player, Elementos elemento) {
        switch (elemento) {
            case IGNEA -> triggerIgneaEvent(level, player);
            case VIDA -> triggerVidaEvent(level, player);
            case SOMBRA -> triggerSombriaEvent(level, player);
            case LUZ -> triggerLuzEvent(level, player);
            case METAL -> triggerMetalEvent(level, player);
            case TEMPESTADE -> triggerTempestadeLightningStormEvent(level, player);
            case NAO_ELEMENTAL -> {
            }
        }
    }

    private static void sendBiomeEventWarning(ServerPlayer player, Elementos elemento) {
        player.sendSystemMessage(Component.translatable(BIOME_EVENT_WARNING_MESSAGE));
        player.sendSystemMessage(Component.translatable(BIOME_EVENT_WARNING_DETAIL_PREFIX + biomeEventWarningId(elemento)));
    }

    private static String biomeEventWarningId(Elementos elemento) {
        return switch (elemento) {
            case IGNEA -> "ignea";
            case SOMBRA -> "sombria";
            case VIDA -> "vida";
            case LUZ -> "luz";
            case TEMPESTADE -> "tempestade";
            case METAL -> "metal";
            case NAO_ELEMENTAL -> "nao_elemental";
        };
    }

    private static void applyRandomTempestadeEffect(ServerPlayer player) {
        if(player.tickCount % 40 != 0){
            return;
        }
        RandomSource random = player.getRandom();
        var effect = random.nextBoolean() ? MobEffects.SPEED : MobEffects.SLOWNESS;
        int amplifier = random.nextInt(3);
        player.addEffect(new MobEffectInstance(effect, 40, amplifier, true, false, true));
    }

    private static void triggerIgneaEvent(ServerLevel level, ServerPlayer player) {
        BlockPos eventPos = findNearbyGroundPos(level, player, 5, 2);
        double x = eventPos.getX() + 0.5;
        double y = eventPos.getY() + 0.1;
        double z = eventPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 80, 1.2, 0.1, 1.2, 0.18);
        level.sendParticles(ParticleTypes.LAVA, x, y + 0.1, z, 25, 0.8, 0.1, 0.8, 0.12);
        level.explode(null, x, y, z, 2.5f, false, Level.ExplosionInteraction.NONE);
    }

    private static void triggerVidaEvent(ServerLevel level, ServerPlayer player) {
        BlockPos eventPos = findNearbyGroundPos(level, player, 4, 2);
        double x = eventPos.getX() + 0.5;
        double y = eventPos.getY() + 0.1;
        double z = eventPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 60, 1.0, 0.15, 1.0, 0.03);
        level.sendParticles(ParticleTypes.COMPOSTER, x, y + 0.1, z, 80, 1.2, 0.2, 1.2, 0.04);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 255, true, false, true));
    }

    private static void triggerSombriaEvent(ServerLevel level, ServerPlayer player) {
        RandomSource random = player.getRandom();
        Mob mob = switch (random.nextInt(3)) {
            case 0 -> EntityTypes.HUSK.create(level, EntitySpawnReason.TRIGGERED);
            case 1 -> EntityTypes.PARCHED.create(level, EntitySpawnReason.TRIGGERED);
            default -> EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.TRIGGERED);
        };
        if (mob == null) {
            return;
        }

        BlockPos spawnPos = findNearbySpawnPos(level, player, 4, 2);
        mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        mob.setYRot(random.nextFloat() * 360.0f);
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 45, 0, true, false, true));
        mob.setTarget(player);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.SMOKE, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 45, 0.5, 0.8, 0.5, 0.04);
    }

    private static void triggerLuzEvent(ServerLevel level, ServerPlayer player) {
        BlockPos eventPos = findNearbyGroundPos(level, player, 5, 2);
        double x = eventPos.getX() + 0.5;
        double y = eventPos.getY();
        double z = eventPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.END_ROD, x, y + 5.0, z, 90, 1.6, 0.5, 1.6, 0.08);
        level.sendParticles(ParticleTypes.FIREWORK, x, y + 2.0, z, 45, 1.0, 1.5, 1.0, 0.08);
        player.hurt(level.damageSources().magic(), 7.0f);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 8, 0, true, false, true));
    }

    private static void triggerMetalEvent(ServerLevel level, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 8, 0, true, false, true));
        Mob golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
        if (golem == null) {
            return;
        }

        BlockPos spawnPos = findNearbySpawnPos(level, player, 5, 2);
        golem.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        golem.setYRot(player.getRandom().nextFloat() * 360.0f);
        golem.setTarget(player);
        golem.getAttribute(Attributes.SCALE).setBaseValue(0.5);
        golem.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(3);
        golem.setAggressive(true);
        golem.setCustomName(Component.translatable("golem.nexus.name"));
        golem.setCustomNameVisible(true);
        level.addFreshEntity(golem);
        level.sendParticles(ParticleTypes.CRIT, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 45, 0.8, 0.9, 0.8, 0.08);
    }

    private static void triggerTempestadeLightningStormEvent(ServerLevel level, ServerPlayer player) {
        level.setRainLevel(1.0f);
        level.setThunderLevel(1.0f);
        ACTIVE_TEMPESTADE_STORMS.put(player.getUUID(), TEMPESTADE_LIGHTNING_STORM_TICKS);
        spawnRandomTempestadeLightning(level, player);
    }

    private static void tickActiveTempestadeStorm(ServerPlayer player) {
        Integer remainingTicks = ACTIVE_TEMPESTADE_STORMS.get(player.getUUID());
        if (remainingTicks == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            ACTIVE_TEMPESTADE_STORMS.remove(player.getUUID());
            return;
        }

        level.setRainLevel(1.0f);
        level.setThunderLevel(1.0f);
        spawnRandomTempestadeLightning(level, player);

        remainingTicks -= 20;
        if (remainingTicks <= 0) {
            ACTIVE_TEMPESTADE_STORMS.remove(player.getUUID());
        } else {
            ACTIVE_TEMPESTADE_STORMS.put(player.getUUID(), remainingTicks);
        }
    }

    private static void spawnRandomTempestadeLightning(ServerLevel level, ServerPlayer player) {
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            BlockPos eventPos = findNearbyGroundPos(level, player, TEMPESTADE_LIGHTNING_STORM_RADIUS, 0);
            lightning.setPos(eventPos.getX() + 0.5, eventPos.getY(), eventPos.getZ() + 0.5);
            level.addFreshEntity(lightning);
        }
    }

    private static BlockPos findNearbyGroundPos(ServerLevel level, ServerPlayer player, int radius, int minDistance) {
        RandomSource random = player.getRandom();
        BlockPos origin = player.blockPosition();
        for (int i = 0; i < 12; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    0,
                    random.nextInt(radius * 2 + 1) - radius
            );
            if (pos.distManhattan(origin) < minDistance) {
                continue;
            }
            return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        }
        return origin;
    }

    private static BlockPos findNearbySpawnPos(ServerLevel level, ServerPlayer player, int radius, int minDistance) {
        RandomSource random = player.getRandom();
        BlockPos origin = player.blockPosition();
        for (int i = 0; i < 12; i++) {
            BlockPos ground = findNearbyGroundPos(level, player, radius, minDistance);
            if (level.getBlockState(ground).isAir() && level.getBlockState(ground.above()).isAir()) {
                return ground;
            }
        }
        return origin;
    }

    private static class ScheduledBiomeEvent {
        private final Elementos elemento;
        private int remainingTicks;
        private boolean warned;

        private ScheduledBiomeEvent(Elementos elemento, int remainingTicks) {
            this.elemento = elemento;
            this.remainingTicks = remainingTicks;
            this.warned = remainingTicks > 0 && remainingTicks <= BIOME_EVENT_WARNING_TICKS;
        }
    }
}
