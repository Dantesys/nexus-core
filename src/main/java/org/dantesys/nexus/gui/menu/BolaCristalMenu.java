package org.dantesys.nexus.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.blocks.blockentity.BolaCristalBE;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.gui.ModMenus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.magic.MagicStatus;

import java.util.Objects;

public class BolaCristalMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 1;
    /** Ordem: capacidade, regeneração, afinidade, ressonância. */
    public static final int STATUS_COUNT = 4;

    private final BolaCristalBE blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;

    public BolaCristalMenu(int id, Inventory playerInventory, BolaCristalBE blockEntity) {
        super(ModMenus.BOLA_CRISTAL_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        // No servidor os valores vêm direto dos status de quem abriu (cada jogador vê os seus).
        this.data = player instanceof ServerPlayer serverPlayer ? new StatusData(serverPlayer) : new SimpleContainerData(STATUS_COUNT);
        addDataSlots(data);

        addSlot(new Slot(blockEntity, BolaCristalBE.NUCLEO_SLOT, 80, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.NUCLEO.get()) && stack.isEnchanted();
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

    public static BolaCristalMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        BolaCristalBE blockEntity = (BolaCristalBE) inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return new BolaCristalMenu(id, inventory, Objects.requireNonNull(blockEntity));
    }

    public BolaCristalBE getBlockEntity() {
        return blockEntity;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            blockEntity.processarNucleo(player);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BOLA_CRISTAL.get());
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

    public Component getStatus(int status) {
        return Component.literal(getRank(data.get(Math.clamp(status, 0, STATUS_COUNT - 1))));
    }

    public int getTextColor() {
        return 0xFF8000FF;
    }

    public static String getRank(int value) {
        if (value < 1) return "F";
        if (value < 170) return "E";
        if (value < 340) return "D";
        if (value < 510) return "C";
        if (value < 680) return "B";
        if (value < 850) return "A";
        return "S";
    }

    private record StatusData(ServerPlayer player) implements ContainerData {
        @Override
        public int get(int index) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            return switch (index) {
                case 0 -> status.getMaxMana();
                case 1 -> status.getRegeneracao();
                case 2 -> status.getAfinidade();
                default -> status.getRessonancia();
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return STATUS_COUNT;
        }
    }
}
