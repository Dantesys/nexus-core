package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.dantesys.nexus.gui.menu.ForjaFullMenu;
import org.dantesys.nexus.magic.ManaReceiver;
import org.dantesys.nexus.tags.ModTags;
import org.dantesys.nexus.recipe.ForjaRecipe;
import org.dantesys.nexus.recipe.ForjaRecipeInput;
import org.dantesys.nexus.recipe.ModRecipes;
import org.dantesys.nexus.blocks.ControladorForja;
import org.jspecify.annotations.Nullable;

public class ControladorForjaBE extends BlockEntity implements Container, MenuProvider, ManaReceiver {
    public static final int LAVA_CAPACITY = 8_000;
    public static final int MANA_CAPACITY = 1_000;
    public static final int LAVA_SLOT = 0;
    public static final int CORE_SLOT = 1;
    public static final int MATERIAL_SLOT = 2;
    public static final int FIRST_EXTRA_MATERIAL_SLOT = 3;
    public static final int LAST_MATERIAL_SLOT = 5;
    public static final int RESULT_SLOT = 6;

    private NonNullList<ItemStack> items = NonNullList.withSize(7, ItemStack.EMPTY);
    private boolean showingPreview;
    private int lava;
    private int mana;
    private int progress;
    private int maxProgress;
    private int generationTicks;
    private boolean resourcesPaid;

    public ControladorForjaBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROLADOR_FORJA_BE.get(), pos, state);
    }

    public boolean isShowingPreview() {
        return showingPreview;
    }

    public void setShowingPreview(boolean showingPreview) {
        if (this.showingPreview == showingPreview) return;
        this.showingPreview = showingPreview;
        markChangedAndSync();
    }

    public int getLava() {
        return lava;
    }

    public int getMana() {
        return mana;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setForgingProgress(int progress, int maxProgress) {
        this.maxProgress = Math.max(0, maxProgress);
        this.progress = Math.clamp(progress, 0, this.maxProgress);
        markChangedAndSync();
    }

    public boolean insertLavaBucket(Player player, ItemStack stack) {
        if (!stack.is(Items.LAVA_BUCKET) || lava > LAVA_CAPACITY - 1_000) {
            return false;
        }
        lava += 1_000;
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!player.addItem(bucket)) player.drop(bucket, false);
        }
        markChangedAndSync();
        return true;
    }

    public boolean consumeResources(int lavaAmount, int manaAmount) {
        if (lavaAmount < 0 || manaAmount < 0 || lava < lavaAmount || mana < manaAmount) {
            return false;
        }
        lava -= lavaAmount;
        mana -= manaAmount;
        markChangedAndSync();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ControladorForjaBE forge) {
        if (!state.getValue(ControladorForja.FORMED)) {
            forge.generationTicks = 0;
            return;
        }

        if (++forge.generationTicks >= 20) {
            forge.generationTicks = 0;
            int generated = ColetorArcanoBasicoBE.collectionRate(level, pos) * 2;
            int previousMana = forge.mana;
            forge.mana = Math.min(MANA_CAPACITY, forge.mana + generated);
            if (forge.mana != previousMana) {
                forge.markChangedAndSync();
            }
        }

        ItemStack bucket = forge.items.get(LAVA_SLOT);
        if (bucket.is(Items.LAVA_BUCKET) && forge.lava <= LAVA_CAPACITY - 1_000) {
            forge.lava += 1_000;
            forge.items.set(LAVA_SLOT, new ItemStack(Items.BUCKET));
            forge.markChangedAndSync();
        }

        forge.processRecipe(level);
    }

    @Override
    public int receiveNonElementalMana(int amount) {
        if (!getBlockState().hasProperty(ControladorForja.FORMED)
                || !getBlockState().getValue(ControladorForja.FORMED)) {
            return 0;
        }
        int accepted = Math.min(Math.max(amount, 0), MANA_CAPACITY - mana);
        if (accepted > 0) {
            mana += accepted;
            markChangedAndSync();
        }
        return accepted;
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
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) markChangedAndSync();
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case LAVA_SLOT -> stack.is(Items.LAVA_BUCKET);
            case CORE_SLOT -> true;
            case MATERIAL_SLOT, FIRST_EXTRA_MATERIAL_SLOT,
                    FIRST_EXTRA_MATERIAL_SLOT + 1, LAST_MATERIAL_SLOT -> true;
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        markChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nexus.forja_full");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        markChangedAndSync();
        return new ForjaFullMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("showingPreview", showingPreview);
        output.putInt("lava", lava);
        output.putInt("mana", mana);
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("generationTicks", generationTicks);
        output.putBoolean("resourcesPaid", resourcesPaid);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        showingPreview = input.getBooleanOr("showingPreview", false);
        lava = Math.clamp(input.getIntOr("lava", 0), 0, LAVA_CAPACITY);
        mana = Math.clamp(input.getIntOr("mana", 0), 0, MANA_CAPACITY);
        maxProgress = Math.max(0, input.getIntOr("maxProgress", 0));
        progress = Math.clamp(input.getIntOr("progress", 0), 0, maxProgress);
        generationTicks = Math.clamp(input.getIntOr("generationTicks", 0), 0, 19);
        resourcesPaid = input.getBooleanOr("resourcesPaid", false);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void processRecipe(Level level) {
        ForjaRecipe recipe = level.getServer().getRecipeManager()
                .getRecipeFor(ModRecipes.FORJA_TYPE.get(), recipeInput(), level)
                .map(holder -> holder.value()).orElse(null);
        if (recipe == null || !canAccept(recipe.assemble(recipeInput()))) {
            if (progress != 0 || maxProgress != 0 || resourcesPaid) {
                resourcesPaid = false;
                resetProgress();
            }
            return;
        }
        if (!resourcesPaid) {
            if (!consumeResources(recipe.lava(), recipe.mana())) return;
            resourcesPaid = true;
            progress = 0;
            maxProgress = recipe.time();
            markChangedAndSync();
        }
        if (++progress < maxProgress) {
            setChanged();
            return;
        }
        ItemStack result = recipe.assemble(recipeInput());
        for (int slot = MATERIAL_SLOT; slot <= LAST_MATERIAL_SLOT; slot++) items.get(slot).shrink(1);
        items.get(CORE_SLOT).shrink(1);
        ItemStack output = items.get(RESULT_SLOT);
        if (output.isEmpty()) items.set(RESULT_SLOT, result);
        else output.grow(result.getCount());
        resourcesPaid = false;
        progress = 0;
        maxProgress = 0;
        markChangedAndSync();
    }

    private ForjaRecipeInput recipeInput() {
        return new ForjaRecipeInput(java.util.List.of(
                items.get(MATERIAL_SLOT), items.get(3), items.get(4), items.get(5), items.get(CORE_SLOT)));
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = items.get(RESULT_SLOT);
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void resetProgress() {
        progress = 0;
        maxProgress = 0;
        markChangedAndSync();
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
