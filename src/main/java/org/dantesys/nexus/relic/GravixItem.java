package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Gravix: bota gravitacional. So vale calcada. */
public final class GravixItem extends RelicItem {
    public GravixItem(Properties properties) {
        super(properties.humanoidArmor(org.dantesys.nexus.items.ModArmorMaterial.RELIC_ARMOR_MATERIAL, net.minecraft.world.item.equipment.ArmorType.BOOTS)
                        .component(DataComponents.EQUIPPABLE, SteelyItem.equip(EquipmentSlot.FEET, "gravix")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.gravix.gravidade_zero"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.gravix.campo_gravitacional"), GravixEffects.FIELD_MANA, GravixEffects::heavyField),
                RelicSkill.active(Component.translatable("relic.nexus.gravix.zona_0"), GravixEffects.ZONE_MANA, GravixEffects::zeroZone),
                RelicSkill.active(Component.translatable("relic.nexus.gravix.gravidade_manipulada"), GravixEffects.SHIFT_MANA, GravixEffects::armShift),
                new RelicSkill(Component.translatable("relic.nexus.gravix.gravity_max"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.gravix.em_deriva_gravidade_zero_ligada"),
                        GravixEffects::canMax, GravixEffects::gravityMax, GravixEffects.MAX_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.FEET, "gravix", stage, false);
    }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(GravixEffects.drifting(player) ? 1f : 0f);
    }

    @Override public Component hudInfo(ItemStack stack) {
        return modelValue(stack, 1, 0) > 0 ? Component.translatable("relic.nexus.gravix.gravidade_zero") : null;
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.gravix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> GravixEffects.FIELD_COOLDOWN;
            case 2 -> GravixEffects.ZONE_COOLDOWN;
            case 3 -> GravixEffects.SHIFT_COOLDOWN;
            case 4 -> GravixEffects.MAX_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
