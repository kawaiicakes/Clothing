package io.github.kawaiicakes.clothing.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClientClothingRenderManager;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static net.minecraft.world.entity.ai.attributes.Attributes.*;

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
    public static final String BASE_MODEL_DATA_NBT_KEY = "BaseModelData";
    public static final String ATTRIBUTES_KEY = "Attributes";
    public static final String EQUIP_SOUND_KEY = "EquipSound";

    public static final ResourceLocation BASE_MODEL_DATA = new ResourceLocation(MOD_ID, "base_model_data");

    private Object clientClothingRenderManager;

    public ClothingItem(EquipmentSlot pSlot) {
        super(
                ArmorMaterials.LEATHER,
                pSlot,
                new Properties()
                        .tab(ClothingTab.CLOTHING_TAB)
                        .stacksTo(1)
        );
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
                "Item of passed stack '" + itemStack + "' is not a ClothingItem instance!"
        );
        return itemStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY);
    }

    public boolean hasClothingPropertyTag(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ClothingItem<?>)) return false;
        if (itemStack.getTag() == null) return false;
        CompoundTag stackTag = itemStack.getTag();
        return stackTag.contains(CLOTHING_PROPERTY_NBT_KEY)
                && stackTag.get(CLOTHING_PROPERTY_NBT_KEY) instanceof CompoundTag;
    }

    /**
     * Sets the custom model data integer for the passed {@link ItemStack}; used by an
     * {@link net.minecraft.client.renderer.block.model.ItemOverride} to determine what base texture to use for the
     * item model.
     * @param itemStack an {@link ItemStack} version of this.
     * @param modelData the hashcode representing the model file to point to as an {@code int}. How this hashcode is
     *                  obtained depends on implementation.
     */
    public void setBaseModelData(ItemStack itemStack, int modelData) {
        this.getClothingPropertyTag(itemStack).putInt(BASE_MODEL_DATA_NBT_KEY, modelData);
    }

    /**
     * Returns the custom model data integer from the passed {@link ItemStack}; used for creating an
     * {@link net.minecraft.client.renderer.block.model.ItemOverride} for
     * {@link net.minecraft.client.renderer.item.ItemProperties#TAG_CUSTOM_MODEL_DATA}.
     * @param itemStack an {@link ItemStack} version of this.
     * @return the {@code int} custom model data item property used for model overrides.
     */
    public int getBaseModelData(ItemStack itemStack) {
        return this.getClothingPropertyTag(itemStack).getInt(BASE_MODEL_DATA_NBT_KEY);
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

        this.setSlot(toReturn, this.getSlot());
        this.setColor(toReturn, 0xFFFFFF);

        this.setAttributeModifiers(
                toReturn,
                this.getDefaultAttributeModifiers(this.getSlot())
        );

        this.setEquipSound(toReturn, this.material.getEquipSound().getLocation());

        return toReturn;
    }

    /**
     * Used to display the {@link ItemStack}s in {@code pItems} in the creative menu. See super for examples. This is
     * handled automatically by the {@link ClothingEntryLoader} of
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
            final ClothingEntryLoader<T> loader = this.loaderForType();

            //noinspection unchecked
            pItems.addAll(loader.generateStacks((T) this));
        } catch (RuntimeException e) {
            LOGGER.error("Unable to generate clothing entries!", e);
        }
    }

    /**
     * Used by {@link #fillItemCategory(CreativeModeTab, NonNullList)}. Implementations return the singleton
     * {@link ClothingEntryLoader} that loads clothing entries for that implementation. Do not cache the
     * return or attempt to mutate it.
     * @return the singleton {@link ClothingEntryLoader} that loads clothing entries for this.
     */
    @NotNull
    public abstract ClothingEntryLoader<T> loaderForType();

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

    /**
     * This should not be freely used. This method exists to allow
     * {@link io.github.kawaiicakes.clothing.common.resources.NbtStackInitializer}s to easily indicate the slot
     * this piece of clothing is worn on. This ensures that only instances of this whose {@link #getSlot()} returns the
     * slot indicated in the clothing entry gets added to the creative menu.
     * @param itemStack the {@link ItemStack} instance of this; regardless of whether the return of {@link #getSlot()}
     *                  matches what the clothing data entry says.
     * @param slot the {@link EquipmentSlot} which a clothing data entry indicates it is worn on.
     * @see io.github.kawaiicakes.clothing.common.resources.NbtStackInitializer#writeToStack(Object, ItemStack)
     * @see ClothingEntryLoader#entryContainsSlotDeclaration(JsonObject)
     */
    public void setSlot(ItemStack itemStack, EquipmentSlot slot) {
        this.getClothingPropertyTag(itemStack).putString(CLOTHING_SLOT_NBT_KEY, slot.getName());
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

    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack pStack) {
        final String original = super.getDescriptionId(pStack);
        final String suffix = this.getClothingName(pStack);

        return suffix.isEmpty() ? original : original + "." + suffix;
    }

    // TODO: cool tooltip stuff? lol (add overlay names in order they are applied)
    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    public void setAttributeModifiers(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers) {
        if (!this.getClothingPropertyTag(stack).contains(ATTRIBUTES_KEY, CompoundTag.TAG_COMPOUND))
            this.getClothingPropertyTag(stack).put(ATTRIBUTES_KEY, new CompoundTag());

        CompoundTag clothingAttributesTag = this.getClothingPropertyTag(stack).getCompound(ATTRIBUTES_KEY);

        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
            ListTag modifierEntries = new ListTag();

            for (AttributeModifier modifier : entry.getValue()) {
                modifierEntries.add(modifier.save());
            }

            clothingAttributesTag.put(entry.getKey().getDescriptionId(), modifierEntries);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (!this.getSlot().equals(slot)) return super.getAttributeModifiers(slot, stack);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        CompoundTag clothingAttributesTag = this.getClothingPropertyTag(stack).getCompound(ATTRIBUTES_KEY);

        // TODO: more, new attributes? mirror changes in entry loader/generator
        Attribute[] attributes = new Attribute[] {
                ARMOR,
                ARMOR_TOUGHNESS,
                ATTACK_DAMAGE,
                ATTACK_KNOCKBACK,
                ATTACK_SPEED,
                KNOCKBACK_RESISTANCE,
                LUCK,
                MAX_HEALTH,
                MOVEMENT_SPEED
        };

        for (Attribute attribute : attributes) {
            if (!clothingAttributesTag.contains(attribute.getDescriptionId())) continue;

            ListTag modifierList = clothingAttributesTag.getList(
                    attribute.getDescriptionId(),
                    Tag.TAG_COMPOUND
            );

            List<AttributeModifier> attributeModifiers = new ArrayList<>(modifierList.size());
            for (Tag tag : modifierList) {
                if (!(tag instanceof CompoundTag compoundTag)) continue;

                AttributeModifier modifier = AttributeModifier.load(compoundTag);

                if (modifier == null) continue;

                attributeModifiers.add(modifier);
            }

            builder.putAll(
                    attribute,
                    attributeModifiers.toArray(AttributeModifier[]::new)
            );
        }

        return builder.build();
    }

    public void setEquipSound(ItemStack stack, ResourceLocation location) {
        this.getClothingPropertyTag(stack).putString(EQUIP_SOUND_KEY, location.toString());
    }

    public SoundEvent getEquipSound(ItemStack stack) {
        ResourceLocation equipSoundLocation = new ResourceLocation(
                this.getClothingPropertyTag(stack).getString(EQUIP_SOUND_KEY)
        );

        SoundEvent equipSound = ForgeRegistries.SOUND_EVENTS.getValue(equipSoundLocation);

        if (equipSound == null) {
            equipSound = this.material.getEquipSound();
            LOGGER.error("No such SoundEvent {}! Falling back on default!", equipSoundLocation);
        }

        return equipSound;
    }

    /**
     * Use {@link ItemStack} sensitive version instead
     * @return null
     */
    @Nullable
    @Override
    public SoundEvent getEquipSound() {
        return null;
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

    /**
     * {@link ModelPart} and references to {@link HumanoidClothingLayer}s which contain the models from which parts may
     * come are client-only classes; directly referencing them in {@link net.minecraft.world.item.Item} increases the
     * chances of serverside crashes due to {@link ClassNotFoundException}s.
     * <br><br>
     * Use this instead to reference {@link ModelPart}s.
     */
    public enum ModelPartReference implements StringRepresentable {
        HEAD("head"),
        HAT("hat"),
        BODY("body"),
        RIGHT_ARM("right_arm"),
        LEFT_ARM("left_arm"),
        RIGHT_LEG("right_leg"),
        LEFT_LEG("left_leg");

        private final String childName;

        ModelPartReference(String childName) {
            this.childName = childName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.childName;
        }

        public static ModelPartReference byName(String pTargetName) {
            for (ModelPartReference reference : values()) {
                if (reference.getSerializedName().equals(pTargetName)) {
                    return reference;
                }
            }

            throw new IllegalArgumentException("Invalid model reference '" + pTargetName + "'");
        }
    }
}