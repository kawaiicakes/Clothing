package io.github.kawaiicakes.clothing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kawaiicakes.clothing.common.item.SpoolItem;
import net.minecraft.world.item.DyeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$4")
public abstract class LoomMenuDyeSlotMixin {
    /**
     * Less destructive way to allow the dye slot to accept spools.
     * @param object the object being passed to the {@code instanceof} call
     * @param original the original result of the {@code instanceof} call
     * @return the result of {@code original} or if {@code object instanceof} {@link SpoolItem}
     */
    @WrapOperation(
            method = "mayPlace",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean mayPlaceHandler(Object object, Operation<Boolean> original) {
        return original.call(object) || object instanceof SpoolItem;
    }
}
