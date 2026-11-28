package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Voltrix: martelo de guerra de uma mao com runas eletricas. */
public final class VoltrixItem extends RelicItem {
    public VoltrixItem(Properties properties) {
        super(properties.sword(ModToolMaterial.RELIC, 5.0F, -2.9F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.voltrix.alta_voltagem"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.voltrix.golpe_em_cadeia"), VoltrixEffects.CHAIN_MANA, VoltrixEffects::armChain),
                RelicSkill.active(Component.translatable("relic.nexus.voltrix.eletrificar"), VoltrixEffects.ELECTRIFY_MANA, VoltrixEffects::electrify),
                RelicSkill.active(Component.translatable("relic.nexus.voltrix.chamado_da_tempestade"), VoltrixEffects.STORM_MANA, VoltrixEffects::storm),
                new RelicSkill(Component.translatable("relic.nexus.voltrix.furia_do_trovao_2"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.voltrix.estar_com_a_alta_voltagem"),
                        VoltrixEffects::canFury, VoltrixEffects::fury, VoltrixEffects.FURY_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.voltrix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> VoltrixEffects.CHAIN_COOLDOWN;
            case 2 -> VoltrixEffects.ELECTRIFY_COOLDOWN;
            case 3 -> VoltrixEffects.STORM_COOLDOWN;
            case 4 -> VoltrixEffects.FURY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
