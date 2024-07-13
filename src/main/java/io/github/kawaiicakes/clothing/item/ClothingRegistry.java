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
    static {
        register("test_helmet", () -> new GenericClothingItem(EquipmentSlot.HEAD, "test"));
        register("test", () -> new GenericClothingItem(EquipmentSlot.CHEST, "test"));
        register("test_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS, "test"));
        register("test_boots", () -> new GenericClothingItem(EquipmentSlot.FEET, "test"));
        register(
                "ouch",
                () -> new GenericClothingItem(
                        EquipmentSlot.CHEST,
                        "ouch",
                        new String[]{"overlay"},
                        16712019
                )
        );
        register("glowie_helm", () -> new GenericClothingItem(EquipmentSlot.HEAD, "glowie", 12345679));
    }
}
