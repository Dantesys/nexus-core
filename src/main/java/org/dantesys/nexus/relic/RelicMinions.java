package org.dantesys.nexus.relic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import org.dantesys.nexus.Nexus;

import java.util.UUID;

/**
 * Lacaios das reliquias (tag RelicTargets.MINION_TAG): atacam quem ataca o dono e quem o dono ataca,
 * nunca atacam o dono nem os aliados dele, e nao dropam nada.
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RelicMinions {
    private RelicMinions() {}

    public static ServerPlayer owner(Entity minion) {
        String id = minion.getPersistentData().getStringOr(RelicTargets.MINION_TAG, "");
        if (id.isEmpty() || minion.level().getServer() == null) return null;
        try { return minion.level().getServer().getPlayerList().getPlayer(UUID.fromString(id)); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static boolean isMinion(Entity e) { return !e.getPersistentData().getStringOr(RelicTargets.MINION_TAG, "").isEmpty(); }

    private static void rally(ServerPlayer owner, LivingEntity target) {
        if (target == null || target == owner || RelicTargets.isAlly(owner, target)) return;
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(32), m -> RelicTargets.isMinionOf(owner, m))) {
            if (mob.getTarget() == null || !mob.getTarget().isAlive()) mob.setTarget(target);
        }
    }

    @SubscribeEvent
    public static void damage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (victim instanceof ServerPlayer owner && event.getSource().getEntity() instanceof LivingEntity attacker) rally(owner, attacker);
        if (event.getSource().getEntity() instanceof ServerPlayer owner) rally(owner, victim);
    }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof LivingEntity target) || !isMinion(event.getEntity())) return;
        ServerPlayer owner = owner(event.getEntity());
        boolean ownerSide = owner != null ? RelicTargets.isAlly(owner, target)
                : target instanceof Player p && p.getUUID().toString().equals(event.getEntity().getPersistentData().getStringOr(RelicTargets.MINION_TAG, ""));
        if (ownerSide) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (isMinion(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void xp(LivingExperienceDropEvent event) {
        if (isMinion(event.getEntity())) event.setCanceled(true);
    }
}
