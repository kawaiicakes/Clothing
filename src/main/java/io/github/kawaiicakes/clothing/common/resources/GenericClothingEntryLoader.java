package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static io.github.kawaiicakes.clothing.common.item.ClothingRegistry.*;

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
                    String textureLocation;
                    String[] overlays;
                    ClothingItem.ModelPartReference[] parts;

                    try {
                        layer = topElement.has("render_layer")
                                ? GenericClothingItem.ModelStrata.byName(
                                        topElement.getAsJsonPrimitive("render_layer").getAsString()
                                    )
                                : genericClothingItem.getGenericLayerForRender(clothingStack);

                        textureLocation = topElement.has("texture")
                                ? topElement.getAsJsonPrimitive("texture").getAsString()
                                : "default";
                        if (textureLocation.isEmpty()) textureLocation = "default";

                        overlays = topElement.has("overlays")
                                ? collapseJsonArrayToStringArray(topElement.getAsJsonArray("overlays"))
                                : new String[0];

                        parts = topElement.has("part_visibility")
                                ? Arrays.stream(
                                        collapseJsonArrayToStringArray(
                                            topElement.getAsJsonArray("part_visibility")
                                        )
                                    )
                                    .map(ClothingItem.ModelPartReference::byName)
                                    .toArray(ClothingItem.ModelPartReference[]::new)
                                : genericClothingItem.defaultPartVisibility();
                    } catch (RuntimeException e) {
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
