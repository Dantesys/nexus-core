package org.dantesys.nexus.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.ArrayList;
import java.util.List;

/** Krefty: martelo do Construtor. Reliquia de utilidade: a mana vira material de construcao. */
public final class KreftyItem extends RelicItem {
    public static final int SLOT_COPY_BLOCKS = 0, SLOT_BUILD_AREA = 1, SLOT_PERFECT_COPY = 2, SLOT_DEMOLITION = 3, SLOT_INSTANT = 4;

    public KreftyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Cópia de Blocos"), 0, KreftyEffects::placePassive),
                RelicSkill.active(Component.literal("Área de Construção"), KreftyRules.BUILD_AREA_MANA, KreftyEffects::buildArea),
                RelicSkill.active(Component.literal("Cópia \"Perfeita\""), KreftyRules.COPY_MANA, KreftyEffects::perfectCopy),
                RelicSkill.active(Component.literal("Área de Demolição"), KreftyRules.DEMOLITION_MANA, KreftyEffects::demolitionArea),
                new RelicSkill(Component.literal("Construção Instantânea"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Para erguer (copiar é livre): Capacidade rank " + KreftyRules.FINAL_CAPACITY_RANK
                                + " ou maior, nenhum bloco no inventário, vida, fome e mana cheias."),
                        KreftyEffects::canInstantBuild, KreftyEffects::instantBuild, 0));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Com um bloco na outra mão, o clique direito coloca o bloco sem consumi-lo, gastando mana. "
                    + "Blocos com inventário saem vazios.";
            case 1 -> "Marque dois pontos: por 60s a área barra a entrada de mobs e entidades. Quem estiver dentro leva tijoladas; "
                    + "jogadores de capacete não levam dano, mas o capacete se desgasta.";
            case 2 -> "Copia o bloco que você está olhando (até 5 blocos), com a direção. "
                    + "Depois ele é colocado sem precisar estar na outra mão. Mirar no ar apaga a cópia.";
            case 3 -> "Marque dois pontos: por 30s caem escombros. Mobs e jogadores não aliados levam dano, lentidão e escuridão.";
            case 4 -> "Sem planta na outra mão: marque dois pontos para gerar a planta (livre). "
                    + "Com a planta na outra mão: ergue a construção onde você mira, se a área estiver limpa.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case SLOT_BUILD_AREA -> KreftyRules.BUILD_AREA_COOLDOWN;
            case SLOT_PERFECT_COPY -> KreftyRules.COPY_COOLDOWN;
            case SLOT_DEMOLITION -> KreftyRules.DEMOLITION_COOLDOWN;
            case SLOT_INSTANT -> KreftyRules.FINAL_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    @Override protected int skillManaCost(ServerPlayer player, ItemStack stack, int slot) {
        return KreftyEffects.manaCost(player, stack, slot, super.skillManaCost(player, stack, slot));
    }

    @Override protected boolean isFreeUse(ServerPlayer player, ItemStack stack, int slot) {
        return KreftyEffects.isFreeUse(player, slot);
    }

    /** custom_model_data: [estagio, blocos que a mana atual permite colocar (-1 sem bloco)]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) KreftyEffects.affordableBlocks(player, stack));
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        String copied = KreftyEffects.copiedName(stack);
        if (!copied.isEmpty()) lines.add(Component.literal("Bloco copiado: ").append(Component.translatable(copied))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return lines;
    }

    @Override public Component hudInfo(ItemStack stack) {
        int blocks = (int) modelValue(stack, 1, -1);
        return blocks < 0 ? null : Component.literal("Blocos: " + blocks);
    }
}
