package de.programmierin.revivegraves.client;

import de.programmierin.revivegraves.entity.GravestoneBlockEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
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

        if (ownerUuid != null) {
            ProfileComponent profile = ProfileComponent.ofDynamic(ownerUuid);
            PlayerSkinCache.Entry skinEntry = skinCache.get(profile);
            state.skullRenderLayer = skinEntry.getRenderLayer();
        } else {
            state.skullRenderLayer = null;
        }
    }

    @Override
    public void render(GravestoneBlockEntityRenderState state,
                       MatrixStack matrices,
                       OrderedRenderCommandQueue renderQueue,
                       CameraRenderState camera) {
        if (state.skullRenderLayer == null) return;

        Direction facing = state.facing;

        matrices.push();

        // The static SkullBlockEntityRenderer.render(null, yaw, ...) does internally:
        //   1. translate(0.5, 0, 0.5)
        //   2. scale(-1, -1, 1)
        //   3. render model at origin with yaw rotation
        //
        // So the skull ends up centered at (0.5, 0, 0.5) in block space, bottom at y=0.
        // The skull model is 8x8x8 pixels = 0.5 blocks tall (before any scaling).
        //
        // We want the skull centered in the niche.
        // Niche model coords (NORTH): (5,8,5) -> (11,13,6)
        // Niche center in block-local: x=0.5, y=10.5/16, z=5.5/16
        //
        // Strategy: translate so that after the internal +0.5/+0.5 the skull lands
        // at the niche center, then scale to fit the 6px niche.
        //
        // After our translate(tx, ty, tz), the internal does translate(0.5, 0, 0.5)
        // giving final position (tx+0.5*scale, ty, tz+0.5*scale) — but wait,
        // the internal translate happens AFTER our scale, so it's in scaled space.
        //
        // Actually: we scale first, THEN the static method translates in the
        // already-scaled coordinate system. So:
        //   final_x = tx + 0.5 * scale
        //   final_y = ty
        //   final_z = tz + 0.5 * scale
        //
        // We want: final_x = nicheX, final_y = nicheY, final_z = nicheZ
        // So: tx = nicheX - 0.5*scale, ty = nicheY, tz = nicheZ - 0.5*scale

        // Niche center per facing
        float nicheX, nicheY, nicheZ;
        float yRot;

        // The skull model's "forward" (face) points in the -Z direction when yaw=0.
        // Vanilla yaw: 0 = facing south, 180 = facing north (opposite of block facing convention)
        float nicheDepth = 6.95f / 16f; // just in front of back wall (Z=7) to avoid Z-fighting
        float scale = 0.34f; // slightly smaller than window to fit with padding
        nicheY = 8.5f / 16f;

        switch (facing) {
            case SOUTH -> {
                nicheX = 0.5f;
                nicheZ = 1.0f - nicheDepth;
                yRot = 180f;
            }
            case WEST -> {
                nicheX = nicheDepth;
                nicheZ = 0.5f;
                yRot = -90f;
            }
            case EAST -> {
                nicheX = 1.0f - nicheDepth;
                nicheZ = 0.5f;
                yRot = 90f;
            }
            default -> { // NORTH
                nicheX = 0.5f;
                nicheZ = nicheDepth;
                yRot = 0f;
            }
        }

        // Pre-translate to compensate for the internal translate(0.5, 0, 0.5)
        // which happens in post-scale space
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
