package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This extends {@link HumanoidArmorLayer} in case a third-party mod references instances of that class to render
 * stuff. Furthermore, it also exists so that vanilla armour may render still when mods like Trinkets are
 * installed, hopefully.
 * @author kawaiicakes
 */
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // TODO: initialize model fields automatically based on what EntityType is passed to the constructor.
    // TODO: ^ above segues into idea of static model "repository" filled with baked models.
    /**
     * Added during {@link net.minecraftforge.client.event.EntityRenderersEvent.AddLayers} to appropriate renderer.
     * @param pBaseClothingModel This model is intended to be used for clothing resting just above the skin; like
     *                           T-shirts or ski masks.
     * @param pThickClothingModel This model is worn over top of the base, but rests underneath armour. Think of a
     *                            three-piece suit, or even just a tie.
     */
    public HumanoidClothingLayer(RenderLayerParent<T, M> pRenderer, A pBaseClothingModel, A pThickClothingModel) {
        super(pRenderer, pBaseClothingModel, pThickClothingModel);
    }

    @Override
    public void render(
            @NotNull PoseStack pMatrixStack,
            @NotNull MultiBufferSource pBuffer, int pPackedLight,
            @NotNull T pLivingEntity, float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        EquipmentSlot[] slots = {
                EquipmentSlot.FEET,
                EquipmentSlot.LEGS,
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = pLivingEntity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ClothingItem clothing)) continue;
            if (!clothing.getSlot().equals(slot)) continue;

            A clothingModel;

            try {
                //noinspection unchecked
                clothingModel = (A) clothing.getClothingModel(pLivingEntity, stack, slot);
            } catch (RuntimeException e) {
                LOGGER.error("Unable to cast model to appropriate type!", e);
                continue;
            }

            boolean hasGlint = stack.hasFoil();

            this.getParentModel().copyPropertiesTo(clothingModel);
            this.setPartVisibility(clothingModel, slot);

            int i = clothing.getColor(stack);
            float r = (float)(i >> 16 & 255) / 255.0F;
            float g = (float)(i >> 8 & 255) / 255.0F;
            float b = (float)(i & 255) / 255.0F;

            this.renderModel(
                    pMatrixStack,
                    pBuffer, pPackedLight,
                    hasGlint,
                    clothingModel,
                    r, g, b, clothing.getAlpha(
                            pLivingEntity,
                            stack, slot,
                            pPackedLight,
                            pLimbSwing, pLimbSwingAmount,
                            pPartialTicks, pAgeInTicks,
                            pNetHeadYaw, pHeadPitch
                    ),
                    this.getArmorResource(pLivingEntity, stack, slot, null)
            );

            ResourceLocation overlayLocation = clothing.overlayResource(
                    pLivingEntity,
                    stack, slot,
                    pPackedLight,
                    pLimbSwing, pLimbSwingAmount,
                    pPartialTicks, pAgeInTicks,
                    pNetHeadYaw, pHeadPitch
            );
            if (overlayLocation == null) continue;
            this.renderModel(
                    pMatrixStack,
                    pBuffer, pPackedLight,
                    hasGlint,
                    clothingModel,
                    1.0F, 1.0F, 1.0F, clothing.getAlpha(
                            pLivingEntity,
                            stack, slot,
                            pPackedLight,
                            pLimbSwing, pLimbSwingAmount,
                            pPartialTicks, pAgeInTicks,
                            pNetHeadYaw, pHeadPitch
                    ),
                    this.getArmorResource(pLivingEntity, stack, slot, "overlay")
            );
        }
    }

    protected void renderModel(
            PoseStack pPoseStack,
            MultiBufferSource pBuffer, int pPackedLight, boolean pGlint,
            Model pModel,
            float pRed, float pGreen, float pBlue, float pAlpha,
            ResourceLocation armorResource
    ) {
        VertexConsumer vertexconsumer =
                ItemRenderer.getArmorFoilBuffer(
                        pBuffer,
                        RenderType.armorCutoutNoCull(armorResource),
                        false,
                        pGlint
                );

        pModel.renderToBuffer(
                pPoseStack,
                vertexconsumer,
                pPackedLight,
                OverlayTexture.NO_OVERLAY,
                pRed,
                pGreen,
                pBlue,
                pAlpha
        );
    }

    @Override
    @NotNull
    @ParametersAreNonnullByDefault
    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        // TODO
        return super.getArmorResource(entity, stack, slot, type);
    }
}