package de.hysky.skyblocker.skyblock.end;

import de.hysky.skyblocker.annotations.RegisterWidget;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.skyblock.item.tooltip.ItemTooltip;
import de.hysky.skyblocker.skyblock.itemlist.ItemRepository;
import de.hysky.skyblocker.skyblock.tabhud.config.WidgetsConfigurationScreen;
import de.hysky.skyblocker.skyblock.tabhud.widget.ComponentBasedWidget;
import de.hysky.skyblocker.skyblock.tabhud.widget.component.Components;
import de.hysky.skyblocker.utils.ItemUtils;
import de.hysky.skyblocker.utils.Location;
import de.hysky.skyblocker.utils.NEURepoManager;
import de.hysky.skyblocker.utils.Utils;
import it.unimi.dsi.fastutil.doubles.DoubleBooleanPair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * HUD widget that tracks valuable items from Ender Nodes in The End.
 * Displays inventory counts and bazaar values for:
 * - Grand Experience Bottle
 * - Titanic Experience Bottle
 * - Mite Gel
 */
@RegisterWidget
public class EnderNodeTrackerWidget extends ComponentBasedWidget {
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private static final MutableText TITLE = Text.literal("Ender Node Tracker").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
	private static final Set<Location> AVAILABLE_LOCATIONS = Set.of(Location.THE_END);

	// Skyblock item IDs for tracked items
	private static final String GRAND_EXPERIENCE_BOTTLE = "GRAND_EXP_BOTTLE";
	private static final String TITANIC_EXPERIENCE_BOTTLE = "TITANIC_EXP_BOTTLE";
	private static final String MITE_GEL = "MITE_GEL";
	private static final String ENCHANTED_END_STONE = "ENCHANTED_ENDSTONE";
	private static final String ENCHANTED_ENDER_PEARL = "ENCHANTED_ENDER_PEARL";
	private static final String ENCHANTED_OBSIDIAN = "ENCHANTED_OBSIDIAN";

	// Lazy-loaded item icons using suppliers for thread safety
	private static final Supplier<ItemStack> GRAND_BOTTLE_ICON = ItemRepository.getItemStackSupplier(GRAND_EXPERIENCE_BOTTLE);
	private static final Supplier<ItemStack> TITANIC_BOTTLE_ICON = ItemRepository.getItemStackSupplier(TITANIC_EXPERIENCE_BOTTLE);
	private static final Supplier<ItemStack> MITE_GEL_ICON = ItemRepository.getItemStackSupplier(MITE_GEL);
	private static final Supplier<ItemStack> ENCHANTED_END_STONE_ICON = ItemRepository.getItemStackSupplier(ENCHANTED_END_STONE);
	private static final Supplier<ItemStack> ENCHANTED_ENDER_PEARL_ICON = ItemRepository.getItemStackSupplier(ENCHANTED_ENDER_PEARL);
	private static final Supplier<ItemStack> ENCHANTED_OBSIDIAN_ICON = ItemRepository.getItemStackSupplier(ENCHANTED_OBSIDIAN);

	private static EnderNodeTrackerWidget instance;

	// Cache for item counts and values
	private final Map<String, Integer> itemCounts = new HashMap<>();
	private final Map<String, DoubleBooleanPair> itemPrices = new HashMap<>();

	public EnderNodeTrackerWidget() {
		super(TITLE, Formatting.DARK_PURPLE.getColorValue(), "hud_ender_node_tracker");
		instance = this;
	}

	public static EnderNodeTrackerWidget getInstance() {
		return instance;
	}

	@Override
	public boolean shouldUpdateBeforeRendering() {
		// Update before each render to keep inventory counts current
		// This is acceptable here as the update is lightweight (just counting items in inventory)
		return true;
	}

	@Override
	public boolean isEnabledIn(Location location) {
		return location.equals(Location.THE_END) &&
			   SkyblockerConfigManager.get().otherLocations.end.enableEnderNodeTracker;
	}

	@Override
	public void setEnabledIn(Location location, boolean enabled) {
		if (!location.equals(Location.THE_END)) return;
		SkyblockerConfigManager.get().otherLocations.end.enableEnderNodeTracker = enabled;
	}

	@Override
	public Set<Location> availableLocations() {
		return AVAILABLE_LOCATIONS;
	}

	@Override
	public boolean shouldRender(Location location) {
		// Show when enabled and in The End, or in preview mode
		return super.shouldRender(location) &&
			   (Utils.isInTheEnd() || CLIENT.currentScreen instanceof WidgetsConfigurationScreen);
	}

	@Override
	public void updateContent() {
		// Show preview data in widget configuration screen
		if (CLIENT.currentScreen instanceof WidgetsConfigurationScreen) {
			addPreviewComponents();
			return;
		}

		// Safety check for player and repository
		if (CLIENT.player == null || NEURepoManager.isLoading() || !ItemRepository.filesImported()) {
			return;
		}

		// Clear previous data
		itemCounts.clear();
		itemPrices.clear();

		// Count items in inventory
		countInventoryItems();

		// Get prices for tracked items
		updatePrices();

		// Add components for each tracked item
		addItemComponent(GRAND_EXPERIENCE_BOTTLE, "Grand Experience Bottle", GRAND_BOTTLE_ICON);
		addItemComponent(TITANIC_EXPERIENCE_BOTTLE, "Titanic Experience Bottle", TITANIC_BOTTLE_ICON);
		addItemComponent(MITE_GEL, "Mite Gel", MITE_GEL_ICON);
		addItemComponent(ENCHANTED_END_STONE, "Enchanted End Stone", ENCHANTED_END_STONE_ICON);
		addItemComponent(ENCHANTED_ENDER_PEARL, "Enchanted End Pearl", ENCHANTED_ENDER_PEARL_ICON);
		addItemComponent(ENCHANTED_OBSIDIAN, "Enchanted Obsidian", ENCHANTED_OBSIDIAN_ICON);
	}

	/**
	 * Shows preview/mock data in the widget configuration screen
	 */
	private void addPreviewComponents() {
		ItemStack grandIcon = GRAND_BOTTLE_ICON.get();
		ItemStack titanicIcon = TITANIC_BOTTLE_ICON.get();
		ItemStack miteIcon = MITE_GEL_ICON.get();

		// Show sample data with realistic values
		addComponent(Components.iconTextComponent(
			grandIcon != null ? grandIcon : new ItemStack(Items.EXPERIENCE_BOTTLE),
			Text.literal("Grand Experience Bottle ")
				.formatted(Formatting.WHITE)
				.append(Text.literal("x25").formatted(Formatting.YELLOW))
				.append(Text.literal(" (").formatted(Formatting.GRAY))
				.append(ItemTooltip.getCoinsMessage(125000, 1))
				.append(Text.literal(")").formatted(Formatting.GRAY))
		));

		addComponent(Components.iconTextComponent(
			titanicIcon != null ? titanicIcon : new ItemStack(Items.EXPERIENCE_BOTTLE),
			Text.literal("Titanic Experience Bottle ")
				.formatted(Formatting.WHITE)
				.append(Text.literal("x3").formatted(Formatting.YELLOW))
				.append(Text.literal(" (").formatted(Formatting.GRAY))
				.append(ItemTooltip.getCoinsMessage(60000000, 1))
				.append(Text.literal(")").formatted(Formatting.GRAY))
		));

		addComponent(Components.iconTextComponent(
			miteIcon != null ? miteIcon : new ItemStack(Items.SLIME_BALL),
			Text.literal("Mite Gel ")
				.formatted(Formatting.WHITE)
				.append(Text.literal("x12").formatted(Formatting.YELLOW))
				.append(Text.literal(" (").formatted(Formatting.GRAY))
				.append(ItemTooltip.getCoinsMessage(24000, 1))
				.append(Text.literal(")").formatted(Formatting.GRAY))
		));
	}

	/**
	 * Counts tracked items in the player's inventory
	 */
	private void countInventoryItems() {
		if (CLIENT.player == null) return;

		// Initialize counts
		itemCounts.put(GRAND_EXPERIENCE_BOTTLE, 0);
		itemCounts.put(TITANIC_EXPERIENCE_BOTTLE, 0);
		itemCounts.put(MITE_GEL, 0);
		itemCounts.put(ENCHANTED_END_STONE, 0);
		itemCounts.put(ENCHANTED_ENDER_PEARL, 0);
		itemCounts.put(ENCHANTED_OBSIDIAN, 0);

		// Iterate through main inventory stacks (slots 0-35, excluding skyblock menu slot)
		for (int i = 0; i < CLIENT.player.getInventory().getMainStacks().size(); i++) {
			if (i == 8) continue; // Skip skyblock star menu slot

			ItemStack stack = CLIENT.player.getInventory().getMainStacks().get(i);
			if (stack.isEmpty()) continue;

			// Use getSkyblockId() which returns the raw ID from the item's custom data
			String skyblockId = stack.getSkyblockId();

			// Check items by name in case the skyblockId is empty and cannot be parsed
			if (skyblockId.isEmpty()) {
				String displayName = stack.getName().getString().toLowerCase();
				if (displayName.contains("grand experience bottle")) {
					skyblockId = GRAND_EXPERIENCE_BOTTLE;
				} else if (displayName.contains("titanic experience bottle")) {
					skyblockId = TITANIC_EXPERIENCE_BOTTLE;
				} else if (displayName.contains("mite gel")) {
					skyblockId = MITE_GEL;
				} else if (displayName.contains("enchanted end stone")) {
					skyblockId = ENCHANTED_END_STONE;
				} else if (displayName.contains("enchanted ender pearl")) {
					skyblockId = ENCHANTED_ENDER_PEARL;
				} else if (displayName.contains("enchanted obsidian")) {
					skyblockId = ENCHANTED_OBSIDIAN;
				}
			}

			if (itemCounts.containsKey(skyblockId)) {
				itemCounts.merge(skyblockId, stack.getCount(), Integer::sum);
			}
		}
	}

	/**
	 * Fetches bazaar prices for tracked items
	 */
	private void updatePrices() {
		itemPrices.put(GRAND_EXPERIENCE_BOTTLE, ItemUtils.getItemPrice(GRAND_EXPERIENCE_BOTTLE));
		itemPrices.put(TITANIC_EXPERIENCE_BOTTLE, ItemUtils.getItemPrice(TITANIC_EXPERIENCE_BOTTLE));
		itemPrices.put(MITE_GEL, ItemUtils.getItemPrice(MITE_GEL));
		itemPrices.put(ENCHANTED_END_STONE, ItemUtils.getItemPrice(ENCHANTED_END_STONE));
		itemPrices.put(ENCHANTED_ENDER_PEARL, ItemUtils.getItemPrice(ENCHANTED_ENDER_PEARL));
		itemPrices.put(ENCHANTED_OBSIDIAN, ItemUtils.getItemPrice(ENCHANTED_OBSIDIAN));
	}

	/**
	 * Adds a component line for a tracked item.
	 * Format: [ITEM_ICON] [ITEM_NAME] x[COUNT] ([TOTAL_VALUE] coins)
	 *
	 * @param itemId the Skyblock item ID
	 * @param displayName the display name for the item
	 * @param iconSupplier supplier for the item icon
	 */
	private void addItemComponent(String itemId, String displayName, Supplier<ItemStack> iconSupplier) {
		int count = itemCounts.getOrDefault(itemId, 0);
		DoubleBooleanPair priceData = itemPrices.getOrDefault(itemId, DoubleBooleanPair.of(0, false));
		double unitPrice = priceData.leftDouble();
		boolean hasData = priceData.rightBoolean();
		double totalValue = unitPrice * count;

		// Get item stack for icon (with fallback)
		ItemStack icon = iconSupplier.get();
		if (icon == null) {
			icon = new ItemStack(Items.BARRIER);
		}

		// Build the text: <name> x<count> (<total value> coins)
		MutableText text = Text.literal(displayName + " ")
				.formatted(Formatting.WHITE)
				.append(Text.literal("x" + count).formatted(Formatting.YELLOW));

		// Add price information
		if (hasData && totalValue > 0) {
			text.append(Text.literal(" (").formatted(Formatting.GRAY))
					.append(ItemTooltip.getCoinsMessage(totalValue, 1))
					.append(Text.literal(")").formatted(Formatting.GRAY));
		} else if (count > 0) {
			text.append(Text.literal(" (No price data)").formatted(Formatting.RED));
		}

		// Add component to widget
		addComponent(Components.iconTextComponent(icon, text));
	}

	@Override
	public Text getDisplayName() {
		return Text.literal("Ender Node Tracker");
	}
}
