package io.github.kawaiicakes.clothing.common.item;

import com.google.common.collect.ImmutableMultimap;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.ClothingRegistry.GENERIC_SHIRT;
import static io.github.kawaiicakes.clothing.ClothingRegistry.SPOOL;

public class ClothingTabs {
    public static final ImmutableMultimap<ClothingItem.MeshStratum, ClothingLayer> OUCH_TANK_TOP = ImmutableMultimap.of(
            ClothingItem.MeshStratum.OUTER,
            new ClothingLayer(
                    new ResourceLocation(MOD_ID, "ouch"), ClothingItem.FALLBACK_COLOR, null
            )
    );

    public static final CreativeModeTab CLOTHING_TAB = new CreativeModeTab("clothing") {
        @Override
        public @NotNull ItemStack makeIcon() {
            ClothingItem clothingItem = GENERIC_SHIRT.get();
            ItemStack toReturn = clothingItem.getDefaultInstance();

            clothingItem.setClothingName(toReturn, new ResourceLocation(MOD_ID, "tank_top"));
            clothingItem.setColor(toReturn, 0xFE0253);
            clothingItem.setOverlays(toReturn, OUCH_TANK_TOP);

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
    };

    public static final CreativeModeTab CLOTHING_TAB_MISC = new CreativeModeTab("clothing_misc") {
        @Override
        public @NotNull ItemStack makeIcon() {
            ItemStack toReturn = SPOOL.get().getDefaultInstance();
            // "ashley!" PINK LIVES ON
            SPOOL.get().setColor(toReturn, 0xFFCEEA);
            return toReturn;
        }
    };
}
