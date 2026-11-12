package org.dantesys.nexus.relic;

import net.minecraft.world.item.ItemStack;

/** Implemente em itens que alteram atributos de reliquias. */
public interface RelicCatalyst {
    /**
     * Elemento de funcionamento. O core não tem elementos: recebe null (mana bruta) e,
     * por padrão, preserva o que recebeu. Catalisadores de addons podem trocar.
     */
    default net.minecraft.core.@org.jspecify.annotations.Nullable Holder<org.dantesys.nexus.api.element.NexusElement> modifyRelicElement(
            ItemStack catalyst, ItemStack relic,
            net.minecraft.core.@org.jspecify.annotations.Nullable Holder<org.dantesys.nexus.api.element.NexusElement> currentElement) {
        return currentElement;
    }

    /** Peso consumido na capacidade da reliquia. */
    default int getRelicWeight(ItemStack catalyst) {
        return 1;
    }

    default double modifyRelicAttribute(ItemStack catalyst, ItemStack relic,
                                        RelicAttribute attribute, double currentValue) {
        return currentValue;
    }
}
