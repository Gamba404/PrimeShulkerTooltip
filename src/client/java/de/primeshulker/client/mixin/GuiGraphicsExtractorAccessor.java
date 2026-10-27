package de.primeshulker.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
	@Accessor("deferredTooltip")
	Runnable primeshulker$getDeferredTooltip();

	@Accessor("deferredTooltip")
	void primeshulker$setDeferredTooltip(Runnable tooltip);
}
