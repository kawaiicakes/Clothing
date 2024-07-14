package io.github.kawaiicakes.clothing.item;

import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingRegistry {
    public static final DeferredRegister<Item> CLOTHING_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    // this saves a little bit of effort when typing lol
    public static void register(String itemName, Supplier<? extends Item> itemSupplier) {
        CLOTHING_REGISTRY.register(itemName, itemSupplier);
    }

    // I don't think I'm going to need to reference specific items anytime soon, so caching this isn't necessary
    // I lied. TODO: make icon for ClothingTab
    static {
        register("generic_hat", () -> new GenericClothingItem(EquipmentSlot.HEAD));
        register("generic_shirt", () -> new GenericClothingItem(EquipmentSlot.CHEST));
        register("generic_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
        register("generic_shoes", () -> new GenericClothingItem(EquipmentSlot.FEET));
        // TODO: colour 16712019
        // register("ouch", () -> new GenericClothingItem(EquipmentSlot.CHEST));
        // TODO: colour 12345679
        // register("glowie_helm", () -> new GenericClothingItem(EquipmentSlot.HEAD));
    }
}
