package io.github.kawaiicakes.clothing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This extends {@link HumanoidArmorLayer} in case a third-party mod references instances of that class to render
 * stuff.
 * @author kawaiicakes
 */
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    protected final A overClothingModel;

    /**
     * Added during {@link net.minecraftforge.client.event.EntityRenderersEvent.AddLayers} to appropriate renderer.
     * @param pBaseClothingModel This model is intended to be used for clothing resting just above the skin; like
     *                           T-shirts or ski masks.
     * @param pThickClothingModel This model is worn over top of the base, but rests underneath armour. Think of a
     *                            three-piece suit, or even just a tie.
     * @param pOverClothingModel This model goes slightly over top of armour. A plate carrier or some other goofy shit
     *                           would probably belong here.
     */
    public HumanoidClothingLayer(
            RenderLayerParent<T, M> pRenderer,
            A pBaseClothingModel, A pThickClothingModel, A pOverClothingModel
    ) {
        super(pRenderer, pBaseClothingModel, pThickClothingModel);
        this.overClothingModel = pOverClothingModel;
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

            // FIXME
            A defaultClothingModel = null;

            boolean hasGlint = stack.hasFoil();

            this.getParentModel().copyPropertiesTo(defaultClothingModel);
            this.setPartVisibility(defaultClothingModel, slot);

            Model clothingModel = getArmorModelHook(pLivingEntity, stack, slot, defaultClothingModel);

            if (clothing instanceof DyeableLeatherItem dyedClothing) {
                int i = dyedClothing.getColor(stack);
                float r = (float)(i >> 16 & 255) / 255.0F;
                float g = (float)(i >> 8 & 255) / 255.0F;
                float b = (float)(i & 255) / 255.0F;

                this.renderModel(
                        pMatrixStack,
                        pBuffer, pPackedLight,
                        hasGlint,
                        clothingModel,
                        r, g, b, clothing.getAlpha(),
                        this.getArmorResource(pLivingEntity, stack, slot, null)
                );

                if (!clothing.hasOverlay()) continue;
                this.renderModel(
                        pMatrixStack,
                        pBuffer, pPackedLight,
                        hasGlint,
                        clothingModel,
                        1.0F, 1.0F, 1.0F, clothing.getAlpha(),
                        this.getArmorResource(pLivingEntity, stack, slot, "overlay")
                );
            } else {
                this.renderModel(
                        pMatrixStack,
                        pBuffer, pPackedLight,
                        hasGlint,
                        clothingModel,
                        1.0F, 1.0F, 1.0F, clothing.getAlpha(),
                        this.getArmorResource(pLivingEntity, stack, slot, null)
                );
            }
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
    protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model) {
        return super.getArmorModelHook(entity, itemStack, slot, model);
    }

    @Override
    @NotNull
    @ParametersAreNonnullByDefault
    public ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        return super.getArmorResource(entity, stack, slot, type);
    }
}