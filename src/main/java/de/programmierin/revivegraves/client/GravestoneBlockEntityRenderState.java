package de.programmierin.revivegraves.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.math.Direction;

public class GravestoneBlockEntityRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public RenderLayer skullRenderLayer;
}
