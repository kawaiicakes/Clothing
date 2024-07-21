package io.github.kawaiicakes.clothing.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

// TODO
/**
 * As the name suggests, implementations are intended to work with any kind of baked model supplied by an
 * {@link net.minecraftforge.client.model.geometry.IGeometryLoader}. In the majority of cases
 * {@link net.minecraft.client.resources.model.BakedModel} instances will work interchangeably at runtime regardless
 * of how the model was loaded or what type it was (e.g. OBJ, JSON). In spite of this, this remains an abstract class
 * as I want to allow implementations to have the ability to do things simply (like making a viking helmet item)
 * or more complex (a "generic" baked model clothing item, behaving similarly to the {@link GenericClothingItem}'s
 * rendering).
 */
public abstract class BakedModelClothingItem extends ClothingItem {
    public BakedModelClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
    }

    /**
     * Implementations indicate the {@link ModelPart} this item will be parented to using a {@link ModelPartReference}.
     * The {@link ItemStack} is included for the implementer's benefit. This method is used in
     * {@link #getDefaultRenderManager()} to reference model parts without explicit references to them in server/client
     * common classes.
     * @param itemStack the {@link ItemStack} instance of this.
     * @return          the {@link ModelPartReference} for the body group this item is worn on.
     */
    public @NotNull abstract ModelPartReference
    getModelPartForParent(ItemStack itemStack);

    /**
     * Used to point to the location of the {@link BakedModel} for render. A baked model is not directly declared
     * as the return type, as this would cause a {@link ClassNotFoundException} serverside. I would recommend caching
     * any models you obtain in the {@link ClientClothingRenderManager}.
     * <br><br>
     * If you want to change the model being rendered, do it through here.
     * @see BakedModelClothingItem#getDefaultRenderManager()
     * @param itemStack the {@link ItemStack} instance of this
     * @return the location of the {@link BakedModel} for render.
     */
    public abstract ResourceLocation bakedModelLocation(ItemStack itemStack);

    /**
     * If for some reason a {@link net.minecraft.client.resources.model.BakedModel} instance isn't rendering properly,
     * implement this method.
     */
    public ClientClothingRenderManager getDefaultRenderManager() {
        return new ClientClothingRenderManager() {
            private BakedModel modelForRender = null;

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
                if (this.modelForRender == null) {
                    this.modelForRender = Minecraft.getInstance().getModelManager().getModel(
                            BakedModelClothingItem.this.bakedModelLocation(pItemStack)
                    );
                }

                ModelPartReference modelPartReference = BakedModelClothingItem.this.getModelPartForParent(pItemStack);

                ModelPart parentModelPart
                        = pClothingLayer.modelPartByReference(modelPartReference);

                pMatrixStack.pushPose();
                parentModelPart.translateAndRotate(pMatrixStack);
                /*
                    These values were set according to what would place the "center" of a model made in
                    Blockbench 4.10.4 at the "center" of the part model; assuming the model's center in Blockbench is
                    at 0, 4, 0.
                 */
                pMatrixStack.translate(-0.50, -0.50, 0.50);
                pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(180.00F));
                pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.00F));

                Minecraft.getInstance().getItemRenderer().render(
                        pItemStack,
                        ItemTransforms.TransformType.NONE,
                        false,
                        pMatrixStack,
                        pBuffer,
                        pPackedLight,
                        OverlayTexture.NO_OVERLAY,
                        this.modelForRender
                );

               pMatrixStack.popPose();
            }
        };
    }

    @Override
    public void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager) {
        clothingManager.accept(this.getDefaultRenderManager());
    }

    /**
     * {@link ModelPart} and references to {@link HumanoidClothingLayer}s which contain the models from which parts may
     * come are client-only classes; directly referencing them in {@link net.minecraft.world.item.Item} increases the
     * chances of serverside crashes due to {@link ClassNotFoundException}s.
     */
    public enum ModelPartReference implements StringRepresentable {
        HEAD("head"),
        HAT("hat"),
        BODY("body"),
        RIGHT_ARM("right_arm"),
        LEFT_ARM("left_arm"),
        RIGHT_LEG("right_leg"),
        LEFT_LEG("left_leg");

        private final String childName;

        ModelPartReference(String childName) {
            this.childName = childName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.childName;
        }
    }
}
