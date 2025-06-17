package de.programmierin.revivegraves.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity {
    /**
     * Must match AbstractClientPlayerEntity(ClientPlayNetworkHandler, GameProfile)
     * and delegate to super(World, GameProfile).
     */
    public AbstractClientPlayerEntityMixin(ClientPlayNetworkHandler networkHandler, GameProfile gameProfile) {
        super(networkHandler.getWorld(), gameProfile);
    }

    // Hier deine Injections oder Overrides
}
