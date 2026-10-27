package de.primeshulker.client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Liest die Zusatzreihen der PrimeBlocks-Shulker (Lagerung I-III) aus
 * {@code custom_data.PublicBukkitValues["primeenchants:storage/row/N"]}.
 *
 * <p>Format pro Reihe (per Beispieldaten ermittelt):
 * 5 Header-Bytes (16, 6, 32, 0, 1), dann Int (big-endian) = Slotanzahl (9), dann pro Slot ein Int (big-endian) Länge
 * (0 = leer) gefolgt von einem gzip-komprimierten NBT-Item ({DataVersion, id, count, components?}).
 * row/1 = Slots 27-35, row/2 = 36-44, row/3 = 45-53.
 */
public final class PrimeStorage {
	private static final String BUKKIT = "PublicBukkitValues";
	private static final String ROW_KEY = "primeenchants:storage/row/";
	public static final int VANILLA_SLOTS = 27;
	private static final int HEADER = 5;

	private static CustomData cachedKey;
	private static List<ItemStack> cachedValue = List.of();

	private PrimeStorage() {
	}

	/** Liefert die Items der Extra-Slots ab Slot 27 (Leerslots als {@link ItemStack#EMPTY}), nie null. */
	public static List<ItemStack> extraItems(ItemStack stack) {
		CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
		if (cd == null) {
			return List.of();
		}
		if (cd == cachedKey) {
			return cachedValue;
		}
		List<ItemStack> result = decode(cd.copyTag());
		cachedKey = cd;
		cachedValue = result;
		return result;
	}

	private static List<ItemStack> decode(CompoundTag root) {
		Optional<CompoundTag> bukkit = root.getCompound(BUKKIT);
		if (bukkit.isEmpty()) {
			return List.of();
		}
		List<ItemStack> out = new ArrayList<>();
		for (int row = 1; row <= 9; row++) {
			Optional<byte[]> bytes = bukkit.get().getByteArray(ROW_KEY + row);
			if (bytes.isEmpty()) {
				break;
			}
			List<ItemStack> slots = decodeRow(bytes.get());
			while (slots.size() < 9) {
				slots.add(ItemStack.EMPTY);
			}
			out.addAll(slots.subList(0, 9));
		}
		return out;
	}

	private static List<ItemStack> decodeRow(byte[] data) {
		List<ItemStack> slots = new ArrayList<>();
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			in.skipNBytes(HEADER);
			int count = in.readInt();
			for (int i = 0; i < count && i < 64; i++) {
				int len = in.readInt();
				if (len <= 0) {
					slots.add(ItemStack.EMPTY);
					continue;
				}
				byte[] blob = in.readNBytes(len);
				slots.add(readItem(blob));
			}
		} catch (Exception e) {
			// unbekanntes Format: bis hierher gelesene Slots behalten
		}
		return slots;
	}

	private static ItemStack readItem(byte[] gzip) {
		try {
			CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(gzip), NbtAccounter.unlimitedHeap());
			var level = Minecraft.getInstance().level;
			if (level == null) {
				return ItemStack.EMPTY;
			}
			tag.remove("DataVersion");
			RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
			return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
