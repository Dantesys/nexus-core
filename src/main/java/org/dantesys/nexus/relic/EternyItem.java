package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

/** Eterny: totem da imortalidade. Suporte e sobrevivencia. */
public final class EternyItem extends RelicItem {
    public EternyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Infinidade"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Campo de Vida"), EternyEffects.FIELD_MANA, EternyEffects::lifeField),
                RelicSkill.active(Component.literal("!Dano"), EternyEffects.NODAMAGE_MANA, EternyEffects::noDamage),
                RelicSkill.active(Component.literal("Vida Extra"), EternyEffects.EXTRA_MANA, EternyEffects::extraLifeCast),
                new RelicSkill(Component.literal("Explosão Vital"), RelicSkill.Kind.ACTIVE,
                        Component.literal("A Infinidade precisa estar em recarga."),
                        EternyEffects::canBurst, EternyEffects::burst, EternyEffects.BURST_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Na mão, funciona como um totem da imortalidade ilimitado, com recarga fixa de 2 minutos.";
            case 1 -> "Área de 5 blocos que te acompanha por 10s e cura você, seus pets e seu time. Vida cheia vira absorção.";
            case 2 -> "Área de 3 blocos por 5–10s: dano de mobs e jogadores nos aliados vira absorção (com teto).";
            case 3 -> "Vida Extra (10 min) em você, aliados e pets num raio de 10: sobrevivem a uma morte.";
            case 4 -> "Explode empurrando os inimigos; por 1 min fica com metade do tamanho, velocidade e regeneração, mas metade da vida máxima.";
            default -> "";
        });
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
