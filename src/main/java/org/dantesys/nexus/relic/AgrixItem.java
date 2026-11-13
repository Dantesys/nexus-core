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
        super(properties.hoe(ModToolMaterial.STEEL_MATERIA, -2.0F, -1.0F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Ciclo da Vida"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Zona Verde"), AgrixEffects.GREEN_MANA, AgrixEffects::greenZone),
                RelicSkill.active(Component.literal("Crescimento Populacional"), AgrixEffects.GROWTH_MANA, AgrixEffects::growth),
                RelicSkill.active(Component.literal("De Volta ao Ovo"), AgrixEffects.EGG_MANA, AgrixEffects::backToEgg),
                new RelicSkill(Component.literal("Fazendinha"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Ressonância rank A ou maior e pelo menos 3 tipos de semente ou plantação no inventário (consumidos)."),
                        AgrixEffects::canFarm, AgrixEffects::farm, AgrixEffects.FARM_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Mobs hostis mortos por você viram uma plantação no chão (terra, grama ou areia) ou dropam uma semente. "
                    + "Animais que você acasala podem ter gêmeos.";
            case 1 -> "Marque dois pontos: plantas, mudas e árvores recebem farinha de osso a cada 1,5s por um tempo.";
            case 2 -> "Marque dois pontos: filhotes crescem mais rápido e os animais voltam a acasalar mais cedo.";
            case 3 -> "Transforma o mob pacífico que você olha no ovo dele, guardando todos os dados.";
            case 4 -> "Marque dois pontos: por 20s, mobs hostis dentro viram mobs pacíficos para sempre.";
            default -> "";
        });
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
