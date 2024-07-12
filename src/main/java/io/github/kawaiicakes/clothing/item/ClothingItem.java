package io.github.kawaiicakes.clothing.item;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNullableByDefault;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * TODO
 * Each implementation of this will likely represent an item that renders as one model type (e.g. JSON, OBJ)
 */
public abstract class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    protected final int defaultColor;

    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties, int defaultColor) {
        super(pMaterial, pSlot, pProperties);
        this.defaultColor = defaultColor;
    }

    // FIXME: prevent reaching across sides: interface like IClientItemExtensions for this method and adjacent ones?
    /**
     * TODO
     * @param pClothingLayer
     * @param pItemStack
     * @param pMatrixStack
     * @param pBuffer
     * @param pPackedLight
     * @param pLivingEntity
     * @param pLimbSwing
     * @param pLimbSwingAmount
     * @param pPartialTicks
     * @param pAgeInTicks
     * @param pNetHeadYaw
     * @param pHeadPitch
     * @param <T>
     * @param <A>
     * @param <M>
     */
    public abstract <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> void render(
            @NotNull HumanoidClothingLayer<T, M, A> pClothingLayer,
            @NotNull ItemStack pItemStack,
            @NotNull PoseStack pMatrixStack,
            @NotNull MultiBufferSource pBuffer, int pPackedLight,
            @NotNull T pLivingEntity,
            float pLimbSwing, float pLimbSwingAmount,
            float pPartialTicks, float pAgeInTicks,
            float pNetHeadYaw, float pHeadPitch
    );

    /**
     * TODO
     */
    @NotNull
    public EquipmentSlot slotForModel() {
        return this.getSlot();
    }

    // FIXME: clothing does not become translucent
    // FIXME: values not equal to 1.0F cause colour of overlay to "infect" base layer for GenericClothingItems
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
}