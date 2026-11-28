package org.dantesys.nexus.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/**
 * Rupty: katana da ruptura. Cada golpe abre marcas de corte que rompem a defesa do alvo contra ela;
 * o que passa de 100% de reducao vira dano extra. Modelo unico em todos os estagios de maestria.
 */
public final class RuptyItem extends RelicItem {
    public RuptyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.RELIC, 3.0f, -2.4f), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.rupty.ruptura_magica"), 0, RuptyEffects::markPassive),
                RelicSkill.active(Component.translatable("relic.nexus.rupty.corte_vertical"), RuptyRules.VERTICAL_MANA, RuptyEffects::verticalCut),
                RelicSkill.active(Component.translatable("relic.nexus.rupty.rotacao"), RuptyRules.ROTATION_MANA, RuptyEffects::rotation),
                RelicSkill.active(Component.translatable("relic.nexus.rupty.marca_aberta"), RuptyRules.OPEN_MANA, RuptyEffects::openMark),
                new RelicSkill(Component.translatable("relic.nexus.rupty.marca_mortal"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.rupty.ter_uma_espada_no_inventario"),
                        RuptyEffects::canUseMortal, RuptyEffects::mortalMark, RuptyRules.MORTAL_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.rupty.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> RuptyRules.VERTICAL_COOLDOWN;
            case 2 -> RuptyRules.ROTATION_COOLDOWN;
            case 3 -> RuptyRules.OPEN_COOLDOWN;
            case 4 -> RuptyRules.MORTAL_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, Marca Mortal preparada (0/1)]. So informativo: o modelo e unico. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(RuptyEffects.mortalPending(player) ? 1f : 0f);
    }

    private static boolean mortalReady(ItemStack stack) { return modelValue(stack, 1, 0) > 0; }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        return mortalReady(stack)
                ? List.of(Component.translatable("relic.nexus.rupty.marca_mortal_preparada").withStyle(ChatFormatting.DARK_RED))
                : List.of();
    }

    @Override public Component hudInfo(ItemStack stack) {
        return mortalReady(stack) ? Component.translatable("relic.nexus.rupty.marca_mortal_pronta").withStyle(ChatFormatting.DARK_RED) : null;
    }
}
