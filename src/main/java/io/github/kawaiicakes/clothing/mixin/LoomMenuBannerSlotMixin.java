package io.github.kawaiicakes.clothing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.world.item.BannerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$3")
public class LoomMenuBannerSlotMixin {
    /**
     * Far less destructive way to allow the banner slot to accept clothing items than in previous versions.
     * @param object the object being passed to the {@code instanceof} call
     * @param original the original result of the {@code instanceof} call
     * @return the result of {@code original} or if {@code object instanceof} {@link ClothingItem}
     */
    @WrapOperation(
            method = "mayPlace",
            constant = @Constant(classValue = BannerItem.class)
    )
    private boolean mayPlaceHandler(Object object, Operation<Boolean> original) {
        return original.call(object) || object instanceof ClothingItem<?>;
    }
}
