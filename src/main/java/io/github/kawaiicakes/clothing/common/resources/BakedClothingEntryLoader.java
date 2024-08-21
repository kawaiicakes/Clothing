package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.common.data.ClothingProperties;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static io.github.kawaiicakes.clothing.ClothingRegistry.*;

public class BakedClothingEntryLoader extends ClothingEntryLoader<BakedModelClothingItem> {
    protected static BakedClothingEntryLoader INSTANCE = null;

    public BakedClothingEntryLoader() {
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
                        models = ClothingProperties.MODEL_PARENTS.readPropertyFromJson(topElement);
                    } catch (RuntimeException e) {
                        LOGGER.error("Error deserializing baked clothing data entry!", e);
                        throw e;
                    }

                    bakedClothingItem.setModelPartLocations(clothingStack, models);
                }
        );
    }

    @Override
    public BakedModelClothingItem[] clothingItemsForLoader() {
        return new BakedModelClothingItem[]{
                BAKED_HAT.get(),
                BAKED_SHIRT.get(),
                BAKED_PANTS.get(),
                BAKED_SHOES.get()
        };
    }
}
