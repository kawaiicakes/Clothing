package io.github.kawaiicakes.clothing.item;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingRegistry {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> CLOTHING_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<GenericClothingItem> GENERIC_HAT
            = CLOTHING_REGISTRY.register("generic_hat", () -> new GenericClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHIRT
            = CLOTHING_REGISTRY.register("generic_shirt", () -> new GenericClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<GenericClothingItem> GENERIC_PANTS
            = CLOTHING_REGISTRY.register("generic_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHOES
            = CLOTHING_REGISTRY.register("generic_shoes", () -> new GenericClothingItem(EquipmentSlot.FEET));
    public static final RegistryObject<BakedModelClothingItem> BAKED_HAT
            = CLOTHING_REGISTRY.register("baked_hat", () -> new BakedModelClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHIRT
            = CLOTHING_REGISTRY.register("baked_shirt", () -> new BakedModelClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<BakedModelClothingItem> BAKED_PANTS
            = CLOTHING_REGISTRY.register("baked_pants", () -> new BakedModelClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHOES
            = CLOTHING_REGISTRY.register("baked_shoes", () -> new BakedModelClothingItem(EquipmentSlot.FEET));

    @Nullable
    public static List<RegistryObject<Item>> getGeneric() {
        try {
            List<RegistryObject<Item>> genericClothingItems = new ArrayList<>();

            for (RegistryObject<Item> registryObject : CLOTHING_REGISTRY.getEntries()) {
                if (!registryObject.isPresent() || !(registryObject.get() instanceof GenericClothingItem))
                    continue;

                genericClothingItems.add(registryObject);
            }

            if (genericClothingItems.isEmpty()) throw new RuntimeException("No generic clothing items exist!");

            return genericClothingItems;
        } catch (RuntimeException e) {
            LOGGER.error("Error returning Clothing from registry!", e);
            return null;
        }
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
}
