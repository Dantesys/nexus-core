package org.dantesys.nexus.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Pele de Mana. O jogador brilha com o contorno padrão (branco), sem usar times do placar,
 * para não tirar ninguém do time do servidor.
 */
public class PeleMana extends MobEffect {
    public PeleMana(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            applyGlow(player);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            applyGlow(player);
        }
    }

    public static void applyGlow(ServerPlayer player) {
        if (!player.hasGlowingTag()) {
            player.setGlowingTag(true);
        }
    }

    public static void clearGlow(ServerPlayer player) {
        if (player.hasGlowingTag()) {
            player.setGlowingTag(false);
        }
        // Mundos antigos: tira o jogador dos times de cor que a Pele de Mana usava.
        PlayerTeam team = player.getTeam();
        if (team != null && team.getName().startsWith("nx_mana_")) {
            player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
        }
    }
}
