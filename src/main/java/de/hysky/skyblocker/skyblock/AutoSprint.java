package de.hysky.skyblocker.skyblock;

import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;

/**
 * Auto-sprint feature that automatically maintains sprint when moving forward.
 * This is an accessibility enhancement to reduce the need for repeatedly pressing the sprint key.
 * 
 * <p>When enabled, the player will automatically sprint when:
 * <ul>
 *   <li>Moving forward (W key is pressed)</li>
 *   <li>Player has enough hunger to sprint</li>
 *   <li>Other vanilla sprint conditions are met</li>
 * </ul>
 * 
 * <p>This feature is client-side only and does not interact with server-specific mechanics.
 */
public class AutoSprint {

	@Init
	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(AutoSprint::tick);
	}

	/**
	 * Called every client tick to handle auto-sprint logic.
	 */
	private static void tick(MinecraftClient client) {
		if (!SkyblockerConfigManager.get().general.autoSprint) {
			return;
		}

		ClientPlayerEntity player = client.player;
		if (player == null) return;

		GameOptions options = client.options;

		// Auto-sprint when moving forward and not already sprinting
		if (options.forwardKey.isPressed() && 
		    !player.isSprinting() && 
		    canSprint(player)) {
			player.setSprinting(true);
		}
	}

	/**
	 * Checks if the player can sprint based on game mechanics.
	 * 
	 * @param player the player to check
	 * @return true if the player can sprint, false otherwise
	 */
	private static boolean canSprint(ClientPlayerEntity player) {
		// Player can sprint if:
		// - Not using an item (eating, drinking, blocking)
		// - Has enough hunger (> 6 hunger points, which is > 3 in the hunger bar)
		// - Not sneaking
		// - Not in water (unless swimming) or touching water
		// - Not on a ladder
		// - Not riding an entity
		return !player.isUsingItem() 
			&& player.getHungerManager().getFoodLevel() > 6 
			&& !player.isSneaking()
			&& !player.isTouchingWater()
			&& !player.isClimbing()
			&& !player.hasVehicle();
	}
}

