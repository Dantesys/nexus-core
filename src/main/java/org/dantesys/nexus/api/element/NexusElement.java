package org.dantesys.nexus.api.element;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Um elemento de mana trazido por um addon (ex.: fogo, luz).
 * O core não registra nenhum: ele só oferece o registro {@link NexusElements#REGISTRY}.
 *
 * @param color cor ARGB usada em textos e efeitos do elemento
 */
public record NexusElement(int color) {

    /** Nome traduzível: element.&lt;namespace&gt;.&lt;nome&gt;. */
    public static Component name(Identifier id) {
        return Component.translatable(id.toLanguageKey("element"));
    }
}
