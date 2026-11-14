package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Rifty: lanca espacial. */
public final class RiftyItem extends RelicItem {
    public RiftyItem(Properties properties) {
        super(properties.spear(ModToolMaterial.STEEL_MATERIA, 0.85F, 0.82F, 0.65F, 4.0F, 9.0F, 8.25F, 5.1F, 12.5F, 4.6F),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Passo Dimensional"), RiftyEffects.STEP_MANA, RiftyEffects::step),
                RelicSkill.active(Component.literal("Dobra Dimensional"), RiftyEffects.FOLD_MANA, RiftyEffects::fold),
                RelicSkill.active(Component.literal("Dimensional Crash"), RiftyEffects.CRASH_MANA, RiftyEffects::crash),
                RelicSkill.active(Component.literal("Subespaço"), RiftyEffects.SUBSPACE_MANA, RiftyEffects::subspace),
                new RelicSkill(Component.literal("Black Hole"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Todas as outras habilidades da Rifty precisam estar em recarga."),
                        RiftyEffects::canBlackHole, RiftyEffects::blackHole, RiftyEffects.HOLE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Atacar sem alvo teleporta até 5 blocos para onde você olha (para antes de blocos). "
                    + "Com um inimigo a até 4 blocos, mira o golpe nele em vez de teleportar.";
            case 1 -> "Rasga uma linha de até 15 blocos: dano cheio em todos os inimigos no caminho e você reaparece no fim.";
            case 2 -> "Quebra a dimensão em cima do alvo (até 15 blocos): ~45% da vida máxima (metade mágico, metade void). "
                    + "Executa mobs com 50% de vida ou menos (jogadores: 25%). Bosses nunca são executados.";
            case 3 -> "Até 20s fora da realidade: atravessa blocos como andaimes (pule para subir, agache para descer), "
                    + "imune a dano físico. Não interage com nada; atacar encerra. Gasta mana enquanto ativo.";
            case 4 -> "Buraco negro por 15s que puxa inimigos, itens e XP. O raio cresce a cada captura (10 → 100). "
                    + "Com mais de 10 capturas, explode no fim.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 0 -> RiftyEffects.STEP_COOLDOWN;
            case 1 -> RiftyEffects.FOLD_COOLDOWN;
            case 2 -> RiftyEffects.CRASH_COOLDOWN;
            case 3 -> RiftyEffects.SUBSPACE_COOLDOWN;
            case 4 -> RiftyEffects.HOLE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
