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
                RelicSkill.passive(Component.literal("Caldeirão do Refino"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Meditação"), CultryEffects.MEDITATE_MANA, CultryEffects::meditate),
                RelicSkill.active(Component.literal("Chamas do Caldeirão"), CultryEffects.FLAMES_MANA, CultryEffects::throwCauldron),
                RelicSkill.active(Component.literal("Palma da Lótus"), CultryEffects.PALM_MANA, CultryEffects::lotusPalm),
                new RelicSkill(Component.literal("Tribulação Celestial"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Nível 100 de XP, vida e fome cheias e já ter comido uma pílula superior."),
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
        return Component.literal(switch (slot) {
            case 0 -> "Bater queima. Mobs mortos viram pílulas que aumentam atributos para sempre. Clique direito refina as pílulas.";
            case 1 -> "Parado e agachado, medita: recupera vida e fome, ganha XP de maestria e XP (que vem em dobro).";
            case 2 -> "Arremessa o caldeirão: um campo de sucção que cresce, queima e transforma mobs em pílulas.";
            case 3 -> "8 pulsos de palma em volta, até 16 blocos, que ferem e empurram (até bosses).";
            case 4 -> "Chama a tribulação: raios nos inimigos e 3 em você. Sobrevivendo, fica Imortal por 1 minuto.";
            default -> "";
        });
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
