package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the camera to chicken eye level when the local player is a ghost chicken.
 * Sets both cameraY and lastCameraY immediately to prevent interpolation artifacts.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    private float cameraY;

    @Shadow
    private float lastCameraY;

    @Shadow
    private Entity focusedEntity;

    @Inject(method = "updateEyeHeight", at = @At("TAIL"))
    private void revivegraves$lowerGhostChickenCamera(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        if (focusedEntity != client.player) return;

        boolean isGhost = client.interactionManager.getCurrentGameMode() == GameMode.ADVENTURE
                && client.player.hasStatusEffect(StatusEffects.INVISIBILITY);

        if (isGhost) {
            this.cameraY = 0.35f;
            this.lastCameraY = 0.35f;
        }
    }
}
