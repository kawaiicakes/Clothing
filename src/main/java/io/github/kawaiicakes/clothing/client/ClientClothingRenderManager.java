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
 * This is responsible for handling rendering of clothing on a per-implementation basis. It's expected that each
 * "type" of model will have its own implementation; e.g. OBJ, JSON, etc.
 * <br><br>
 * Its purpose is to prevent accidentally reaching across sides, and is quite similar to
 * {@link net.minecraftforge.client.extensions.common.IClientItemExtensions} both in intent and usage.
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
        if (!(stack.getItem() instanceof ClothingItem<?> clothingItem))
            throw new IllegalArgumentException("Passed stack's item is not a ClothingItem instance!");
        return of(clothingItem);
    }
    static ClientClothingRenderManager of(ClothingItem<?> clothingItem) {
        return clothingItem.getClientClothingRenderManager() instanceof ClientClothingRenderManager m ? m : DEFAULT;
    }

    /**
     * This method renders something to the passed {@code pBuffer}. What it renders and how it does it is entirely
     * dependent on your goals and the implementation of the item this is responsible for. A lot of valuable render
     * parameters are included for these purposes.
     * @param pClothingLayer the {@link HumanoidClothingLayer} responsible for doing the rendering. Its type arguments
     *                       will depend on what kind of entity was instantiated for.
     * @param pItemStack    the {@link ItemStack} representing the piece of clothing for render.
     * @param pMatrixStack  the {@link PoseStack} for rendering with. You may manipulate it to adjust model positioning.
     * @param pBuffer       the {@link MultiBufferSource} being rendered to.
     * @param pPackedLight  an {@code int} whose purpose I'm unsure of.
     * @param pLivingEntity the {@link LivingEntity} who {@code pClothingLayer} is for and would be wearing
     *                      the {@code pItemStack}.
     * @param <T> the type of the passed {@code pLivingEntity}.
     * @param <A> the type of the {@code pClothingLayer}'s generic models.
     * @param <M> the type of the {@code pClothingLayer}'s entity's {@link HumanoidModel}. Usually identical to
     *              {@link A}.
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
