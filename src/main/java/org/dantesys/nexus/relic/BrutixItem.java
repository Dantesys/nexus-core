package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Brutix: machado do barbaro. Quanto mais apanha, mais forte fica. */
public final class BrutixItem extends RelicItem {
    public BrutixItem(Properties properties) {
        super(properties.axe(ModToolMaterial.STEEL_MATERIA, 6.0F, -3.1F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Fúria"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Boomerang"), BrutixEffects.BOOMERANG_MANA, BrutixEffects::boomerang),
                RelicSkill.active(Component.literal("Giro Destruidor"), BrutixEffects.SPIN_MANA, BrutixEffects::spin),
                RelicSkill.active(Component.literal("Grito de Guerra"), BrutixEffects.ROAR_MANA, BrutixEffects::roar),
                new RelicSkill(Component.literal("Descarga de Fúria"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Fúria em pelo menos metade dos estágios máximos."),
                        BrutixEffects::canDischarge, BrutixEffects::discharge, BrutixEffects.DISCHARGE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Levar dano enche a fúria; cada barra cheia dá um estágio de força (3, até 5 com a maestria). "
                    + "Após ~10s sem dano, perde um estágio por vez.";
            case 1 -> "Arremessa o machado até 10 blocos (+2 por estágio de fúria); ele volta até você, cortando na ida e na volta.";
            case 2 -> "Gira por 3s acertando e empurrando quem está em volta. Gasta a fúria: mais estágios, mais dano e raio.";
            case 3 -> "Rugido que enfraquece e afasta os inimigos em volta (mais forte com a fúria, sem gastá-la).";
            case 4 -> "Corte diagonal brutal à frente: dano base alto + bônus por estágio gasto. Depois, fraqueza e fúria desligada por 10s.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> BrutixEffects.BOOMERANG_COOLDOWN;
            case 2 -> BrutixEffects.SPIN_COOLDOWN;
            case 3 -> BrutixEffects.ROAR_COOLDOWN;
            case 4 -> BrutixEffects.DISCHARGE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, estagios de furia, maximo, % da barra, machado em voo]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) BrutixEffects.stages(player), (float) BrutixEffects.maxStages(player),
                (float) Math.round(BrutixEffects.barPercent(player)), BrutixEffects.flying(player) ? 1f : 0f);
    }

    /** A furia aparece como barra propria no HUD (MagicHud.renderFury). */
    @Override public Component hudInfo(ItemStack stack) { return null; }
}
