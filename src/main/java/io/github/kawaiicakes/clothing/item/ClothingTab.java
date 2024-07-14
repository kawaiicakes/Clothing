package io.github.kawaiicakes.clothing.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class ClothingTab extends CreativeModeTab {
    public static final CreativeModeTab CLOTHING_TAB = new ClothingTab();

    public ClothingTab() {
        super("clothing");
    }

    // TODO
    @Override
    public @NotNull ItemStack makeIcon() {
        DyeableArmorItem dyeableArmorItem = (DyeableArmorItem) Items.LEATHER_CHESTPLATE;
        ItemStack toReturn = dyeableArmorItem.getDefaultInstance();
        dyeableArmorItem.setColor(toReturn, 0xFFCEEA);
        return toReturn;
    }
}
