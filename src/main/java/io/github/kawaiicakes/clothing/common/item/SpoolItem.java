package io.github.kawaiicakes.clothing.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SpoolItem extends Item implements DyeableLeatherItem {
    public SpoolItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public int getColor(ItemStack pStack) {
        CompoundTag compoundtag = pStack.getTagElement("display");
        return compoundtag != null && compoundtag.contains("color", 99)
                ? compoundtag.getInt("color")
                : 0xFFFFFF;
    }
}
