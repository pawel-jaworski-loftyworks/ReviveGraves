package de.programmierin.revivegraves.client;

import de.programmierin.revivegraves.entity.GravestoneBlockEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class GravestoneBlockEntityRenderer
        implements BlockEntityRenderer<GravestoneBlockEntity, GravestoneBlockEntityRenderState> {

    private final SkullBlockEntityModel skullModel;
    private final PlayerSkinCache skinCache;

    public GravestoneBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.skullModel = SkullBlockEntityRenderer.getModels(
                ctx.loadedEntityModels(), SkullBlock.Type.PLAYER);
        this.skinCache = ctx.playerSkinRenderCache();
    }

    @Override
    public GravestoneBlockEntityRenderState createRenderState() {
        return new GravestoneBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(GravestoneBlockEntity entity,
                                  GravestoneBlockEntityRenderState state,
                                  float tickDelta,
                                  Vec3d cameraPos,
                                  ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        BlockEntityRenderer.super.updateRenderState(entity, state, tickDelta, cameraPos, crumbling);

        UUID ownerUuid = entity.getOwner();

        state.facing = entity.getCachedState().get(HorizontalFacingBlock.FACING);

        // Always render a skull when the gravestone block exists.
        // Use vanilla's cutout render layer (Steve default skin).
        // ownerUuid may be null if BlockEntity data hasn't synced to this client yet.
        state.skullRenderLayer = SkullBlockEntityRenderer.getCutoutRenderLayer(
                SkullBlock.Type.PLAYER, null);
    }

    @Override
    public void render(GravestoneBlockEntityRenderState state,
                       MatrixStack matrices,
                       OrderedRenderCommandQueue renderQueue,
                       CameraRenderState camera) {
        if (state.skullRenderLayer == null) return;

        Direction facing = state.facing;

        matrices.push();

        // SkullBlockEntityRenderer.render(null, yaw) internally does translate(0.5, 0, 0.5) + scale(-1,-1,1).
        // We pre-translate to compensate: tx = nicheX - 0.5*scale, tz = nicheZ - 0.5*scale

        float nicheDepth = 6.95f / 16f; // just in front of back wall to avoid Z-fighting
        float scale = 0.34f;
        float nicheY = 8.5f / 16f;

        // Niche center + skull yaw per facing (skull face points -Z at yaw=0)
        float nicheX, nicheZ, yRot;
        switch (facing) {
            case SOUTH -> { nicheX = 0.5f;            nicheZ = 1.0f - nicheDepth; yRot = 180f; }
            case WEST  -> { nicheX = nicheDepth;       nicheZ = 0.5f;             yRot = -90f; }
            case EAST  -> { nicheX = 1.0f - nicheDepth; nicheZ = 0.5f;            yRot = 90f;  }
            default    -> { nicheX = 0.5f;             nicheZ = nicheDepth;        yRot = 0f;   }
        }

        float tx = nicheX - 0.5f * scale;
        float ty = nicheY;
        float tz = nicheZ - 0.5f * scale;

        matrices.translate(tx, ty, tz);
        matrices.scale(scale, scale, scale);

        SkullBlockEntityRenderer.render(
                null, yRot, 0f, matrices, renderQueue,
                state.lightmapCoordinates, skullModel,
                state.skullRenderLayer, 0, state.crumblingOverlay
        );

        matrices.pop();
    }
}
