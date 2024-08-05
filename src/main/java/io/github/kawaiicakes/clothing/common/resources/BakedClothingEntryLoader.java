package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BakedClothingEntryLoader extends ClothingEntryLoader<BakedModelClothingItem> {
    protected static BakedClothingEntryLoader INSTANCE = null;

    protected BakedClothingEntryLoader() {
        super("baked");
    }

    public static BakedClothingEntryLoader getInstance() {
        if (INSTANCE == null) {
            return new BakedClothingEntryLoader();
        }

        return INSTANCE;
    }

    @Override
    public @NotNull NbtStackInitializer<BakedModelClothingItem> deserializeFromJson(
            ResourceLocation entryId,
            JsonObject topElement
    ) {
        return (
                (bakedClothingItem, clothingStack) ->  {
                    Map<ClothingItem.ModelPartReference, ResourceLocation> models;

                    try {
                        Map<ClothingItem.ModelPartReference, ResourceLocation> errorModel = new HashMap<>();
                        errorModel.put(
                                bakedClothingItem.defaultModelPart(), new ResourceLocation("clothing:error")
                        );

                        // I want exceptions logged
                        models = errorModel;
                        if (topElement.has("models")) {
                            JsonObject modelObject = topElement.getAsJsonObject("models");
                            Map<ClothingItem.ModelPartReference, ResourceLocation> deserialized = new HashMap<>();
                            for (String key : modelObject.keySet()) {
                                ClothingItem.ModelPartReference byName = ClothingItem.ModelPartReference.byName(key);
                                ResourceLocation modelLocation
                                        = new ResourceLocation(modelObject.getAsJsonPrimitive(key).getAsString());

                                deserialized.put(byName, modelLocation);
                            }
                            models = deserialized;
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error("Error deserializing baked clothing data entry!", e);
                        throw e;
                    }

                    bakedClothingItem.setModelPartLocations(clothingStack, models);
                }
        );
    }
}
