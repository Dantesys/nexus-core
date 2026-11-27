package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Sculky (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class SculkyEffects {
    private SculkyEffects() {}

    public static final int AMP_MANA = 80, AMP_COOLDOWN = 12000, AMP_TICKS = 1200;
    public static final int PRESSURE_MANA = 60, PRESSURE_COOLDOWN = 1200;
    public static final int STEP_MANA = 30, STEP_COOLDOWN = 300, PATCH_TICKS = 1200, PATCH_RADIUS = 5;
    public static final int BOOM_MANA = 300, BOOM_COOLDOWN = 18000, BOOM_RANGE = 30;
    public static final String AMP_TAG = "SculkyAmplificado";

    private static SculkyItem item() { return ModItems.SCULKY.get(); }

    /** Segurando a Sculky (Eco Vivo liberado). */
    public static boolean echo(Entity e) { return e instanceof Player p && item().heldBy(p) && item().isUnlocked(p, 0); }

    private static final Map<UUID, Long> AMP = new HashMap<>();
    public static boolean amplified(Player p) { Long u = AMP.get(p.getUUID()); return u != null && u > p.level().getGameTime(); }

    // ---------- Nv 1 — Eco Vivo ----------

    @SubscribeEvent
    public static void sonic(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM) && echo(event.getEntity())) event.setCanceled(true);
    }

    private static final Map<String, Long> SEEN = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void vibration(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) return;
        Entity cause = event.getCause();
        if (cause != null && echo(cause)) { event.setCanceled(true); return; }
        Vec3 pos = event.getEventPosition();
        long now = level.getGameTime();
        for (ServerPlayer holder : level.players()) {
            if (!echo(holder) || holder == cause) continue;
            boolean amp = amplified(holder);
            double radius = amp ? 32 : 16;
            if (holder.position().distanceToSqr(pos) > radius * radius) continue;
            String key = holder.getUUID() + ":" + (cause != null ? cause.getUUID() : BlockPos.containing(pos));
            if (SEEN.getOrDefault(key, 0L) > now) continue;
            SEEN.put(key, now + 10);
            level.sendParticles(holder, new VibrationParticleOption(new EntityPositionSource(holder, holder.getEyeHeight()), 15), true, true,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            level.sendParticles(holder, ParticleTypes.SCULK_CHARGE_POP, true, true, pos.x, pos.y + 0.2, pos.z, 4, 0.2, 0.2, 0.2, 0.02);
            if (cause instanceof LivingEntity && cause != holder) PrivateVisuals.glow(holder, cause, amp ? 60 : 20, net.minecraft.world.scores.TeamColor.DARK_AQUA, true);
        }
    }

    // ---------- Nv 5 — Amplificador ----------

    public static boolean amplify(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        AMP.put(player.getUUID(), player.level().getGameTime() + AMP_TICKS);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(AMP_TAG, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static void endAmp(ServerPlayer player) {
        if (AMP.remove(player.getUUID()) == null) return;
        ItemStack stack = item().findIn(player);
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(AMP_TAG));
        item().startCooldown(player, stack, 1, AMP_COOLDOWN);
    }

    // ---------- Nv 10 — Pressao Sonora ----------

    private static final Map<UUID, Long> PRESSURE = new HashMap<>();

    public static boolean pressure(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        PRESSURE.put(player.getUUID(), player.level().getGameTime() + (amplified(player) ? 400 : 200));
        return true;
    }

    private static void beat(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean amp = amplified(player);
        double radius = amp ? 10 : 5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= radius)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0), player);
            e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("sculky_sonico"), player), amp ? 2 : 1);
            Vec3 out = e.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.4);
            RelicUtil.push(e, e.getDeltaMovement().add(out.x, 0.1, out.z));
            if (e instanceof Player p) p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0), player);
            if (e.isUsingItem()) e.stopUsingItem();
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        EternyEffects.ring(level, player, radius, ParticleTypes.SCULK_SOUL);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    // ---------- Nv 15 — Passo Sculk ----------

    private record Patch(ServerLevel level, Map<BlockPos, BlockState> original, long until) {}
    private static final List<Patch> PATCHES = new ArrayList<>();

    private static boolean sculk(BlockState s) { return s.is(Blocks.SCULK) || s.is(Blocks.SCULK_CATALYST) || s.is(Blocks.SCULK_SENSOR) || s.is(Blocks.SCULK_SHRIEKER); }

    private static boolean connected(ServerLevel level, BlockPos from, BlockPos to) {
        Deque<BlockPos> queue = new ArrayDeque<>(List.of(from));
        Set<BlockPos> seen = new HashSet<>(queue);
        while (!queue.isEmpty() && seen.size() < 4000) {
            BlockPos pos = queue.poll();
            if (pos.equals(to)) return true;
            for (BlockPos n : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockPos im = n.immutable();
                if (!seen.contains(im) && sculk(level.getBlockState(im))) { seen.add(im); queue.add(im); }
            }
        }
        return false;
    }

    public static boolean sculkStep(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos under = player.blockPosition().below();
        if (sculk(level.getBlockState(under))) {
            HitResult hit = player.pick(48, 0, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && sculk(level.getBlockState(block.getBlockPos()))
                    && connected(level, under, block.getBlockPos())) {
                BlockPos dest = block.getBlockPos().above();
                level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                player.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
                RelicUtil.pushToFreeSpace(player);
                level.playSound(null, dest, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 1.5f, 0.8f);
                return true;
            }
        }
        Map<BlockPos, BlockState> original = new HashMap<>();
        BlockPos center = player.blockPosition();
        for (int dx = -PATCH_RADIUS; dx <= PATCH_RADIUS; dx++) for (int dz = -PATCH_RADIUS; dz <= PATCH_RADIUS; dz++) {
            if (dx * dx + dz * dz > PATCH_RADIUS * PATCH_RADIUS) continue;
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = center.offset(dx, dy, dz);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.above()).canBeReplaced()) continue;
                if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0 || !state.isCollisionShapeFullBlock(level, pos) || sculk(state)) break;
                original.put(pos.immutable(), state);
                level.setBlock(pos, Blocks.SCULK.defaultBlockState(), 2);
                break;
            }
        }
        PATCHES.add(new Patch(level, original, level.getGameTime() + PATCH_TICKS));
        level.playSound(null, center, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
        return true;
    }

    // ---------- Nv 20 — Sonic Boom ----------

    public static boolean canBoom(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        return amplified(player) && item().ready(stack, now, 2) && item().ready(stack, now, 3);
    }

    public static boolean sonicBoom(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        item().startCooldown(player, stack, 2, PRESSURE_COOLDOWN);
        item().startCooldown(player, stack, 3, STEP_COOLDOWN);
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(BOOM_RANGE));
        for (int i = 1; i <= BOOM_RANGE; i++) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5),
                e -> RelicTargets.canHit(player, e) && e.getBoundingBox().inflate(0.5).clip(eye, end).isPresent())) {
            if (e instanceof Player || RelicUtil.isBoss(e)) e.hurtServer(level, level.damageSources().sonicBoom(player), 20);
            else RelicUtil.guaranteedKill(level, e, player);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 1.0f);
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Long amp = AMP.get(player.getUUID());
        if (amp != null && now >= amp) endAmp(player);
        Long pressure = PRESSURE.get(player.getUUID());
        if (pressure != null) {
            if (now >= pressure) PRESSURE.remove(player.getUUID());
            else if (now % 20 == 0) beat(player);
        }
        // Amplificado: enxerga quem esta sob o Silencio da Thiefy.
        if (now % 20 == 0 && echo(player) && amplified(player)) {
            for (Player other : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(32), p -> p != player && ThiefyEffects.silenced(p))) {
                PrivateVisuals.glow(player, other, 30, net.minecraft.world.scores.TeamColor.DARK_AQUA, true);
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || PATCHES.isEmpty()) return;
        long now = level.getGameTime();
        PATCHES.removeIf(p -> {
            if (p.level != level || now < p.until) return false;
            restore(p);
            return true;
        });
        if (now % 200 == 0) SEEN.values().removeIf(t -> t < now);
    }

    private static void restore(Patch p) {
        p.original.forEach((pos, state) -> { if (p.level.getBlockState(pos).is(Blocks.SCULK)) p.level.setBlock(pos, state, 2); });
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        PATCHES.forEach(SculkyEffects::restore);
        PATCHES.clear();
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) endAmp(p);
    }
}
