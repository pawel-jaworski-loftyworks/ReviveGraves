package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects when the local player is a ghost chicken and sets the flag on the render state.
 * Ghost state = Invisibility effect + Adventure mode (set by server on death).
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void revivegraves$detectGhostChicken(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Only apply to the local player
        if (client.player == null || entity != client.player) {
            return;
        }

        boolean isGhost = client.interactionManager != null
                && client.interactionManager.getCurrentGameMode() == GameMode.ADVENTURE
                && client.player.hasStatusEffect(StatusEffects.INVISIBILITY);

        ((PlayerEntityRenderStateMixin) (Object) state).revivegraves$isGhostChicken = isGhost;
    }
}
