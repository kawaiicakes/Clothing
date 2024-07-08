package io.github.kawaiicakes.clothing.item.impl;

import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.ClothingMaterials;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * Implementation of {@link ClothingItem} for simple cosmetics like T-shirts or anything that looks like thinner armour.
 */
public class GenericClothingItem extends ClothingItem {
    // TODO: final assets, etc.
    // TODO: item icon changes with texture
    public GenericClothingItem(EquipmentSlot pSlot) {
        super(
                ClothingMaterials.CLOTH,
                pSlot,
                new Properties()
                        .tab(CreativeModeTab.TAB_COMBAT)
                        .stacksTo(1),
                0xFFFFFF
        );
    }

    @Override
    public @NotNull String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // TODO: final implementation as per super
        // FIXME: formal implementation of obtaining texture name from ClothingItem; I see why the ArmorMaterial was
        // used now lol
        final boolean usesGenericInnerLayer = EquipmentSlot.LEGS.equals(slot);
        return String.format(
                java.util.Locale.ROOT,
                "%s:textures/models/armor/clothing/test_%s%s.png",
                MOD_ID,
                (usesGenericInnerLayer ? "legs" : "body"),
                type == null ? "" : String.format(java.util.Locale.ROOT, "_%s", type)
        );
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull HumanoidModel<? extends LivingEntity> getClothingModel(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            HumanoidModel<? extends LivingEntity> genericModel
    ) {
        return genericModel;
    }
}