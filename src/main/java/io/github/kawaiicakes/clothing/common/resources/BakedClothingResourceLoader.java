package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

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
                    ClothingItem.ModelPartReference modelPart;
                    ResourceLocation modelLocation;
                    int color;

                    try {
                        slot = EquipmentSlot.byName(topElement.getAsJsonPrimitive("slot").getAsString());

                        modelPart = topElement.has("parent")
                                ? ClothingItem.ModelPartReference.byName(
                                        topElement.getAsJsonPrimitive("parent").getAsString()
                                    )
                                : bakedClothingItem.defaultModelPart();

                        modelLocation = topElement.has("model")
                                ? new ResourceLocation(topElement.getAsJsonPrimitive("model").getAsString())
                                : new ResourceLocation("clothing:error");

                        color = topElement.has("color")
                                ? topElement.getAsJsonPrimitive("color").getAsInt()
                                : 0xFFFFFF;

                    } catch (RuntimeException e) {
                        LOGGER.error("Error deserializing generic clothing data entry!", e);
                        throw e;
                    }

                    bakedClothingItem.setClothingName(clothingStack, entryId.getPath());
                    bakedClothingItem.setSlot(clothingStack, slot);
                    bakedClothingItem.setModelPartForParent(clothingStack, modelPart);
                    bakedClothingItem.setBakedModelLocation(clothingStack, modelLocation);
                    bakedClothingItem.setColor(clothingStack, color);
                }
        );
    }
}
