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
                RelicSkill.passive(Component.translatable("relic.nexus.krefty.copia_de_blocos"), 0, KreftyEffects::placePassive),
                RelicSkill.active(Component.translatable("relic.nexus.krefty.area_de_construcao"), KreftyRules.BUILD_AREA_MANA, KreftyEffects::buildArea),
                RelicSkill.active(Component.translatable("relic.nexus.krefty.copia_perfeita"), KreftyRules.COPY_MANA, KreftyEffects::perfectCopy),
                RelicSkill.active(Component.translatable("relic.nexus.krefty.area_de_demolicao"), KreftyRules.DEMOLITION_MANA, KreftyEffects::demolitionArea),
                new RelicSkill(Component.translatable("relic.nexus.krefty.construcao_instantanea"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.krefty.para_erguer_copiar_e_livre", KreftyRules.FINAL_CAPACITY_RANK.name()),
                        KreftyEffects::canInstantBuild, KreftyEffects::instantBuild, 0));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.krefty.skill." + slot + ".desc");
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
        if (!copied.isEmpty()) lines.add(Component.translatable("relic.nexus.krefty.bloco_copiado").append(Component.translatable(copied))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return lines;
    }

    @Override public Component hudInfo(ItemStack stack) {
        int blocks = (int) modelValue(stack, 1, -1);
        return blocks < 0 ? null : Component.translatable("relic.nexus.krefty.blocos", blocks);
    }
}
