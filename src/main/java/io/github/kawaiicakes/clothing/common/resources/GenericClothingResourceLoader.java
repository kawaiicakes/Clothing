package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GenericClothingResourceLoader extends ClothingResourceLoader<GenericClothingItem> {
    protected static GenericClothingResourceLoader INSTANCE = null;

    protected GenericClothingResourceLoader() {
        super("generic");
        INSTANCE = this;
    }

    public static GenericClothingResourceLoader getInstance() {
        if (INSTANCE == null) {
            return new GenericClothingResourceLoader();
        }

        return INSTANCE;
    }

    @Override
    public @NotNull NbtStackInitializer<GenericClothingItem> deserializeFromJson(
            ResourceLocation entryId,
            JsonObject topElement
    ) {
        return (
                (clothingItem, clothingStack) ->  {
                    // TODO
                }
        );
    }
}
