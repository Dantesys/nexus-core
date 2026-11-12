package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TanqueForjaBE extends BlockEntity {
    public static final int CAPACITY = 4;
    private int lavaBuckets;

    public TanqueForjaBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANQUE_FORJA_BE.get(), pos, state);
    }

    public int getLavaBuckets() {
        return lavaBuckets;
    }

    public boolean addBucket() {
        if (lavaBuckets >= CAPACITY) return false;
        lavaBuckets++;
        setChanged();
        return true;
    }

    public boolean removeBucket() {
        if (lavaBuckets <= 0) return false;
        lavaBuckets--;
        setChanged();
        return true;
    }

    public Component status() {
        return Component.translatable("message.nexus.tanque_forja.status", lavaBuckets, CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("lavaBuckets", lavaBuckets);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lavaBuckets = Math.clamp(input.getIntOr("lavaBuckets", 0), 0, CAPACITY);
    }
}
