package io.github.kawaiicakes.clothing.common.resources;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GenericClothingEntryLoader extends ClothingEntryLoader<GenericClothingItem> {
    protected static GenericClothingEntryLoader INSTANCE = null;

    protected GenericClothingEntryLoader() {
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
                    EquipmentSlot slot;
                    GenericClothingItem.ModelStrata layer;
                    String textureLocation;
                    String[] overlays;
                    ClothingItem.ModelPartReference[] parts;
                    int color;

                    try {
                        slot = EquipmentSlot.byName(topElement.getAsJsonPrimitive("slot").getAsString());

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

                        color = topElement.has("color")
                                ? topElement.getAsJsonPrimitive("color").getAsInt()
                                : 0xFFFFFF;

                    } catch (RuntimeException e) {
                        LOGGER.error("Error deserializing generic clothing data entry!", e);
                        throw e;
                    }

                    genericClothingItem.setClothingName(clothingStack, entryId.getPath());
                    genericClothingItem.setSlot(clothingStack, slot);
                    genericClothingItem.setGenericLayerForRender(clothingStack, layer);
                    genericClothingItem.setTextureLocation(clothingStack, textureLocation);
                    genericClothingItem.setOverlays(clothingStack, overlays);
                    genericClothingItem.setPartsForVisibility(clothingStack, parts);
                    genericClothingItem.setColor(clothingStack, color);
                }
        );
    }
}
