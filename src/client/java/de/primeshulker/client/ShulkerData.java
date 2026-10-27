package de.primeshulker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Inhalt und Größe einer Shulker für den Tooltip.
 * Die Reihenzahl ergibt sich aus dem Inhalt und aus der PrimeBlocks-Lagerung (Stufe I-III = 4-6 Reihen).
 */
public record ShulkerData(List<ItemStack> items, int rows, DyeColor color) implements TooltipComponent {
	public static final int COLUMNS = 9;
	public static final int VANILLA_ROWS = 3;
	public static final int MAX_ROWS = 12;

	private static final Pattern STORAGE = Pattern.compile("(lagerung|storage)\\s*(iii|ii|i|[1-6])?", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROWS_LORE = Pattern.compile("(\\d+)\\s*Reihen", Pattern.CASE_INSENSITIVE);

	public static boolean shiftDown() {
		return Minecraft.getInstance().hasShiftDown();
	}

	public static Optional<ShulkerData> of(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock box)) {
			return Optional.empty();
		}
		ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		List<ItemStack> items = new ArrayList<>(contents.itemCopies().toList());
		List<ItemStack> extra = PrimeStorage.extraItems(stack);
		if (!extra.isEmpty()) {
			while (items.size() < PrimeStorage.VANILLA_SLOTS) {
				items.add(ItemStack.EMPTY);
			}
			items.addAll(extra);
		}
		int rows = Math.max(VANILLA_ROWS + storageLevel(stack), (items.size() + COLUMNS - 1) / COLUMNS);
		rows = Math.clamp(rows, VANILLA_ROWS, MAX_ROWS);
		return Optional.of(new ShulkerData(items, rows, box.getColor()));
	}

	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	/** Lagerungs-Stufe (0-3) aus Verzauberung, Lore ("6 Reihen") oder Name. */
	static int storageLevel(ItemStack stack) {
		int level = 0;
		ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (var entry : enchants.entrySet()) {
			Holder<Enchantment> holder = entry.getKey();
			int enchantLevel = entry.getIntValue();
			String text = Enchantment.getFullname(holder, enchantLevel).getString();
			String id = holder.unwrapKey().map(key -> key.identifier().toString()).orElse("");
			if (STORAGE.matcher(text).find() || STORAGE.matcher(id).find()) {
				level = Math.max(level, Math.max(parseStorage(text, enchantLevel), enchantLevel));
			}
		}
		for (Component line : stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines()) {
			String text = line.getString();
			level = Math.max(level, parseStorage(text, 0));
			Matcher rows = ROWS_LORE.matcher(text);
			if (rows.find()) {
				level = Math.max(level, Integer.parseInt(rows.group(1)) - VANILLA_ROWS);
			}
		}
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name != null) {
			level = Math.max(level, parseStorage(name.getString(), 0));
		}
		return Math.min(level, MAX_ROWS - VANILLA_ROWS);
	}

	private static int parseStorage(String text, int fallback) {
		Matcher m = STORAGE.matcher(text);
		if (!m.find()) {
			return 0;
		}
		String level = m.group(2);
		if (level == null) {
			return fallback;
		}
		return switch (level.toLowerCase()) {
			case "i" -> 1;
			case "ii" -> 2;
			case "iii" -> 3;
			default -> Integer.parseInt(level);
		};
	}
}
