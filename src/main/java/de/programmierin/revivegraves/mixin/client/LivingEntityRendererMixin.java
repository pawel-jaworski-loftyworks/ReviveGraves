package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ChickenEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.ChickenEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenVariant;
import net.minecraft.entity.passive.ChickenVariants;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts LivingEntityRenderer.render to replace ghost chicken players
 * with an actual chicken model when viewed in F5 mode.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void revivegraves$renderGhostChicken(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        if (!((de.programmierin.revivegraves.ghost.GhostChickenRenderState) playerState).revivegraves$isGhostChicken()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        // Get the chicken renderer from the entity render manager
        EntityRenderManager renderManager = client.getEntityRenderDispatcher();
        EntityRenderer<?, ?> renderer = ((EntityRenderManagerAccessor) renderManager).revivegraves$getRenderers().get(EntityType.CHICKEN);

        if (!(renderer instanceof ChickenEntityRenderer chickenRenderer)) {
            return;
        }

        // Build a ChickenEntityRenderState with position/rotation from the player
        ChickenEntityRenderState chickenState = new ChickenEntityRenderState();
        // Base EntityRenderState fields
        chickenState.x = state.x;
        chickenState.y = state.y;
        chickenState.z = state.z;
        chickenState.age = state.age;
        chickenState.light = state.light;
        chickenState.invisible = false;
        chickenState.sneaking = state.sneaking;
        chickenState.width = 0.4f;
        chickenState.height = 0.7f;
        chickenState.squaredDistanceToCamera = state.squaredDistanceToCamera;
        chickenState.shadowRadius = 0.3f;

        // LivingEntityRenderState fields — animation
        chickenState.bodyYaw = state.bodyYaw;
        chickenState.relativeHeadYaw = state.relativeHeadYaw;
        chickenState.pitch = state.pitch;
        chickenState.limbSwingAnimationProgress = state.limbSwingAnimationProgress;
        chickenState.limbSwingAmplitude = state.limbSwingAmplitude;
        // In inventory screens, use the player's actual scale (0.389) so the
        // inventory's entity-fitting logic produces a correctly sized chicken.
        // In-world (F5), use 1.0 for a normal-sized chicken model.
        boolean isInventoryRender = client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
        chickenState.baseScale = isInventoryRender ? state.baseScale : 1.0f;
        chickenState.ageScale = 1.0f;
        chickenState.baby = false;

        // ChickenEntityRenderState fields — wing flap animation
        // Simulate gentle wing flap based on age (time)
        float flapSpeed = 0.6f;
        float flapAmount = 0.3f;
        if (state.limbSwingAmplitude > 0.01f) {
            // Walking — flap more
            flapSpeed = 1.0f;
            flapAmount = 0.5f;
        }
        chickenState.flapProgress = state.age * flapSpeed;
        chickenState.maxWingDeviation = flapAmount;

        // Set the chicken variant - required for rendering (ChickenEntityRenderer returns early if null)
        client.world.getRegistryManager()
                .getOrThrow(RegistryKeys.CHICKEN_VARIANT)
                .getOptional(ChickenVariants.TEMPERATE)
                .ifPresent(entry -> chickenState.variant = entry.value());

        // Render the chicken instead of the player
        chickenRenderer.render(chickenState, matrices, queue, cameraState);
        ci.cancel();
    }
}
