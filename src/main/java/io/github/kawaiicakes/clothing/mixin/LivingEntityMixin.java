package io.github.kawaiicakes.clothing.mixin;

import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "playEquipSound", at = @At("HEAD"))
    private void playEquipSound(ItemStack itemstack, CallbackInfo ci) {
        if (itemstack.getItem() instanceof ClothingItem<?> clothingItem) {
            this.playSound(clothingItem.getEquipSound(itemstack));
        }
    }
}
