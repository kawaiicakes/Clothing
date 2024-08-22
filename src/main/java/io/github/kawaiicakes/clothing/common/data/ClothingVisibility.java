package io.github.kawaiicakes.clothing.common.data;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;

/**
 * A {@link ClothingVisibility} is simply a convenience class containing the model part visibilities for the
 * {@link ClothingLayer} it's declared in. Used for meshes as models do not rely on part visibility.
 * Contains methods for easy serialization.
 * <br><br>
 * Despite its function, booleans are not used. Instead, presence of a
 * {@link io.github.kawaiicakes.clothing.common.item.ClothingItem.ModelPartReference} in {@link #visibilityMap}
 * implies that the part is visible.
 * @see io.github.kawaiicakes.clothing.client.HumanoidClothingLayer#setPartVisibility(HumanoidModel, EquipmentSlot)
 */
public class ClothingVisibility {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final ClothingItem.ModelPartReference[] visibilityMap;

    public ClothingVisibility(ClothingItem.ModelPartReference[] visibility) {
        this.visibilityMap = visibility;
    }

    public ClothingItem.ModelPartReference[] asArray() {
        return this.visibilityMap;
    }

    public ListTag toNbt() {
        ListTag toReturn = new ListTag();

        for (ClothingItem.ModelPartReference modelPartReference : this.visibilityMap) {
            toReturn.add(StringTag.valueOf(modelPartReference.getSerializedName()));
        }

        return toReturn;
    }

    public JsonArray toJson() {
        JsonArray toReturn = new JsonArray();

        for (ClothingItem.ModelPartReference modelPartReference : this.visibilityMap) {
            toReturn.add(modelPartReference.getSerializedName());
        }

        return toReturn;
    }

    public static ClothingVisibility fromNbt(ListTag tag) {
        ClothingItem.ModelPartReference[] toReturn = new ClothingItem.ModelPartReference[tag.size()];

        try {
            for (int i = 0; i < tag.size(); i++) {
                //noinspection RedundantCast
                toReturn[i] = ClothingItem.ModelPartReference.byName(((StringTag) tag.get(i)).getAsString());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to deserialize ClothingVisibility from NBT!", e);
            toReturn = new ClothingItem.ModelPartReference[0];
        }

        return new ClothingVisibility(toReturn);
    }

    public static ClothingVisibility fromJson(JsonArray json) {
        ClothingItem.ModelPartReference[] toReturn = new ClothingItem.ModelPartReference[json.size()];

        try {
            for (int i = 0; i < json.size(); i++) {
                toReturn[i] = ClothingItem.ModelPartReference.byName(json.get(i).getAsJsonPrimitive().getAsString());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to deserialize ClothingVisibility from NBT!", e);
            toReturn = new ClothingItem.ModelPartReference[0];
        }

        return new ClothingVisibility(toReturn);
    }
}
