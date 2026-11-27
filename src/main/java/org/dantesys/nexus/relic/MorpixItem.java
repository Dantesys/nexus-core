package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Morpix: seringa do metamorfo. Guarda DNA e copia os poderes dos mobs. */
public final class MorpixItem extends RelicItem {
    public MorpixItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("DNA"), 0, (p, s, c) -> false),
                new RelicSkill(Component.literal("Mimetizar"), RelicSkill.Kind.ACTIVE, Component.literal("Precisa de DNA na seringa."),
                        MorpixEffects::hasDna, MorpixEffects::mimic, MorpixEffects.MIMIC_MANA),
                RelicSkill.passive(Component.literal("Combinação"), 0, (p, s, c) -> false),
                new RelicSkill(Component.literal("Cobaia"), RelicSkill.Kind.ACTIVE, Component.literal("Agache e clique com o botão direito num aliado ou pet."),
                        MorpixEffects::canInject, MorpixEffects::inject, MorpixEffects.INJECT_MANA),
                new RelicSkill(Component.literal("Transformação"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Forma humana (sem DNA em uso) e DNA na seringa."),
                        MorpixEffects::canTransform, MorpixEffects::transform, MorpixEffects.TRANSFORM_MANA));
    }

    /** Clique direito: usa o poder ativo do DNA em uso (explosao, flecha, bola de fogo, teleporte, tinta...). */
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && !player.isShiftKeyDown()) MorpixEffects.usePowers(sp);
        return InteractionResult.SUCCESS;
    }

    @Override public Component hudInfo(ItemStack stack) {
        String dna = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(MorpixEffects.DNA_NAME, "");
        return dna.isEmpty() ? null : Component.literal("DNA: " + dna);
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        Component info = hudInfo(stack);
        return info == null ? List.of() : List.of(info);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Agachar e bater num mob ou jogador guarda o DNA dele na seringa (bosses a partir da maestria 15).";
            case 1 -> "Consome o DNA e te dá os poderes daquele mob até o próximo Mimetizar. Clique direito usa o poder ativo.";
            case 2 -> "Permite somar 2 DNAs diferentes; um terceiro troca o mais antigo. DNA de jogador volta ao normal.";
            case 3 -> "Agachar + clique direito num aliado ou pet aplica o DNA nele até ele morrer (sem bosses).";
            case 4 -> "Vira de verdade o mob do DNA por 1 minuto, mais forte que o original. Se a forma morrer, você volta.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> MorpixEffects.MIMIC_COOLDOWN;
            case 3 -> MorpixEffects.INJECT_COOLDOWN;
            case 4 -> MorpixEffects.TRANSFORM_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
