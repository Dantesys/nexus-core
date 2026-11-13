package org.dantesys.nexus.relic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/** Quem conta como aliado das reliquias: o dono, os pets dele e o mesmo time. Jogadores so com PvP. */
public final class RelicTargets {
    private RelicTargets() {}

    public static boolean isAlly(ServerPlayer owner, LivingEntity target) {
        if (target == owner || owner.isAlliedTo(target)) return true;
        return target instanceof OwnableEntity pet && pet.getOwner() == owner;
    }

    public static boolean canHit(ServerPlayer owner, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || target instanceof ArmorStand) return false;
        if (isAlly(owner, target)) return false;
        if (target instanceof Player other) return !other.isCreative() && owner.canHarmPlayer(other);
        return true;
    }
}
