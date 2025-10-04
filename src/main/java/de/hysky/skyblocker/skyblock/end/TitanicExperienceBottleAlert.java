package de.hysky.skyblocker.skyblock.end;

import de.hysky.skyblocker.skyblock.item.tooltip.ItemTooltip;
import de.hysky.skyblocker.utils.ItemUtils;
import de.hysky.skyblocker.utils.chat.ChatFilterResult;
import de.hysky.skyblocker.utils.chat.ChatPatternListener;
import de.hysky.skyblocker.utils.render.title.Title;
import de.hysky.skyblocker.utils.render.title.TitleContainer;
import it.unimi.dsi.fastutil.doubles.DoubleBooleanPair;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;

/**
 * Displays a notification when a Titanic Experience Bottle is found from an ender node.
 */
public class TitanicExperienceBottleAlert extends ChatPatternListener {
	private static final String TITANIC_EXPERIENCE_BOTTLE_ID = "TITANIC_EXPERIENCE_BOTTLE";

	public TitanicExperienceBottleAlert() {
		super("You found Titanic Experience Bottle");
	}

	@Override
	protected ChatFilterResult state() {
		// Always active, doesn't filter the message
		return ChatFilterResult.PASS;
	}

	@Override
	protected boolean onMatch(Text message, Matcher matcher) {
		// Get the bazaar price for Titanic Experience Bottle
		DoubleBooleanPair priceData = ItemUtils.getItemPrice(TITANIC_EXPERIENCE_BOTTLE_ID);
		double price = priceData.leftDouble();
		boolean hasData = priceData.rightBoolean();

		// Create the title text
		MutableText titleText = Text.literal("Titanic Experience Bottle found!")
				.formatted(Formatting.GOLD, Formatting.BOLD);

		// Create the subtitle with price information
		MutableText priceText;
		if (hasData && price > 0) {
			priceText = Text.literal("\nCurrent bazaar price: ")
					.formatted(Formatting.YELLOW)
					.append(ItemTooltip.getCoinsMessage(price, 1));
		} else {
			priceText = Text.literal("\nCurrent bazaar price: ")
					.formatted(Formatting.YELLOW)
					.append(Text.literal("No data").formatted(Formatting.RED));
		}

		// Combine title and subtitle
		MutableText fullText = titleText.append(priceText);

		// Create and display the title for 3 seconds
		Title title = new Title(fullText);
		TitleContainer.addTitleAndPlaySound(title, 60);

		return false; // Don't filter the original message
	}
}
