package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Voltrix: martelo de guerra de uma mao com runas eletricas. */
public final class VoltrixItem extends RelicItem {
    public VoltrixItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 5.0F, -2.9F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Alta Voltagem"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Golpe em Cadeia"), VoltrixEffects.CHAIN_MANA, VoltrixEffects::armChain),
                RelicSkill.active(Component.literal("Eletrificar"), VoltrixEffects.ELECTRIFY_MANA, VoltrixEffects::electrify),
                RelicSkill.active(Component.literal("Chamado da Tempestade"), VoltrixEffects.STORM_MANA, VoltrixEffects::storm),
                new RelicSkill(Component.literal("Fúria do Trovão"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Estar com a Alta Voltagem ativa (Força de um raio recente)."),
                        VoltrixEffects::canFury, VoltrixEffects::fury, VoltrixEffects.FURY_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Raios não te ferem e dão Força II por 5s (inclusive raios de outras relíquias).";
            case 1 -> "O próximo golpe solta um raio que salta entre os inimigos (3 blocos por salto), ferindo e paralisando.";
            case 2 -> "Por 30s, golpes que você dá ou recebe paralisam o outro lado por 1s.";
            case 3 -> "Por 20s, chuva de raios numa área de 10 blocos (pode acertar aliados).";
            case 4 -> "Por 30s, todo golpe faz cair um raio no alvo, como dano extra.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> VoltrixEffects.CHAIN_COOLDOWN;
            case 2 -> VoltrixEffects.ELECTRIFY_COOLDOWN;
            case 3 -> VoltrixEffects.STORM_COOLDOWN;
            case 4 -> VoltrixEffects.FURY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
