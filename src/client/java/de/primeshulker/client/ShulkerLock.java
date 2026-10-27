package de.primeshulker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Zustand der "Tooltip festhalten"-Funktion (Shift + Alt): Mit festgehaltenem Raster lassen sich die Items einzeln anschauen. */
public final class ShulkerLock {
	private static Screen lockedScreen;

	private ShulkerLock() {
	}

	public static boolean keysPressed() {
		Minecraft mc = Minecraft.getInstance();
		return mc.hasShiftDown() && mc.hasAltDown();
	}

	public static void set(Screen screen, boolean locked) {
		lockedScreen = locked ? screen : null;
	}

	public static boolean isActive() {
		return lockedScreen != null && lockedScreen == Minecraft.getInstance().gui.screen();
	}
}
