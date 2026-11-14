package org.dantesys.nexus.relic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Item displays usados como visual (machado em voo, clava gigante). */
public final class Displays {
    private Displays() {}

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) list.add(FloatTag.valueOf(v));
        return list;
    }

    public static Display.ItemDisplay item(ServerLevel level, ItemStack stack, Vec3 pos, float scale) {
        return item(level, stack, pos, scale, new float[]{0, 0, 0, 1});
    }

    /** leftRotation: quaternion (x, y, z, w) aplicado ao item antes da rotacao da entidade. */
    public static Display.ItemDisplay item(ServerLevel level, ItemStack stack, Vec3 pos, float scale, float[] leftRotation) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        CompoundTag tag = new CompoundTag();
        ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack.copyWithCount(1)).result().ifPresent(t -> tag.put("item", t));
        tag.putString("item_display", "fixed");
        tag.putInt("teleport_duration", 2);
        CompoundTag transformation = new CompoundTag();
        transformation.put("left_rotation", floats(leftRotation));
        transformation.put("right_rotation", floats(0, 0, 0, 1));
        transformation.put("translation", floats(0, 0, 0));
        transformation.put("scale", floats(scale, scale, scale));
        tag.put("transformation", transformation);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        display.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(display);
        return display;
    }
}
