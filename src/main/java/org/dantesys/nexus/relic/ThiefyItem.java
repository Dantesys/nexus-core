package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.util.MatrizModos;

/** Thiefy: luva do ladrao. */
public final class ThiefyItem extends RelicItem {
    public ThiefyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Roubo"), 0, (p, s, c) -> false),
                new RelicSkill(Component.literal("Na Surdina"), RelicSkill.Kind.ACTIVE, Component.literal("Olhe para um jogador a até 3 blocos."),
                        ThiefyEffects::canSneakSteal, ThiefyEffects::sneakSteal, ThiefyEffects.SNEAK_MANA),
                RelicSkill.active(Component.literal("Clone"), ThiefyEffects.CLONE_MANA, ThiefyEffects::clone),
                RelicSkill.active(Component.literal("Silêncio"), ThiefyEffects.SILENCE_MANA, ThiefyEffects::silence),
                new RelicSkill(Component.literal("O Grande Roubo"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Inventário vazio, incluindo a armadura (relíquias não contam)."),
                        ThiefyEffects::canHeist, ThiefyEffects::heist, ThiefyEffects.HEIST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Ao bater, chance (10% a 30%) de roubar um stack do alvo; fica um papel no lugar. Sem armadura, você fica invisível.";
            case 1 -> "Abre o inventário de um jogador a até 3 blocos por 5s, sem ele ver, e você rouba 1 item.";
            case 2 -> "Cria um clone seu que atrai os inimigos; você fica totalmente invisível por 10s ou até atacar.";
            case 3 -> "Por 5 min, você não faz som nem vibração (passa despercebido pelo Warden e pelo sculk).";
            case 4 -> "Lista jogadores e baús numa área de 20 blocos; escolha um e roube tudo dele (exceto relíquias).";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        boolean silent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(ThiefyEffects.SILENCE_TAG, false);
        int ticks = switch (slot) {
            case 1 -> ThiefyEffects.SNEAK_COOLDOWN;
            case 2 -> ThiefyEffects.CLONE_COOLDOWN;
            case 3 -> silent ? 0 : ThiefyEffects.SILENCE_COOLDOWN;
            case 4 -> ThiefyEffects.HEIST_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
