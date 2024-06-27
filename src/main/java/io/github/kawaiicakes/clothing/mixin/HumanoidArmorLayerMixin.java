package io.github.kawaiicakes.clothing.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The nature of {@link HumanoidArmorLayer#getArmorResource(Entity, ItemStack, EquipmentSlot, String)} handicaps how
 * much I can do wrt texturing custom models. Since a new implementation of {@link HumanoidArmorLayer} is what I've
 * elected to do ({@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer}), this mixin exists to prevent
 * clothing from being "double-dipped" for render.
 * <br><br>
 * P.S. I'm not sure if it's necessary to annotate this with client dist only...
 * @author kawaiicakes
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>> {
    @Inject(
            method = "renderArmorPiece",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void renderArmorPiece(
            PoseStack pPoseStack, MultiBufferSource pBuffer,
            T pLivingEntity,
            EquipmentSlot pSlot,
            int p_117123_, A pModel,
            CallbackInfo ci
    ) {
        // IDK how to capture locals properly or seed MixinExtras into this build, so I'm calling this again lol
        if (pLivingEntity.getItemBySlot(pSlot).getItem() instanceof ClothingItem) ci.cancel();
    }
}
