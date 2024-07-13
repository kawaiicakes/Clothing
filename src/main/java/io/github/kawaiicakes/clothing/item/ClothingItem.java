package io.github.kawaiicakes.clothing.item;

import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * TODO
 * Each implementation of this will likely represent an item that renders as one model type (e.g. JSON, OBJ)
 */
public abstract class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    public static final String CLOTHING_PROPERTY_NBT_KEY = "ClothingProperties";
    private Object clientClothingRenderManager;
    protected final int defaultColor;

    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties, int defaultColor) {
        super(pMaterial, pSlot, pProperties);
        this.defaultColor = defaultColor;
        this.initializeClientClothingRenderManager();
    }

    /**
     * TODO
     * @param itemStack
     * @return
     */
    @NotNull
    public CompoundTag getClothingPropertyTag(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof GenericClothingItem)) throw new IllegalArgumentException();
        return itemStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY);
    }

    /**
     * TODO
     * @return
     */
    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        CompoundTag rootTag = new CompoundTag();

        toReturn.getOrCreateTag().put(CLOTHING_PROPERTY_NBT_KEY, rootTag);

        return toReturn;
    }

    /**
     * TODO
     * @param pCategory
     * @param pItems
     */
    @Override
    public abstract void fillItemCategory(@NotNull CreativeModeTab pCategory, @NotNull NonNullList<ItemStack> pItems);

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
     * Overridden Forge method; see super for more details. This method returns a <code>String</code> representing the
     * path to the texture that should be used for this piece of clothing. It's used internally by both Minecraft
     * and this mod to return the texture for a model. It would be easiest if this was implemented per model type,
     * so it's left abstract.
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The subtype, can be any valid String according to
     *              {@link net.minecraft.resources.ResourceLocation#isValidResourceLocation(String)}.
     * @return The <code>String</code> representing the path to the texture that should be used for this piece of
     *         clothing.
     */
    @Override
    @NotNull
    public abstract String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type);

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