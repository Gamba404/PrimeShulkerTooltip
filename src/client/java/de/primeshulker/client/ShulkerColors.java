package de.primeshulker.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

/** Hintergrundfarbe pro Shulker: Dye-Farbe, bei der ungefärbten Kiste der Durchschnitt ihrer Textur. */
public final class ShulkerColors {
	private static final int FALLBACK = 0x8B5E8B;
	private static int undyed = -1;

	private ShulkerColors() {
	}

	public static int of(DyeColor color) {
		if (color != null) {
			return color.getTextureDiffuseColor() & 0xFFFFFF;
		}
		if (undyed < 0) {
			undyed = averageTexture("textures/entity/shulker/shulker.png");
		}
		return undyed < 0 ? FALLBACK : undyed;
	}

	private static int averageTexture(String path) {
		try {
			var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace(path));
			if (resource.isEmpty()) {
				return -1;
			}
			try (var in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
				long r = 0;
				long g = 0;
				long b = 0;
				long count = 0;
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						int argb = image.getPixel(x, y);
						if ((argb >>> 24) < 200) {
							continue;
						}
						r += (argb >> 16) & 0xFF;
						g += (argb >> 8) & 0xFF;
						b += argb & 0xFF;
						count++;
					}
				}
				return count == 0 ? -1 : ((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count);
			}
		} catch (Exception e) {
			return -1;
		}
	}
}
