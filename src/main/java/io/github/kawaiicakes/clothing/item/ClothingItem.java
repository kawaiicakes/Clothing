package io.github.kawaiicakes.clothing.item;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.common.resources.ClothingResourceLoader;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

/**
 * Each implementation of this will likely represent an item that renders as one model type (e.g. JSON, OBJ). The
 * {@code ClothingItem} simply subclasses {@link ArmorItem} and is made to flexibly create and render pieces of
 * clothing. The {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer} is reliant on implementations
 * of this class' methods.
 */
public abstract class ClothingItem<T extends ClothingItem<?>> extends ArmorItem implements DyeableLeatherItem {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final String CLOTHING_PROPERTY_NBT_KEY = "ClothingProperties";
    public static final String CLOTHING_NAME_KEY = "name";
    public static final String CLOTHING_SLOT_NBT_KEY = "slot";

    private Object clientClothingRenderManager;

    public ClothingItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
        this.initializeClientClothingRenderManager();
    }

    /**
     * Obtains the recommended root NBT {@link CompoundTag} for clothing properties.
     * @param itemStack an {@link ItemStack} of this item.
     * @return the recommended root NBT {@link CompoundTag} for clothing properties.
     */
    @NotNull
    public CompoundTag getClothingPropertyTag(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ClothingItem)) throw new IllegalArgumentException(
                "Item of passed stack " + itemStack + " is not a ClothingItem instance!"
        );
        return itemStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY);
    }

    /**
     * Returns the default {@link ItemStack} for this. Since it's anticipated that rendering properties are stored in
     * the stack's {@link CompoundTag}, the top-level NBT structure has been pre-prepared here.
     * <br><br>
     * Further implementations should adjust the NBT data as necessary; further reading provides an example.
     * @see GenericClothingItem#getDefaultInstance()
     * @return the default {@link ItemStack} for this.
     */
    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        CompoundTag rootTag = new CompoundTag();
        toReturn.getOrCreateTag().put(CLOTHING_PROPERTY_NBT_KEY, rootTag);

        // The slot tag does not have a setter method by design to discourage changing it
        this.getClothingPropertyTag(toReturn).putString(CLOTHING_SLOT_NBT_KEY, this.getSlot().getName());
        this.setColor(toReturn, 0xFFFFFF);

        return toReturn;
    }

    /**
     * Used to display the {@link ItemStack}s in {@code pItems} in the creative menu. See super for examples. This is
     * handled automatically by the {@link io.github.kawaiicakes.clothing.common.resources.ClothingResourceLoader} of
     * the implementing class. Clothing data is declared in serverside datapacks, then received on the client for
     * use here.
     * @param pCategory the {@link CreativeModeTab} to place the items in. See {@link Item#allowedIn(CreativeModeTab)}
     *                  for usage.
     * @param pItems    the {@link NonNullList} of {@link ItemStack}s that contains the items for display.
     */
    @Override
    public void fillItemCategory(@NotNull CreativeModeTab pCategory, @NotNull NonNullList<ItemStack> pItems) {
        if (!this.allowedIn(pCategory)) return;

        try {
            final ClothingResourceLoader<T> loader = this.loaderForType();

            //noinspection unchecked
            pItems.addAll(loader.generateStacks((T) this));
        } catch (RuntimeException e) {
            LOGGER.error("Unable to generate clothing entries!", e);
        }
    }

    /**
     * Used by {@link #fillItemCategory(CreativeModeTab, NonNullList)}. Implementations return the singleton
     * {@link ClothingResourceLoader} that loads clothing entries for that implementation. Do not cache the
     * return or attempt to mutate it.
     * @return the singleton {@link ClothingResourceLoader} that loads clothing entries for this.
     */
    @NotNull
    public abstract ClothingResourceLoader<T> loaderForType();

    /**
     * Implementations essentially provide an instance of {@link ClientClothingRenderManager} to the client-exclusive
     * part of this item. The {@link ClientClothingRenderManager} is responsible for rendering clothing to a buffer.
     * How this is done will depend on what type of model is used, thus your own implementations are necessary.
     * <br><br>
     * This mod provides implementations of this that will suffice in the majority of use cases.
     * @see GenericClothingItem#acceptClientClothingRenderManager(Consumer)
     * @param clothingManager the {@link java.util.function.Supplier} of {@link ClientClothingRenderManager}.
     *                        Do not implement in this class; use an anonymous class or a separate implementation.
     */
    public abstract void acceptClientClothingRenderManager(Consumer<ClientClothingRenderManager> clothingManager);

    /**
     * Overridden Forge method; see super for more details. This method returns a <code>String</code> representing the
     * path to the texture that should be used for this piece of clothing. It's used internally by both Minecraft
     * and this mod to return the texture for a model. It would be easiest if this was implemented per model type,
     * so it's left abstract.
     * <br><br>
     * It's fine to immediately return null if you aren't relying on the generic models in
     * {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer} or on a similar implementation, since this
     * method is ultimately used in {@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer} for these
     * purposes.
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The subtype, can be any valid String according to
     *              {@link net.minecraft.resources.ResourceLocation#isValidResourceLocation(String)}.
     * @return The <code>String</code> representing the path to the texture that should be used for this piece of
     *         clothing.
     */
    @Override
    @Nullable
    public abstract String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type);

    public String getClothingName(ItemStack itemStack) {
        return this.getClothingPropertyTag(itemStack).getString(CLOTHING_NAME_KEY);
    }

    public void setClothingName(ItemStack itemStack, String name) {
        this.getClothingPropertyTag(itemStack).putString(CLOTHING_NAME_KEY, name);
    }

    /**
     * Used for verification purposes while loading {@link ItemStack}s into the creative menu, etc. This method should
     * always return the same {@link EquipmentSlot} as {@link #getSlot()}.
     * @param itemStack the {@link ItemStack} representing this
     */
    public EquipmentSlot getSlot(ItemStack itemStack) {
        return EquipmentSlot.byName(this.getClothingPropertyTag(itemStack).getString(CLOTHING_SLOT_NBT_KEY));
    }

    @Override
    public int getColor(@NotNull ItemStack pStack) {
        try {
            return pStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY).getInt(TAG_COLOR);
        } catch (RuntimeException ignored) {
            return 0xFFFFFF;
        }
    }

    @Override
    public void setColor(@NotNull ItemStack pStack, int pColor) {
        pStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY).putInt(TAG_COLOR, pColor);
    }

    @Override
    public boolean hasCustomColor(@NotNull ItemStack pStack) {
        CompoundTag root = this.getClothingPropertyTag(pStack);
        return root.contains(TAG_COLOR, 99);
    }

    @Override
    public void clearColor(@NotNull ItemStack pStack) {
        this.setColor(pStack, 0xFFFFFF);
    }

    // TODO: custom name from data entries/lang key generated from clothing
    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack pStack) {
        final String original = super.getDescriptionId(pStack);
        try {
            final String suffix = this.getClothingName(pStack);
            if (suffix.isEmpty()) throw new RuntimeException();
            return original + "." + suffix;
        } catch (RuntimeException e) {
            LOGGER.error("ItemStack {} does not have a valid clothing name in its NBT!", pStack, e);
            LOGGER.error("Falling back on default name!");
            return original;
        }
    }

    // TODO: cool tooltip stuff? lol
    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
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