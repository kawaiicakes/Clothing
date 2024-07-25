package io.github.kawaiicakes.clothing.common.resources;

import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * This is a functional interface whose purpose is to store "directions" for putting NBT data into an {@link ItemStack}
 * whose item corresponds to type {@link T}. These stacks are then loaded into the client's creative menu tab.
 * @param <T> a {@link io.github.kawaiicakes.clothing.item.ClothingItem} subclass.
 * @see io.github.kawaiicakes.clothing.item.ClothingItem#fillItemCategory(CreativeModeTab, NonNullList)
 * @see ClothingResourceLoader#generateStacks(ClothingItem)
 */
@FunctionalInterface
public interface NbtStackInitializer<T> {
    void writeToStack(T clothingItem, ItemStack clothingStack);
}
