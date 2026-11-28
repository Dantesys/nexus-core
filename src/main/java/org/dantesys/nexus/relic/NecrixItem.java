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
                RelicSkill.passive(Component.translatable("relic.nexus.necrix.aliado_dos_mortos"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.necrix.guerreiros_osseos"), NecrixEffects.WARRIORS_MANA, NecrixEffects::warriors),
                RelicSkill.active(Component.translatable("relic.nexus.necrix.zona_ossea"), NecrixEffects.ZONE_MANA, NecrixEffects::boneZone),
                new RelicSkill(Component.translatable("relic.nexus.necrix.a_elite_ossea"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.necrix.so_um_mini_wither_por"),
                        NecrixEffects::canElite, NecrixEffects::elite, NecrixEffects.ELITE_MANA),
                new RelicSkill(Component.translatable("relic.nexus.necrix.exercito_de_ossos"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.necrix.1_stack_de_ossos_e"),
                        NecrixEffects::canArmy, NecrixEffects::army, NecrixEffects.ARMY_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.necrix.skill." + slot + ".desc");
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
