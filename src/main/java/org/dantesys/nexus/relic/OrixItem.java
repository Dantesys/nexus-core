package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Orix: picareta de mineracao, focada em minerios (metade do antigo Mineiro do plugin). */
public final class OrixItem extends RelicItem {
    public OrixItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterial.STEEL_MATERIA, 1.0F, -2.8F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Minerar Tudo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Sonar"), OrixEffects.SONAR_MANA, OrixEffects::sonar),
                RelicSkill.active(Component.literal("Toque de Midas"), OrixEffects.MIDAS_MANA, OrixEffects::midas),
                RelicSkill.active(Component.literal("Mineradinha Off"), OrixEffects.MINING_OFF_MANA, OrixEffects::miningOff),
                new RelicSkill(Component.literal("All In"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Ressonância rank A ou maior e pelo menos 64 minérios brutos ou gemas no inventário."),
                        OrixEffects::canAllIn, OrixEffects::allIn, OrixEffects.ALL_IN_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Pedra, ardósia e pedra negra quebradas, e mobs hostis mortos, podem gerar minério bruto. "
                    + "Chance e raridade crescem com a maestria; bosses sempre dropam.";
            case 1 -> "Destaca os minérios em volta (10×10×10, cresce com a Afinidade) através das paredes, só para você.";
            case 2 -> "Transforma o bloco tocado: pedra vira minério; minério vira bloco bruto ou o próximo da escala.";
            case 3 -> "Quebra todos os minérios em 10×10×10 à sua volta e manda os drops para o inventário.";
            case 4 -> "Transforma o redor em minérios a partir do inventário e dá pressa, velocidade e fortuna por 60s. "
                    + "O que não for minerado volta ao normal.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> OrixEffects.SONAR_COOLDOWN;
            case 2 -> OrixEffects.MIDAS_COOLDOWN;
            case 3 -> OrixEffects.MINING_OFF_COOLDOWN;
            case 4 -> OrixEffects.ALL_IN_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
