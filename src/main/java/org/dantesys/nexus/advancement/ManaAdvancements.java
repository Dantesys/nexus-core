package org.dantesys.nexus.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.magic.MagicStatus;

/** Concede os avanços ligados aos 4 status de mana definidos na Bola de Cristal. */
public final class ManaAdvancements {
    private static final int RANK_S = 850;

    private ManaAdvancements() {
    }

    public static void check(ServerPlayer player, MagicStatus status) {
        if (!status.isCreated()) {
            return;
        }
        grant(player, "despertar_da_mana");

        boolean capacidadeS = status.getMaxMana() >= RANK_S;
        boolean regeneracaoS = status.getRegeneracao() >= RANK_S;
        boolean afinidadeS = status.getAfinidade() >= RANK_S;
        boolean ressonanciaS = status.getRessonancia() >= RANK_S;

        if (afinidadeS) grant(player, "afinidade_absoluta");
        if (ressonanciaS) grant(player, "ressonancia_perfeita");
        if (capacidadeS && regeneracaoS && afinidadeS && ressonanciaS) grant(player, "um_com_o_nexus");
        if (status.getMaxMana() >= MagicStatus.MAX_STATUS
                && status.getRegeneracao() >= MagicStatus.MAX_STATUS
                && status.getAfinidade() >= MagicStatus.MAX_STATUS
                && status.getRessonancia() >= MagicStatus.MAX_STATUS) {
            grant(player, "mestre_nexus");
        }
    }

    private static void grant(ServerPlayer player, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Nexus.MODID, path);
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "conquistado");
        }
    }
}
