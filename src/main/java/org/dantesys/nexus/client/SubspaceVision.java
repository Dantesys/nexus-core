package org.dantesys.nexus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.Subespaco;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Subespaco da Rifty, so no cliente de quem esta nele: tira a textura de "dentro do bloco" da tela e
 * desenha os blocos solidos em volta translucidos, para o jogador se orientar enquanto atravessa.
 */
@EventBusSubscriber(modid = Nexus.MODID, value = Dist.CLIENT)
public final class SubspaceVision {
    private SubspaceVision() {}

    private static final int RADIUS = 5;
    private static final int FULL_BRIGHT = 0xF000F0;

    private record Face(BlockPos pos, Direction dir, TextureAtlasSprite sprite, int alpha) {}

    @SubscribeEvent
    static void screenEffect(ExtractBlockScreenEffectEvent event) {
        if (event.getOverlayType() == ExtractBlockScreenEffectEvent.OverlayType.BLOCK && Subespaco.active(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static boolean solid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.canOcclude();
    }

    @SubscribeEvent
    static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || !Subespaco.active(player)) return;
        Level level = minecraft.level;
        BlockPos center = BlockPos.containing(player.getEyePosition());
        var models = minecraft.getModelManager().getBlockStateModelSet();
        List<Face> faces = new ArrayList<>();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            double dist = Math.sqrt(pos.distSqr(center));
            if (dist > RADIUS + 0.5 || !solid(level, pos)) continue;
            BlockState state = level.getBlockState(pos);
            TextureAtlasSprite sprite = models.getParticleMaterial(state).sprite();
            int alpha = (int) (110 * (1.0 - dist / (RADIUS + 1.0))) + 25;
            for (Direction dir : Direction.values()) {
                neighbor.setWithOffset(pos, dir);
                // So as faces que dao para fora do bloco solido (bordas de cavernas, superficie etc.).
                if (!solid(level, neighbor)) faces.add(new Face(pos.immutable(), dir, sprite, alpha));
            }
        }
        if (faces.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            for (Face face : faces) quad(pose, buffer, face);
        });
        poseStack.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, Face face) {
        float x = face.pos().getX(), y = face.pos().getY(), z = face.pos().getZ();
        TextureAtlasSprite s = face.sprite();
        int color = ARGB.color(face.alpha(), 210, 190, 255);
        Vector3f[] corners = corners(face.dir());
        float[][] uv = {{s.getU0(), s.getV1()}, {s.getU1(), s.getV1()}, {s.getU1(), s.getV0()}, {s.getU0(), s.getV0()}};
        var normal = face.dir().getUnitVec3i();
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, x + corners[i].x, y + corners[i].y, z + corners[i].z).setColor(color).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, normal.getX(), normal.getY(), normal.getZ());
        }
    }

    /** Cantos da face do cubo unitario (levemente para fora, para nao brigar com a face real). */
    private static Vector3f[] corners(Direction dir) {
        float o = -0.002f, i = 1.002f;
        return switch (dir) {
            case DOWN -> new Vector3f[]{new Vector3f(o, o, i), new Vector3f(i, o, i), new Vector3f(i, o, o), new Vector3f(o, o, o)};
            case UP -> new Vector3f[]{new Vector3f(o, i, o), new Vector3f(i, i, o), new Vector3f(i, i, i), new Vector3f(o, i, i)};
            case NORTH -> new Vector3f[]{new Vector3f(i, o, o), new Vector3f(o, o, o), new Vector3f(o, i, o), new Vector3f(i, i, o)};
            case SOUTH -> new Vector3f[]{new Vector3f(o, o, i), new Vector3f(i, o, i), new Vector3f(i, i, i), new Vector3f(o, i, i)};
            case WEST -> new Vector3f[]{new Vector3f(o, o, o), new Vector3f(o, o, i), new Vector3f(o, i, i), new Vector3f(o, i, o)};
            case EAST -> new Vector3f[]{new Vector3f(i, o, i), new Vector3f(i, o, o), new Vector3f(i, i, o), new Vector3f(i, i, i)};
        };
    }
}
