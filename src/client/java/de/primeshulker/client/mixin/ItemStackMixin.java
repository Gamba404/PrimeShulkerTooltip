package de.primeshulker.client.mixin;

import java.util.Optional;

import de.primeshulker.client.ShulkerData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(at = @At("HEAD"), method = "getTooltipImage()Ljava/util/Optional;", cancellable = true)
	private void primeshulker$image(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (!ShulkerData.shiftDown()) {
			return;
		}
		ShulkerData.of((ItemStack) (Object) this).ifPresent(d -> cir.setReturnValue(Optional.of(d)));
	}
}
