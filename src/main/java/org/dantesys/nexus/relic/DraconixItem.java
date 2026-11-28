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

/** Draconix: ovo de dragao que choca na maestria maxima. */
public final class DraconixItem extends RelicItem {
    public DraconixItem(Properties properties) {
        super(properties.fireResistant(), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.draconix.olhar_draconico"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.draconix.baforada"), DraconixEffects.BREATH_MANA, DraconixEffects::breath),
                RelicSkill.passive(Component.translatable("relic.nexus.draconix.pele_escamosa"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.draconix.asas"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.draconix.o_verdadeiro_dragao"), 0, DraconixEffects::trueDragon));
    }

    /** Agachado: liga/desliga a Pele Escamosa. Planando com a pele: impulso das asas. */
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            if (player.getVehicle() instanceof org.dantesys.nexus.entity.DragaoDraconix) DraconixEffects.roar(sp);
            else if (player.isShiftKeyDown() && isUnlocked(sp, 2)) DraconixEffects.toggleScales(sp);
            else if (player.isFallFlying() && isUnlocked(sp, 3)) DraconixEffects.wingBoost(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(DraconixEffects.scales(player) ? 1f : 0f);
    }

    /** Com o dragão chamado, só a Baforada (gigante) e a própria final. */
    @Override protected boolean skillsAvailable(ServerPlayer player, ItemStack stack, int slot) {
        return !DraconixEffects.dragonOut(player) || slot == 1 || slot == 4;
    }

    @Override public Component hudInfo(ItemStack stack) {
        return modelValue(stack, 1, 0) > 0 ? Component.translatable("relic.nexus.draconix.pele_escamosa") : null;
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.draconix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = slot == 1 ? DraconixEffects.BREATH_COOLDOWN : slot == 4 ? DraconixEffects.SWAP_COOLDOWN : 0;
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
