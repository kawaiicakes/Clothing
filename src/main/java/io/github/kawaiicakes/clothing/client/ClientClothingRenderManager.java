package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNullableByDefault;

/**
 * TODO
 */
public interface ClientClothingRenderManager {
    Logger LOGGER = LogUtils.getLogger();
    ClientClothingRenderManager DEFAULT = new ClientClothingRenderManager() {
        @Override
        public <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void render(
                @NotNull HumanoidClothingLayer<T, M, A> pClothingLayer,
                @NotNull ItemStack pItemStack,
                @NotNull PoseStack pMatrixStack,
                @NotNull MultiBufferSource pBuffer, int pPackedLight,
                @NotNull T pLivingEntity,
                float pLimbSwing, float pLimbSwingAmount,
                float pPartialTicks, float pAgeInTicks,
                float pNetHeadYaw, float pHeadPitch
        ) {
            LOGGER.error("If you are seeing this, ClientClothingRenderManager has failed to initialize!");
        }
    };
    static ClientClothingRenderManager of(ItemStack stack) {
        if (!(stack.getItem() instanceof ClothingItem clothingItem))
            throw new IllegalArgumentException("Passed stack's item is not a ClothingItem instance!");
        return of(clothingItem);
    }
    static ClientClothingRenderManager of(ClothingItem clothingItem) {
        return clothingItem.getClientClothingRenderManager() instanceof ClientClothingRenderManager m ? m : DEFAULT;
    }

    /**
     * TODO
     * @param pClothingLayer
     * @param pItemStack
     * @param pMatrixStack
     * @param pBuffer
     * @param pPackedLight
     * @param pLivingEntity
     * @param pLimbSwing
     * @param pLimbSwingAmount
     * @param pPartialTicks
     * @param pAgeInTicks
     * @param pNetHeadYaw
     * @param pHeadPitch
     * @param <T>
     * @param <A>
     * @param <M>
     */
    <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void render(
            @NotNull HumanoidClothingLayer<T, M, A> pClothingLayer,
            @NotNull ItemStack pItemStack,
            @NotNull PoseStack pMatrixStack,
            @NotNull MultiBufferSource pBuffer, int pPackedLight,
            @NotNull T pLivingEntity,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    );

    // FIXME: clothing does not become translucent for GenericClothingItems
    // FIXME: values not equal to 1.0F cause colour of overlay to "infect" base layer for GenericClothingItems
    /**
     * Implementations will return the alpha value for render.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @return The value of alpha as a float. Permitted values are 0.0 to 1.0 inclusive.
     */
    @SuppressWarnings("unused")
    @ParametersAreNullableByDefault
    default float getAlpha(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return 1.0F;
    }
}
