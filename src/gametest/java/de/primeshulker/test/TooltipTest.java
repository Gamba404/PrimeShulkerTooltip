package de.primeshulker.test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.inventory.Slot;

/** Rendert die Tooltips im echten Spiel und speichert Screenshots (build/run/clientGameTest/screenshots). */
public class TooltipTest implements FabricClientGameTest {
	private static final String[] COLORS = {"", "white_", "orange_", "magenta_", "light_blue_", "yellow_", "lime_", "pink_", "gray_",
		"light_gray_", "cyan_", "purple_", "blue_", "brown_", "green_", "red_", "black_"};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.getInput().pressKey(o -> o.keyInventory);
			context.waitForScreen(net.minecraft.client.gui.screens.inventory.InventoryScreen.class);

			String vanillaContent = container("diamond", 5, "netherite_ingot", 3, "golden_apple", 12, "oak_log", 64, "ender_pearl", 16, "elytra", 1,
				"experience_bottle", 32, "cooked_beef", 64, "arrow", 64);
			show(context, world, "light_blue_shulker_box[container=" + vanillaContent + "]", "01-vanilla-ohne-shift", false);
			show(context, world, "light_blue_shulker_box[container=" + vanillaContent + "]", "02-vanilla-mit-shift", true);
			show(context, world, "orange_shulker_box", "03-leer", true);

			for (int rows = 4; rows <= 6; rows++) {
				String extra = "custom_data={PublicBukkitValues:{";
				String[][] rowItems = {
					{"diamond_block", "emerald_block", "gold_block", "iron_block", "lapis_block", "redstone_block", "coal_block", "copper_block", "netherite_block"},
					{"oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log", "bamboo_block"},
					{"cooked_beef", "bread", "golden_carrot", "cake", "apple", "melon_slice", "pumpkin_pie", "cookie", "honey_bottle"}};
				for (int r = 0; r < rows - 3; r++) {
					extra += (r > 0 ? "," : "") + "\"primeenchants:storage/row/" + (r + 1) + "\":" + rowBytes(rowItems[r]);
				}
				extra += "}}";
				String item = "lime_shulker_box[container=" + vanillaContent + "," + extra + ",lore=[{text:\"Shulker hat " + rows + " Reihen Lagerplatz\"}]]";
				show(context, world, item, "04-prime-" + rows + "-reihen", true);
			}

			lockedHover(context, world, "light_blue_shulker_box[container=" + vanillaContent + "]");

			int n = 0;
			for (String c : COLORS) {
				show(context, world, c + "shulker_box[container=" + container("diamond", 1) + "]",
					String.format("farbe-%02d-%s", n++, c.isEmpty() ? "standard" : c.substring(0, c.length() - 1)), true);
			}
		}
	}

	private static void show(ClientGameTestContext context, TestSingleplayerContext world, String item, String name, boolean shift) {
		world.getServer().runCommand("clear @p");
		world.getServer().runCommand("item replace entity @p container.9 with " + item);
		context.waitTicks(6);
		double[] pos = findBox(context);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(4);
		if (shift) {
			context.getInput().holdShift();
			context.waitTicks(4);
		}
		context.takeScreenshot(name);
		if (shift) {
			context.getInput().releaseShift();
		}
	}

	/** Shift + Alt: Tooltip festhalten, dann mit der Maus auf ein Item im Raster fahren. */
	private static void lockedHover(ClientGameTestContext context, TestSingleplayerContext world, String item) {
		world.getServer().runCommand("clear @p");
		world.getServer().runCommand("item replace entity @p container.9 with " + item);
		context.waitTicks(6);
		double[] pos = findBox(context);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(4);
		context.getInput().holdShift();
		context.getInput().holdAlt();
		context.waitTicks(4);
		context.getInput().setCursorPos(pos[0] + 135, pos[1] + 15);
		context.waitTicks(6);
		context.takeScreenshot("05-fixiert-item-hover");
		context.getInput().releaseAlt();
		context.getInput().releaseShift();
	}

	private static double[] findBox(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			var screen = (AbstractContainerScreen<?>) mc.gui.screen();
			for (Slot slot : screen.getMenu().slots) {
				if (slot.getItem().getItem().toString().contains("shulker_box")) {
					double scale = mc.getWindow().getGuiScale();
					return new double[] {((screen.width - 176) / 2 + slot.x + 8) * scale, ((screen.height - 166) / 2 + slot.y + 8) * scale};
				}
			}
			return new double[] {0, 0};
		});
	}

	/** container-Komponente aus (Item, Anzahl)-Paaren, ab Slot 0, mit einer Lücke bei jedem 4. Slot. */
	private static String container(Object... itemsAndCounts) {
		StringBuilder sb = new StringBuilder("[");
		int slot = 0;
		for (int i = 0; i < itemsAndCounts.length; i += 2) {
			if (slot % 4 == 3) {
				slot++;
			}
			sb.append(i > 0 ? "," : "").append("{slot:").append(slot++).append(",item:{id:\"minecraft:").append(itemsAndCounts[i])
				.append("\",count:").append(itemsAndCounts[i + 1]).append("}}");
		}
		return sb.append("]").toString();
	}

	/** PrimeBlocks-Zeilenformat: 5 Header-Bytes, Slotanzahl, je Slot Länge + gzip-NBT. */
	private static String rowBytes(String[] ids) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream data = new DataOutputStream(out);
			data.write(new byte[] {16, 6, 32, 0, 1});
			data.writeInt(9);
			for (String id : ids) {
				CompoundTag tag = new CompoundTag();
				tag.putInt("DataVersion", 1);
				tag.putString("id", "minecraft:" + id);
				tag.putInt("count", 1);
				ByteArrayOutputStream blob = new ByteArrayOutputStream();
				NbtIo.writeCompressed(tag, blob);
				data.writeInt(blob.size());
				data.write(blob.toByteArray());
			}
			StringBuilder sb = new StringBuilder("[B;");
			byte[] bytes = out.toByteArray();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(i > 0 ? "," : "").append(bytes[i]).append('b');
			}
			return sb.append("]").toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
