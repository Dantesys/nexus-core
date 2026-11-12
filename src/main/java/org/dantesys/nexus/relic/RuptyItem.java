package org.dantesys.nexus.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/**
 * Rupty: espada de ruptura. Ignora a defesa do oponente e se fragmenta, atravessando e cortando tudo no caminho.
 * Tem 20 fragmentos, mas so 10 ficam liberados ate o Fragmentar (nivel 15).
 */
public final class RuptyItem extends RelicItem {
    public RuptyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0f, -2.4f), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Marca de Ruptura"), 0, RuptyEffects::markPassive),
                RelicSkill.active(Component.literal("Estocada Fragmentada"), RuptyRules.STAB_MANA, RuptyEffects::stab),
                RelicSkill.active(Component.literal("União"), RuptyRules.UNION_MANA, RuptyEffects::union),
                RelicSkill.passive(Component.literal("Fragmentar"), 0, (player, stack, context) -> false),
                new RelicSkill(Component.literal("Sem Defesa"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Afinidade rank " + RuptyRules.FINAL_AFFINITY_RANK
                                + " ou maior, os 20 fragmentos na lâmina e menos de 50% de vida."),
                        RuptyItem::canUseFinal, RuptyEffects::noDefense, RuptyRules.FINAL_MANA));
    }

    private static boolean canUseFinal(ServerPlayer player, ItemStack stack) {
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        return RuptyRules.canUseFinal(magic.getAfinidade(), RuptyEffects.blade(player), RuptyEffects.total(player),
                player.getHealth(), player.getMaxHealth());
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Cada acerto aplica 1 marca (um fragmento sai da lâmina). Máx. 5 por alvo. "
                    + "Cada marca reduz 10% da defesa do alvo contra a Rupty e dura 20s, depois volta para a espada.";
            case 1 -> "Linha de até 5 blocos à frente. O mob atingido recebe as marcas (perto quase 5, longe menos). "
                    + "Os fragmentos que sobram flutuam na linha por 20s e marcam quem passar.";
            case 2 -> "Chama todas as marcas de volta, cortando os mobs no caminho. "
                    + "Ganha força proporcional aos fragmentos que voltaram.";
            case 3 -> "Libera os outros 10 fragmentos (10 → 20) e dobra o limite por alvo (5 → 10, até 100% da defesa).";
            case 4 -> "Os fragmentos voam em volta de você (raio 5) por 2 min, ignorando defesa e atravessando blocos. "
                    + "Dano mágico (a Pele de Mana bloqueia) + corte físico.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> RuptyRules.STAB_COOLDOWN;
            case 2 -> RuptyRules.UNION_COOLDOWN;
            case 4 -> RuptyRules.FINAL_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, pecas escondidas, fragmentos na lamina, total]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        int total = RuptyEffects.total(player), blade = RuptyEffects.blade(player);
        return List.of((float) RuptyRules.hiddenPieces(total - blade), (float) blade, (float) total);
    }

    private static int[] fragments(ItemStack stack) {
        int total = (int) modelValue(stack, 3, RuptyRules.BASE_FRAGMENTS);
        return new int[]{(int) modelValue(stack, 2, total), total};
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        int[] f = fragments(stack);
        return List.of(Component.literal("Fragmentos na lâmina: " + f[0] + "/" + f[1]).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override public Component hudInfo(ItemStack stack) {
        int[] f = fragments(stack);
        return Component.literal("Fragmentos " + f[0] + "/" + f[1]);
    }
}
