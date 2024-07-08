package io.github.kawaiicakes.clothing.client;

import io.github.kawaiicakes.clothing.client.model.ClothingModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central repository for holding baked entity models. The types of entities models are made for is dependent entirely
 * on what entities have {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}s added to their
 * renderers in the constructor of the {@link net.minecraft.client.renderer.entity.LivingEntityRenderer} instance. If
 * in future versions these references change, the idea that any entity which can properly render vanilla armour should
 * support clothing models shall persist.
 */
@OnlyIn(Dist.CLIENT)
public class ClothingModelRepository {
    private static final Map<ResourceLocation, ClothingModel> MODELS = new HashMap<>();

    public static ClothingModel getModel(ResourceLocation modelId) {
        return MODELS.get(modelId);
    }

    public static ClothingModel getModel(String modelId) {
        return getModel(new ResourceLocation(modelId));
    }

    // TODO: identify a good place to call this for documentation
    /**
     * Use this method to register your {@link ClothingModel}s with this mod. Everything else will be taken care of;
     * you only need to worry about making the actual models for the most part. That may depend on your implementation,
     * though.
     * @param clothingModelSupplier the <code>Supplier</code> providing your model.
     * @throws IllegalArgumentException thrown if the passed model is null or has a duplicate ID.
     */
    public static void registerModel(Supplier<ClothingModel> clothingModelSupplier) {
        final ClothingModel clothingModel = clothingModelSupplier.get();

        if (clothingModel == null)
            throw new IllegalArgumentException(
                    "Supplier of ClothingModel provides a null value!");

        if (MODELS.putIfAbsent(clothingModel.modelId, clothingModel) != null)
            throw new IllegalArgumentException("Passed ClothingModel has a duplicate ID!");
    }

    @SubscribeEvent
    @ApiStatus.Internal
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Map.Entry<ResourceLocation, ClothingModel> modelEntry : MODELS.entrySet()) {
            ClothingModel clothingModel = modelEntry.getValue();
            clothingModel.registerLayers(event);
        }
    }

    @SubscribeEvent
    @ApiStatus.Internal
    public static void bakeParts(EntityRenderersEvent.AddLayers event) {
        for (Map.Entry<ResourceLocation, ClothingModel> modelEntry : MODELS.entrySet()) {
            ClothingModel clothingModel = modelEntry.getValue();
            clothingModel.bakeParts(event);
        }
    }
}
