package io.github.kawaiicakes.clothing.common.item.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * As the name suggests, instances are intended to work with any kind of baked model supplied by an
 * {@link IGeometryLoader}. In the majority of cases
 * {@link BakedModel} instances will work interchangeably at runtime regardless
 * of how the model was loaded or what type it was (e.g. OBJ, JSON).
 */
public class BakedModelClothingItem extends ClothingItem<BakedModelClothingItem> {
    public static final String MODEL_PARENTS_KEY = "modelParents";

    public BakedModelClothingItem(EquipmentSlot pSlot) {
        super(pSlot);
    }

    /**
     * Indicates the {@link ModelPart}s to which the mapped {@link ResourceLocation} will be rendered using a
     * {@link ModelPartReference} as a key.
     * The {@link ItemStack} is included for the implementer's benefit. This method is used in
     * {@link #getDefaultRenderManager()} to reference model parts without explicit references to them in server/client
     * common classes.
     * @param itemStack the {@link ItemStack} instance of this.
     * @return          the {@link Map} of key {@link ModelPartReference}s for each {@link ResourceLocation} referencing
     *                  the body part the baked model will render to.
     */
    public @NotNull Map<ModelPartReference, ResourceLocation> getModelPartLocations(ItemStack itemStack) {
        CompoundTag modelPartTag = this.getClothingPropertiesTag(itemStack).getCompound(MODEL_PARENTS_KEY);

        Map<ModelPartReference, ResourceLocation> toReturn = new HashMap<>(modelPartTag.size());

        for (String part : modelPartTag.getAllKeys()) {
            if (!(modelPartTag.get(part) instanceof StringTag modelLocation)) throw new IllegalArgumentException();
            toReturn.put(ModelPartReference.byName(part), new ResourceLocation(modelLocation.toString()));
        }

        return toReturn;
    }

    public void setModelPartLocations(ItemStack itemStack, Map<ModelPartReference, ResourceLocation> modelParts) {
        CompoundTag modelPartMap = new CompoundTag();

        for (Map.Entry<ModelPartReference, ResourceLocation> entry : modelParts.entrySet()) {
            modelPartMap.putString(entry.getKey().getSerializedName(), entry.getValue().toString());
        }

        this.getClothingPropertiesTag(itemStack).put(MODEL_PARENTS_KEY, modelPartMap);
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
     * @param modelPartReference the {@link ModelPartReference} upon
     *                           which a model is parented to.
     * @return the location of the {@link BakedModel} for render.
     */
    @Nullable
    public ResourceLocation getModelPartLocation(ItemStack itemStack, ModelPartReference modelPartReference) {
        String location = this.getClothingPropertiesTag(itemStack)
                .getCompound(MODEL_PARENTS_KEY)
                .getString(modelPartReference.getSerializedName());
        return location.isEmpty() ? null : new ResourceLocation(location);
    }

    public void setModelPartLocation(
            ItemStack itemStack, ModelPartReference modelPartReference, ResourceLocation modelLocation
    ) {
        Map<ModelPartReference, ResourceLocation> existing = this.getModelPartLocations(itemStack);
        existing.put(modelPartReference, modelLocation);
        this.setModelPartLocations(itemStack, existing);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        Map<ModelPartReference, ResourceLocation> modelParts = new HashMap<>();
        modelParts.put(this.defaultModelPart(), new ResourceLocation("clothing:error"));
        this.setModelPartLocations(
                toReturn,
                modelParts
        );

        return toReturn;
    }

    @Override
    public @NotNull ClothingEntryLoader<BakedModelClothingItem> loaderForType() {
        return BakedClothingEntryLoader.getInstance();
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
            private Map<ModelPartReference, BakedModel> modelsForRender = null;
            private Map<ModelPartReference, ResourceLocation> modelLocations = null;

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
                Map<ModelPartReference, ResourceLocation> newestLocations
                        = BakedModelClothingItem.this.getModelPartLocations(pItemStack);
                if (!newestLocations.equals(this.modelLocations)) {
                    this.modelLocations = newestLocations;
                    this.modelsForRender = new HashMap<>();
                }

                for (Map.Entry<ModelPartReference, ResourceLocation> entry : this.modelLocations.entrySet()) {
                    ModelPartReference modelPartReference = entry.getKey();

                    BakedModel forRender = this.modelsForRender.get(modelPartReference);
                    if (forRender == null) {
                        this.modelsForRender.put(
                                modelPartReference,
                                Minecraft.getInstance().getModelManager().getModel(
                                        Objects.requireNonNull(BakedModelClothingItem.this.getModelPartLocation(
                                                pItemStack, modelPartReference
                                        ))
                                )
                        );
                        forRender = this.modelsForRender.get(modelPartReference);
                    }

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
                            forRender
                    );

                   pMatrixStack.popPose();
                }
            }
        };
    }

    @Override
    public void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager) {
        clothingManager.accept(this.getDefaultRenderManager());
    }
}
