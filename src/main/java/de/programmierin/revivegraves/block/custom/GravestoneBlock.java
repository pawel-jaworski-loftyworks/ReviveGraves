package de.programmierin.revivegraves.block.custom;

import com.mojang.serialization.MapCodec;
import de.programmierin.revivegraves.ReviveGraves;
import de.programmierin.revivegraves.advancement.ModAdvancements;
import de.programmierin.revivegraves.advancement.PlayerStatsState;
import de.programmierin.revivegraves.config.ModConfig;
import de.programmierin.revivegraves.entity.GravestoneBlockEntity;
import de.programmierin.revivegraves.ghost.GhostChickenState;
import de.programmierin.revivegraves.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GravestoneBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<GravestoneBlock> CODEC = createCodec(GravestoneBlock::new);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 4.0, 14.0, 16.0, 13.0);

    public GravestoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, this.getDefaultState().get(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GravestoneBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Prevent manual placement — gravestone is only created programmatically on death
        return null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public ActionResult onUse(BlockState state,
                              World world,
                              BlockPos pos,
                              PlayerEntity clicker,
                              BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof GravestoneBlockEntity gbe)) {
            return super.onUse(state, world, pos, clicker, hit);
        }

        UUID ownerUuid = gbe.getOwner();
        if (ownerUuid == null) {
            return super.onUse(state, world, pos, clicker, hit);
        }

        ServerPlayerEntity dead = ((ServerWorld) world)
                .getServer()
                .getPlayerManager()
                .getPlayer(ownerUuid);

        GhostChickenState ghostState = GhostChickenState.get(((ServerWorld) world).getServer());
        if (dead != null && ghostState.isGhost(dead.getUuid()) && dead.getHealth() > 0) {

            // Check for revive token first
            ItemStack main = clicker.getMainHandStack();
            ItemStack off  = clicker.getOffHandStack();
            boolean hasToken = main.getItem() == ModItems.REVIVE_TOKEN
                    || off.getItem() == ModItems.REVIVE_TOKEN;

            if (!hasToken) {
                clicker.sendMessage(Text.translatable("message.revivegraves.need_token"), false);
                return ActionResult.CONSUME;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            ReviveGraves.teleportToGrave(dead, serverWorld, pos);

            // Consume token after successful teleport
            if (main.getItem() == ModItems.REVIVE_TOKEN) {
                main.decrement(1);
            } else {
                off.decrement(1);
            }

            // 1. Remove from ghost tracking FIRST (stops packet suppression)
            ghostState.removeGhost(dead.getUuid());
            ReviveGraves.clearGhostTickData(dead.getUuid());

            // 2. Remove effects (invisibility, invulnerable, speed, re-list in tab)
            GhostChickenState.removeGhostState(dead);

            // 3. Explicitly clear invisible flag (safety — ensures DataTracker has correct value)
            dead.setInvisible(false);

            // 4. Restore original game mode BEFORE tracker refresh
            //    so the fresh spawn packet has the correct game mode
            GameMode originalMode = gbe.getOriginalGameMode();
            dead.changeGameMode(originalMode != null ? originalMode : GameMode.SURVIVAL);

            // 4b. Restore stored inventory
            if (ModConfig.INSTANCE.gravestone.storeItems && !gbe.getStoredItems().isEmpty()) {
                for (StackWithSlot item : gbe.getStoredItems()) {
                    if (item.slot() >= 0 && item.slot() < dead.getInventory().size()) {
                        dead.getInventory().setStack(item.slot(), item.stack().copy());
                    } else {
                        dead.dropItem(item.stack().copy(), false);
                    }
                }
            }

            // 4c. Restore stored XP
            if (ModConfig.INSTANCE.gravestone.storeXp && gbe.getStoredXp() > 0) {
                dead.addExperience(gbe.getStoredXp());
            }

            // 5. Force entity tracker refresh so other clients see a player again
            //    This sends fresh spawn + metadata packets with all current state
            serverWorld.getChunkManager().unloadEntity(dead);
            serverWorld.getChunkManager().loadEntity(dead);

            serverWorld.spawnParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    30,
                    0.3, 0.5, 0.3,
                    0.0
            );

            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.BLOCKS,
                    1f,
                    1f
            );

            gbe.discardHologram(serverWorld);
            world.removeBlock(pos, false);

            // Track revive stats and grant advancements for the reviver
            if (clicker instanceof ServerPlayerEntity reviver) {
                PlayerStatsState statsState = PlayerStatsState.get(serverWorld.getServer());
                statsState.incrementReviveCount(reviver.getUuid());
                ModAdvancements.checkReviveAdvancements(reviver, statsState);
            }

            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, clicker, hit);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof GravestoneBlockEntity gbe) {
            gbe.discardHologram(world);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
