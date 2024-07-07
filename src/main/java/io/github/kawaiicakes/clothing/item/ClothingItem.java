package io.github.kawaiicakes.clothing.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

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
     * This method is used to determine which body groups will render for this piece of clothing.
     * @return a <code>Set</code> containing the {@link EquipmentSlot}s that correspond to the body groups for render.
     */
    @NotNull
    public Set<EquipmentSlot> slotsForRender() {
        return Collections.singleton(this.getSlot());
    }

    /**
     * Implementations will return the alpha value for render.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot this piece of clothing goes in.}
     * @return The value of alpha as a float. Permitted values are 0.0 to 1.0 inclusive.
     */
    @ParametersAreNullableByDefault
    public float getAlpha(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return 1.0F;
    }

    /**
     * Determines whether an attempt will be made to render an overlay onto this piece of clothing.
     * @param livingEntity the {@link LivingEntity} the clothing is on.
     * @param stack the {@link ItemStack} representing this piece of clothing.
     * @param slot the {@link EquipmentSlot} this piece of clothing goes in.
     * @return Self-explanatory.
     */
    @ParametersAreNullableByDefault
    public boolean hasOverlay(
            LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot,
            int packedLight,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    ) {
        return false;
    }

    /**
     * Overridden Forge method; see super for details. This method returns a <code>String</code> representing the
     * path to the texture that should be used for this piece of clothing. Ideally this format
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The subtype, can be null or "overlay".
     * @return The <code>String</code> representing the path to the texture that should be used for this piece of
     *         clothing.
     */
    @Override
    @NotNull
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // TODO: final implementation and javadoc
        final boolean usesGenericInnerLayer = EquipmentSlot.LEGS.equals(slot);
        @SuppressWarnings("deprecation")
        final ResourceLocation itemKey = this.builtInRegistryHolder().key().location();
        final String itemString = itemKey.getNamespace() + "/" + itemKey.getPath();
        return String.format(
                java.util.Locale.ROOT,
                "%s:textures/models/armor/%s_%s%s.png",
                MOD_ID,
                itemString,
                (usesGenericInnerLayer ? "legs" : "body"),
                type == null ? "" : String.format(java.util.Locale.ROOT, "_%s", type)
        );
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