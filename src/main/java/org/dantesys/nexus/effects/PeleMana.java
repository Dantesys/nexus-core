package org.dantesys.nexus.effects;

import net.minecraft.world.scores.TeamColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;

public class PeleMana extends MobEffect {
    public PeleMana(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            applyGlow(player, status.getSelectedMana());
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        if (entity instanceof ServerPlayer player) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            applyGlow(player, status.getSelectedMana());
        }
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            applyGlow(player, status.getSelectedMana());
        }
    }

    public static void applyGlow(ServerPlayer player, Elementos elemento) {
        Scoreboard scoreboard = player.level().getScoreboard();
        PlayerTeam team = getOrCreateManaSkinTeam(scoreboard, elemento);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        player.setGlowingTag(true);
    }

    public static void clearGlow(ServerPlayer player) {
        player.setGlowingTag(false);
        PlayerTeam team = player.getTeam();
        if (team != null && team.getName().startsWith("nx_mana_")) {
            player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
        }
    }

    private static PlayerTeam getOrCreateManaSkinTeam(Scoreboard scoreboard, Elementos elemento) {
        String teamName = manaSkinTeamName(elemento);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        team.setColor(java.util.Optional.of(manaSkinColor(elemento)));
        return team;
    }

    private static String manaSkinTeamName(Elementos elemento) {
        return switch (elemento) {
            case TEMPESTADE -> "nx_mana_storm";
            case SOMBRA -> "nx_mana_shadow";
            case IGNEA -> "nx_mana_fire";
            case LUZ -> "nx_mana_light";
            case VIDA -> "nx_mana_life";
            case METAL -> "nx_mana_metal";
            case NAO_ELEMENTAL -> "nx_mana_arcane";
        };
    }

    private static TeamColor manaSkinColor(Elementos elemento) {
        return switch (elemento) {
            case TEMPESTADE -> TeamColor.AQUA;
            case SOMBRA -> TeamColor.DARK_PURPLE;
            case IGNEA -> TeamColor.RED;
            case LUZ -> TeamColor.YELLOW;
            case VIDA -> TeamColor.GREEN;
            case METAL -> TeamColor.GRAY;
            case NAO_ELEMENTAL -> TeamColor.LIGHT_PURPLE;
        };
    }
}
