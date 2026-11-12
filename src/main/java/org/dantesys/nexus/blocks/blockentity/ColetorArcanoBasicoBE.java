package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.blocks.ColetorArcanoBasico;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.magic.ManaReceiver;
import org.dantesys.nexus.worldgen.ModBiomes;

import java.util.Comparator;

public class ColetorArcanoBasicoBE extends BlockEntity {
    public static final int CAPACITY = 10;
    private static final int RANGE = 5;
    private static final int COLLECTION_INTERVAL = 20;
    private static final int MANA_SECOND = 5;

    private int mana;
    private int ticks;
    private BlockPos configuredTarget;
    private float renderAngle;
    private long lastRenderTime = Long.MIN_VALUE;

    public ColetorArcanoBasicoBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLETOR_ARCANO_BASICO_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ColetorArcanoBasicoBE collector) {
        if (++collector.ticks < COLLECTION_INTERVAL) {
            return;
        }
        collector.ticks = 0;

        int previous = collector.mana;
        collector.mana = Math.min(CAPACITY, collector.mana + collectionRate(level, pos));
        boolean collecting = collector.sendToTarget(level, pos);
        if (!collecting) {
            collector.mana = previous;
        } else if (level instanceof ServerLevel serverLevel) {
            collector.spawnCollectionParticles(serverLevel, pos);
        }
        BlockState currentState = level.getBlockState(pos);
        if (currentState.hasProperty(ColetorArcanoBasico.ACTIVE)
                && currentState.getValue(ColetorArcanoBasico.ACTIVE) != collecting) {
            level.setBlock(pos, currentState.setValue(ColetorArcanoBasico.ACTIVE, collecting), 3);
        }
        if (collector.mana != previous) {
            collector.setChanged();
        }
    }

    public static int collectionRate(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(ModBiomes.VIDA)) {
            return MANA_SECOND*4;
        }
        if (biome.is(ModBiomes.IGNEO) || biome.is(ModBiomes.SOMBRIA)
                || biome.is(ModBiomes.LUZ) || biome.is(ModBiomes.METAL)
                || biome.is(ModBiomes.TEMPESTADE)) {
            return MANA_SECOND*2;
        }
        return MANA_SECOND;
    }

    private boolean sendToTarget(Level level, BlockPos origin) {
        if (mana <= 0) {
            return false;
        }
        if (configuredTarget != null) {
            if (!isConfiguredTargetValid(level, origin)) {
                configuredTarget = null;
                setChanged();
            } else {
                return sendToBlockAt(level, origin, configuredTarget);
            }
        }
        if (sendToBlock(level, origin)) {
            return true;
        }

        AABB area = new AABB(origin).inflate(RANGE);
        var entities = level.getEntities((Entity) null, area, this::canReceive);
        entities.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin.getCenter())));
        for (Entity entity : entities) {
            if (sendToEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean sendToBlock(Level level, BlockPos origin) {
        for (BlockPos targetPos : findReceiverBlocks(level, origin)) {
            if (sendToBlockAt(level, origin, targetPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean sendToBlockAt(Level level, BlockPos origin, BlockPos targetPos) {
        if (!(level.getBlockEntity(targetPos) instanceof ManaReceiver receiver)) {
            return false;
        }

        int accepted = Math.clamp(receiver.receiveNonElementalMana(mana), 0, mana);
        if (accepted <= 0) {
            return false;
        }

        mana -= accepted;
        if (level instanceof ServerLevel serverLevel) {
            spawnManaTrail(serverLevel, origin.getCenter().add(0.0, 0.65, 0.0), targetPos.getCenter());
        }
        return true;
    }

    private boolean isConfiguredTargetValid(Level level, BlockPos origin) {
        return configuredTarget != null
                && configuredTarget.distSqr(origin) <= RANGE * RANGE
                && level.getBlockEntity(configuredTarget) instanceof ManaReceiver;
    }

    private static java.util.List<BlockPos> findReceiverBlocks(Level level, BlockPos origin) {
        java.util.List<BlockPos> targets = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        for (int distance = 1; distance <= RANGE; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int y = -distance; y <= distance; y++) {
                    for (int z = -distance; z <= distance; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != distance) continue;
                        if (x * x + y * y + z * z > RANGE * RANGE) continue;

                        targetPos.setWithOffset(origin, x, y, z);
                        if (level.getBlockEntity(targetPos) instanceof ManaReceiver) {
                            targets.add(targetPos.immutable());
                        }
                    }
                }
            }
        }
        targets.sort(Comparator
                .comparingDouble((BlockPos target) -> target.distSqr(origin))
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        return targets;
    }

    private boolean canReceive(Entity entity) {
        if (entity instanceof ManaReceiver) return true;
        if (!(entity instanceof ServerPlayer player)) return false;
        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        return status.isCreated() && status.getMana() < status.getMaxMana();
    }

    private boolean sendToEntity(Entity entity) {
        if (entity instanceof ManaReceiver receiver) {
            int accepted = Math.clamp(receiver.receiveNonElementalMana(mana), 0, mana);
            mana -= accepted;
            if (accepted > 0 && level instanceof ServerLevel serverLevel) {
                spawnManaTrail(serverLevel, worldPosition.getCenter().add(0.0, 0.65, 0.0),
                        entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0));
            }
            return accepted > 0;
        }
        if (entity instanceof ServerPlayer player) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            int accepted = Math.min(mana, status.getMaxMana() - status.getMana());
            status.addMana(accepted);
            mana -= accepted;
            player.syncData(ModAttachmentType.MAGIC_STATS);
            if (accepted > 0 && level instanceof ServerLevel serverLevel) {
                spawnManaTrail(serverLevel, worldPosition.getCenter().add(0.0, 0.65, 0.0),
                        player.position().add(0.0, player.getBbHeight() * 0.5, 0.0));
            }
            return accepted > 0;
        }
        return false;
    }

    public boolean cycleConfiguredTarget(Player player) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        java.util.List<BlockPos> targets = findReceiverBlocks(level, worldPosition);
        if (targets.isEmpty()) {
            configuredTarget = null;
            setChanged();
            return false;
        }

        int currentIndex = configuredTarget == null ? -1 : targets.indexOf(configuredTarget);
        if (currentIndex >= targets.size() - 1) {
            configuredTarget = null;
            setChanged();
            return false;
        }

        configuredTarget = targets.get(currentIndex + 1);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            spawnManaTrail(serverLevel, worldPosition.getCenter().add(0.0, 0.65, 0.0), configuredTarget.getCenter());
        }
        return true;
    }

    public int getMana() {
        return mana;
    }

    public BlockPos getConfiguredTarget() {
        return configuredTarget;
    }

    public int getRange() {
        return RANGE;
    }

    private void spawnCollectionParticles(ServerLevel level, BlockPos pos) {
        Vec3 center = pos.getCenter();
        level.sendParticles(ParticleTypes.ENCHANT,
                center.x(), center.y() + 0.9, center.z(),
                12, 0.35, 0.25, 0.35, 0.02);
    }

    private void spawnManaTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        int steps = Math.max(4, (int) (direction.length() * 5.0));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(direction.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.END_ROD,
                    point.x(), point.y(), point.z(),
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public float getRenderAngle(float partialTick) {
        if (level == null) return renderAngle;
        long renderTime = level.getGameTime();
        if (lastRenderTime != Long.MIN_VALUE && getBlockState().getValue(ColetorArcanoBasico.ACTIVE)) {
            renderAngle = (renderAngle + (renderTime - lastRenderTime) * 4.0F) % 360.0F;
        }
        lastRenderTime = renderTime;
        return renderAngle + (getBlockState().getValue(ColetorArcanoBasico.ACTIVE) ? partialTick * 4.0F : 0.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mana", mana);
        output.putInt("ticks", ticks);
        if (configuredTarget != null) {
            output.putInt("configuredTargetX", configuredTarget.getX());
            output.putInt("configuredTargetY", configuredTarget.getY());
            output.putInt("configuredTargetZ", configuredTarget.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mana = Math.clamp(input.getIntOr("mana", 0), 0, CAPACITY);
        ticks = Math.clamp(input.getIntOr("ticks", 0), 0, COLLECTION_INTERVAL - 1);
        if (input.getInt("configuredTargetX").isPresent()
                && input.getInt("configuredTargetY").isPresent()
                && input.getInt("configuredTargetZ").isPresent()) {
            configuredTarget = new BlockPos(
                    input.getIntOr("configuredTargetX", 0),
                    input.getIntOr("configuredTargetY", 0),
                    input.getIntOr("configuredTargetZ", 0));
        } else {
            configuredTarget = null;
        }
    }
}
