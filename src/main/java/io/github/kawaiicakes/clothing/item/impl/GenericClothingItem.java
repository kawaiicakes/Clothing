package io.github.kawaiicakes.clothing.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.ClothingMaterials;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * Implementation of {@link ClothingItem} for simple cosmetics like T-shirts or anything that looks like thinner armour.
 */
public class GenericClothingItem extends ClothingItem {
    // TODO: final assets, etc.
    // TODO: item icon changes with texture
    public GenericClothingItem(EquipmentSlot pSlot) {
        this(pSlot, 0xFFFFFF);
    }

    public GenericClothingItem(EquipmentSlot pSlot, int defaultColor) {
        super(
                ClothingMaterials.CLOTH,
                pSlot,
                new Properties()
                        .tab(CreativeModeTab.TAB_COMBAT)
                        .stacksTo(1),
                defaultColor
        );
    }

    @Override
    public <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void render(
            @NotNull HumanoidClothingLayer<T, M, A> pClothingLayer,
            @NotNull ItemStack pItemStack,
            @NotNull PoseStack pMatrixStack,
            @NotNull MultiBufferSource pBuffer,
            int pPackedLight,
            @NotNull T pLivingEntity,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        boolean hasGlint = pItemStack.hasFoil();

        A clothingModel = pClothingLayer.getArmorModel(this.slotForModel());

        pClothingLayer.getParentModel().copyPropertiesTo(clothingModel);
        pClothingLayer.setPartVisibility(clothingModel, this.slotForModel());

        int i = this.getColor(pItemStack);
        float r = (float)(i >> 16 & 255) / 255.0F;
        float g = (float)(i >> 8 & 255) / 255.0F;
        float b = (float)(i & 255) / 255.0F;

        this.renderModel(
                pMatrixStack,
                pBuffer, pPackedLight,
                hasGlint,
                clothingModel,
                r, g, b, this.getAlpha(
                        pLivingEntity,
                        pItemStack, this.slotForModel(),
                        pPackedLight,
                        pLimbSwing, pLimbSwingAmount,
                        pPartialTicks, pAgeInTicks,
                        pNetHeadYaw, pHeadPitch
                ),
                pClothingLayer.getArmorResource(pLivingEntity, pItemStack, this.slotForModel(), null)
        );

        if (
                !this.hasOverlay(
                    pLivingEntity, pItemStack, this.slotForModel(),
                    pPackedLight,
                    pLimbSwing, pLimbSwingAmount,
                    pPartialTicks, pAgeInTicks,
                    pNetHeadYaw, pHeadPitch
                )
        ) return;

        this.renderModel(
                pMatrixStack,
                pBuffer, pPackedLight,
                hasGlint,
                clothingModel,
                1.0F, 1.0F, 1.0F, this.getAlpha(
                        pLivingEntity,
                        pItemStack, this.slotForModel(),
                        pPackedLight,
                        pLimbSwing, pLimbSwingAmount,
                        pPartialTicks, pAgeInTicks,
                        pNetHeadYaw, pHeadPitch
                ),
                pClothingLayer.getArmorResource(pLivingEntity, pItemStack, this.slotForModel(), "overlay")
        );
    }

    // TODO
    public <T extends LivingEntity> boolean hasOverlay(
            T pLivingEntity,
            ItemStack pItemStack, EquipmentSlot equipmentSlot,
            int pPackedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return false;
    }

    @Override
    public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // TODO: final implementation as per super
        // FIXME: formal implementation of obtaining texture name from ClothingItem; I see why the ArmorMaterial was
        // used now lol
        final boolean usesGenericInnerLayer = EquipmentSlot.LEGS.equals(slot);
        return String.format(
                java.util.Locale.ROOT,
                "%s:textures/models/armor/clothing/test_%s%s.png",
                MOD_ID,
                (usesGenericInnerLayer ? "legs" : "body"),
                type == null ? "" : String.format(java.util.Locale.ROOT, "_%s", type)
        );
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /**
     * TODO: helper method
     * @param pPoseStack
     * @param pBuffer
     * @param pPackedLight
     * @param pGlint
     * @param pModel
     * @param pRed
     * @param pGreen
     * @param pBlue
     * @param pAlpha
     * @param armorResource
     */
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
}