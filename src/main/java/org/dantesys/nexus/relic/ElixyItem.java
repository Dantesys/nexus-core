package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import org.dantesys.nexus.util.MatrizModos;

/** Elixy: alquimista, a reliquia da pocao. */
public final class ElixyItem extends RelicItem {
    public ElixyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Negação"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Poção do Louco"), ElixyEffects.MAD_MANA, ElixyEffects::madPotion),
                RelicSkill.active(Component.literal("Criador de Poção"), ElixyEffects.CREATE_MANA, ElixyEffects::createPotion),
                RelicSkill.active(Component.literal("Ouro dos Tolos"), ElixyEffects.GOLD_MANA, ElixyEffects::foolsGold),
                new RelicSkill(Component.literal("Transmutação"), RelicSkill.Kind.ACTIVE,
                        Component.literal("2 poções de efeitos diferentes no inventário (gastas)."),
                        ElixyEffects::canTransmute, ElixyEffects::transmute, ElixyEffects.TRANSMUTE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Todo efeito negativo em você vira o contrário (mesmo nível e duração). Náusea é anulada.";
            case 1 -> "Arremessa uma poção com 1 a 5 efeitos negativos aleatórios. Recarga de 10s por efeito.";
            case 2 -> "Gera uma poção com um efeito positivo aleatório (nível III ou mais) por 1 minuto.";
            case 3 -> "Poção que transforma mobs sem dono em ouro. Em jogadores e bosses, dano de Dano Instantâneo I.";
            case 4 -> "Por 30s, área de 10 blocos: hostis viram pacíficos para sempre; inimigos perdem efeitos bons e ganham ruins a cada segundo.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> 200 * Math.max(1, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(ElixyEffects.COUNT_TAG, 1));
            case 2 -> ElixyEffects.CREATE_COOLDOWN;
            case 3 -> ElixyEffects.GOLD_COOLDOWN;
            case 4 -> ElixyEffects.TRANSMUTE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
