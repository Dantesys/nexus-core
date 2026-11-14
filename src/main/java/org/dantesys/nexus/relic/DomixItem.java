package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Domix: domador. Reliquia de utilidade voltada a pets e criaturas. */
public final class DomixItem extends RelicItem {
    public DomixItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Boop"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Alcateia"), DomixEffects.PACK_MANA, DomixEffects::pack),
                RelicSkill.active(Component.literal("Imunidade"), DomixEffects.IMMUNITY_MANA, DomixEffects::immunity),
                RelicSkill.active(Component.literal("Bicharada"), DomixEffects.CRITTERS_MANA, DomixEffects::critters),
                new RelicSkill(Component.literal("Poder Emprestado"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Olhar para um pet seu com a vida cheia, que esteja olhando para você."),
                        DomixEffects::canBorrow, DomixEffects::borrow, DomixEffects.BORROW_MANA));
    }

    /** Durante o Poder Emprestado de lhama, camelo ou nautilo, a tecla da reliquia usa o poder (cuspe ou dash). */
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (DomixEffects.usePowerKey(player)) return true;
        return super.activate(player, stack, slot);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Clique direito com a relíquia doma na hora qualquer mob domável sem dono.";
            case 1 -> "Invoca 5 lobos temporários mais fortes (Afinidade) por 20–60s. Atacam hostis por perto e somem sem dropar.";
            case 2 -> "Seus pets num raio de 10 blocos ficam imunes a dano por 10s (você não).";
            case 3 -> "Invoca 6 a 10 pets temporários sorteados por ~20s.";
            case 4 -> "Olhe para um pet seu: você ganha o poder dele por 1–2 min e o pet some até acabar. "
                    + "Lhama, camelo e náutilo: a tecla da relíquia usa o poder.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> DomixEffects.PACK_COOLDOWN;
            case 2 -> DomixEffects.IMMUNITY_COOLDOWN;
            case 3 -> DomixEffects.CRITTERS_COOLDOWN;
            case 4 -> DomixEffects.BORROW_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, poder ativo (indice), segundos restantes]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) DomixEffects.powerIndex(player), (float) DomixEffects.powerSecondsLeft(player));
    }

    @Override public Component hudInfo(ItemStack stack) {
        int power = (int) modelValue(stack, 1, -1), seconds = (int) modelValue(stack, 2, 0);
        return power < 0 || seconds <= 0 ? null : Component.literal("Poder: " + DomixEffects.POWER_NAMES[power] + " " + seconds + "s");
    }
}
