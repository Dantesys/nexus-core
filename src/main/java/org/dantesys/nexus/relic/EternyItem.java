package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

/** Eterny: totem da imortalidade. Suporte e sobrevivencia. */
public final class EternyItem extends RelicItem {
    public EternyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.eterny.infinidade"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.eterny.campo_de_vida"), EternyEffects.FIELD_MANA, EternyEffects::lifeField),
                RelicSkill.active(Component.translatable("relic.nexus.eterny.dano"), EternyEffects.NODAMAGE_MANA, EternyEffects::noDamage),
                RelicSkill.active(Component.translatable("relic.nexus.eterny.vida_extra"), EternyEffects.EXTRA_MANA, EternyEffects::extraLifeCast),
                new RelicSkill(Component.translatable("relic.nexus.eterny.explosao_vital"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.eterny.a_infinidade_precisa_estar_em"),
                        EternyEffects::canBurst, EternyEffects::burst, EternyEffects.BURST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.eterny.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> EternyEffects.FIELD_COOLDOWN;
            case 2 -> EternyEffects.NODAMAGE_COOLDOWN;
            case 3 -> EternyEffects.EXTRA_COOLDOWN;
            case 4 -> EternyEffects.BURST_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    @Override public Component hudInfo(ItemStack stack) {
        return null;
    }
}
