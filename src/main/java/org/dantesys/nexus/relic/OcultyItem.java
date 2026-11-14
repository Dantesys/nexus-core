package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Oculty: oculos do espiao. Usado na cabeca, no lugar do capacete. */
public final class OcultyItem extends RelicItem {
    public OcultyItem(Properties properties) {
        super(properties.equippable(EquipmentSlot.HEAD), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Modo Furtivo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Micro Agente"), OcultyEffects.MICRO_MANA, OcultyEffects::micro),
                RelicSkill.active(Component.literal("X-Ray"), OcultyEffects.XRAY_MANA, OcultyEffects::xray),
                RelicSkill.active(Component.literal("Plano F"), OcultyEffects.PLAN_MANA, OcultyEffects::planF),
                new RelicSkill(Component.literal("Imaterial Spy"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Nenhum mob ou jogador num raio de 5 blocos e pelo menos um num raio de 100."),
                        OcultyEffects::canSpy, OcultyEffects::spy, OcultyEffects.SPY_MANA));
    }

    /** Durante o Imaterial Spy, a tecla da reliquia abre o inventario do jogador que voce esta olhando. */
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (OcultyEffects.spying(player)) return OcultyEffects.peek(player);
        return super.activate(player, stack, slot);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Na cabeça: visão noturna. Agachado: invisível, gastando mana por segundo. Dano (levar ou causar) quebra; "
                    + "mobs perdem você de vista e brilhos não te revelam.";
            case 1 -> "Fica no tamanho mínimo por alguns segundos, com velocidade e super pulo; depois volta a crescer aos poucos.";
            case 2 -> "Marca mobs e jogadores em 15×15×15 com brilho só para você (hostis vermelho, pacíficos verde, jogadores azul).";
            case 3 -> "Bomba de fumaça: cega quem está perto e os mobs perdem o alvo. Você ganha um efeito sorteado (com fraqueza).";
            case 4 -> "Vira espectador por 30s, com o corpo parado no lugar. Volta antes se alguém chegar a 5 blocos do corpo. "
                    + "Durante o efeito, a tecla da relíquia mostra o inventário do jogador que você olha.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> OcultyEffects.MICRO_COOLDOWN;
            case 2 -> OcultyEffects.XRAY_COOLDOWN;
            case 3 -> OcultyEffects.PLAN_COOLDOWN;
            case 4 -> OcultyEffects.SPY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, proximo Plano F, segundos restantes do Micro Agente]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) OcultyEffects.nextPlan(stack), (float) OcultyEffects.microSecondsLeft(player));
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        return List.of(Component.literal("Próximo Plano F: " + OcultyEffects.PLAN_NAMES[(int) modelValue(stack, 1, 0)]));
    }

    @Override public Component hudInfo(ItemStack stack) {
        int seconds = (int) modelValue(stack, 2, 0);
        String plan = "Plano F: " + OcultyEffects.PLAN_NAMES[(int) modelValue(stack, 1, 0)];
        return Component.literal(seconds > 0 ? "Micro " + seconds + "s · " + plan : plan);
    }
}
