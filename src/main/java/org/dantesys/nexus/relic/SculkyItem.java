package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.util.MatrizModos;

/** Sculky: chifre de cabra de sculk. Som, vibracoes, Warden e eco. */
public final class SculkyItem extends RelicItem {
    public SculkyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.sculky.eco_vivo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.sculky.amplificador"), SculkyEffects.AMP_MANA, SculkyEffects::amplify),
                RelicSkill.active(Component.translatable("relic.nexus.sculky.pressao_sonora"), SculkyEffects.PRESSURE_MANA, SculkyEffects::pressure),
                RelicSkill.active(Component.translatable("relic.nexus.sculky.passo_sculk"), SculkyEffects.STEP_MANA, SculkyEffects::sculkStep),
                new RelicSkill(Component.translatable("relic.nexus.sculky.sonic_boom"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.sculky.amplificador_ativo_e_pressao_sonora"),
                        SculkyEffects::canBoom, SculkyEffects::sonicBoom, SculkyEffects.BOOM_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.sculky.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean amp = tag.getBooleanOr(SculkyEffects.AMP_TAG, false);
        int ticks = switch (slot) {
            case 1 -> amp ? 0 : SculkyEffects.AMP_COOLDOWN;
            case 2 -> SculkyEffects.PRESSURE_COOLDOWN;
            case 3 -> amp ? SculkyEffects.STEP_COOLDOWN / 2 : SculkyEffects.STEP_COOLDOWN;
            case 4 -> SculkyEffects.BOOM_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
