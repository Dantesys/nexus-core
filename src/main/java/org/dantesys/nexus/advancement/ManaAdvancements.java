package org.dantesys.nexus.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.magic.ElementalStatus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;

import java.util.EnumMap;
import java.util.Map;

/** Concede os avanços ligados aos valores elementais definidos na Bola de Cristal. */
public final class ManaAdvancements {
    private static final int RANK_S = 850;
    private static final int PERFECT_VALUE = 1000;
    private static final Elementos[] ELEMENTAL_MANA = {
            Elementos.IGNEA, Elementos.VIDA, Elementos.LUZ,
            Elementos.SOMBRA, Elementos.METAL, Elementos.TEMPESTADE
    };
    private static final Map<Elementos, String> IDS = new EnumMap<>(Elementos.class);

    static {
        IDS.put(Elementos.IGNEA, "ignea");
        IDS.put(Elementos.VIDA, "vida");
        IDS.put(Elementos.LUZ, "luz");
        IDS.put(Elementos.SOMBRA, "sombria");
        IDS.put(Elementos.METAL, "metal");
        IDS.put(Elementos.TEMPESTADE, "tempestade");
    }

    private ManaAdvancements() {
    }

    public static void check(ServerPlayer player, MagicStatus status) {
        boolean allUnlocked = true;
        boolean allAffinityS = true;
        boolean allResonanceS = true;
        boolean allPerfect = true;

        for (Elementos elemento : ELEMENTAL_MANA) {
            ElementalStatus elemental = status.get(elemento);
            allUnlocked &= elemental.isUnlocked();
            allAffinityS &= elemental.isUnlocked() && elemental.getAfinidade() >= RANK_S;
            allResonanceS &= elemental.isUnlocked() && elemental.getRessonancia() >= RANK_S;
            boolean perfect = elemental.isUnlocked()
                    && elemental.getAfinidade() >= PERFECT_VALUE
                    && elemental.getRessonancia() >= PERFECT_VALUE;
            allPerfect &= perfect;

            if (elemental.isUnlocked() && elemental.getAfinidade() >= RANK_S) {
                grant(player, "afinidade_" + IDS.get(elemento));
            }
            if (elemental.isUnlocked() && elemental.getRessonancia() >= RANK_S) {
                grant(player, "ressonancia_" + IDS.get(elemento));
            }
            if (perfect) {
                grant(player, "encarnacao_" + IDS.get(elemento));
            }
        }

        if (allUnlocked) grant(player, "todas_as_faces_da_mana");
        if (allAffinityS) grant(player, "afinidade_absoluta");
        if (allResonanceS) grant(player, "ressonancia_perfeita");
        if (allAffinityS && allResonanceS) grant(player, "um_com_o_nexus");
        if (allPerfect) grant(player, "mestre_nexus");
    }

    private static void grant(ServerPlayer player, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Nexus.MODID, path);
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "conquistado");
        }
    }
}
