package io.github.kawaiicakes.clothing.item;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingRegistry {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> CLOTHING_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    // this saves a little bit of effort when typing lol
    public static void register(String itemName, Supplier<? extends Item> itemSupplier) {
        CLOTHING_REGISTRY.register(itemName, itemSupplier);
    }

    @Nullable
    public static Item[] getAll() {
        try {
            Item[] toReturn = new Item[CLOTHING_REGISTRY.getEntries().size()];

            int arbitraryIndexNumber = 0;
            for (RegistryObject<Item> registryObject : CLOTHING_REGISTRY.getEntries()) {
                toReturn[arbitraryIndexNumber++] = registryObject.get();
            }

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error returning Clothing from registry!", e);
            return null;
        }
    }

    @Nullable
    public static Item get(String itemName) {
        try {
            RegistryObject<Item> itemRegistryObject = CLOTHING_REGISTRY.getEntries().stream().filter(
                    (i) -> i.getId().getPath().equals(itemName)
            ).findFirst().orElseThrow();

            return itemRegistryObject.get();
        } catch (NoSuchElementException | NullPointerException e) {
            LOGGER.error("No such Clothing item as {}!", itemName, e);
            return null;
        }
    }

    // TODO: make custom icon for ClothingTab
    static {
        register("generic_hat", () -> new GenericClothingItem(EquipmentSlot.HEAD));
        register("generic_shirt", () -> new GenericClothingItem(EquipmentSlot.CHEST));
        register("generic_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
        register("generic_shoes", () -> new GenericClothingItem(EquipmentSlot.FEET));
    }
}
