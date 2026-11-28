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
                RelicSkill.passive(Component.translatable("relic.nexus.thiefy.roubo"), 0, (p, s, c) -> false),
                new RelicSkill(Component.translatable("relic.nexus.thiefy.na_surdina"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.thiefy.olhe_para_um_jogador_a"),
                        ThiefyEffects::canSneakSteal, ThiefyEffects::sneakSteal, ThiefyEffects.SNEAK_MANA),
                RelicSkill.active(Component.translatable("relic.nexus.thiefy.clone"), ThiefyEffects.CLONE_MANA, ThiefyEffects::clone),
                RelicSkill.active(Component.translatable("relic.nexus.thiefy.silencio"), ThiefyEffects.SILENCE_MANA, ThiefyEffects::silence),
                new RelicSkill(Component.translatable("relic.nexus.thiefy.o_grande_roubo"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.thiefy.inventario_vazio_incluindo_a_armadura"),
                        ThiefyEffects::canHeist, ThiefyEffects::heist, ThiefyEffects.HEIST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.thiefy.skill." + slot + ".desc");
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
