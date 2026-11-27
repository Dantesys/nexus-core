package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Chiefy: faca do cozinheiro. */
public final class ChiefyItem extends RelicItem {
    public ChiefyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0F, -2.0F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Lâmina em Chamas"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Pinhata"), ChiefyEffects.PINATA_MANA, ChiefyEffects::pinata),
                RelicSkill.active(Component.literal("Mestre Cuca"), ChiefyEffects.SEASON_MANA, ChiefyEffects::season),
                RelicSkill.active(Component.literal("Tempestade Culinária"), ChiefyEffects.STORM_MANA, ChiefyEffects::foodStorm),
                new RelicSkill(Component.literal("Banquete"), RelicSkill.Kind.ACTIVE,
                        Component.literal("2 stacks de comida no inventário (gastos)."),
                        ChiefyEffects::canFeast, ChiefyEffects::feast, ChiefyEffects.FEAST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Comida dropada por mobs que você mata já vem cozida e melhorada (+50% de fome e saturação).";
            case 1 -> "O mob atingido vira uma pinhata de comida. Em jogadores e bosses, só dano (jogadores perdem fome).";
            case 2 -> "Tempera até 16 comidas do inventário: cada tipo dá um efeito bom por 30s ao ser comido.";
            case 3 -> "Por 15s, chove comida em quem estiver na área. Inimigos que comem demais (4 acertos) explodem.";
            case 4 -> "Por 30s, área de 5 blocos: inimigos enchem um contador e, cheio, comem até explodir. Aliados ficam saciados.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> ChiefyEffects.PINATA_COOLDOWN;
            case 2 -> ChiefyEffects.SEASON_COOLDOWN;
            case 3 -> ChiefyEffects.STORM_COOLDOWN;
            case 4 -> ChiefyEffects.FEAST_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
