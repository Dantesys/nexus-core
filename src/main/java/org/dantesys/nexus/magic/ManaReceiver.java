package org.dantesys.nexus.magic;

/** A target capable of receiving non-elemental mana. */
public interface ManaReceiver {
    /**
     * @param amount maximum amount offered
     * @return amount actually accepted
     */
    int receiveNonElementalMana(int amount);
}
