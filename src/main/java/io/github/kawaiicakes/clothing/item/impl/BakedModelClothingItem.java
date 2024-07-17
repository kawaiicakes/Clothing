package io.github.kawaiicakes.clothing.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
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
     * Implementations indicate the {@link ModelPart} this item will be parented to. The {@link ItemStack} and
     * {@link HumanoidClothingLayer} are included for the implementer's benefit.
     * <br><br>
     * One should take care not to mutate any model parts returned from the {@code clothingLayer} as those mutations
     * will be reflected on the actual model. Consider using {@link ModelPart#copyFrom(ModelPart)} prior to performing
     * a mutating operation.
     * @param itemStack the {@link ItemStack} instance of this.
     * @param clothingLayer the {@link HumanoidClothingLayer} of the
     *                      {@link net.minecraft.client.renderer.entity.LivingEntityRenderer}. The parameter type is
     *                      left as an {@link Object} as a {@link ClassNotFoundException} would be thrown on the
     *                      serverside otherwise. An {@link IllegalArgumentException} will be thrown if the passed
     *                      object is not a {@link HumanoidClothingLayer} instance.
     * @return          the {@link ModelPart} this item is worn on.
     */
    public abstract ModelPart
    getModelPartForParent(ItemStack itemStack, Object clothingLayer);

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
            private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

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

                ModelPart parentModelPart
                        = BakedModelClothingItem.this.getModelPartForParent(pItemStack, pClothingLayer);

                pMatrixStack.pushPose();
                parentModelPart.translateAndRotate(pMatrixStack);

                // TODO: test if this even works properly
                this.itemRenderer.render(
                        pItemStack,
                        ItemTransforms.TransformType.NONE,
                        false,
                        pMatrixStack,
                        pBuffer,
                        pPackedLight,
                        0,
                        this.modelForRender);

               pMatrixStack.popPose();
            }
        };
    }

    @Override
    public void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager) {
        clothingManager.accept(this.getDefaultRenderManager());
    }
}
