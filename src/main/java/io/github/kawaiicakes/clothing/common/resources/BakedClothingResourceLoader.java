package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BakedClothingResourceLoader extends ClothingResourceLoader<BakedModelClothingItem> {
    protected static BakedClothingResourceLoader INSTANCE = null;

    protected BakedClothingResourceLoader() {
        super("baked");
    }

    public static BakedClothingResourceLoader getInstance() {
        if (INSTANCE == null) {
            return new BakedClothingResourceLoader();
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
                    EquipmentSlot slot;
                    Map<ClothingItem.ModelPartReference, ResourceLocation> models;
                    int color;

                    try {
                        slot = EquipmentSlot.byName(topElement.getAsJsonPrimitive("slot").getAsString());

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

                        color = topElement.has("color")
                                ? topElement.getAsJsonPrimitive("color").getAsInt()
                                : 0xFFFFFF;

                    } catch (RuntimeException e) {
                        LOGGER.error("Error deserializing baked clothing data entry!", e);
                        throw e;
                    }

                    bakedClothingItem.setClothingName(clothingStack, entryId.getPath());
                    bakedClothingItem.setSlot(clothingStack, slot);
                    bakedClothingItem.setModelPartsForParent(clothingStack, models);
                    bakedClothingItem.setColor(clothingStack, color);
                }
        );
    }
}
