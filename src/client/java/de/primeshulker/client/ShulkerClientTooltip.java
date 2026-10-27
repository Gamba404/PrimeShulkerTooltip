package de.primeshulker.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ShulkerClientTooltip implements ClientTooltipComponent {
	private static final int SLOT = 18;
	private static final int PADDING = 4;
	private static final float COLOR_KEEP = 0.62f;
	private static final float COLOR_DIM = 0.85f;

	private final ShulkerData data;

	public ShulkerClientTooltip(ShulkerData data) {
		this.data = data;
	}

	@Override
	public int getWidth(Font font) {
		return ShulkerData.COLUMNS * SLOT + 2 * PADDING;
	}

	@Override
	public int getHeight(Font font) {
		return data.rows() * SLOT + 2 * PADDING + 2;
	}

	@Override
	public void extractImage(Font font, int x, int y, int totalWidth, int totalHeight, GuiGraphicsExtractor graphics) {
		int width = getWidth(font);
		int height = data.rows() * SLOT + 2 * PADDING;
		int base = soften(ShulkerColors.of(data.color()));
		int border = shade(base, 0.45f);
		int slotFill = shade(base, 1.12f);

		graphics.fill(x, y, x + width, y + height, 0xFF000000 | border);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | base);

		for (int row = 0; row < data.rows(); row++) {
			for (int col = 0; col < ShulkerData.COLUMNS; col++) {
				int sx = x + PADDING + col * SLOT;
				int sy = y + PADDING + row * SLOT;
				graphics.fill(sx, sy, sx + SLOT, sy + SLOT, 0xFF000000 | border);
				graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xFF000000 | slotFill);
				int index = row * ShulkerData.COLUMNS + col;
				if (index < data.items().size() && !data.items().get(index).isEmpty()) {
					ItemStack stack = data.items().get(index);
					graphics.item(stack, sx + 1, sy + 1);
					graphics.itemDecorations(font, stack, sx + 1, sy + 1);
				}
			}
		}
		if (data.isEmpty()) {
			drawEmptyLabel(font, graphics, x, y, width, height);
		}
	}

	private static void drawEmptyLabel(Font font, GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x40000000);
		String text = "Leer";
		int textWidth = font.width(text);
		int tx = x + (width - textWidth) / 2;
		int ty = y + (height - font.lineHeight) / 2;
		graphics.fill(tx - 6, ty - 4, tx + textWidth + 6, ty + font.lineHeight + 3, 0xE0000000);
		graphics.text(font, text, tx, ty, 0xFFAAAAAA);
	}

	/** Nimmt der Farbe etwas Sättigung und Helligkeit, damit sie nicht grell wirkt. */
	private static int soften(int rgb) {
		float r = (rgb >> 16) & 0xFF;
		float g = (rgb >> 8) & 0xFF;
		float b = rgb & 0xFF;
		float gray = (r + g + b) / 3f;
		return ((int) ((gray + (r - gray) * COLOR_KEEP) * COLOR_DIM) << 16)
			| ((int) ((gray + (g - gray) * COLOR_KEEP) * COLOR_DIM) << 8)
			| (int) ((gray + (b - gray) * COLOR_KEEP) * COLOR_DIM);
	}

	private static int shade(int rgb, float factor) {
		int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
		return (r << 16) | (g << 8) | b;
	}
}
