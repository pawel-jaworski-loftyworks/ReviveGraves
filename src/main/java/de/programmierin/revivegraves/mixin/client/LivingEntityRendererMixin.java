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

        if (!((PlayerEntityRenderStateMixin) (Object) playerState).revivegraves$isGhostChicken) {
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
        chickenState.x = state.x;
        chickenState.y = state.y;
        chickenState.z = state.z;
        chickenState.age = state.age;
        chickenState.bodyYaw = state.bodyYaw;
        chickenState.pitch = state.pitch;
        chickenState.light = state.light;
        chickenState.invisible = false; // Ghost chickens should be visible to themselves
        chickenState.sneaking = state.sneaking;
        chickenState.width = state.width;
        chickenState.height = state.height;
        chickenState.squaredDistanceToCamera = state.squaredDistanceToCamera;
        chickenState.shadowRadius = 0.3f;

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
