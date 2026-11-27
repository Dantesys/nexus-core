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
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0f, -2.4f), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Ruptura Mágica"), 0, RuptyEffects::markPassive),
                RelicSkill.active(Component.literal("Corte Vertical"), RuptyRules.VERTICAL_MANA, RuptyEffects::verticalCut),
                RelicSkill.active(Component.literal("Rotação"), RuptyRules.ROTATION_MANA, RuptyEffects::rotation),
                RelicSkill.active(Component.literal("Marca Aberta"), RuptyRules.OPEN_MANA, RuptyEffects::openMark),
                new RelicSkill(Component.literal("Marca Mortal"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Ter uma espada no inventário (consumida na ativação)."),
                        RuptyEffects::canUseMortal, RuptyEffects::mortalMark, RuptyRules.MORTAL_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Cada golpe cria uma marca de corte no alvo: -5% de defesa contra a Rupty. "
                    + "Cada marca dura 20s (não renova). Máx. 10 por alvo (50%), inclusive bosses. "
                    + "Redução acima de 100% vira dano extra.";
            case 1 -> "Corte em linha reta de até 5 blocos à frente, atingindo todos na linha: "
                    + "dano de um golpe normal e 3 marcas em cada um.";
            case 2 -> "Golpe em 360° (raio 3) com 1,5× o dano normal. 5 marcas em quem está a até 1,5 bloco, "
                    + "3 em quem está mais longe; depois empurra os atingidos.";
            case 3 -> "As marcas existentes dobram de efeito e duração (10% cada, 40s) "
                    + "e cada alvo marcado perde meio coração a cada 2s.";
            case 4 -> "Consome uma espada do inventário. O próximo golpe aplica uma marca que só sai com a morte: "
                    + "-50% de defesa contra a Rupty e, a cada 2s, metade do dano da espada consumida "
                    + "(encantamentos de dano contam). Resiste ao totem.";
            default -> "";
        });
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
                ? List.of(Component.literal("Marca Mortal preparada").withStyle(ChatFormatting.DARK_RED))
                : List.of();
    }

    @Override public Component hudInfo(ItemStack stack) {
        return mortalReady(stack) ? Component.literal("Marca Mortal pronta").withStyle(ChatFormatting.DARK_RED) : null;
    }
}
