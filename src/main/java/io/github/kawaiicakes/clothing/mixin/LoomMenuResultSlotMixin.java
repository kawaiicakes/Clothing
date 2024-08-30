package io.github.kawaiicakes.clothing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kawaiicakes.clothing.common.LoomMenuMixinGetter;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$6")
public abstract class LoomMenuResultSlotMixin {

    @Shadow @Final
    LoomMenu this$0;

    /**
     * Intended to wrap the call to set the banner index data slot to -1 to ALSO set the clothing stratum ordinal to
     * -1. The stratum ordinal is set if the passed integer {@code i} is -1, as it is unknown what other mods might
     * mixin to here and add their own calls to {@link DataSlot#set(int)}.
     */
    @WrapOperation(
            method = "onTake",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"
            )
    )
    private void onTakeHandler(DataSlot instance, int i, Operation<Void> original) {
        if (i == -1) ((LoomMenuMixinGetter) this.this$0).setClothing$stratumOrdinal(-1);
        original.call(instance, i);
    }
}
