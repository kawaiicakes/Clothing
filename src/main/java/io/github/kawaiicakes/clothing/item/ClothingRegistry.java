package io.github.kawaiicakes.clothing.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingRegistry {
    public static final DeferredRegister<Item> CLOTHING_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<ClothingItem> TEST = CLOTHING_REGISTRY.register(
            "test",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.CHEST, new Item.Properties(), 16777215) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/generic_template.png";
                }
            }
    );

    public static final RegistryObject<ClothingItem> TEST_HELMET = CLOTHING_REGISTRY.register(
            "test_helmet",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.HEAD, new Item.Properties(), 16777215) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/generic_template.png";
                }
            }
    );

    public static final RegistryObject<ClothingItem> TEST_PANTS = CLOTHING_REGISTRY.register(
            "test_pants",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.LEGS, new Item.Properties(), 16777215) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/generic_template.png";
                }
            }
    );

    public static final RegistryObject<ClothingItem> TEST_BOOTS = CLOTHING_REGISTRY.register(
            "test_boots",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.FEET, new Item.Properties(), 16777215) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/generic_template.png";
                }
            }
    );

    public static final RegistryObject<ClothingItem> TEST_2 = CLOTHING_REGISTRY.register(
            "glowie_helm",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.HEAD, new Item.Properties(), 12345679) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    return "clothing:textures/models/armor/glowie_helm.png";
                }
            }
    );

    public static final RegistryObject<ClothingItem> TEST_3 = CLOTHING_REGISTRY.register(
            "ouch",
            () -> new ClothingItem(ArmorMaterials.NETHERITE, EquipmentSlot.CHEST, new Item.Properties(), 16712019) {
                @Override
                public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<? extends LivingEntity> genericModel) {
                    return genericModel;
                }

                // FIXME: clothing does not become translucent
                @Override
                public float getAlpha(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
                    // FIXME: values not equal to 1.0F cause colour of overlay to "infect" base layer
                    return 1.0F;
                }

                @Override
                public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                    if (type == null) {
                        // TODO: implement getArmorResource in HumanoidClothingLayer
                        return "clothing:textures/models/armor/ouch.png";
                    }
                    return "clothing:textures/models/armor/ouch_overlay.png";
                }

                @Override
                public @NotNull ResourceLocation overlayResource(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, int packedLight, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
                    return new ResourceLocation(MOD_ID, "ouch_overlay");
                }
            }
    );
}
