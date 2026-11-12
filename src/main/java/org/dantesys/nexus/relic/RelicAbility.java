package org.dantesys.nexus.relic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Efeito ativo executado por uma reliquia no servidor. */
@FunctionalInterface
public interface RelicAbility {
    /** @return true quando a habilidade foi executada e deve consumir mana/recarga. */
    boolean activate(ServerPlayer player, ItemStack relic, RelicAbilityContext context);
}
