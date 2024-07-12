package io.github.kawaiicakes.clothing.item;

import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

/**
 * TODO
 * Each implementation of this will likely represent an item that renders as one model type (e.g. JSON, OBJ)
 */
public abstract class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    private Object clientClothingRenderManager;
    protected final int defaultColor;

    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties, int defaultColor) {
        super(pMaterial, pSlot, pProperties);
        this.defaultColor = defaultColor;
        this.initializeClientClothingRenderManager();
    }

    /**
     * TODO: this is a critical method to document considering it's how implementations are expected to render models
     * @param clothingManager
     */
    public abstract void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager);

    /**
     * TODO
     */
    @NotNull
    public EquipmentSlot slotForModel() {
        return this.getSlot();
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

    @ApiStatus.Internal
    public Object getClientClothingRenderManager() {
        return this.clientClothingRenderManager;
    }

    @ApiStatus.Internal
    private void initializeClientClothingRenderManager() {
        if (!FMLEnvironment.dist.equals(Dist.CLIENT) || FMLLoader.getLaunchHandler().isData()) return;

        acceptClientClothingRenderManager(
                clothingManager -> {
                    /*
                        necessary since acceptClientClothingRenderManager and this class are expected to be implemented.
                        As to why this check is done to begin with, I have no clue. Forge does it in its client
                        extensions, so I'll adopt it
                     */
                    //noinspection EqualsBetweenInconvertibleTypes
                    if (this.equals(clothingManager))
                        throw new IllegalStateException(
                                "Don't implement ClientClothingRenderManager in this ClothingItem!"
                        );
                    this.clientClothingRenderManager = clothingManager;
                }
        );
    }
}