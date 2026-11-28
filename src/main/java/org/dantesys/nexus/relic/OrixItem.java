package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Orix: picareta de mineracao, focada em minerios (metade do antigo Mineiro do plugin). */
public final class OrixItem extends RelicItem {
    public OrixItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterial.RELIC, 1.0F, -2.8F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.orix.minerar_tudo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.orix.sonar"), OrixEffects.SONAR_MANA, OrixEffects::sonar),
                RelicSkill.active(Component.translatable("relic.nexus.orix.toque_de_midas"), OrixEffects.MIDAS_MANA, OrixEffects::midas),
                RelicSkill.active(Component.translatable("relic.nexus.orix.mineradinha_off"), OrixEffects.MINING_OFF_MANA, OrixEffects::miningOff),
                new RelicSkill(Component.translatable("relic.nexus.orix.all_in"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.orix.ressonancia_rank_a_ou_maior"),
                        OrixEffects::canAllIn, OrixEffects::allIn, OrixEffects.ALL_IN_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.orix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> OrixEffects.SONAR_COOLDOWN;
            case 2 -> OrixEffects.MIDAS_COOLDOWN;
            case 3 -> OrixEffects.MINING_OFF_COOLDOWN;
            case 4 -> OrixEffects.ALL_IN_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
