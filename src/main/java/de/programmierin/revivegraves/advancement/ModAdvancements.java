package de.programmierin.revivegraves.advancement;

import de.programmierin.revivegraves.ReviveGraves;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Advancement ID constants and helper methods for granting advancements.
 */
public class ModAdvancements {

	// Advancement IDs
	public static final Identifier ROOT = Identifier.of(ReviveGraves.MOD_ID, "root");
	public static final Identifier EMERGENCY_SUPPLIES = Identifier.of(ReviveGraves.MOD_ID, "emergency_supplies");
	public static final Identifier FIRST_FALL = Identifier.of(ReviveGraves.MOD_ID, "first_fall");
	public static final Identifier FREQUENT_FLYER = Identifier.of(ReviveGraves.MOD_ID, "frequent_flyer");
	public static final Identifier LINGERING_SPIRIT = Identifier.of(ReviveGraves.MOD_ID, "lingering_spirit");
	public static final Identifier ETERNAL_HAUNTING = Identifier.of(ReviveGraves.MOD_ID, "eternal_haunting");
	public static final Identifier HELPING_HAND = Identifier.of(ReviveGraves.MOD_ID, "helping_hand");
	public static final Identifier MEDIC = Identifier.of(ReviveGraves.MOD_ID, "medic");

	// Ghost time thresholds in ticks
	public static final long GHOST_TICKS_30_MIN = 30L * 60 * 20;      // 36,000
	public static final long GHOST_TICKS_2_HOURS = 2L * 60 * 60 * 20; // 144,000

	/**
	 * Grants an advancement to a player if not already completed.
	 * @return true if the advancement was newly granted
	 */
	public static boolean grant(ServerPlayerEntity player, Identifier advancementId) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) return false;

		AdvancementEntry advancementEntry = server.getAdvancementLoader().get(advancementId);
		if (advancementEntry == null) return false;

		AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancementEntry);
		if (progress.isDone()) return false;

		player.getAdvancementTracker().grantCriterion(advancementEntry, "granted");
		return true;
	}

	/**
	 * Broadcasts an advancement chat announcement manually.
	 * Used for advancements granted during JOIN where vanilla's announcement doesn't fire.
	 */
	public static void announceAdvancement(ServerPlayerEntity player, Identifier advancementId) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) return;

		AdvancementEntry advancementEntry = server.getAdvancementLoader().get(advancementId);
		if (advancementEntry == null) return;

		AdvancementDisplay display = advancementEntry.value().display().orElse(null);
		if (display == null || !display.shouldAnnounceToChat()) return;

		String translationKey = switch (display.getFrame()) {
			case GOAL -> "chat.type.advancement.goal";
			case CHALLENGE -> "chat.type.advancement.challenge";
			default -> "chat.type.advancement.task";
		};
		// Use vanilla's formatted text (brackets + hover with description)
		Text advancementText = Advancement.getNameFromIdentity(advancementEntry);
		Text message = Text.translatable(translationKey, player.getDisplayName(), advancementText);
		for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
			onlinePlayer.sendMessage(message, false);
		}
	}

	/**
	 * Checks if a player has already completed an advancement.
	 */
	public static boolean isGranted(ServerPlayerEntity player, Identifier advancementId) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) return false;

		AdvancementEntry advancementEntry = server.getAdvancementLoader().get(advancementId);
		if (advancementEntry == null) return false;

		return player.getAdvancementTracker().getProgress(advancementEntry).isDone();
	}

	/**
	 * Checks and grants death-related advancements based on current stats.
	 */
	public static void checkDeathAdvancements(ServerPlayerEntity player, PlayerStatsState statsState) {
		int deaths = statsState.getStats(player.getUuid()).deathCount();
		if (deaths >= 1) grant(player, FIRST_FALL);
		if (deaths >= 5) grant(player, FREQUENT_FLYER);
	}

	/**
	 * Checks and grants revive-related advancements based on current stats.
	 */
	public static void checkReviveAdvancements(ServerPlayerEntity player, PlayerStatsState statsState) {
		int revives = statsState.getStats(player.getUuid()).reviveCount();
		if (revives >= 1) grant(player, HELPING_HAND);
		if (revives >= 5) grant(player, MEDIC);
	}

	/**
	 * Checks and grants ghost time advancements based on cumulative ghost ticks.
	 */
	public static void checkGhostTimeAdvancements(ServerPlayerEntity player, PlayerStatsState statsState) {
		long ghostTicks = statsState.getStats(player.getUuid()).ghostTicks();
		if (ghostTicks >= GHOST_TICKS_30_MIN) grant(player, LINGERING_SPIRIT);
		if (ghostTicks >= GHOST_TICKS_2_HOURS) grant(player, ETERNAL_HAUNTING);
	}
}
