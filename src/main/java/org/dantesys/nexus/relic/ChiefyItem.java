package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Chiefy: faca do cozinheiro. */
public final class ChiefyItem extends RelicItem {
    public ChiefyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.RELIC, 3.0F, -2.0F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.chiefy.lamina_em_chamas"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.chiefy.pinhata"), ChiefyEffects.PINATA_MANA, ChiefyEffects::pinata),
                RelicSkill.active(Component.translatable("relic.nexus.chiefy.mestre_cuca"), ChiefyEffects.SEASON_MANA, ChiefyEffects::season),
                RelicSkill.active(Component.translatable("relic.nexus.chiefy.tempestade_culinaria"), ChiefyEffects.STORM_MANA, ChiefyEffects::foodStorm),
                new RelicSkill(Component.translatable("relic.nexus.chiefy.banquete"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.chiefy.2_stacks_de_comida_no"),
                        ChiefyEffects::canFeast, ChiefyEffects::feast, ChiefyEffects.FEAST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.chiefy.skill." + slot + ".desc");
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
