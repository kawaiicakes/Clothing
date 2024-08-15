package io.github.kawaiicakes.clothing.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingItemRenderer;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
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
            return MODEL_LIST_CACHE.computeIfAbsent(
                    modelHash,
                    (i) -> {
                        if (!(clothingStack.getItem() instanceof ClothingItem<?> clothing))
                            throw new IllegalArgumentException(
                                    "Passed ItemStack '" + clothingStack + "' is not a ClothingItem!"
                            );

                        ModelManager manager = Minecraft.getInstance().getModelManager();

                        BakedModel missingModel = manager.getModel(ModelBakery.MISSING_MODEL_LOCATION);

                        List<BakedModel> toReturn = new ArrayList<>();

                        ResourceLocation baseLocation
                                = ClothingItemRenderer.baseModelLocation(clothing.getClothingName(clothingStack));
                        BakedModel baseModel = manager.getModel(baseLocation);

                        if (baseModel.equals(missingModel)) {
                            LOGGER.error("Base clothing model '{}' does not exist!", baseLocation);
                        }

                        toReturn.add(baseModel);

                        if (!(clothingStack.getItem() instanceof GenericClothingItem generic)) return toReturn;

                        ResourceLocation[] overlays = generic.getOverlays(clothingStack);

                        for (int j = overlays.length - 1; j >= 0; j--) {
                            ResourceLocation overlay = overlays[j];
                            ResourceLocation overlayLocation = ClothingItemRenderer.overlayModelLocation(overlay);

                            BakedModel overlayModel = manager.getModel(overlayLocation);

                            if (overlayModel.equals(missingModel)) {
                                LOGGER.error("Overlay item model '{}' does not exist!", overlayLocation);
                            }

                            toReturn.add(overlayModel);
                        }

                        return toReturn;
                    }
            );
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
            if (!(itemStack.getItem() instanceof ClothingItem<?> clothingItem)) return List.of();

            int modelHash;

            if (clothingItem instanceof GenericClothingItem generic) {
                modelHash = generic.getTextureLocation(itemStack).hashCode()
                        + Arrays.hashCode(generic.getOverlays(itemStack));
            } else if (clothingItem instanceof BakedModelClothingItem baked) {
                modelHash = baked.getModelPartLocations(itemStack).hashCode();
            } else {
                throw new IllegalArgumentException("Passed ItemStack '" + itemStack + "' is an unknown ClothingItem!");
            }

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
