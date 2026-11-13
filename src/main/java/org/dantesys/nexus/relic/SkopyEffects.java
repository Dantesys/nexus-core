package org.dantesys.nexus.relic;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Skopy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class SkopyEffects {
    private SkopyEffects() {}

    public static final int SHOT_MANA = 5;
    public static final int CAMO_MANA = 50, CAMO_COOLDOWN = 600, CAMO_TICKS = 160;
    public static final float CAMO_BONUS = 6;
    public static final int MOSCA_MANA = 40, MOSCA_COOLDOWN = 600, MOSCA_MARK_TICKS = 300;
    public static final int GHOST_MANA = 60, GHOST_COOLDOWN = 600, GHOST_OBSTACLES = 3;
    public static final double GHOST_RANGE = 200, GHOST_SPEED = 4;
    public static final float GHOST_DAMAGE = 12;
    public static final int ULTRA_MANA = 300, ULTRA_COOLDOWN = 6000;
    public static final double ULTRA_RANGE = 1000;

    private static final String TAG = "NexusSkopy", TAG_MOSCA = "NexusSkopyMosca";
    private static final ResourceKey<DamageType> ULTRASONIC = RelicUtil.damageType("skopy_ultrasonic");

    private static final class State {
        long camoUntil;
        boolean camoBonus, ghostReady;
        int moscaStage;
        LivingEntity moscaTarget;
        long moscaUntil;
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class Ghost {
        final AbstractArrow arrow; final ServerPlayer owner; final Vec3 start; final float power;
        Vec3 previous; int obstacles;
        final Set<BlockPos> blocks = new HashSet<>();
        final Set<UUID> hit = new HashSet<>();
        boolean fieldCounted;
        Ghost(AbstractArrow arrow, ServerPlayer owner, float power) {
            this.arrow = arrow; this.owner = owner; this.power = power; this.start = arrow.position(); this.previous = arrow.position();
        }
    }
    private static final List<Ghost> GHOSTS = new ArrayList<>();

    private static SkopyItem item() { return ModItems.SKOPY.get(); }
    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player.getUUID(), id -> new State()); }
    private static long now(ServerPlayer player) { return player.level().getGameTime(); }

    // ---------- Tiro ----------

    public static boolean shoot(ServerPlayer player, ItemStack stack, float power) {
        var magic = RelicUtil.magic(player);
        if (!player.hasInfiniteMaterials()) {
            if (!magic.useMana(SHOT_MANA)) {
                RelicUtil.say(player, "Mana insuficiente para atirar.");
                return false;
            }
            player.syncData(org.dantesys.nexus.data.ModAttachmentType.MAGIC_STATS);
        }
        ServerLevel level = (ServerLevel) player.level();
        State s = state(player);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f + power * 0.3f);

        // Na Mosca: a segunda flecha vai direto no alvo marcado.
        if (s.moscaStage == 2) {
            s.moscaStage = 0;
            LivingEntity target = s.moscaTarget;
            if (target != null && target.isAlive() && target.level() == level && now(player) < s.moscaUntil) {
                Arrow ghost = new Arrow(level, player, new ItemStack(Items.ARROW), stack);
                float damage = (float) Math.ceil(2.0 * power * 3.0) + (power >= 1 ? 2 : 0) + bonus(player, s);
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getEyeY(), player.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
                level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(), 30, 0.3, 0.5, 0.3, 0.2);
                if (target.hurtServer(level, level.damageSources().arrow(ghost, player), damage)) onArrowHit(player, target);
                return true;
            }
            RelicUtil.say(player, "Na Mosca: o alvo marcado sumiu.");
        }

        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), stack);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.getPersistentData().putBoolean(TAG, true);
        if (s.moscaStage == 1) arrow.getPersistentData().putBoolean(TAG_MOSCA, true);
        float extra = bonus(player, s);
        if (extra > 0) arrow.setBaseDamage(2.0 + extra / Math.max(1, power * 3));
        if (s.ghostReady) {
            s.ghostReady = false;
            arrow.setNoGravity(true);
            arrow.setNoPhysics(true);
            Vec3 look = player.getLookAngle();
            arrow.setPos(player.getEyePosition().add(look.scale(0.5)));
            arrow.setDeltaMovement(look.scale(GHOST_SPEED));
            level.addFreshEntity(arrow);
            GHOSTS.add(new Ghost(arrow, player, power));
            return true;
        }
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
        if (power >= 1.0F) arrow.setCritArrow(true);
        level.addFreshEntity(arrow);
        return true;
    }

    /** Bonus da Camuflagem: o tiro quebra a invisibilidade. */
    private static float bonus(ServerPlayer player, State s) {
        if (!s.camoBonus) return 0;
        s.camoBonus = false;
        endCamo(player, s);
        return CAMO_BONUS;
    }

    // ---------- Nv 1 — Sinal Arcano ----------

    private static void onArrowHit(ServerPlayer player, LivingEntity target) {
        SkopyItem skopy = item();
        skopy.addMasteryXp(player, 1);
        if (!skopy.isUnlocked(player, 0)) return;
        var effects = ConfigLists.effects(Config.SKOPY_EFFECTS.get());
        int ticks = 60 + skopy.masteryLevel(player) * 3 + RelicUtil.magic(player).getRessonancia() / 20;
        if (!effects.isEmpty()) {
            Holder<MobEffect> effect = effects.get(player.getRandom().nextInt(effects.size()));
            target.addEffect(new MobEffectInstance(effect, ticks, 0), player);
        }
        PrivateVisuals.glow(player, target, ticks);
    }

    @SubscribeEvent
    public static void arrowHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBooleanOr(TAG, false)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player) || event.getHealthDamage() <= 0) return;
        // Flecha rebatida pelo Campo Reflexivo nao conta para a Skopy.
        if (arrow.getPersistentData().getBooleanOr("NexusCampoRefletido", false)) return;
        LivingEntity target = event.getEntity();
        onArrowHit(player, target);
        if (arrow.getPersistentData().getBooleanOr(TAG_MOSCA, false)) {
            State s = state(player);
            if (s.moscaStage == 1) {
                s.moscaStage = 2;
                s.moscaTarget = target;
                s.moscaUntil = now(player) + MOSCA_MARK_TICKS;
                RelicUtil.say(player, "Na Mosca: alvo marcado. O próximo tiro vai direto nele.");
            }
        }
    }

    // ---------- Nv 5 — Camuflagem ----------

    public static boolean camouflage(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        int ticks = CAMO_TICKS + context.resonance() / 10;
        s.camoUntil = now(player) + ticks;
        s.camoBonus = true;
        player.removeEffect(MobEffects.GLOWING);
        PrivateVisuals.clearGlowOn(player);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) mob.setTarget(null);
        hideEquipment(player);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    private static void hideEquipment(ServerPlayer player) {
        List<Pair<EquipmentSlot, ItemStack>> empty = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) empty.add(Pair.of(slot, ItemStack.EMPTY));
        }
        ((ServerLevel) player.level()).getChunkSource().sendToTrackingPlayers(player, new ClientboundSetEquipmentPacket(player.getId(), empty));
    }

    private static void showEquipment(ServerPlayer player) {
        List<Pair<EquipmentSlot, ItemStack>> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                items.add(Pair.of(slot, player.getItemBySlot(slot).copy()));
            }
        }
        ((ServerLevel) player.level()).getChunkSource().sendToTrackingPlayers(player, new ClientboundSetEquipmentPacket(player.getId(), items));
    }

    private static void endCamo(ServerPlayer player, State s) {
        if (s.camoUntil <= 0) return;
        s.camoUntil = 0;
        s.camoBonus = false;
        player.removeEffect(MobEffects.INVISIBILITY);
        showEquipment(player);
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) {
            State s = STATES.get(player.getUUID());
            if (s != null && s.camoUntil > now(player)) event.setCanceled(true);
        }
    }

    // ---------- Nv 10 — Na Mosca ----------

    public static boolean naMosca(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        s.moscaStage = 1;
        s.moscaTarget = null;
        RelicUtil.say(player, "Na Mosca: o próximo tiro marca o alvo.");
        return true;
    }

    // ---------- Nv 15 — Ghost ----------

    public static boolean ghost(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).ghostReady = true;
        RelicUtil.say(player, "Ghost: o próximo tiro é espectral.");
        return true;
    }

    private static boolean tickGhost(Ghost g) {
        AbstractArrow arrow = g.arrow;
        if (!arrow.isAlive() || g.owner.isRemoved()) return true;
        ServerLevel level = (ServerLevel) arrow.level();
        Vec3 from = g.previous, to = arrow.position();
        g.previous = to;
        double length = from.distanceTo(to);
        for (double d = 0; d <= length; d += 0.3) {
            Vec3 p = from.lerp(to, length == 0 ? 0 : d / length);
            BlockPos pos = BlockPos.containing(p);
            if (!g.blocks.contains(pos) && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                g.blocks.add(pos);
                if (g.obstacles++ >= GHOST_OBSTACLES) { arrow.discard(); return true; }
            }
            if (!g.fieldCounted && BastryEffects.reflectingField(level, p, g.owner) != null) {
                g.fieldCounted = true;
                if (g.obstacles++ >= GHOST_OBSTACLES) { arrow.discard(); return true; }
            }
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1),
                t -> t != g.owner && t.isAlive() && !(t instanceof ArmorStand) && !g.hit.contains(t.getUUID())
                        && (t.getBoundingBox().inflate(0.3).contains(from) || t.getBoundingBox().inflate(0.3).clip(from, to).isPresent()))) {
            g.hit.add(target.getUUID());
            float damage = (float) (GHOST_DAMAGE * g.power * Math.max(0.1, 1 - 0.15 * g.obstacles));
            if (target.hurtServer(level, level.damageSources().arrow(arrow, g.owner), damage)) onArrowHit(g.owner, target);
            if (g.obstacles++ >= GHOST_OBSTACLES) { arrow.discard(); return true; }
        }
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 2, 0.05, 0.05, 0.05, 0);
        if (to.distanceTo(g.start) > GHOST_RANGE) { arrow.discard(); return true; }
        arrow.setDeltaMovement(arrow.getDeltaMovement().normalize().scale(GHOST_SPEED));
        return false;
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        GHOSTS.removeIf(g -> g.arrow.level() == level && tickGhost(g));
    }

    // ---------- Nv 20 — Ultrasonic ----------

    public static boolean canUltrasonic(ServerPlayer player, ItemStack stack) {
        if (!RelicUtil.hasRank(RelicUtil.magic(player).getRegeneracao(), Ranks.A) || player.hasEffect(MobEffects.BLINDNESS)) return false;
        if (RelicUtil.count(player, s -> s.is(ItemTags.ARROWS)) > 0) return false;
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5),
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && e.distanceTo(player) <= 5).isEmpty();
    }

    public static boolean ultrasonic(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition(), dir = player.getLookAngle();
        int obstacles = 0;
        Set<BlockPos> blocks = new HashSet<>();
        Set<UUID> hit = new HashSet<>();
        double step = 0.5;
        for (double d = 1; d <= ULTRA_RANGE; d += step) {
            Vec3 p = start.add(dir.scale(d));
            BlockPos pos = BlockPos.containing(p);
            if (!level.isLoaded(pos)) break;
            if (!blocks.contains(pos) && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                blocks.add(pos);
                obstacles++;
            }
            if (d <= 200 && ((int) (d / step)) % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(0.5),
                    t -> t != player && t.isAlive() && !(t instanceof ArmorStand) && !hit.contains(t.getUUID()))) {
                hit.add(target.getUUID());
                double fraction = Math.max(0, 1 - 0.01 * obstacles);
                if (RelicUtil.isBoss(target)) fraction = Math.min(fraction, 0.5);
                float damage = (float) (target.getHealth() * fraction);
                if (damage > 0) target.hurtServer(level, RelicUtil.source(level, ULTRASONIC, player), damage);
                obstacles++;
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.4f);
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        long now = now(player);
        if (s.camoUntil > 0) {
            if (now >= s.camoUntil) endCamo(player, s);
            else if (now % 10 == 0) hideEquipment(player);
        }
        if (s.moscaStage == 2 && now >= s.moscaUntil) s.moscaStage = 0;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State s = STATES.remove(player.getUUID());
            if (s != null && s.camoUntil > 0) player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
        GHOSTS.clear();
    }
}
