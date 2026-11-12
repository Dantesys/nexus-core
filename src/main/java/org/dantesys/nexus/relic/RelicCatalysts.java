package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.tags.ModTags;

import java.util.ArrayList;
import java.util.List;

/** Operacoes centralizadas para os catalisadores instalados em uma reliquia. */
public final class RelicCatalysts {
    private static final String COUNT_TAG = "NexusRelicCatalystCount";
    private static final String SLOT_TAG = "NexusRelicCatalyst.";

    private RelicCatalysts() {}

    public static List<ItemStack> get(ItemStack relic) {
        List<ItemStack> result = new ArrayList<>();
        int count = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr(COUNT_TAG, 0);
        for (int slot = 0; slot < count; slot++) {
            ItemStack catalyst = readSlot(relic, slot);
            if (!catalyst.isEmpty()) result.add(catalyst);
        }
        return List.copyOf(result);
    }

    public static boolean install(ItemStack relic, ItemStack catalyst) {
        if (!(relic.getItem() instanceof RelicItem relicItem) || catalyst.isEmpty()
                || !catalyst.is(ModTags.Items.CATALISADOR)) return false;
        if (!(catalyst.getItem() instanceof RelicCatalyst modifier)) return false;
        int weight = modifier.getRelicWeight(catalyst);
        if (weight <= 0 || getUsedWeight(relic) + weight > relicItem.getCatalystCapacity()) return false;
        int count = getCount(relic);
        String catalystId = BuiltInRegistries.ITEM.getKey(catalyst.getItem()).toString();
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putString(SLOT_TAG + count, catalystId);
            tag.putInt(COUNT_TAG, count + 1);
        });
        return true;
    }

    public static int getUsedWeight(ItemStack relic) {
        int weight = 0;
        for (ItemStack catalyst : get(relic)) {
            if (catalyst.getItem() instanceof RelicCatalyst modifier) {
                weight += Math.max(0, modifier.getRelicWeight(catalyst));
            }
        }
        return weight;
    }

    private static int getCount(ItemStack relic) {
        return Math.max(0, relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(COUNT_TAG, 0));
    }

    private static ItemStack readSlot(ItemStack relic, int slot) {
        String rawId = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(SLOT_TAG + slot, "");
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }
}
