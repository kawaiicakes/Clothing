package io.github.kawaiicakes.clothing.common.item;

import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.ClothingRegistry.GENERIC_SHIRT;

public class ClothingTab extends CreativeModeTab {
    public static final CreativeModeTab CLOTHING_TAB = new ClothingTab();

    public ClothingTab() {
        super("clothing");
    }

    @Override
    public @NotNull ItemStack makeIcon() {
        GenericClothingItem genericClothingItem = GENERIC_SHIRT.get();
        ItemStack toReturn = genericClothingItem.getDefaultInstance();

        genericClothingItem.setClothingName(toReturn, new ResourceLocation(MOD_ID, "tank_top"));
        genericClothingItem.setColor(toReturn, 0xFE0253);
        genericClothingItem.setOverlays(toReturn, new ResourceLocation[]{new ResourceLocation(MOD_ID, "ouch")});

        return toReturn;
    }

    /**
     * This only exists because I have grown a little fond of this placeholder asset lol
     */
    @SuppressWarnings("unused")
    public static ItemStack makeOldIcon() {
        ItemStack oldIcon = Items.LEATHER_CHESTPLATE.getDefaultInstance();
        ((DyeableArmorItem) Items.LEATHER_CHESTPLATE).setColor(oldIcon, 0xFFCEEA);
        return oldIcon;
    }
}
