package de.primeshulker.client.mixin;

import java.util.function.Consumer;

import de.primeshulker.client.ShulkerData;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blendet die Vanilla-Textliste aus, solange der Raster-Tooltip (Shift) sie ersetzt. */
@Mixin(ItemContainerContents.class)
public class ItemContainerContentsMixin {
	@Inject(at = @At("HEAD"), method = "addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V", cancellable = true)
	private void primeshulker$hideText(Item.TooltipContext ctx, Consumer<Component> out, TooltipFlag flag, DataComponentGetter getter, CallbackInfo ci) {
		if (ShulkerData.shiftDown()) {
			ci.cancel();
		}
	}
}
