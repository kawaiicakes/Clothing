package io.github.kawaiicakes.clothing.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.ClothingItem.MeshStratum;
import io.github.kawaiicakes.clothing.common.item.ClothingItem.ModelPartReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.model.renderable.BakedModelRenderable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.ERROR_MODEL_LOCATION;

/**
 * This extends {@link HumanoidArmorLayer} in case a third-party mod references instances of that class to render
 * stuff.
 * <br><br>
 * You could view this as the brain of this mod, where everything comes together to give it its primary functionality.
 * This class, like its parent, is responsible for handling the rendering of "stuff" onto an entity based on what
 * {@link ItemStack}s exist in the {@link EquipmentSlot}s of the entity. This class caches two {@link HumanoidModel}
 * instances just like its parent, an additional two such that every "body group" has its own layer, and two more
 * that allow for rendering over the vanilla armour.
 * <br><br>
 * That said, this class works intimately with {@link ClothingItem}s to allow rendering {@link BakedModelRenderable}s;
 * permitting usage of OBJ and JSON models.
 * @version Forge 1.19.2
 * @author kawaiicakes
 */
@OnlyIn(Dist.CLIENT)
public class HumanoidClothingLayer<
        T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T,M,A>
{
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean FLUSH_QUEUED = false;

    protected final A baseModel;
    protected final A overModel;
    protected final A overLegsArmorModel;
    protected final A overMainArmorModel;

    protected Map<CompoundTag, Map<ClothingItem.ModelPartReference, BakedModel>> bakedModels = null;

    // TODO: add fallbacks everywhere necessary so clothing with fucked up NBT doesn't just break the game. A Source engine ERROR model would be nice for baked models that can't be found too
    /**
     * Added during {@link EntityRenderersEvent.AddLayers} to appropriate renderer. Creates a
     * {@link RenderLayer} that behaves vaguely like its parent,
     * {@link HumanoidArmorLayer}, but is made to work specifically with {@link ClothingItem} instances.
     * <br><br>
     * Each parameter of type {@link A} represents a model for rendering as in {@link HumanoidArmorLayer}, each of whom
     * is layered on top of the previously provided one.
     */
    public HumanoidClothingLayer(
            RenderLayerParent<T, M> pRenderer,
            A baseModel, A innerModel, A outerModel, A overModel,
            A overLegsArmorModel, A overMainArmorModel
    ) {
        super(
                pRenderer,
                innerModel,
                outerModel
        );

        this.baseModel = baseModel;
        this.overModel = overModel;
        this.overLegsArmorModel = overLegsArmorModel;
        this.overMainArmorModel = overMainArmorModel;
    }

    /**
     * Flushes all cached assets in every instance of this. Flush is effective immediately after
     * {@link #renderBakedModels(ItemStack, PoseStack, MultiBufferSource, int)} is called.
     */
    public static void flushModelCaches() {
        FLUSH_QUEUED = true;
    }

    /**
     * Renders stuff onto <code>pLivingEntity</code> according to what exists in its <code>EquipmentSlot</code>s. This
     * method renders clothing to the buffer according to the instructions specified by the {@link ClothingItem}
     * in a slot
     */
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
            if (!(stack.getItem() instanceof ClothingItem clothingItem)) continue;
            if (!clothingItem.getSlot().equals(slot)) continue;

            this.renderClothingFromItemStack(
                    stack,
                    pLivingEntity,
                    pMatrixStack, pBuffer,
                    pPackedLight,
                    pLimbSwing, pLimbSwingAmount,
                    pPartialTicks, pAgeInTicks,
                    pNetHeadYaw, pHeadPitch
            );
        }
    }

    /**
     * Extraction of render logic to new method is to permit per-slot rendering rather than calling
     * {@link #render(PoseStack, MultiBufferSource, int, LivingEntity, float, float, float, float, float, float)}
     * for each slot if rendering needs to be done by a third party.
     * <br><br>
     * In particular, this allows for compatibility with Curios API, but will likely come in handy in the future as
     * it offers much more flexibility.
     * @param stack the {@link ItemStack} representation of a {@link ClothingItem}.
     */
    public void renderClothingFromItemStack(
            ItemStack stack,
            T pEntity,
            PoseStack pMatrixStack, MultiBufferSource pBuffer,
            int pPackedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        try {
            if (!(stack.getItem() instanceof ClothingItem clothingItem)) return;

            boolean hasGlint = stack.hasFoil();
            Map<MeshStratum, ClothingLayer> meshes = clothingItem.getMeshes(stack);
            Multimap<MeshStratum, ClothingLayer> overlays = clothingItem.getOverlays(stack);

            for (Map.Entry<MeshStratum, ClothingLayer> entry : meshes.entrySet()) {
                A clothingModel = this.modelForLayer(entry.getKey());

                this.getParentModel().copyPropertiesTo(clothingModel);

                assert entry.getValue().clothingVisibility() != null;
                this.setPartVisibility(
                        clothingModel, entry.getValue().clothingVisibility().asArray()
                );

                int i = clothingItem.getColor(stack);
                float r = (float) (i >> 16 & 255) / 255.0F;
                float g = (float) (i >> 8 & 255) / 255.0F;
                float b = (float) (i & 255) / 255.0F;

                this.renderBakedModels(stack, pMatrixStack, pBuffer, pPackedLight);

                this.renderMesh(
                        pMatrixStack,
                        pBuffer, pPackedLight,
                        hasGlint,
                        clothingModel,
                        r, g, b, this.getAlpha(
                                pEntity,
                                stack, clothingItem.getSlot(),
                                pPackedLight,
                                pLimbSwing, pLimbSwingAmount,
                                pPartialTicks, pAgeInTicks,
                                pNetHeadYaw, pHeadPitch
                        ),
                        getMeshResource(entry.getValue().textureLocation())
                );

                if (overlays.isEmpty()) continue;

                List<ClothingLayer> overlaysForStratum = (List<ClothingLayer>) overlays.get(entry.getKey());

                for (int j = overlaysForStratum.size() - 1; j >= 0 ; j--) {
                    ClothingLayer overlay = overlaysForStratum.get(j);

                    this.renderMesh(
                            pMatrixStack,
                            pBuffer, pPackedLight,
                            hasGlint,
                            clothingModel,
                            1.0F, 1.0F, 1.0F, this.getAlpha(
                                    null,
                                    stack, clothingItem.getSlot(),
                                    pPackedLight,
                                    pLimbSwing, pLimbSwingAmount,
                                    pPartialTicks, pAgeInTicks,
                                    pNetHeadYaw, pHeadPitch
                            ),
                            getOverlayResource(overlay.textureLocation())
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while rendering clothing!", e);
        }
    }

    public void renderMesh(
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

    public void renderBakedModels(
            ItemStack pItemStack,
            PoseStack pMatrixStack,
            MultiBufferSource pBuffer, int pPackedLight
    ) {
        if (!(pItemStack.getItem() instanceof ClothingItem clothingItem)) {
            LOGGER.error("Passed ItemStack '{}' is not a clothing item!", pItemStack);
            return;
        }

        if (FLUSH_QUEUED) {
            this.bakedModels = new HashMap<>();
            FLUSH_QUEUED = false;
        }

        Map<ClothingItem.ModelPartReference, BakedModel> modelsForRender = this.bakedModels.computeIfAbsent(
                clothingItem.getClothingPropertiesTag(pItemStack),
                (key) -> parseModelsFromLocations(clothingItem, pItemStack)
        );

        for (Map.Entry<ClothingItem.ModelPartReference, BakedModel> entry : modelsForRender.entrySet()) {
            ClothingItem.ModelPartReference modelPartReference = entry.getKey();
            BakedModel forRender = entry.getValue();

            if (forRender == null) {
                forRender = getErrorModel();
            }

            ModelPart parentModelPart
                    = this.modelPartByReference(modelPartReference);

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

    // FIXME: clothing does not become translucent
    // FIXME: values not equal to 1.0F cause colour of overlay to "infect" base layer for GenericClothingItems
    // TODO: fully implement this. this is incomplete and does nothing
    /**
     * Returns the alpha value for render.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @return The value of alpha as a float. Permitted values are 0.0 to 1.0 inclusive.
     */
    @SuppressWarnings("unused")
    @ParametersAreNullableByDefault
    public float getAlpha(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return 1.0F;
    }

    /**
     * Identical to super; but access declaration made public
     */
    @Override
    public void setPartVisibility(@NotNull A pModel, @NotNull EquipmentSlot pSlot) {
        super.setPartVisibility(pModel, pSlot);
    }

    /**
     * Overload of {@link #setPartVisibility(HumanoidModel, EquipmentSlot)} unique to this class.
     * Allows for greater control of setting part visibility.
     * @param pModel the {@link A} to set part visibility on.
     * @param pParts the array of {@link ClothingItem.ModelPartReference}s to toggle
     *               visibility for.
     */
    public void setPartVisibility(@NotNull A pModel, @NotNull ClothingItem.ModelPartReference[] pParts) {
        pModel.setAllVisible(false);
        if (pParts == null || pParts.length == 0) throw new IllegalArgumentException("Empty part list!");

        for (ClothingItem.ModelPartReference part : pParts) {
            this.modelPartByReference(pModel, part).visible = true;
        }
    }

    /**
     * Simply returns the appropriate generic model from the corresponding
     * {@link ClothingItem.MeshStratum}.
     * @param meshStratum the {@link ClothingItem.MeshStratum} whose
     *                    value corresponds to one of the model fields.
     * @return the appropriate {@link A} to render to.
     */
    public A modelForLayer(ClothingItem.MeshStratum meshStratum) {
        return switch (meshStratum) {
            case BASE -> this.baseModel;
            case INNER -> this.innerModel;
            case OUTER -> this.outerModel;
            case OVER -> this.overModel;
            case OVER_LEG_ARMOR -> this.overLegsArmorModel;
            case OVER_ARMOR -> this.overMainArmorModel;
        };
    }

    /**
     * Simply returns the appropriate model part from the corresponding
     * {@link ClothingItem.ModelPartReference}. Exists to avoid
     * directly referencing client-only classes in common classes.
     * @param model the {@link A} model to return a part from
     * @param reference the {@link ClothingItem.ModelPartReference}
     *                  corresponding to the desired {@link ModelPart}
     * @return the desired {@link ModelPart}
     */
    public @NotNull ModelPart modelPartByReference(A model, ClothingItem.ModelPartReference reference) {
        return switch (reference) {
            case HEAD -> model.head;
            case HAT -> model.hat;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }

    /**
     * Overload that returns model parts from the parent model according to {@link #getParentModel()}.
     * @param reference the {@link ClothingItem.ModelPartReference}
     *                  corresponding to the desired {@link ModelPart}
     * @return the desired {@link ModelPart}
     */
    public @NotNull ModelPart modelPartByReference(ClothingItem.ModelPartReference reference) {
        // this should be fine as it should always be a widening cast back to a HumanoidModel
        @SuppressWarnings("unchecked")
        A parent = (A) this.getParentModel();
        return this.modelPartByReference(parent, reference);
    }

    public static ResourceLocation getMeshResource(ResourceLocation textureLocation) {
        return new ResourceLocation(
                textureLocation.getNamespace(),
                "textures/models/clothing/" + textureLocation.getPath() + ".png"
        );
    }

    public static ResourceLocation getOverlayResource(ResourceLocation textureLocation) {
        return new ResourceLocation(
                textureLocation.getNamespace(),
                "textures/models/clothing/overlays/" + textureLocation.getPath() + ".png"
        );
    }

    public static Map<ModelPartReference, BakedModel> parseModelsFromLocations(ClothingItem item, ItemStack stack) {
        ImmutableMap.Builder<ModelPartReference, BakedModel> toReturn = ImmutableMap.builder();

        for (Map.Entry<ClothingItem.ModelPartReference, ResourceLocation> entry : item.getModels(stack).entrySet()) {
            BakedModel forPart = Minecraft.getInstance().getModelManager().getModel(entry.getValue());
            toReturn.put(entry.getKey(), forPart);
        }

        return toReturn.buildOrThrow();
    }

    /**
     * Don't call this until the ModelManager has finished baking
     */
    public static BakedModel getErrorModel() {
        return Minecraft.getInstance().getModelManager().getModel(ERROR_MODEL_LOCATION);
    }
}