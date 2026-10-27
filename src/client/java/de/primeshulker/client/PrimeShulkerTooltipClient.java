package de.primeshulker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;

public class PrimeShulkerTooltipClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerData shulker ? new ShulkerClientTooltip(shulker) : null);
	}
}
