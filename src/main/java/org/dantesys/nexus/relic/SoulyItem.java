package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Souly: foice do ceifador. Combate ligado a morte e a colheita de almas. */
public final class SoulyItem extends RelicItem {
    public SoulyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.RELIC, 5.0F, -2.8F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.souly.totem_por_totem_2"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.souly.medo_mortal"), SoulyEffects.FEAR_MANA, SoulyEffects::fear),
                RelicSkill.active(Component.translatable("relic.nexus.souly.consumo_de_vida"), SoulyEffects.CONSUME_MANA, SoulyEffects::consume),
                RelicSkill.passive(Component.translatable("relic.nexus.souly.soul_coletor"), 0, (p, s, c) -> false),
                new RelicSkill(Component.translatable("relic.nexus.souly.soul_eater"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.souly.1_totem_no_inventario_e"),
                        SoulyEffects::canEat, SoulyEffects::eat, SoulyEffects.EATER_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.souly.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> SoulyEffects.FEAR_COOLDOWN;
            case 2 -> SoulyEffects.CONSUME_COOLDOWN;
            case 4 -> SoulyEffects.EATER_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
