package io.github.kawaiicakes.clothing.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingResourceLoader;
import io.github.kawaiicakes.clothing.common.resources.ClothingResourceLoader;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.ClothingMaterials;
import io.github.kawaiicakes.clothing.item.ClothingTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * As the name suggests, instances are intended to work with any kind of baked model supplied by an
 * {@link IGeometryLoader}. In the majority of cases
 * {@link BakedModel} instances will work interchangeably at runtime regardless
 * of how the model was loaded or what type it was (e.g. OBJ, JSON).
 */
public class BakedModelClothingItem extends ClothingItem<BakedModelClothingItem> {
    public static final String MODEL_ID_KEY = "modelId";
    public static final String MODEL_PART_REFERENCE_KEY = "parentPart";

    /**
     * If you use this constructor, make sure to override {@link #fillItemCategory(CreativeModeTab, NonNullList)}
     * so entries aren't duplicated
     * @param pMaterial the {@link ArmorMaterial} to use
     * @param pSlot the {@link EquipmentSlot} this will be worn on
     * @param pProperties the {@link Properties} for this item
     */
    public BakedModelClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
    }

    public BakedModelClothingItem(EquipmentSlot pSlot) {
        this(
                ClothingMaterials.CLOTH,
                pSlot,
                new Properties()
                        .tab(ClothingTab.CLOTHING_TAB)
                        .stacksTo(1)
        );
    }

    /**
     * Indicates the {@link ModelPart} this item will be parented to using a {@link ModelPartReference}.
     * The {@link ItemStack} is included for the implementer's benefit. This method is used in
     * {@link #getDefaultRenderManager()} to reference model parts without explicit references to them in server/client
     * common classes.
     * @param itemStack the {@link ItemStack} instance of this.
     * @return          the {@link ModelPartReference} for the body group this item is worn on.
     */
    public @NotNull ModelPartReference getModelPartForParent(ItemStack itemStack) {
        String modelPartReferenceString = this.getClothingPropertyTag(itemStack).getString(MODEL_PART_REFERENCE_KEY);
        return ModelPartReference.byName(modelPartReferenceString);
    }

    public void setModelPartForParent(ItemStack itemStack, ModelPartReference modelPart) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_PART_REFERENCE_KEY, modelPart.getSerializedName());
    }

    public ModelPartReference defaultModelPart() {
        return switch (this.getSlot()) {
            case MAINHAND -> ModelPartReference.RIGHT_ARM;
            case OFFHAND -> ModelPartReference.LEFT_ARM;
            case FEET, LEGS -> ModelPartReference.RIGHT_LEG;
            case CHEST -> ModelPartReference.BODY;
            case HEAD -> ModelPartReference.HEAD;
        };
    }

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
    public ResourceLocation bakedModelLocation(ItemStack itemStack) {
        return new ResourceLocation(this.getClothingPropertyTag(itemStack).getString(MODEL_ID_KEY));
    }

    public void setBakedModelLocation(ItemStack itemStack, ResourceLocation modelLocation) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_ID_KEY, modelLocation.toString());
        int texHash = modelLocation.hashCode();

        assert itemStack.getTag() != null;
        itemStack.getTag().putInt(
                "CustomModelData", texHash
        );
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        this.setModelPartForParent(toReturn, this.defaultModelPart());
        this.setBakedModelLocation(toReturn, new ResourceLocation("clothing:error"));

        return toReturn;
    }

    @Override
    public @NotNull ClothingResourceLoader<BakedModelClothingItem> loaderForType() {
        return BakedClothingResourceLoader.getInstance();
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return null;
    }

    /**
     * If for some reason a {@link BakedModel} instance isn't rendering properly,
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
}
