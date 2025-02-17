package io.github.kawaiicakes.clothing.common.resources;

import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * This is a functional interface whose purpose is to store "directions" for putting NBT data into an {@link ItemStack}
 * whose item corresponds to a {@link ClothingItem}. These stacks are then loaded into the client's creative menu tab.
 * @see ClothingItem#fillItemCategory(CreativeModeTab, NonNullList)
 * @see ClothingEntryLoader#getStacks()
 */
@FunctionalInterface
public interface NbtStackInitializer {
    /**
     * Implementations MUST write to the stack's slot using {@link ClothingItem#setSlot(ItemStack, EquipmentSlot)}.
     * This information MUST be written to the {@code clothingStack}; even if a formal {@link IllegalArgumentException}
     * is only thrown if the JSON clothing entry data itself has an invalid slot declaration. It's encouraged that
     * implementations throw their own {@link IllegalArgumentException}s if some non-optional information is missing.
     * @param clothingItem the clothing item for which a stack will be made.
     * @param clothingStack the {@link ItemStack} returned from {@link ClothingItem#getDefaultInstance()}.
     *                      Operate on this argument to manipulate the stack that will be returned later down the line.
     */
    void writeToStack(ClothingItem clothingItem, ItemStack clothingStack);

    default NbtStackInitializer and(NbtStackInitializer other) {
        Objects.requireNonNull(other);
        return (
                (clothingItem, clothingStack) -> {
                    this.writeToStack(clothingItem, clothingStack);
                    other.writeToStack(clothingItem, clothingStack);
                }
        );
    }
}
