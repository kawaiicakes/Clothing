package io.github.kawaiicakes.clothing.item.impl;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GenericBakedModelClothingItem extends BakedModelClothingItem {
    public static final String MODEL_ID_KEY = "modelId";
    public static final String MODEL_PART_REFERENCE_KEY = "parentPart";

    public GenericBakedModelClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
    }

    @Override
    public @NotNull ModelPartReference getModelPartForParent(ItemStack itemStack) {
        String modelPartReferenceString = this.getClothingPropertyTag(itemStack).getString(MODEL_PART_REFERENCE_KEY);
        return ModelPartReference.byName(modelPartReferenceString);
    }

    public void setModelPartForReference(ItemStack itemStack, ModelPartReference modelPart) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_PART_REFERENCE_KEY, modelPart.getSerializedName());
    }

    @Override
    public boolean hasDynamicColorModel() {
        return false;
    }

    @Override
    public void fillItemCategory(@NotNull CreativeModeTab pCategory, @NotNull NonNullList<ItemStack> pItems) {

    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return null;
    }

    @Override
    public ResourceLocation bakedModelLocation(ItemStack itemStack) {
        return new ResourceLocation(this.getClothingPropertyTag(itemStack).getString(MODEL_ID_KEY));
    }

    public void setBakedModelLocation(ItemStack itemStack, ResourceLocation modelLocation) {
        this.getClothingPropertyTag(itemStack).putString(MODEL_ID_KEY, modelLocation.toString());
        int texHash = modelLocation.hashCode();

        assert itemStack.getTag() != null;
        itemStack.getTag().putInt(
                "CustomModelData", texHash
        );
    }
}
