package io.github.kawaiicakes.clothing.item;

import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

    // I don't think I'm going to need to reference specific items anytime soon, so this is fine to save memory
    static {
        register("test_helmet", () -> new GenericClothingItem(EquipmentSlot.HEAD));
        register("test", () -> new GenericClothingItem(EquipmentSlot.CHEST));
        register("test_pants", () -> new GenericClothingItem(EquipmentSlot.LEGS));
        register("test_boots", () -> new GenericClothingItem(EquipmentSlot.FEET));

        register("ouch", () -> new ClothingItem(ClothingMaterials.CLOTH, EquipmentSlot.CHEST, new Item.Properties(), 16712019) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }


                @Override
                public float getAlpha(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {

                    return 1.0F;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    if (type == null) {
                        return "clothing:textures/models/armor/clothing/ouch.png";
                    }
                    return "clothing:textures/models/armor/clothing/ouch_overlay.png";
                }

                @Override
                public boolean hasOverlay(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
                    return true;
                }
            }
        );

        register("glowie_helm", () -> new ClothingItem(ClothingMaterials.CLOTH, EquipmentSlot.HEAD, new Item.Properties(), 12345679) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/clothing/glowie_helm.png";
                }
            }
        );
    }
}
