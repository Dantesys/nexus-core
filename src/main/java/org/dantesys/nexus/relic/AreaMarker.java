package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Marcacao de areas por dois pontos (tecla da reliquia em cada ponto; Shift cancela), com previa
 * so para o dono. As areas cobrem os blocos marcados e HEIGHT blocos acima do ponto mais alto.
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class AreaMarker {
    private AreaMarker() {}

    public static final int HEIGHT = 3, MAX_DISTANCE = 32;
    private static final DustParticleOptions OK = new DustParticleOptions(0x40D040, 1.0f);
    private static final DustParticleOptions BAD = new DustParticleOptions(0xFF2020, 1.0f);

    private record Pending(RelicItem relic, int slot, ResourceKey<Level> dim, BlockPos first, int maxSide) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public static boolean hasPending(ServerPlayer player, RelicItem relic, int slot) {
        Pending p = PENDING.get(player.getUUID());
        return p != null && p.relic() == relic && p.slot() == slot && p.dim() == player.level().dimension();
    }

    /** Volume da area que seria criada agora (ou 0 sem marcacao pendente). */
    public static int pendingVolume(ServerPlayer player, RelicItem relic, int slot) {
        if (!hasPending(player, relic, slot)) return 0;
        BlockHitResult hit = RelicUtil.targetBlock(player, MAX_DISTANCE);
        if (hit == null) return 0;
        AABB box = box(PENDING.get(player.getUUID()).first(), hit.getBlockPos());
        return (int) Math.round(box.getXsize() * box.getYsize() * box.getZsize());
    }

    public static AABB box(BlockPos a, BlockPos b) {
        return AABB.encapsulatingFullBlocks(a, b).expandTowards(0, HEIGHT, 0);
    }

    public static boolean fits(BlockPos a, BlockPos b, int maxSide) {
        return Math.abs(a.getX() - b.getX()) < maxSide && Math.abs(a.getY() - b.getY()) < maxSide
                && Math.abs(a.getZ() - b.getZ()) < maxSide;
    }

    /** Marca um ponto; retorna a caixa quando os dois pontos estao prontos, senao null. */
    public static AABB mark(ServerPlayer player, RelicItem relic, int slot, int maxSide) {
        UUID id = player.getUUID();
        if (player.isShiftKeyDown()) {
            if (PENDING.remove(id) != null) RelicUtil.say(player, Component.translatable("message.nexus.relic.marcacao_cancelada"));
            return null;
        }
        BlockHitResult hit = RelicUtil.targetBlock(player, MAX_DISTANCE);
        if (hit == null) {
            RelicUtil.say(player, Component.translatable("message.nexus.relic.mire_em_um_bloco_ate", MAX_DISTANCE));
            return null;
        }
        if (!hasPending(player, relic, slot)) {
            PENDING.put(id, new Pending(relic, slot, player.level().dimension(), hit.getBlockPos(), maxSide));
            RelicUtil.say(player, Component.translatable("message.nexus.relic.ponto_1_marcado_mire_no"));
            return null;
        }
        BlockPos first = PENDING.get(id).first(), second = hit.getBlockPos();
        if (!fits(first, second, maxSide)) {
            RelicUtil.say(player, Component.translatable("message.nexus.relic.area_grande_demais_max_blocos", maxSide));
            return null;
        }
        PENDING.remove(id);
        return box(first, second);
    }

    public static void edges(AABB box, Consumer<Vec3> out) {
        double[] xs = {box.minX, box.maxX}, ys = {box.minY, box.maxY}, zs = {box.minZ, box.maxZ};
        for (double y : ys) for (double z : zs) for (double x = box.minX; x <= box.maxX; x += 1) out.accept(new Vec3(x, y, z));
        for (double x : xs) for (double z : zs) for (double y = box.minY; y <= box.maxY; y += 1) out.accept(new Vec3(x, y, z));
        for (double x : xs) for (double y : ys) for (double z = box.minZ; z <= box.maxZ; z += 1) out.accept(new Vec3(x, y, z));
    }

    /** Contorno visivel para todos (areas ativas). */
    public static void outline(ServerLevel level, AABB box, DustParticleOptions color) {
        edges(box, p -> level.sendParticles(color, p.x, p.y, p.z, 1, 0, 0, 0, 0));
    }

    @SubscribeEvent
    public static void preview(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameTime() % 10 != 0) return;
        Pending p = PENDING.get(player.getUUID());
        if (p == null || p.dim() != player.level().dimension()) return;
        ItemStack held = RelicItem.controlledRelic(player);
        if (held.getItem() != p.relic()) return;
        BlockHitResult hit = RelicUtil.targetBlock(player, MAX_DISTANCE);
        BlockPos second = hit == null ? p.first() : hit.getBlockPos();
        DustParticleOptions color = fits(p.first(), second, p.maxSide()) ? OK : BAD;
        ServerLevel level = (ServerLevel) player.level();
        edges(box(p.first(), second), v -> level.sendParticles(player, color, true, true, v.x, v.y, v.z, 1, 0, 0, 0, 0));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PENDING.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { PENDING.clear(); }
}
