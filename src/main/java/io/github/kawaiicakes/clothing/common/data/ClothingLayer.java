package io.github.kawaiicakes.clothing.common.data;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * A {@link ClothingLayer} is simply a convenience record containing the information required to render stuff like
 * overlays and clothing; namely a texture location and colour. Also contains methods for serializing the data.
 */
public record ClothingLayer(ResourceLocation textureLocation, int color, @Nullable ClothingVisibility clothingVisibility) {
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();
        toReturn.putString("texture", this.textureLocation.toString());
        toReturn.putInt("color", this.color);
        if (this.clothingVisibility != null) toReturn.put("visibility", this.clothingVisibility.toNbt());
        return toReturn;
    }

    public JsonObject toJson() {
        JsonObject toReturn = new JsonObject();
        toReturn.addProperty("texture", this.textureLocation.toString());
        toReturn.addProperty("color", this.color);
        if (this.clothingVisibility != null) toReturn.add("visibility", this.clothingVisibility.toJson());
        return toReturn;
    }

    public static ClothingLayer fromNbt(CompoundTag tag) {
        ResourceLocation textureLocation;
        int color;
        ClothingVisibility visibility;

        try {
            // Suppressed redundant cast warning since I want an exception to be thrown if the tag type mismatches.
            //noinspection RedundantCast
            textureLocation = new ResourceLocation(
                    ((StringTag) Objects.requireNonNull(tag.get("texture"))).getAsString()
            );
            color = ((IntTag) Objects.requireNonNull(tag.get("color"))).getAsInt();
            visibility = tag.contains("visibility", Tag.TAG_LIST)
                    ? ClothingVisibility.fromNbt(tag.getList("visibility", Tag.TAG_STRING))
                    : null;
        } catch (Exception e) {
            LOGGER.error("Unable to deserialize ClothingLayer!", e);
            textureLocation = ClothingItem.DEFAULT_TEXTURE_LOCATION;
            color = 0xFFFFFF;
            visibility = null;
        }

        return new ClothingLayer(textureLocation, color, visibility);
    }

    public static ClothingLayer fromJson(JsonObject json) {
        ResourceLocation textureLocation;
        int color;
        ClothingVisibility visibility;

        try {
            textureLocation = new ResourceLocation(json.getAsJsonPrimitive("texture").getAsString());
            color = json.getAsJsonPrimitive("color").getAsInt();
            visibility = json.has("visibility") && json.get("visibility").isJsonArray()
                    ? ClothingVisibility.fromJson(json.getAsJsonArray("visibility"))
                    : null;
        } catch (Exception e) {
            LOGGER.error("Unable to deserialize ClothingLayer!", e);
            textureLocation = ClothingItem.DEFAULT_TEXTURE_LOCATION;
            color = 0xFFFFFF;
            visibility = null;
        }

        return new ClothingLayer(textureLocation, color, visibility);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClothingLayer other)) return false;
        return this.textureLocation.equals(other.textureLocation)
                && this.color == other.color
                && this.clothingVisibility == other.clothingVisibility;
    }
}
