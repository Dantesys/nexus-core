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
                RelicSkill.passive(Component.translatable("relic.nexus.elixy.negacao"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.elixy.pocao_do_louco"), ElixyEffects.MAD_MANA, ElixyEffects::madPotion),
                RelicSkill.active(Component.translatable("relic.nexus.elixy.criador_de_pocao"), ElixyEffects.CREATE_MANA, ElixyEffects::createPotion),
                RelicSkill.active(Component.translatable("relic.nexus.elixy.ouro_dos_tolos"), ElixyEffects.GOLD_MANA, ElixyEffects::foolsGold),
                new RelicSkill(Component.translatable("relic.nexus.elixy.transmutacao"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.elixy.2_pocoes_de_efeitos_diferentes"),
                        ElixyEffects::canTransmute, ElixyEffects::transmute, ElixyEffects.TRANSMUTE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.elixy.skill." + slot + ".desc");
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
