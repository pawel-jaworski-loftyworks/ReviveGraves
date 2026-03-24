package de.programmierin.revivegraves;

import de.programmierin.revivegraves.item.ModItemGroups;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import de.programmierin.revivegraves.block.ModBlocks;
import de.programmierin.revivegraves.block.custom.GravestoneBlock;
import de.programmierin.revivegraves.entity.GravestoneBlockEntity;
import de.programmierin.revivegraves.entity.ModBlockEntities;
import de.programmierin.revivegraves.item.ModItems;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import de.programmierin.revivegraves.ghost.GhostChickenState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ReviveGraves implements ModInitializer {
	public static final String MOD_ID = "revivegraves";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Tick tracking for ghost chicken sounds (random 5-15s intervals)
	private static final Map<UUID, Integer> nextSoundTick = new HashMap<>();

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return;

			World raw = player.getEntityWorld();

			if (!(raw instanceof ServerWorld world)) return;

			// Store original game mode before changing to spectator
			GameMode originalMode = player.interactionManager.getGameMode();

			double px = player.getX(), pz = player.getZ();
			BlockPos deathPos;
			if (source == world.getDamageSources().outOfWorld()) {
				int y = world.getBottomY() + 1;
				deathPos = findSafePlacement(world, new BlockPos(MathHelper.floor(px), y, MathHelper.floor(pz)));
			} else {
				deathPos = findSafePlacement(world, player.getBlockPos());
			}
			world.setBlockState(deathPos,
				ModBlocks.GRAVESTONE.getDefaultState()
					.with(HorizontalFacingBlock.FACING, player.getHorizontalFacing()),
				3);
			BlockEntity be = world.getBlockEntity(deathPos);
			if (be instanceof GravestoneBlockEntity gbe) {
				gbe.setOwner(player.getUuid());
				gbe.setOwnerName(player.getGameProfile().name());
				gbe.setOriginalGameMode(originalMode);
				gbe.spawnHologram(world);
			}

			// Track ghost state and enforce one-gravestone-per-player invariant
			GhostChickenState ghostState = GhostChickenState.get(world.getServer());
			GhostChickenState.GraveLocation existingGrave = ghostState.getGravestoneLocation(player.getUuid());
			if (existingGrave != null) {
				RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD,
						Identifier.of(existingGrave.dimension()));
				ServerWorld graveWorld = world.getServer().getWorld(dimKey);
				if (graveWorld != null) {
					graveWorld.removeBlock(existingGrave.pos(), false);
				}
			}
			String dimension = world.getRegistryKey().getValue().toString();
			ghostState.addGhost(player.getUuid(), dimension, deathPos);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((ServerPlayerEntity oldPlayer,
												   ServerPlayerEntity newPlayer,
												   boolean alive) -> {
			if (!alive) {
				GhostChickenState ghostState = GhostChickenState.get(((ServerWorld) newPlayer.getEntityWorld()).getServer());
				if (ghostState.isGhost(newPlayer.getUuid())) {
					GhostChickenState.applyGhostState(newPlayer);
				}
			}
		});

		// Suppress death for ghost players
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player) {
				GhostChickenState ghostState = GhostChickenState.get(((ServerWorld) player.getEntityWorld()).getServer());
				if (ghostState.isGhost(player.getUuid())) {
					return false;
				}
			}
			return true;
		});

		Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(MOD_ID, "gravestone"),
				ModBlockEntities.GRAVESTONE
		);

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			if (state.getBlock() instanceof GravestoneBlock) {
				if (!world.isClient()) {
					player.sendMessage(Text.translatable("message.revivegraves.gravestone_indestructible"), false);
				}
				return false;
			}
			return true;
		});

		// --- Task 7: Ghost tick handler (particles, sounds, void protection, item clear) ---
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			GhostChickenState ghostState = GhostChickenState.get(server);
			int tick = server.getTicks();

			for (UUID uuid : ghostState.getGhostPlayerUuids()) {
				ServerPlayerEntity ghost = server.getPlayerManager().getPlayer(uuid);
				if (ghost == null) continue;

				// Soul particles every 10 ticks
				if (tick % 10 == 0) {
					ServerWorld ghostWorld = (ServerWorld) ghost.getEntityWorld();
					ghostWorld.spawnParticles(
							ParticleTypes.SOUL_FIRE_FLAME,
							ghost.getX(), ghost.getY() + 0.3, ghost.getZ(),
							2,
							0.15, 0.1, 0.15,
							0.0
					);
				}

				// Chicken sounds at random 5-15 second intervals
				int nextTick = nextSoundTick.getOrDefault(uuid, 0);
				if (tick >= nextTick) {
					ServerWorld ghostWorld = (ServerWorld) ghost.getEntityWorld();
					ghostWorld.playSound(
							null,
							ghost.getBlockPos(),
							SoundEvents.ENTITY_CHICKEN_AMBIENT,
							SoundCategory.NEUTRAL,
							1.0f, 1.0f
					);
					nextSoundTick.put(uuid, tick + 100 + ThreadLocalRandom.current().nextInt(201));
				}

				// Void protection: teleport to gravestone if too far below world
				if (ghost.getY() < ghost.getEntityWorld().getBottomY() - 10) {
					GhostChickenState.GraveLocation graveLoc = ghostState.getGravestoneLocation(uuid);
					if (graveLoc != null) {
						RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(graveLoc.dimension()));
						ServerWorld graveWorld = server.getWorld(dimKey);
						if (graveWorld != null) {
							ghost.teleport(
									graveWorld,
									graveLoc.pos().getX() + 0.5,
									graveLoc.pos().getY() + 1.0,
									graveLoc.pos().getZ() + 0.5,
									EnumSet.noneOf(PositionFlag.class),
									ghost.getYaw(), ghost.getPitch(),
									false
							);
						}
					}
				}

				// Item clear: if ghost has items, remove them
				if (!ghost.getInventory().isEmpty()) {
					ghost.getInventory().clear();
				}
			}
		});

		// --- Task 8: Reconnect handling ---
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			GhostChickenState ghostState = GhostChickenState.get(server);
			if (!ghostState.isGhost(player.getUuid())) return;

			// Verify gravestone still exists (cross-dimension)
			GhostChickenState.GraveLocation graveLoc = ghostState.getGravestoneLocation(player.getUuid());
			if (graveLoc != null) {
				RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(graveLoc.dimension()));
				ServerWorld graveWorld = server.getWorld(dimKey);
				if (graveWorld != null) {
					graveWorld.getChunk(graveLoc.pos());
					BlockEntity be = graveWorld.getBlockEntity(graveLoc.pos());
					if (be instanceof GravestoneBlockEntity gbe && player.getUuid().equals(gbe.getOwner())) {
						GhostChickenState.applyGhostState(player);
						return;
					}
				}
			}
			// Gravestone gone — clean up
			ghostState.removeGhost(player.getUuid());
			clearGhostTickData(player.getUuid());
		});
	}

	/**
	 * Removes tick tracking data for a ghost player (called on revive or cleanup).
	 */
	public static void clearGhostTickData(UUID uuid) {
		nextSoundTick.remove(uuid);
	}

	private static BlockPos findSafePlacement(ServerWorld world, BlockPos pos) {
		if (world.getBlockState(pos).isReplaceable()) {
			return pos;
		}
		// Search upward for a replaceable block
		for (int dy = 1; dy <= 5; dy++) {
			BlockPos up = pos.up(dy);
			if (world.getBlockState(up).isReplaceable()) {
				return up;
			}
		}
		// Fallback: place at original position regardless
		return pos;
	}
}
