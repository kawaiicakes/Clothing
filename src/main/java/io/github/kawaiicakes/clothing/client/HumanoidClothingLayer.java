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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Set;

/**
 * This extends {@link HumanoidArmorLayer} in case a third-party mod references instances of that class to render
 * stuff. Furthermore, it also exists so that vanilla armour may render still when mods like Trinkets are
 * installed, hopefully.
 * <br><br>
 * The <code>innerModel</code> and <code>outerModel</code> in super are instantiated by "generic models" of types
 * <code>base</code> and <code>over</code>, respectively. Base renders underneath vanilla armour but slightly over the
 * player's skin (including the skin overlay). Over renders slightly above vanilla armour.
 * @author kawaiicakes
 */
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     I've opted to keep everything in one new layer despite the previous comments here. This is because having to
     define an entire humanoid model for one piece of clothing could actually allow for more complex models.
     Accordingly, overrides for #setPartVisibility and support for ClothingModels will be added.
     */
    /**
     * Added during {@link net.minecraftforge.client.event.EntityRenderersEvent.AddLayers} to appropriate renderer.
     */
    public HumanoidClothingLayer(RenderLayerParent<T, M> pRenderer, A innerModel, A outerModel) {
        // TODO: having four models all-in-all, one for each slot, could be a good idea

        super(
                pRenderer,
                innerModel,
                outerModel
        );
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
                clothingModel = (A) clothing.getClothingModel(
                        pLivingEntity, stack, slot,
                        this.getArmorModel(slot)
                );
            } catch (RuntimeException e) {
                LOGGER.error("Unable to cast model to appropriate type!", e);
                continue;
            }

            boolean hasGlint = stack.hasFoil();

            this.getParentModel().copyPropertiesTo(clothingModel);
            this.setPartVisibility(clothingModel, slot, stack);

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

            if (!clothing.hasOverlay(
                    pLivingEntity, stack, slot,
                    pPackedLight,
                    pLimbSwing, pLimbSwingAmount,
                    pPartialTicks, pAgeInTicks,
                    pNetHeadYaw, pHeadPitch
            )) continue;
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

    protected void setPartVisibility(@NotNull A pModel, @NotNull EquipmentSlot pSlot, ItemStack pItemStack) {
        if (!(pItemStack.getItem() instanceof ClothingItem clothingItem)) {
            super.setPartVisibility(pModel, pSlot);
            return;
        }

        Set<EquipmentSlot> slotsForRender = clothingItem.slotsForRender();
        pModel.setAllVisible(false);

        if (slotsForRender.contains(EquipmentSlot.HEAD)) {
            pModel.head.visible = true;
            pModel.hat.visible = true;
        }

        if (slotsForRender.contains(EquipmentSlot.CHEST)) {
            pModel.body.visible = true;
            pModel.rightArm.visible = true;
            pModel.leftArm.visible = true;
        }

        if (slotsForRender.contains(EquipmentSlot.LEGS)) {
            pModel.body.visible = true;
            pModel.rightLeg.visible = true;
            pModel.leftLeg.visible = true;
        }

        if (slotsForRender.contains(EquipmentSlot.FEET)) {
            pModel.rightLeg.visible = true;
            pModel.leftLeg.visible = true;
        }
    }
}