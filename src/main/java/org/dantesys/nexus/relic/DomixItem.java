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
                RelicSkill.passive(Component.translatable("relic.nexus.domix.boop"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.domix.alcateia"), DomixEffects.PACK_MANA, DomixEffects::pack),
                RelicSkill.active(Component.translatable("relic.nexus.domix.imunidade"), DomixEffects.IMMUNITY_MANA, DomixEffects::immunity),
                RelicSkill.active(Component.translatable("relic.nexus.domix.bicharada"), DomixEffects.CRITTERS_MANA, DomixEffects::critters),
                new RelicSkill(Component.translatable("relic.nexus.domix.poder_emprestado_2"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.domix.olhar_para_um_pet_seu"),
                        DomixEffects::canBorrow, DomixEffects::borrow, DomixEffects.BORROW_MANA));
    }

    /** Durante o Poder Emprestado de lhama, camelo ou nautilo, a tecla da reliquia usa o poder (cuspe ou dash). */
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (DomixEffects.usePowerKey(player)) return true;
        return super.activate(player, stack, slot);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.domix.skill." + slot + ".desc");
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
        return power < 0 || seconds <= 0 ? null : Component.translatable("relic.nexus.domix.poder_s", Component.translatable(DomixEffects.POWER_NAMES[power]), seconds);
    }
}
