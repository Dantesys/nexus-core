package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.dantesys.nexus.Nexus;

import java.util.*;

/**
 * Efeitos visuais enviados so para um jogador:
 * contornos de blocos atraves das paredes (block displays falsos, com cor de brilho) e brilho em entidades.
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class PrivateVisuals {
    private PrivateVisuals() {}

    private record Outline(UUID viewer, int[] ids, long until) {}
    private record Glow(UUID viewer, Entity target, long until) {}
    private static final List<Outline> OUTLINES = new ArrayList<>();
    private static final List<Glow> GLOWS = new ArrayList<>();

    /** Mostra contornos coloridos nos blocos, visiveis atraves das paredes, so para o viewer. */
    public static void outlineBlocks(ServerPlayer viewer, Map<BlockPos, Integer> blocks, int ticks) {
        ServerLevel level = (ServerLevel) viewer.level();
        int[] ids = new int[blocks.size()];
        int i = 0;
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            Display.BlockDisplay display = new Display.BlockDisplay(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, level);
            CompoundTag tag = new CompoundTag();
            BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().ifPresent(t -> tag.put("block_state", t));
            tag.putInt("glow_color_override", entry.getValue());
            display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            display.setPos(pos.getX(), pos.getY(), pos.getZ());
            display.setGlowingTag(true);
            ids[i++] = display.getId();
            viewer.connection.send(new ClientboundAddEntityPacket(display.getId(), display.getUUID(), pos.getX(), pos.getY(), pos.getZ(),
                    0, 0, net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, 0, Vec3.ZERO, 0));
            var values = display.getEntityData().getNonDefaultValues();
            if (values != null) viewer.connection.send(new ClientboundSetEntityDataPacket(display.getId(), values));
        }
        OUTLINES.add(new Outline(viewer.getUUID(), ids, level.getGameTime() + ticks));
    }

    /** Faz a entidade brilhar so para o viewer. */
    public static void glow(ServerPlayer viewer, Entity target, int ticks) {
        GLOWS.removeIf(g -> g.viewer().equals(viewer.getUUID()) && g.target() == target);
        GLOWS.add(new Glow(viewer.getUUID(), target, viewer.level().getGameTime() + ticks));
    }

    /** Remove todos os brilhos privados sobre a entidade (ex.: Camuflagem). */
    public static void clearGlowOn(Entity target) {
        Iterator<Glow> it = GLOWS.iterator();
        while (it.hasNext()) {
            Glow g = it.next();
            if (g.target() == target) {
                it.remove();
                ServerPlayer viewer = target.level().getServer().getPlayerList().getPlayer(g.viewer());
                if (viewer != null) sendFlags(viewer, target, false);
            }
        }
    }

    private static byte flags(Entity entity, boolean glow) {
        int f = 0;
        if (entity.isOnFire()) f |= 0x01;
        if (entity.isShiftKeyDown()) f |= 0x02;
        if (entity.isSprinting()) f |= 0x08;
        if (entity.isSwimming()) f |= 0x10;
        if (entity.isInvisible()) f |= 0x20;
        if (glow || entity.hasGlowingTag()) f |= 0x40;
        if (entity instanceof LivingEntity living && living.isFallFlying()) f |= 0x80;
        return (byte) f;
    }

    private static void sendFlags(ServerPlayer viewer, Entity target, boolean glow) {
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(),
                List.of(new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, flags(target, glow)))));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Outline> outlines = OUTLINES.iterator();
        while (outlines.hasNext()) {
            Outline o = outlines.next();
            ServerPlayer viewer = server.getPlayerList().getPlayer(o.viewer());
            if (viewer == null) { outlines.remove(); continue; }
            if (now >= o.until()) {
                viewer.connection.send(new ClientboundRemoveEntitiesPacket(o.ids()));
                outlines.remove();
            }
        }
        Iterator<Glow> glows = GLOWS.iterator();
        while (glows.hasNext()) {
            Glow g = glows.next();
            ServerPlayer viewer = server.getPlayerList().getPlayer(g.viewer());
            boolean alive = g.target().isAlive() && viewer != null && g.target().level() == viewer.level();
            if (!alive || now >= g.until()) {
                if (viewer != null && g.target().isAlive()) sendFlags(viewer, g.target(), false);
                glows.remove();
            } else if (now % 2 == 0) {
                sendFlags(viewer, g.target(), true);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        OUTLINES.removeIf(o -> o.viewer().equals(event.getEntity().getUUID()));
        GLOWS.removeIf(g -> g.viewer().equals(event.getEntity().getUUID()));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        OUTLINES.clear();
        GLOWS.clear();
    }
}
