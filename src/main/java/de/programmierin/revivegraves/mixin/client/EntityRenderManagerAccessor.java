package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor for EntityRenderManager's private renderers map.
 * Used to obtain the ChickenEntityRenderer for ghost chicken rendering.
 */
@Mixin(EntityRenderManager.class)
public interface EntityRenderManagerAccessor {
    @Accessor("renderers")
    Map<EntityType<?>, EntityRenderer<?, ?>> revivegraves$getRenderers();
}
