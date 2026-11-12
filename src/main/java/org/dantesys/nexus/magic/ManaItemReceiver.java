package org.dantesys.nexus.magic;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.api.element.NexusElement;
import org.jspecify.annotations.Nullable;

/** Item que pode receber mana de um jogador. */
public interface ManaItemReceiver {
    /**
     * @param stack item que recebe a mana
     * @param element elemento da mana oferecida, ou null para mana não elemental (a única do core)
     * @param amount quantidade máxima oferecida
     * @return quantidade realmente aceita
     */
    int receiveMana(ItemStack stack, @Nullable Holder<NexusElement> element, int amount);
}