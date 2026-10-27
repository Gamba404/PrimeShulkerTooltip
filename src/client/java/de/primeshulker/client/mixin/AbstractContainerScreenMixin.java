package de.primeshulker.client.mixin;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.primeshulker.client.ShulkerData;
import de.primeshulker.client.ShulkerLock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Shift + Alt hält das Raster der Shulker unter der Maus fest. Die Kiste bleibt "gehovert", auch wenn die Maus
 * weiterwandert, so lassen sich die Items im Raster einzeln anschauen.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;
	@Final
	@Shadow
	protected AbstractContainerMenu menu;

	@Unique
	private Slot primeshulker$lockSlot;
	@Unique
	private int primeshulker$lockX;
	@Unique
	private int primeshulker$lockY;

	@Shadow
	protected abstract List<Component> getTooltipFromContainerItem(ItemStack stack);

	@Inject(at = @At("HEAD"), method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", cancellable = true)
	private void primeshulker$keepSlotHovered(Slot slot, double x, double y, CallbackInfoReturnable<Boolean> cir) {
		if (primeshulker$lockSlot == null) {
			return;
		}
		if (!primeshulker$lockSlot.hasItem() || !menu.slots.contains(primeshulker$lockSlot) || !menu.getCarried().isEmpty()) {
			primeshulker$lockSlot = null;
			return;
		}
		cir.setReturnValue(slot == primeshulker$lockSlot);
	}

	@WrapOperation(method = "extractTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"))
	private void primeshulker$lockTooltip(GuiGraphicsExtractor graphics, Font font, List<Component> lines, Optional<TooltipComponent> image,
		int x, int y, Identifier style, boolean extraSpace, Operation<Void> original) {
		if (!ShulkerLock.keysPressed()) {
			primeshulker$lockSlot = null;
		} else if (primeshulker$lockSlot == null && hoveredSlot != null && ShulkerData.of(hoveredSlot.getItem()).isPresent()) {
			primeshulker$lockSlot = hoveredSlot;
			primeshulker$lockX = x;
			primeshulker$lockY = y;
		}
		if (primeshulker$lockSlot != null) {
			ItemStack stack = primeshulker$lockSlot.getItem();
			lines = getTooltipFromContainerItem(stack);
			image = stack.getTooltipImage();
			style = stack.get(DataComponents.TOOLTIP_STYLE);
			x = primeshulker$lockX;
			y = primeshulker$lockY;
		}
		ShulkerLock.set((Screen) (Object) this, primeshulker$lockSlot != null);
		original.call(graphics, font, lines, image, x, y, style, extraSpace);
	}
}
