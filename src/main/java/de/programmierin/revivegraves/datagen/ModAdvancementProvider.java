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
import net.minecraft.item.ItemConvertible;
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

		AdvancementEntry emergencySupplies = advancement(consumer, root, ModItems.REVIVE_TOKEN, ModAdvancements.EMERGENCY_SUPPLIES, AdvancementFrame.TASK);
		AdvancementEntry firstFall = advancement(consumer, root, Items.SKELETON_SKULL, ModAdvancements.FIRST_FALL, AdvancementFrame.TASK);
		advancement(consumer, firstFall, Items.TOTEM_OF_UNDYING, ModAdvancements.FREQUENT_FLYER, AdvancementFrame.GOAL);
		AdvancementEntry lingeringSpirit = advancement(consumer, firstFall, Items.SOUL_LANTERN, ModAdvancements.LINGERING_SPIRIT, AdvancementFrame.GOAL);
		advancement(consumer, lingeringSpirit, Items.SOUL_CAMPFIRE, ModAdvancements.ETERNAL_HAUNTING, AdvancementFrame.CHALLENGE);
		AdvancementEntry helpingHand = advancement(consumer, root, Items.GOLDEN_APPLE, ModAdvancements.HELPING_HAND, AdvancementFrame.TASK);
		advancement(consumer, helpingHand, Items.ENCHANTED_GOLDEN_APPLE, ModAdvancements.MEDIC, AdvancementFrame.CHALLENGE);
	}

	private static AdvancementEntry advancement(Consumer<AdvancementEntry> consumer,
			AdvancementEntry parent, ItemConvertible icon, Identifier id, AdvancementFrame frame) {
		String key = "advancements." + id.getNamespace() + "." + id.getPath();
		return Advancement.Builder.create()
				.parent(parent)
				.display(icon.asItem().getDefaultStack(),
						Text.translatable(key + ".title"),
						Text.translatable(key + ".description"),
						null, frame, true, true, false)
				.criterion("granted", new AdvancementCriterion<>(Criteria.IMPOSSIBLE, new ImpossibleCriterion.Conditions()))
				.build(consumer, id.toString());
	}
}
