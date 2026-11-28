package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Rifty: lanca espacial. */
public final class RiftyItem extends RelicItem {
    public RiftyItem(Properties properties) {
        super(properties.spear(ModToolMaterial.RELIC, 1.15F, 1.25F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.rifty.passo_dimensional"), RiftyEffects.STEP_MANA, RiftyEffects::step),
                RelicSkill.active(Component.translatable("relic.nexus.rifty.dobra_dimensional"), RiftyEffects.FOLD_MANA, RiftyEffects::fold),
                RelicSkill.active(Component.translatable("relic.nexus.rifty.dimensional_crash"), RiftyEffects.CRASH_MANA, RiftyEffects::crash),
                RelicSkill.active(Component.translatable("relic.nexus.rifty.subespaco"), RiftyEffects.SUBSPACE_MANA, RiftyEffects::subspace),
                new RelicSkill(Component.translatable("relic.nexus.rifty.black_hole"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.rifty.todas_as_outras_habilidades_da"),
                        RiftyEffects::canBlackHole, RiftyEffects::blackHole, RiftyEffects.HOLE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.rifty.skill." + slot + ".desc");
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
