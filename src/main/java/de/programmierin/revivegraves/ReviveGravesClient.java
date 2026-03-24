package de.programmierin.revivegraves;

import de.programmierin.revivegraves.block.ModBlocks;
import de.programmierin.revivegraves.client.GravestoneBlockEntityRenderer;
import de.programmierin.revivegraves.entity.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;

public class ReviveGravesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(ModBlockEntities.GRAVESTONE, GravestoneBlockEntityRenderer::new);
        BlockRenderLayerMap.putBlock(ModBlocks.GRAVESTONE, BlockRenderLayer.CUTOUT);
    }
}
