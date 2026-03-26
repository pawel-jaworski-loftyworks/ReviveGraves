package de.programmierin.revivegraves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.programmierin.revivegraves.ReviveGraves;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
	public static ModConfig INSTANCE = new ModConfig();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("revivegraves.json");
	private static final Path README_PATH = FabricLoader.getInstance().getConfigDir().resolve("revivegraves-readme.txt");

	public LootConfig loot = new LootConfig();
	public StartTokensConfig startTokens = new StartTokensConfig();
	public GravestoneConfig gravestone = new GravestoneConfig();
	public GhostConfig ghost = new GhostConfig();

	public static class LootConfig {
		public boolean enabled = true;
		public float endCityChance = 0.01f;
		public float ominousVaultChance = 0.03f;
	}

	public static class StartTokensConfig {
		public boolean enabled = true;
		public int amount = 5;
	}

	public static class GravestoneConfig {
		public boolean timerEnabled = false;
		public int timerSeconds = 300;
		public boolean storeItems = true;
		public boolean storeXp = false;
		public boolean fireflyParticlesEnabled = true;
	}

	public static class GhostConfig {
		public float speedMultiplier = 0.75f;
		public boolean sprintEnabled = false;
		public boolean spawnAtGravestone = false;
		public boolean slowFallingEnabled = true;
		public boolean particlesEnabled = true;
	}

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				INSTANCE = GSON.fromJson(reader, ModConfig.class);
				if (INSTANCE == null) {
					INSTANCE = new ModConfig();
				}
				// Gson can leave inner objects null if sections are missing from JSON
				if (INSTANCE.loot == null) INSTANCE.loot = new LootConfig();
				if (INSTANCE.startTokens == null) INSTANCE.startTokens = new StartTokensConfig();
				if (INSTANCE.gravestone == null) INSTANCE.gravestone = new GravestoneConfig();
				if (INSTANCE.ghost == null) INSTANCE.ghost = new GhostConfig();
				INSTANCE.validate();
				ReviveGraves.LOGGER.info("Loaded config from {}", CONFIG_PATH);
			} catch (Exception e) {
				ReviveGraves.LOGGER.error("Failed to load config, using defaults", e);
				INSTANCE = new ModConfig();
			}
		} else {
			INSTANCE = new ModConfig();
			ReviveGraves.LOGGER.info("No config found, generating defaults at {}", CONFIG_PATH);
		}
		save();
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
			GSON.toJson(INSTANCE, writer);
		} catch (IOException e) {
			ReviveGraves.LOGGER.error("Failed to save config", e);
		}
		writeReadme();
	}

	private void validate() {
		loot.endCityChance = clamp("loot.endCityChance", loot.endCityChance, 0.0f, 1.0f);
		loot.ominousVaultChance = clamp("loot.ominousVaultChance", loot.ominousVaultChance, 0.0f, 1.0f);
		startTokens.amount = clamp("startTokens.amount", startTokens.amount, 0, 64);
		gravestone.timerSeconds = clamp("gravestone.timerSeconds", gravestone.timerSeconds, 10, 86400);
		ghost.speedMultiplier = clamp("ghost.speedMultiplier", ghost.speedMultiplier, 0.1f, 5.0f);
	}

	private <T extends Comparable<T>> T clamp(String name, T value, T min, T max) {
		if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
			T clamped = value.compareTo(min) < 0 ? min : max;
			ReviveGraves.LOGGER.warn("Config '{}' value {} out of range [{}, {}], clamped to {}",
					name, value, min, max, clamped);
			return clamped;
		}
		return value;
	}

	private static void writeReadme() {
		String readme = """
				ReviveGraves Configuration
				==========================

				File: revivegraves.json

				LOOT
				  enabled            : Master switch for Revive Token loot injection (true/false)
				  endCityChance      : Drop chance in End City treasure chests (0.0-1.0, default 0.01 = 1%)
				  ominousVaultChance : Drop chance in Ominous Trial Vault (0.0-1.0, default 0.03 = 3%)

				START TOKENS
				  enabled : Give new players Revive Tokens on first join (true/false)
				  amount  : Number of tokens (0-64, default 5)

				GRAVESTONE
				  timerEnabled : Enable gravestone expiry timer (true/false, default false)
				  timerSeconds : Seconds until gravestone disappears (10-86400, default 300 = 5 min)
				                When expired, ghost player remains permanently a chicken.
				  storeItems   : Store player inventory in gravestone on death (true/false, default true)
				                Items are returned on revive. When false, items drop on the ground as usual.
				  storeXp      : Store player XP in gravestone on death (true/false, default false)
				                XP is returned on revive. When false, XP drops as orbs as usual.
				  fireflyParticlesEnabled : Show firefly particles around gravestone (true/false, default true)

				GHOST
				  speedMultiplier    : Ghost speed as multiplier of normal player speed (0.1-5.0, default 0.75 = 75%)
				  sprintEnabled      : Whether ghosts can sprint (true/false, default false)
				  spawnAtGravestone  : false = ghost spawns at player spawnpoint, true = ghost spawns at gravestone
				  slowFallingEnabled : Ghost has slow falling like a real chicken (true/false, default true)
				  particlesEnabled   : Show soul fire flame particles around ghost (true/false, default true)

				All changes require a full server restart to take effect.
				""";
		try {
			Files.writeString(README_PATH, readme);
		} catch (IOException e) {
			ReviveGraves.LOGGER.error("Failed to write config readme", e);
		}
	}
}
