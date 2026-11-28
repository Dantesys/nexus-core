package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Cultry: caldeirao portatil do cultivo (xianxia). */
public final class CultryItem extends RelicItem {
    public CultryItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.cultry.caldeirao_do_refino"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.cultry.meditacao"), CultryEffects.MEDITATE_MANA, CultryEffects::meditate),
                RelicSkill.active(Component.translatable("relic.nexus.cultry.chamas_do_caldeirao"), CultryEffects.FLAMES_MANA, CultryEffects::throwCauldron),
                RelicSkill.active(Component.translatable("relic.nexus.cultry.palma_da_lotus"), CultryEffects.PALM_MANA, CultryEffects::lotusPalm),
                new RelicSkill(Component.translatable("relic.nexus.cultry.tribulacao_celestial"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.cultry.nivel_100_de_xp_vida"),
                        CultryEffects::canTribulation, CultryEffects::tribulation, CultryEffects.TRIBULATION_MANA));
    }

    /** Clique direito abre o caldeirao para refinar pilulas (3 iguais viram 1 de grau maior). */
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && !player.isShiftKeyDown() && isUnlocked(sp, 0)) CultryEffects.openRefinery(sp);
        return InteractionResult.SUCCESS;
    }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(CultryEffects.thrown(player) ? 1f : 0f);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.cultry.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> CultryEffects.MEDITATE_COOLDOWN;
            case 2 -> CultryEffects.FLAMES_COOLDOWN;
            case 3 -> CultryEffects.PALM_COOLDOWN;
            case 4 -> CultryEffects.TRIBULATION_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
