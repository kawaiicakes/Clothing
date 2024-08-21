package io.github.kawaiicakes.clothing.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * A {@link ClothingProperty} is an object representing a serializable property of a piece of clothing stored in that
 * piece of clothing's NBT data; more specifically in the
 * {@link io.github.kawaiicakes.clothing.common.item.ClothingItem#CLOTHING_PROPERTY_NBT_KEY}. This interface exists to
 * help standardize serialization to/from both NBT, JSON, and {@link FriendlyByteBuf}s; ultimately reducing workload
 * and the chances for messing up. Given the way it's set up, I would recommend that implementations use a singleton
 * pattern or some derivative thereof.
 * @param <T> the type of {@link Object} which this property ultimately represents.
 * @author kawaiicakes
 */
public abstract class ClothingProperty<T> {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final String nbtKey;

    protected ClothingProperty(String nbtKey) {
        this.nbtKey = nbtKey;
    }

    public String getNbtKey() {
        return this.nbtKey;
    }

    /**
     * Implementations should make the return a deep copy of the default value; so as not to mutate the underlying data
     * @return the instance of {@link T} representing the default value for this property.
     */
    public abstract T getDefaultValue();
    public abstract Tag writeToTag(T property);
    public abstract JsonElement writeToJson(T property);
    public abstract T readFromTag(Tag property);
    public abstract T readFromJson(JsonElement property);

    public void writePropertyToStack(ItemStack stack, T property) {
        try {
            if (!(stack.getItem() instanceof ClothingItem<?> clothingItem))
                throw new IllegalArgumentException(
                        "Passed ItemStack "
                                + stack
                                + " is not a clothing item!"
                );

            clothingItem.getClothingPropertiesTag(stack).put(this.getNbtKey(), this.writeToTag(property));
        } catch (Exception e) {
            LOGGER.error("Unable to write property '{}' to stack!", this.getNbtKey(), e);
        }
    }

    public T readPropertyFromStack(ItemStack stack) {
        try {
            if (!(stack.getItem() instanceof ClothingItem<?> clothingItem))
                throw new IllegalArgumentException(
                        "Passed ItemStack "
                                + stack
                                + " is not a clothing item!"
                );

            Tag propertyAsTag = clothingItem.getClothingPropertiesTag(stack).get(this.getNbtKey());

            return this.readFromTag(propertyAsTag);
        } catch (Exception e) {
            LOGGER.error("Unable to read property '{}' from stack! Falling on default!", this.getNbtKey(), e);
            return this.getDefaultValue();
        }
    }

    public void writePropertyToJson(JsonObject clothingPropertiesObject, T property) {
        try {
            clothingPropertiesObject.add(this.getNbtKey(), this.writeToJson(property));
        } catch (Exception e) {
            LOGGER.error("Unable to write property '{}' to JSON!", this.getNbtKey(), e);
            LOGGER.error("Attempted to write into: {}", clothingPropertiesObject);
        }
    }

    public T readPropertyFromJson(JsonObject clothingPropertiesObject) {
        try {
            if (!clothingPropertiesObject.has(this.getNbtKey()))
                return this.getDefaultValue();

            return this.readFromJson(clothingPropertiesObject.get(this.getNbtKey()));
        } catch (Exception e) {
            LOGGER.error("Unable to read property '{}' from JSON!", this.getNbtKey(), e);
            LOGGER.error("Attempted to read from: {}", clothingPropertiesObject);
            return this.getDefaultValue();
        }
    }
}
