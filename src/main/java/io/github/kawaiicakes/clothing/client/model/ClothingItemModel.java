package io.github.kawaiicakes.clothing.client.model;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingItemRenderer;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.ClothingItem.MeshStratum;
import io.github.kawaiicakes.clothing.common.item.OverlayPatternItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.ERROR_MODEL_LOCATION;

public class ClothingItemModel implements IUnbakedGeometry<ClothingItemModel> {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final BlockModel model;

    public ClothingItemModel(BlockModel model) {
        this.model = model;
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBakery bakery,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides,
            ResourceLocation modelLocation
    ) {
        BakedModel original = this.model.bake(
                bakery,
                this.model,
                spriteGetter,
                modelState,
                modelLocation,
                true
        );

        return new Baked<>(original);
    }

    @Override
    public Collection<Material> getMaterials(
            IGeometryBakingContext context,
            Function<ResourceLocation, UnbakedModel> modelGetter,
            Set<Pair<String, String>> missingTextureErrors
    ) {
        return this.model.getMaterials(modelGetter, missingTextureErrors);
    }

    public static class Baked<T extends BakedModel> extends BakedModelWrapper<T> {
        protected static Map<ResourceLocation, BakedModel> MODEL_CACHE = new HashMap<>();
        protected static Map<Integer, List<BakedModel>> MODEL_LIST_CACHE = new HashMap<>();

        public static void flushModelCaches() {
            MODEL_CACHE = new HashMap<>();
            MODEL_LIST_CACHE = new HashMap<>();
        }

        protected static BakedModel get(ResourceLocation modelLocation) {
            return MODEL_CACHE.computeIfAbsent(
                    modelLocation,
                    (location) -> Minecraft.getInstance().getModelManager().getModel(location)
            );
        }

        protected static List<BakedModel> getList(int modelHash, ItemStack clothingStack) {
            try {
                return MODEL_LIST_CACHE.computeIfAbsent(
                        modelHash,
                        (i) -> {
                            List<BakedModel> toReturn = new ArrayList<>();

                            if (clothingStack.getItem() instanceof OverlayPatternItem pattern) {
                                toReturn.add(get(new ResourceLocation("clothing:item/overlay_pattern_base")));
                                toReturn.add(
                                        get(ClothingItemRenderer.overlayModelLocation(
                                                pattern.getOverlay(clothingStack)
                                        ))
                                );
                                return toReturn;
                            }

                            if (!(clothingStack.getItem() instanceof ClothingItem clothing))
                                throw new IllegalArgumentException(
                                        "Passed ItemStack '" + clothingStack + "' is not a ClothingItem!"
                                );

                            BakedModel missingModel = get(ModelBakery.MISSING_MODEL_LOCATION);

                            ResourceLocation baseLocation
                                    = ClothingItemRenderer.entryModelLocation(clothing.getClothingName(clothingStack));
                            BakedModel baseModel = get(baseLocation);

                            if (baseModel.equals(missingModel)) {
                                LOGGER.error("Base clothing model '{}' does not exist!", baseLocation);
                            }

                            toReturn.add(baseModel);

                            ImmutableListMultimap<MeshStratum, ClothingLayer> overlays
                                    = clothing.getOverlays(clothingStack);

                            for (MeshStratum stratum : MeshStratum.values()) {
                                if (!overlays.containsKey(stratum)) continue;
                                List<ClothingLayer> layers = overlays.get(stratum);

                                for (int j = layers.size() - 1; j >= 0; j--) {
                                    ClothingLayer overlay = layers.get(j);

                                    ResourceLocation overlayLocation
                                            = ClothingItemRenderer.overlayModelLocation(overlay.textureLocation());

                                    BakedModel overlayModel = get(overlayLocation);

                                    if (overlayModel.equals(missingModel)) {
                                        LOGGER.error("Overlay item model '{}' does not exist!", overlayLocation);
                                    }

                                    toReturn.add(overlayModel);
                                }
                            }

                            return toReturn;
                        }
                );
            } catch (Exception e) {
                LOGGER.error("Unable to return list of passes for render!", e);
                return List.of(Minecraft.getInstance().getModelManager().getModel(ERROR_MODEL_LOCATION));
            }
        }

        public Baked(T originalModel) {
            super(originalModel);
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        @Override
        @NotNull
        @ParametersAreNonnullByDefault
        public BakedModel applyTransform(ItemTransforms.TransformType cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
            //noinspection deprecation
            this.originalModel.getTransforms()
                    .getTransform(cameraTransformType)
                    .apply(applyLeftHandTransform, poseStack);
            return this;
        }

        @Override
        public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
            if (itemStack.getItem() instanceof OverlayPatternItem patternItem) {
                return getList(
                        patternItem.hashCode() + patternItem.getOverlay(itemStack).hashCode(),
                        itemStack
                );
            }

            if (!(itemStack.getItem() instanceof ClothingItem clothingItem)) return List.of();

            int modelHash;

            modelHash = clothingItem.getClothingName(itemStack).hashCode()
                    + clothingItem.getOverlays(itemStack).hashCode();

            return getList(modelHash, itemStack);
        }
    }

    public static class Loader implements IGeometryLoader<ClothingItemModel> {
        public static final String ID = "item_model_loader";
        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public ClothingItemModel read(
                JsonObject jsonObject, JsonDeserializationContext deserializationContext
        ) throws JsonParseException {
            // it's not necessary to completely overhaul the vanilla model loader
            jsonObject.remove("loader");
            BlockModel toReturn = deserializationContext.deserialize(jsonObject, BlockModel.class);
            return new ClothingItemModel(toReturn);
        }
    }
}
