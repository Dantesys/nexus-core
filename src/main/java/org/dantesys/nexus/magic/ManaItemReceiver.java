package org.dantesys.nexus.magic;

import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.Elementos;

/** An item capable of receiving elemental mana from a player. */
public interface ManaItemReceiver {
    /**
     * @param stack item stack receiving mana
     * @param elemento elemental mana being offered
     * @param amount maximum amount offered
     * @return amount actually accepted
     */
    int receiveMana(ItemStack stack, Elementos elemento, int amount);
}
