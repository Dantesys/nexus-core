package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

/** Cryoni: ampulheta de gelo (gelo e tempo). */
public final class CryoniItem extends RelicItem {
    public CryoniItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.cryoni.sangue_criogenico"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.cryoni.tempo_lento"), CryoniEffects.SLOW_MANA, CryoniEffects::slowTime),
                new RelicSkill(Component.translatable("relic.nexus.cryoni.oh_o_reverse"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.cryoni.precisa_de_10s_de_historico"),
                        CryoniEffects::canReverse, CryoniEffects::reverse, CryoniEffects.REVERSE_MANA),
                RelicSkill.active(Component.translatable("relic.nexus.cryoni.zona_congelada"), CryoniEffects.FREEZE_MANA, CryoniEffects::frozenZone),
                new RelicSkill(Component.translatable("relic.nexus.cryoni.zero_absoluto"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.cryoni.1_stack_de_gelo_gasto"),
                        CryoniEffects::canZero, CryoniEffects::absoluteZero, CryoniEffects.ZERO_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.cryoni.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> CryoniEffects.SLOW_COOLDOWN;
            case 2 -> CryoniEffects.REVERSE_COOLDOWN;
            case 3 -> CryoniEffects.FREEZE_COOLDOWN;
            case 4 -> CryoniEffects.ZERO_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
