package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.util.MatrizModos;

/** Necrix: cajado do necromante. */
public final class NecrixItem extends RelicItem {
    public NecrixItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Aliado dos Mortos"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Guerreiros Ósseos"), NecrixEffects.WARRIORS_MANA, NecrixEffects::warriors),
                RelicSkill.active(Component.literal("Zona Óssea"), NecrixEffects.ZONE_MANA, NecrixEffects::boneZone),
                new RelicSkill(Component.literal("A Elite Óssea"), RelicSkill.Kind.ACTIVE, Component.literal("Só um mini Wither por vez."),
                        NecrixEffects::canElite, NecrixEffects::elite, NecrixEffects.ELITE_MANA),
                new RelicSkill(Component.literal("Exército de Ossos"), RelicSkill.Kind.ACTIVE,
                        Component.literal("1 stack de ossos e 3 cabeças de esqueleto Wither (gastos)."),
                        NecrixEffects::canArmy, NecrixEffects::army, NecrixEffects.ARMY_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Zumbis e esqueletos (e variantes) não te atacam, a não ser que você ataque primeiro.";
            case 1 -> "Invoca 5 esqueletos guerreiros (pets) que melhoram com a maestria. Usar de novo apaga os antigos.";
            case 2 -> "Campo de 10 blocos por 10s: dano por segundo; quem morre nele vira guarda esqueleto seu. Quem sai leva Wither.";
            case 3 -> "Invoca um mini Wither por até 20s que ataca os inimigos com caveiras. Recarga conta da morte dele.";
            case 4 -> "Exército imortal de ~20 esqueletos por 30s (quem cai é substituído), com um mini Wither.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        boolean elite = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(NecrixEffects.ELITE_TAG, false);
        int ticks = switch (slot) {
            case 1 -> NecrixEffects.WARRIORS_COOLDOWN;
            case 2 -> NecrixEffects.ZONE_COOLDOWN;
            case 3 -> elite ? 0 : NecrixEffects.ELITE_COOLDOWN;
            case 4 -> NecrixEffects.ARMY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
