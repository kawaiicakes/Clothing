package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import net.minecraft.resources.ResourceLocation;
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
                (clothingItem, clothingStack) ->  {
                    // TODO
                }
        );
    }
}
