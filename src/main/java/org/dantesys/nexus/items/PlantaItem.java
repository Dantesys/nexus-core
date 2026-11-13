package org.dantesys.nexus.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Rotation;

/** Planta da Construcao Instantanea (Krefty). Guarda so o ID da planta salva no mundo e um resumo. */
public class PlantaItem extends Item {
    public static final String ID = "PlantaId", SX = "Sx", SY = "Sy", SZ = "Sz", BLOCKS = "Blocos", COST = "Custo", ROT = "Rotacao";

    public PlantaItem(Properties properties) { super(properties.stacksTo(1)); }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static String id(ItemStack stack) {
        return stack.getItem() instanceof PlantaItem ? data(stack).getStringOr(ID, "") : "";
    }

    public static Rotation rotation(ItemStack stack) {
        return Rotation.values()[Math.floorMod(data(stack).getIntOr(ROT, 0), 4)];
    }

    public static void rotate(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(ROT, Math.floorMod(tag.getIntOr(ROT, 0) + 1, 4)));
    }

    public static void write(ItemStack stack, String id, int sx, int sy, int sz, int blocks, int cost) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(ID, id);
            tag.putInt(SX, sx);
            tag.putInt(SY, sy);
            tag.putInt(SZ, sz);
            tag.putInt(BLOCKS, blocks);
            tag.putInt(COST, cost);
            tag.putInt(ROT, 0);
        });
    }
}
