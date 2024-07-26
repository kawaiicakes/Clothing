package io.github.kawaiicakes.clothing.common.resources;

import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
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
    /**
     * Implementations MUST write to the stack's slot using {@link ClothingItem#setSlot(ItemStack, EquipmentSlot)}.
     * An {@link IllegalArgumentException} will be thrown otherwise.
     * @param clothingItem the {@link T} clothing item for which a stack will be made.
     * @param clothingStack the {@link ItemStack} returned from {@link ClothingItem#getDefaultInstance()} of {@link T}.
     *                      Operate on this argument to manipulate the stack that will be returned later down the line.
     */
    void writeToStack(T clothingItem, ItemStack clothingStack);
}
