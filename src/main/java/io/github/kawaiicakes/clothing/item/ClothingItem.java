package io.github.kawaiicakes.clothing.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.function.Consumer;

public abstract class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    protected final int defaultColor;

    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties, int defaultColor) {
        super(pMaterial, pSlot, pProperties);
        this.defaultColor = defaultColor;
    }

    /**
     * Implementations should return the desired model for this piece of clothing. Bear in mind that the models are
     * rendered as layers in {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer}.
     * <br><br>
     * If you only plan to use the default armour model, just immediately return <code>genericModel</code>.
     * @param livingEntity the {@link LivingEntity} this model is made for.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}.
     * @return the desired {@link HumanoidModel} for this piece of clothing.
     */
    @NotNull
    @ParametersAreNullableByDefault
    public abstract HumanoidModel<? extends LivingEntity> getClothingModel(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            HumanoidModel<? extends LivingEntity> genericModel
    );

    /**
     * Implementations will return the alpha value for render.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @return The value of alpha as a float. Permitted values are 0.0 to 1.0 inclusive.
     */
    @ParametersAreNullableByDefault
    public abstract float getAlpha(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    );

    /**
     * If this returns non-null, an attempt will be made to render the overlay onto this piece of clothing.
     * The overlay is not affected by dyeing.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @return the {@link ResourceLocation} pointing to the texture of the overlay. Ideally, texture names should
     * conform to the format //TODO
     */
    @Nullable
    @ParametersAreNullableByDefault
    public abstract ResourceLocation overlayResource(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    );

    /**
     * If the implementation of this class is returning the <code>genericModel</code> passed to it in
     * {@link #getClothingModel(LivingEntity, ItemStack, EquipmentSlot, HumanoidModel)}, then implementations of this
     * method determine whether the base model or over model should be used.
     * @return <code>true</code> if the over model should be used. <code>false</code> for the base.
     */
    @ParametersAreNullableByDefault
    public boolean usesGenericOverModel(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return false;
    }

    @Override
    public int getColor(@NotNull ItemStack pStack) {
        // contrived implementation is done in case a third-party mod changes the super
        int toReturn = DyeableLeatherItem.super.getColor(pStack);
        CompoundTag nbt = pStack.getTagElement(TAG_DISPLAY);
        if ((nbt == null || !nbt.contains(TAG_COLOR, 99)) && toReturn == DEFAULT_LEATHER_COLOR)
            toReturn = this.defaultColor;
        return toReturn;
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(
                // Forge javadocs say not to use a concrete implementation of IClientItemExtensions here
                new IClientItemExtensions() {
                    @Override
                    public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity livingEntity,
                            ItemStack itemStack, EquipmentSlot equipmentSlot,
                            HumanoidModel<?> original
                    ) {
                        return ClothingItem.this.getClothingModel(livingEntity, itemStack, equipmentSlot, original);
                    }
                }
        );
    }
}