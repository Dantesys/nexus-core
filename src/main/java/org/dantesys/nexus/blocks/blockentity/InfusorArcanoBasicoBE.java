package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.dantesys.nexus.magic.ManaReceiver;
import org.dantesys.nexus.blocks.InfusorArcanoBasico;
import org.dantesys.nexus.gui.menu.InfusorBasicoMenu;
import org.dantesys.nexus.recipe.InfusorRecipeInput;
import org.dantesys.nexus.recipe.ModRecipes;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public class InfusorArcanoBasicoBE extends BlockEntity implements Container, MenuProvider, ManaReceiver {
    private static final int[] CRAFT_SLOTS = {0,1,2,3};
    public static final int INPUT_SLOT_COUNT = 4;
    public static final int RESULT_SLOT = 4;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int progress = 0;
    private int manaCost = 0;
    public InfusorArcanoBasicoBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSOR_ARCANO_BASICO_BE.get(), pos, state);
    }

    public int getProgress(){
        return this.progress;
    }

    public int getManaCost() {
        return manaCost;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) {
            markChangedAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        markChangedAndSync();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        resetProgress();
        markChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nexus.infusor_arcano_basico");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new InfusorBasicoMenu(i, inventory, this);
    }

    public boolean returnItems(Player player) {
        boolean returnedAny = false;
        if(items.get(RESULT_SLOT).isEmpty()) {
            for (int slot = items.size() - 2; slot >= 0; slot--) {
                ItemStack stack = removeItemNoUpdate(slot);
                if (!stack.isEmpty()) {
                    returnedAny = true;
                    if (!player.addItem(stack)) {
                        player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
                    }
                }
            }
        }else{
            ItemStack stack = removeItemNoUpdate(RESULT_SLOT);
            if (!stack.isEmpty()) {
                returnedAny = true;
                if (!player.addItem(stack)) {
                    player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }

        if (returnedAny) {
            markChangedAndSync();
        }
        return returnedAny;
    }

    public boolean insertHeldItem(Player player, ItemStack heldStack) {
        int slot = getSlotFor(heldStack);
        if (slot < 0 || !items.get(slot).isEmpty()) {
            return false;
        }

        items.set(slot, heldStack.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) {
            heldStack.shrink(1);
        }
        markChangedAndSync();

        if (slot == RESULT_SLOT) {
            return false;
        }
        return true;
    }

    private int getSlotFor(ItemStack stack) {
        for (int slot = items.size() - 2; slot >= 0; slot--) {
            if (items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putInt("manaCost", manaCost);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        progress = Math.max(0, input.getIntOr("progress", 0));
        manaCost = Math.max(0, input.getIntOr("manaCost", 0));
        ContainerHelper.loadAllItems(input, items);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfusorArcanoBasicoBE infusor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Sem ingredientes não há receita: evita buscar receita todo tick em infusor vazio.
        if (Arrays.stream(CRAFT_SLOTS).allMatch(slot -> infusor.items.get(slot).isEmpty())) {
            infusor.resetProgress();
            infusor.updateCraftingState(level, pos, false);
            return;
        }
        var recipe = infusor.findRecipe(serverLevel);

        boolean canCraft = recipe != null && infusor.canAccept(recipe.output().create());
        if (!canCraft) {
            infusor.resetProgress();
            infusor.updateCraftingState(level, pos, false);
            return;
        }

        if (infusor.manaCost != recipe.getManaCost()) {
            infusor.progress = 0;
            infusor.manaCost = recipe.getManaCost();
            infusor.markChangedAndSync();
        }
        infusor.updateCraftingState(level, pos, infusor.progress > 0);
        if (infusor.progress < infusor.manaCost) {
            return;
        }

        for (int slot : CRAFT_SLOTS) {
            infusor.items.get(slot).shrink(1);
        }
        ItemStack result = recipe.output().create();
        ItemStack currentOutput = infusor.items.get(RESULT_SLOT);
        if (currentOutput.isEmpty()) {
            infusor.items.set(RESULT_SLOT, result);
        } else {
            currentOutput.grow(result.getCount());
        }
        infusor.resetProgress();
        infusor.updateCraftingState(level, pos, false);
        infusor.markChangedAndSync();
    }

    @Override
    public int receiveNonElementalMana(int amount) {
        if (amount <= 0 || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        var recipe = findRecipe(serverLevel);
        if (recipe == null || !canAccept(recipe.output().create())) {
            return 0;
        }

        if (manaCost != recipe.getManaCost()) {
            progress = 0;
            manaCost = recipe.getManaCost();
        }
        int accepted = Math.min(amount, manaCost - progress);
        progress += accepted;
        markChangedAndSync();
        return accepted;
    }

    public int getMana() {
        return progress;
    }

    private InfusorRecipeInput createRecipeInput() {
        return new InfusorRecipeInput(
                Arrays.stream(CRAFT_SLOTS).mapToObj(slot -> items.get(slot)).toList(),
                progress);
    }

    private org.dantesys.nexus.recipe.InfusorRecipe findRecipe(ServerLevel serverLevel) {
        return serverLevel.getServer().getRecipeManager()
                .getRecipeFor(ModRecipes.INFUSOR_TYPE.get(), createRecipeInput(), serverLevel)
                .map(holder -> holder.value())
                .orElse(null);
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = items.get(RESULT_SLOT);
        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void resetProgress() {
        if (progress != 0 || manaCost != 0) {
            progress = 0;
            manaCost = 0;
            markChangedAndSync();
        }
    }

    private void updateCraftingState(Level level, BlockPos pos, boolean crafting) {
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(InfusorArcanoBasico.CRAFTING)
                && current.getValue(InfusorArcanoBasico.CRAFTING) != crafting) {
            level.setBlock(pos, current.setValue(InfusorArcanoBasico.CRAFTING, crafting), 3);
        }
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
