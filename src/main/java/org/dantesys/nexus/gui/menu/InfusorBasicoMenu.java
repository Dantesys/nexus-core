package org.dantesys.nexus.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.blocks.blockentity.InfusorArcanoBasicoBE;
import org.dantesys.nexus.gui.ModMenus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.tags.ModTags;

import java.util.Objects;

public class InfusorBasicoMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 5;

    private final InfusorArcanoBasicoBE blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public InfusorBasicoMenu(int id, Inventory playerInventory, InfusorArcanoBasicoBE blockEntity) {
        super(ModMenus.INFUSOR_BASICO_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        int[] syncedData = new int[2];
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide()) {
                    return index >= 0 && index < syncedData.length ? syncedData[index] : 0;
                }
                return switch (index) {
                    case 0 -> blockEntity.getProgress();
                    case 1 -> blockEntity.getManaCost();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < syncedData.length) {
                    syncedData[index] = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addDataSlots(data);

        addSlot(new Slot(blockEntity, 0, 44, 16));
        addSlot(new Slot(blockEntity, 1, 117, 16));
        addSlot(new Slot(blockEntity, 2, 44, 52));
        addSlot(new Slot(blockEntity, 3, 117, 52));
        addSlot(new Slot(blockEntity, 4, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INFUSOR_ARCANO_BASICO.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    public static InfusorBasicoMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        InfusorArcanoBasicoBE blockEntity = (InfusorArcanoBasicoBE) inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return new InfusorBasicoMenu(id, inventory, Objects.requireNonNull(blockEntity));
    }

    public boolean hasInput(int slot) {
        return slot >= 0 && slot < 4 && getSlot(slot).hasItem();
    }

    public int getMana() {
        return data.get(0);
    }

    public int getManaCost() {
        return data.get(1);
    }
    public Identifier getTextureFromSlot(int slot){
        ItemStack stack = this.getSlot(slot).getItem();
        Identifier rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/padrao.png");
        if(stack.is(ModTags.Items.ESMERALDAS)){
            if(stack.is(ModItems.ESMERALDA_IGNEA.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/igneo.png");
            }else if(stack.is(ModItems.ESMERALDA_LUZ.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/luz.png");
            }else if(stack.is(ModItems.ESMERALDA_METAL.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/metal.png");
            }else if(stack.is(ModItems.ESMERALDA_SOMBRIA.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/sombrio.png");
            }else if(stack.is(ModItems.ESMERALDA_TEMPESTADE.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/tempestade.png");
            }else if(stack.is(ModItems.ESMERALDA_VIDA.get())){
                rs = Identifier.fromNamespaceAndPath(Nexus.MODID,"textures/gui/infusor/vida.png");
            }
        }
        return rs;
    }
}
