package org.dantesys.nexus.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
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

public class NexusPlayerEvents {
    private static final int LOW_MANA_NAUSEA_THRESHOLD_PERCENT = 10;
    /** Custo por segundo da Pele de Mana: 100, caindo até 50 com ressonância 1000. */
    private static final int MANA_SKIN_BASE_COST = 100;

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
        if (updateManaSkinEffect(player, status)) {
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        boolean changed = status.regenerarMana();
        if (status.isManaSkinActive()) {
            if (!status.useMana(getManaSkinCost(status))) {
                status.setManaSkinActive(false);
                updateManaSkinEffect(player, status);
            }
            changed = true;
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

    /** Devolve true se a Pele de Mana foi desligada aqui (o cliente precisa saber). */
    private static boolean updateManaSkinEffect(ServerPlayer player, MagicStatus status) {
        if (!status.isManaSkinActive() || status.getMana() <= 0) {
            boolean desligou = status.isManaSkinActive();
            status.setManaSkinActive(false);
            player.removeEffect(ModEffects.PELE_MANA);
            PeleMana.clearGlow(player);
            return desligou;
        }

        PeleMana.applyGlow(player);
        player.addEffect(new MobEffectInstance(ModEffects.PELE_MANA, 40, 0, true, false, true));
        return false;
    }

    private static int getManaSkinCost(MagicStatus status) {
        return MANA_SKIN_BASE_COST - MANA_SKIN_BASE_COST / 2 * status.getRessonancia() / MagicStatus.MAX_STATUS;
    }
}
