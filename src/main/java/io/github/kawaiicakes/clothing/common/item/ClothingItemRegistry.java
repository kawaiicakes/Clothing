package io.github.kawaiicakes.clothing.common.item;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.block.ClothingBlockRegistry.TEXTILE_LOOM_BLOCK;
import static io.github.kawaiicakes.clothing.common.item.ClothingTab.CLOTHING_TAB;
import static net.minecraftforge.registries.ForgeRegistries.ITEMS;

public class ClothingItemRegistry {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> CLOTHING_ITEM_REGISTRY
            = DeferredRegister.create(ITEMS, MOD_ID);

    public static final DeferredRegister<Item> ITEM_REGISTRY
            = DeferredRegister.create(ITEMS, MOD_ID);

    public static final RegistryObject<GenericClothingItem> GENERIC_HAT
            = CLOTHING_ITEM_REGISTRY.register("generic_hat", () -> new GenericClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHIRT
            = CLOTHING_ITEM_REGISTRY.register("generic_shirt", () -> new GenericClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<GenericClothingItem> GENERIC_PANTS
            = CLOTHING_ITEM_REGISTRY.register("generic_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<GenericClothingItem> GENERIC_SHOES
            = CLOTHING_ITEM_REGISTRY.register("generic_shoes", () -> new GenericClothingItem(EquipmentSlot.FEET));
    public static final RegistryObject<BakedModelClothingItem> BAKED_HAT
            = CLOTHING_ITEM_REGISTRY.register("baked_hat", () -> new BakedModelClothingItem(EquipmentSlot.HEAD));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHIRT
            = CLOTHING_ITEM_REGISTRY.register("baked_shirt", () -> new BakedModelClothingItem(EquipmentSlot.CHEST));
    public static final RegistryObject<BakedModelClothingItem> BAKED_PANTS
            = CLOTHING_ITEM_REGISTRY.register("baked_pants", () -> new BakedModelClothingItem(EquipmentSlot.LEGS));
    public static final RegistryObject<BakedModelClothingItem> BAKED_SHOES
            = CLOTHING_ITEM_REGISTRY.register("baked_shoes", () -> new BakedModelClothingItem(EquipmentSlot.FEET));

    public static final RegistryObject<BlockItem> TEXTILE_LOOM_ITEM = ITEM_REGISTRY.register(
            "textile_loom",
            () -> new BlockItem(TEXTILE_LOOM_BLOCK.get(), new Item.Properties().tab(CLOTHING_TAB))
    );

    @Nullable
    public static List<RegistryObject<Item>> getGeneric() {
        try {
            List<RegistryObject<Item>> genericClothingItems = new ArrayList<>();

            for (RegistryObject<Item> registryObject : CLOTHING_ITEM_REGISTRY.getEntries()) {
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
    public static ClothingItem<?>[] getAll() {
        try {
            ClothingItem<?>[] toReturn = new ClothingItem<?>[CLOTHING_ITEM_REGISTRY.getEntries().size()];

            int arbitraryIndexNumber = 0;
            for (RegistryObject<Item> registryObject : CLOTHING_ITEM_REGISTRY.getEntries()) {
                toReturn[arbitraryIndexNumber++] = (ClothingItem<?>) registryObject.get();
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
            RegistryObject<Item> itemRegistryObject = CLOTHING_ITEM_REGISTRY.getEntries().stream().filter(
                    (i) -> i.getId().getPath().equals(itemName)
            ).findFirst().orElseThrow();

            return itemRegistryObject.get();
        } catch (NoSuchElementException | NullPointerException e) {
            LOGGER.error("No such Clothing item as {}!", itemName, e);
            return null;
        }
    }
}
