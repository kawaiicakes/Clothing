package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.common.data.ClothingProperties;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingRegistry.*;

public class GenericClothingEntryLoader extends ClothingEntryLoader<GenericClothingItem> {
    protected static GenericClothingEntryLoader INSTANCE = null;

    public GenericClothingEntryLoader() {
        super("generic");
        INSTANCE = this;
    }

    public static GenericClothingEntryLoader getInstance() {
        if (INSTANCE == null) {
            return new GenericClothingEntryLoader();
        }

        return INSTANCE;
    }

    @Override
    public @NotNull NbtStackInitializer<GenericClothingItem> deserializeFromJson(
            ResourceLocation entryId,
            JsonObject topElement
    ) {
        return (
                (genericClothingItem, clothingStack) ->  {
                    GenericClothingItem.ModelStrata layer;
                    ResourceLocation textureLocation;
                    ResourceLocation[] overlays;
                    ClothingItem.ModelPartReference[] parts;

                    try {
                        layer = ClothingProperties.MODEL_LAYER.readPropertyFromJson(topElement);
                        textureLocation = ClothingProperties.TEXTURE_LOCATION.readPropertyFromJson(topElement);
                        overlays = ClothingProperties.OVERLAYS.readPropertyFromJson(topElement);
                        parts = ClothingProperties.VISIBLE_PARTS.readPropertyFromJson(topElement);
                    } catch (Exception e) {
                        LOGGER.error("Error deserializing generic clothing data entry!", e);
                        throw e;
                    }

                    genericClothingItem.setGenericLayerForRender(clothingStack, layer);
                    genericClothingItem.setTextureLocation(clothingStack, textureLocation);
                    genericClothingItem.setOverlays(clothingStack, overlays);
                    genericClothingItem.setPartsForVisibility(clothingStack, parts);
                }
        );
    }

    @Override
    public GenericClothingItem[] clothingItemsForLoader() {
        return new GenericClothingItem[]{
                GENERIC_HAT.get(),
                GENERIC_SHIRT.get(),
                GENERIC_PANTS.get(),
                GENERIC_SHOES.get()
        };
    }
}
