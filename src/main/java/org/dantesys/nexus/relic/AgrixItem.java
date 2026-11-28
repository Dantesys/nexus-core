package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Agrix: reliquia de utilidade voltada a agricultura e criacao. */
public final class AgrixItem extends RelicItem {
    public static final int SLOT_GREEN = 1, SLOT_GROWTH = 2, SLOT_EGG = 3, SLOT_FARM = 4;

    public AgrixItem(Properties properties) {
        super(properties.hoe(ModToolMaterial.RELIC, -2.0F, -1.0F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.agrix.ciclo_da_vida"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.agrix.zona_verde"), AgrixEffects.GREEN_MANA, AgrixEffects::greenZone),
                RelicSkill.active(Component.translatable("relic.nexus.agrix.crescimento_populacional"), AgrixEffects.GROWTH_MANA, AgrixEffects::growth),
                RelicSkill.active(Component.translatable("relic.nexus.agrix.de_volta_ao_ovo"), AgrixEffects.EGG_MANA, AgrixEffects::backToEgg),
                new RelicSkill(Component.translatable("relic.nexus.agrix.fazendinha"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.agrix.ressonancia_rank_a_ou_maior"),
                        AgrixEffects::canFarm, AgrixEffects::farm, AgrixEffects.FARM_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.agrix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case SLOT_GREEN -> AgrixEffects.GREEN_COOLDOWN;
            case SLOT_GROWTH -> AgrixEffects.GROWTH_COOLDOWN;
            case SLOT_EGG -> AgrixEffects.EGG_COOLDOWN;
            case SLOT_FARM -> AgrixEffects.FARM_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** Areas: o primeiro ponto e livre; o custo cresce com o tamanho. */
    @Override protected boolean isFreeUse(ServerPlayer player, ItemStack stack, int slot) {
        if (slot != SLOT_GREEN && slot != SLOT_GROWTH && slot != SLOT_FARM) return false;
        return player.isShiftKeyDown() || !AreaMarker.hasPending(player, this, slot);
    }

    @Override protected int skillManaCost(ServerPlayer player, ItemStack stack, int slot) {
        int base = super.skillManaCost(player, stack, slot);
        if (slot != SLOT_GREEN && slot != SLOT_GROWTH && slot != SLOT_FARM) return base;
        return base + (int) Math.ceil(AreaMarker.pendingVolume(player, this, slot) * 0.05);
    }
}
