package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Ranks;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Utilidades compartilhadas pelas reliquias. */
public final class RelicUtil {
    private RelicUtil() {}

    /** Bosses: tag nexus:bosses (configuravel por datapack) ou c:bosses. */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Nexus.MODID, "bosses"));

    public static void say(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text), true); }

    public static boolean isBoss(Entity entity) {
        var holder = entity.getType().builtInRegistryHolder();
        return holder.is(BOSSES) || holder.is(Tags.EntityTypes.BOSSES);
    }

    public static boolean hasRank(int value, Ranks rank) { return Ranks.of(value).ordinal() >= rank.ordinal(); }

    public static MagicStatus magic(Player player) { return player.getData(ModAttachmentType.MAGIC_STATS); }

    public static BlockHitResult targetBlock(Player player, double distance) {
        HitResult hit = player.pick(distance, 0.0F, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
    }

    /** Entidade viva na mira, sem atravessar blocos. */
    public static LivingEntity targetEntity(Player player, double distance, Predicate<LivingEntity> filter) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(distance));
        BlockHitResult block = targetBlock(player, distance);
        if (block != null) end = block.getLocation();
        final Vec3 limit = end;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, limit).inflate(1),
                e -> e != player && filter.test(e))) {
            var hit = entity.getBoundingBox().inflate(0.3).clip(eye, limit);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
                bestDist = hit.get().distanceToSqr(eye);
                best = entity;
            }
        }
        return best;
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), attacker);
    }

    public static ResourceKey<DamageType> damageType(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.spawnAtLocation((ServerLevel) player.level(), stack);
    }

    public static int count(Player player, Predicate<ItemStack> filter) {
        var inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && filter.test(stack)) total += stack.getCount();
        }
        return total;
    }

    /** Remove ate amount itens que passam no filtro; retorna quantos removeu. */
    public static int consume(Player player, Predicate<ItemStack> filter, int amount) {
        var inventory = player.getInventory();
        int removed = 0;
        for (int i = 0; i < inventory.getContainerSize() && removed < amount; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !filter.test(stack)) continue;
            int take = Math.min(stack.getCount(), amount - removed);
            stack.shrink(take);
            removed += take;
        }
        return removed;
    }

    public static boolean hasItem(Player player, Item item) { return count(player, s -> s.is(item)) > 0; }

    /** Acha a reliquia no inventario do jogador (qualquer slot). */
    public static ItemStack find(Player player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Quebra um bloco disparando o evento normal de quebra (protecao de terreno) e entrega os drops ao sink.
     * Retorna false se o evento cancelar. Nao verifica dureza: quem chama decide o que pode quebrar.
     */
    public static boolean breakBlock(ServerPlayer player, BlockPos pos, ItemStack tool, Consumer<ItemStack> sink) {
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (!level.mayInteract(player, pos)) return false;
        var event = CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state);
        if (event.isCanceled()) return false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
        state.spawnAfterBreak(level, pos, tool, true);
        level.destroyBlock(pos, false, player);
        drops.forEach(sink);
        return true;
    }
}
