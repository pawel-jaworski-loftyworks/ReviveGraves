package de.programmierin.revivegraves.datagen;

import de.programmierin.revivegraves.ReviveGraves;
import de.programmierin.revivegraves.advancement.ModAdvancements;
import de.programmierin.revivegraves.block.ModBlocks;
import de.programmierin.revivegraves.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends FabricAdvancementProvider {

	public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(output, registryLookup);
	}

	@Override
	public void generateAdvancement(RegistryWrapper.WrapperLookup wrapperLookup, Consumer<AdvancementEntry> consumer) {
		// Root — creates the mod tab with soul sand background
		AdvancementEntry root = Advancement.Builder.create()
				.display(
						ModBlocks.GRAVESTONE_ITEM.getDefaultStack(),
						Text.translatable("advancements.revivegraves.root.title"),
						Text.translatable("advancements.revivegraves.root.description"),
						Identifier.of(ReviveGraves.MOD_ID, "gui/advancements/backgrounds/soul_sand"),
						AdvancementFrame.TASK,
						false, // showToast
						false, // announceToChat
						false  // hidden
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.ROOT.toString());

		// Emergency Supplies — receive start tokens
		AdvancementEntry emergencySupplies = Advancement.Builder.create()
				.parent(root)
				.display(
						ModItems.REVIVE_TOKEN.getDefaultStack(),
						Text.translatable("advancements.revivegraves.emergency_supplies.title"),
						Text.translatable("advancements.revivegraves.emergency_supplies.description"),
						null,
						AdvancementFrame.TASK,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.EMERGENCY_SUPPLIES.toString());

		// First Fall — die for the first time
		AdvancementEntry firstFall = Advancement.Builder.create()
				.parent(root)
				.display(
						Items.SKELETON_SKULL.getDefaultStack(),
						Text.translatable("advancements.revivegraves.first_fall.title"),
						Text.translatable("advancements.revivegraves.first_fall.description"),
						null,
						AdvancementFrame.TASK,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.FIRST_FALL.toString());

		// Frequent Flyer — die 5 times (child of First Fall)
		Advancement.Builder.create()
				.parent(firstFall)
				.display(
						Items.TOTEM_OF_UNDYING.getDefaultStack(),
						Text.translatable("advancements.revivegraves.frequent_flyer.title"),
						Text.translatable("advancements.revivegraves.frequent_flyer.description"),
						null,
						AdvancementFrame.GOAL,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.FREQUENT_FLYER.toString());

		// Lingering Spirit — 30 min as ghost (child of First Fall)
		AdvancementEntry lingeringSpirit = Advancement.Builder.create()
				.parent(firstFall)
				.display(
						Items.SOUL_LANTERN.getDefaultStack(),
						Text.translatable("advancements.revivegraves.lingering_spirit.title"),
						Text.translatable("advancements.revivegraves.lingering_spirit.description"),
						null,
						AdvancementFrame.GOAL,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.LINGERING_SPIRIT.toString());

		// Eternal Haunting — 2 hours as ghost (child of Lingering Spirit)
		Advancement.Builder.create()
				.parent(lingeringSpirit)
				.display(
						Items.SOUL_CAMPFIRE.getDefaultStack(),
						Text.translatable("advancements.revivegraves.eternal_haunting.title"),
						Text.translatable("advancements.revivegraves.eternal_haunting.description"),
						null,
						AdvancementFrame.CHALLENGE,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.ETERNAL_HAUNTING.toString());

		// Helping Hand — revive another player (child of root)
		AdvancementEntry helpingHand = Advancement.Builder.create()
				.parent(root)
				.display(
						Items.GOLDEN_APPLE.getDefaultStack(),
						Text.translatable("advancements.revivegraves.helping_hand.title"),
						Text.translatable("advancements.revivegraves.helping_hand.description"),
						null,
						AdvancementFrame.TASK,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.HELPING_HAND.toString());

		// Medic! — revive 5 players (child of Helping Hand)
		Advancement.Builder.create()
				.parent(helpingHand)
				.display(
						Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(),
						Text.translatable("advancements.revivegraves.medic.title"),
						Text.translatable("advancements.revivegraves.medic.description"),
						null,
						AdvancementFrame.CHALLENGE,
						true, true, false
				)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, ModAdvancements.MEDIC.toString());
	}
}
