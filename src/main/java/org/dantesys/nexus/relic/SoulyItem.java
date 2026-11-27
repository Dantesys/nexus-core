package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Souly: foice do ceifador. Combate ligado a morte e a colheita de almas. */
public final class SoulyItem extends RelicItem {
    public SoulyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 5.0F, -2.8F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Totem por Totem"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Medo Mortal"), SoulyEffects.FEAR_MANA, SoulyEffects::fear),
                RelicSkill.active(Component.literal("Consumo de Vida"), SoulyEffects.CONSUME_MANA, SoulyEffects::consume),
                RelicSkill.passive(Component.literal("Soul Coletor"), 0, (p, s, c) -> false),
                new RelicSkill(Component.literal("Soul Eater"), RelicSkill.Kind.ACTIVE,
                        Component.literal("1 totem no inventário e um jogador ou boss inimigo a até 200 blocos."),
                        SoulyEffects::canEat, SoulyEffects::eat, SoulyEffects.EATER_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Com totem no inventário, seu golpe mortal anula o totem do alvo (inclusive Eterny), gastando um de cada lado. "
                    + "Seus totens ativam do inventário, mas você não usa totem na mão. 10% de chance de totem ao matar jogador ou boss.";
            case 1 -> "Escuridão e Fraqueza em volta. Por até 1 min, cada golpe rouba vida máxima do alvo, que vira sua (vira absorção no fim).";
            case 2 -> "Por 30s: −2 corações de vida máxima, Força II e Velocidade II; 25% do dano causado cura você.";
            case 3 -> "Marca um jogador ou boss por 5 min. Com totem (gasto): dano dobrado e cada golpe tira vida máxima. Se morrer para a Souly, vira totem.";
            case 4 -> "Gasta todos os totens marcando jogadores/bosses inimigos a até 200 blocos. Por 30s, agachar olhando um marcado o teleporta às costas dele.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> SoulyEffects.FEAR_COOLDOWN;
            case 2 -> SoulyEffects.CONSUME_COOLDOWN;
            case 4 -> SoulyEffects.EATER_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
