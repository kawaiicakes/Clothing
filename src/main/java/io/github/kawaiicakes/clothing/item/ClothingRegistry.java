package io.github.kawaiicakes.clothing.item;

import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

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
        register("test_helmet", () -> new GenericClothingItem(EquipmentSlot.HEAD));
        register("test", () -> new GenericClothingItem(EquipmentSlot.CHEST));
        register("test_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
        register("test_boots", () -> new GenericClothingItem(EquipmentSlot.FEET));

        register("ouch", () -> new GenericClothingItem(EquipmentSlot.CHEST, 16712019) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                ItemStack toReturn = super.getDefaultInstance();
                CompoundTag rootTag = toReturn.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY);

                ListTag overlays = new ListTag();
                overlays.add(StringTag.valueOf("overlay"));
                rootTag.put(OVERLAY_NBT_KEY, overlays);

                return toReturn;
            }

            @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    if (type == null) {
                        return "clothing:textures/models/armor/clothing/ouch.png";
                    }
                    return "clothing:textures/models/armor/clothing/ouch_overlay.png";
                }
            }
        );

        register("glowie_helm", () -> new GenericClothingItem(EquipmentSlot.HEAD, 12345679) {
                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/clothing/glowie_helm.png";
                }
            }
        );
    }
}
