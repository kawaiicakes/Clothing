package io.github.kawaiicakes.clothing.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static io.github.kawaiicakes.clothing.ClothingRegistry.BLEACH_POTION;

@Mixin(PotionUtils.class)
public abstract class PotionUtilsMixin {
    @Shadow
    public static Potion getPotion(ItemStack pStack) {
        return null;
    }

    @WrapMethod(method = "getColor(Lnet/minecraft/world/item/ItemStack;)I")
    private static int getColorBleach(ItemStack pStack, Operation<Integer> original) {
        if (BLEACH_POTION.get().equals(getPotion(pStack))) return 0xE2FABE;

        return original.call(pStack);
    }

    @WrapMethod(method = "getColor(Lnet/minecraft/world/item/alchemy/Potion;)I")
    private static int getColorBleach(Potion potion, Operation<Integer> original) {
        if (BLEACH_POTION.get().equals(potion)) return 0xE2FABE;

        return original.call(potion);
    }
}
