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
                RelicSkill.passive(Component.literal("Olhar Dracônico"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Baforada"), DraconixEffects.BREATH_MANA, DraconixEffects::breath),
                RelicSkill.passive(Component.literal("Pele Escamosa"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.literal("Asas"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("O Verdadeiro Dragão"), 0, DraconixEffects::trueDragon));
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
        return modelValue(stack, 1, 0) > 0 ? Component.literal("Pele Escamosa") : null;
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Visão Noturna; hostis a 8 blocos fogem de você; endermen não se irritam; jogadores que te encaram causam 2% menos dano.";
            case 1 -> "Baforada em cone (8 blocos): dano e fogo dracônico (não apaga com água, ignora Resistência ao Fogo).";
            case 2 -> "Agachar + clique direito liga a pele: armadura acima do netherite, imune a fogo e lava. Sem armadura comum.";
            case 3 -> "Com a pele ligada, ganha asas para planar; clique direito planando dá um impulso (5% da mana).";
            case 4 -> "Chocar (uma vez): derrote o Ender Dragon, jogue sopro do dragão na lava e entre na lava roxa sem pele e sem armadura. "
                    + "Ativar chama o dragão (já montado se estiver caindo ou voando) ou o guarda. Com ele fora, Baforada vira a baforada gigante dele, "
                    + "clique direito montado dá um rugido que espanta e enfraquece, e as passivas da relíquia ficam com o dragão. Se ele morrer, 30 min para chocar de novo.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = slot == 1 ? DraconixEffects.BREATH_COOLDOWN : slot == 4 ? DraconixEffects.SWAP_COOLDOWN : 0;
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
