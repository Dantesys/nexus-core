package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

/** Cryoni: ampulheta de gelo (gelo e tempo). */
public final class CryoniItem extends RelicItem {
    public CryoniItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Sangue Criogênico"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Tempo Lento"), CryoniEffects.SLOW_MANA, CryoniEffects::slowTime),
                new RelicSkill(Component.literal("Oh o Reverse"), RelicSkill.Kind.ACTIVE, Component.literal("Precisa de 10s de histórico na mesma dimensão."),
                        CryoniEffects::canReverse, CryoniEffects::reverse, CryoniEffects.REVERSE_MANA),
                RelicSkill.active(Component.literal("Zona Congelada"), CryoniEffects.FREEZE_MANA, CryoniEffects::frozenZone),
                new RelicSkill(Component.literal("Zero Absoluto"), RelicSkill.Kind.ACTIVE, Component.literal("1 stack de gelo (gasto)."),
                        CryoniEffects::canZero, CryoniEffects::absoluteZero, CryoniEffects.ZERO_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Imune a fogo, lava e frio. Inimigos a 1,5 bloco de você congelam (como na neve em pó) em 2s.";
            case 1 -> "Campo de 5 blocos por 10s onde tudo fica a 50% da velocidade (bosses 30%).";
            case 2 -> "Volta 10 segundos no tempo: posição, vida, fome e efeitos daquele momento.";
            case 3 -> "Por 5s, o tempo para num raio de 20: só você se move. Seus golpes acumulam e caem todos no fim.";
            case 4 -> "5 ondas congelantes até 30 blocos: 3 ondas congelam por 20s; as 5 viram estátua de gelo (morte garantida).";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> CryoniEffects.SLOW_COOLDOWN;
            case 2 -> CryoniEffects.REVERSE_COOLDOWN;
            case 3 -> CryoniEffects.FREEZE_COOLDOWN;
            case 4 -> CryoniEffects.ZERO_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
