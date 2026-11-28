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
                RelicSkill.passive(Component.translatable("relic.nexus.morpix.dna"), 0, (p, s, c) -> false),
                new RelicSkill(Component.translatable("relic.nexus.morpix.mimetizar"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.morpix.precisa_de_dna_na_seringa"),
                        MorpixEffects::hasDna, MorpixEffects::mimic, MorpixEffects.MIMIC_MANA),
                RelicSkill.passive(Component.translatable("relic.nexus.morpix.combinacao"), 0, (p, s, c) -> false),
                new RelicSkill(Component.translatable("relic.nexus.morpix.cobaia"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.morpix.agache_e_clique_com_o"),
                        MorpixEffects::canInject, MorpixEffects::inject, MorpixEffects.INJECT_MANA),
                new RelicSkill(Component.translatable("relic.nexus.morpix.transformacao"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.morpix.forma_humana_sem_dna_em"),
                        MorpixEffects::canTransform, MorpixEffects::transform, MorpixEffects.TRANSFORM_MANA));
    }

    /** Clique direito: usa o poder ativo do DNA em uso (explosao, flecha, bola de fogo, teleporte, tinta...). */
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && !player.isShiftKeyDown()) MorpixEffects.usePowers(sp);
        return InteractionResult.SUCCESS;
    }

    @Override public Component hudInfo(ItemStack stack) {
        String dna = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(MorpixEffects.DNA_NAME, "");
        return dna.isEmpty() ? null : Component.translatable("relic.nexus.morpix.dna_2", Component.translatable(dna));
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        Component info = hudInfo(stack);
        return info == null ? List.of() : List.of(info);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.morpix.skill." + slot + ".desc");
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
