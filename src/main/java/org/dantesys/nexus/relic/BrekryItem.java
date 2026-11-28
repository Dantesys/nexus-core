package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Brekry: picareta focada em quebrar blocos e abrir caminho. */
public final class BrekryItem extends RelicItem {
    public BrekryItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterial.RELIC, 1.0F, -2.8F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.active(Component.translatable("relic.nexus.brekry.dinamite"), BrekryEffects.DYNAMITE_MANA, BrekryEffects::dynamite),
                RelicSkill.active(Component.translatable("relic.nexus.brekry.tunel"), BrekryEffects.TUNNEL_MANA, BrekryEffects::tunnel),
                RelicSkill.active(Component.translatable("relic.nexus.brekry.frenesi"), BrekryEffects.FRENZY_MANA, BrekryEffects::frenzy),
                RelicSkill.active(Component.translatable("relic.nexus.brekry.limpeza"), BrekryEffects.CLEANUP_MANA, BrekryEffects::cleanup),
                new RelicSkill(Component.translatable("relic.nexus.brekry.breakable"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.brekry.afinidade_rank_a_ou_maior"),
                        BrekryEffects::canBreakable, BrekryEffects::breakable, BrekryEffects.BREAKABLE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.brekry.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 0 -> BrekryEffects.DYNAMITE_COOLDOWN;
            case 1 -> BrekryEffects.TUNNEL_COOLDOWN;
            case 2 -> BrekryEffects.FRENZY_COOLDOWN;
            case 3 -> BrekryEffects.CLEANUP_COOLDOWN;
            default -> BrekryEffects.BREAKABLE_COOLDOWN;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
