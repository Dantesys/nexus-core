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
                RelicSkill.passive(Component.literal("Eco Vivo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Amplificador"), SculkyEffects.AMP_MANA, SculkyEffects::amplify),
                RelicSkill.active(Component.literal("Pressão Sonora"), SculkyEffects.PRESSURE_MANA, SculkyEffects::pressure),
                RelicSkill.active(Component.literal("Passo Sculk"), SculkyEffects.STEP_MANA, SculkyEffects::sculkStep),
                new RelicSkill(Component.literal("Sonic Boom"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Amplificador ativo e Pressão Sonora e Passo Sculk prontos."),
                        SculkyEffects::canBoom, SculkyEffects::sonicBoom, SculkyEffects.BOOM_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Na mão: o Warden te ignora e o Sonic Boom não fere; você vê as vibrações num raio de 16 (contorno de quem as fez).";
            case 1 -> "Por 1 minuto, melhora todas as habilidades e a passiva da relíquia.";
            case 2 -> "Por 10s, batidas de grave em volta: brilho, dano, empurrão, náusea e interrompe quem come ou mira.";
            case 3 -> "Cria uma mancha de sculk por 1 min. Em cima de sculk, teleporta para o sculk da mesma mancha para onde olha.";
            case 4 -> "Dispara um Sonic Boom de 30 blocos que atravessa tudo: mata mobs comuns e fere muito jogadores e bosses.";
            default -> "";
        });
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
