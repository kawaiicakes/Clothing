package io.github.kawaiicakes.clothing.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingItemRenderer;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.NonNullList;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import java.util.*;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static net.minecraft.core.cauldron.CauldronInteraction.DYED_ITEM;

// TODO: the default colour given by a clothing entry is reflected in #hasCustomColor
// TODO: add fallbacks everywhere necessary so clothing with fucked up NBT doesn't just break the game. A Source engine ERROR model would be nice for baked models that can't be found too
/**
 * Each implementation of this will likely represent an item that renders as one model type (e.g. JSON, OBJ). The
 * {@code ClothingItem} simply subclasses {@link ArmorItem} and is made to flexibly create and render pieces of
 * clothing. The {@link io.github.kawaiicakes.clothing.client.HumanoidClothingLayer} is reliant on implementations
 * of this class' methods.
 */
public class ClothingItem extends ArmorItem implements DyeableLeatherItem {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final String CLOTHING_PROPERTY_NBT_KEY = "ClothingProperties";
    public static final String CLOTHING_NAME_KEY = "name";
    public static final String CLOTHING_SLOT_NBT_KEY = "slot";
    public static final String CLOTHING_LORE_NBT_KEY = "lore";
    public static final String ATTRIBUTES_KEY = "attributes";
    public static final String EQUIP_SOUND_KEY = "equip_sound";
    public static final String MAX_DAMAGE_KEY = "durability";
    public static final String MODEL_LAYER_NBT_KEY = "modelLayer";
    public static final String TEXTURE_LOCATION_NBT_KEY = "texture";
    public static final String OVERLAY_NBT_KEY = "overlays";
    public static final String PART_VISIBILITY_KEY = "partVisibility";
    public static final String MODEL_PARENTS_KEY = "modelParents";
    public static final ResourceLocation DEFAULT_TEXTURE_NBT_KEY = new ResourceLocation(MOD_ID, "default");

    public static final CauldronInteraction NEW_DYED_ITEM = (pBlockState, pLevel, pPos, pPlayer, pHand, pStack) -> {
        InteractionResult result = DYED_ITEM.interact(pBlockState, pLevel, pPos, pPlayer, pHand, pStack);

        if (result.equals(InteractionResult.sidedSuccess(false))) {
            pLevel.playSound(
                    null, pPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F
            );
        }

        return result;
    };

    public static final CauldronInteraction OVERLAY_ITEM
            = (pBlockState, pLevel, pBlockPos, pPlayer, pHand, pStack) -> {
        if (!(pStack.getItem() instanceof ClothingItem clothing)) return InteractionResult.PASS;
        if (clothing.getOverlays(pStack).length == 0) return InteractionResult.PASS;
        if (pLevel.isClientSide) return InteractionResult.sidedSuccess(true);

        ResourceLocation[] originalOverlays = clothing.getOverlays(pStack);

        int newLength = originalOverlays.length - 1;
        ResourceLocation[] newOverlays = new ResourceLocation[newLength];

        if (newLength > 0)
            System.arraycopy(originalOverlays, 1, newOverlays, 0, newLength);

        clothing.setOverlays(pStack, newOverlays);
        pLevel.playSound(
                null, pBlockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F
        );
        LayeredCauldronBlock.lowerFillLevel(pBlockState, pLevel, pBlockPos);

        return InteractionResult.sidedSuccess(false);
    };

    // TODO: allow multiple layers when rendering meshes
    public ClothingItem(EquipmentSlot pSlot) {
        super(
                ArmorMaterials.LEATHER,
                pSlot,
                new Properties()
                        .tab(ClothingTabs.CLOTHING_TAB)
                        .stacksTo(1)
        );
    }

    /**
     * Obtains the recommended root NBT {@link CompoundTag} for clothing properties.
     * @param itemStack an {@link ItemStack} of this item.
     * @return the recommended root NBT {@link CompoundTag} for clothing properties.
     */
    @NotNull
    public CompoundTag getClothingPropertiesTag(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ClothingItem)) throw new IllegalArgumentException(
                "Item of passed stack '" + itemStack + "' is not a ClothingItem instance!"
        );
        return itemStack.getOrCreateTag().getCompound(CLOTHING_PROPERTY_NBT_KEY);
    }

    /**
     * Returns the default {@link ItemStack} for this. Since it's anticipated that rendering properties are stored in
     * the stack's {@link CompoundTag}, the NBT structures have been pre-prepared here.
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
        this.setMaxDamage(toReturn, 0);
        this.setEquipSound(toReturn, this.material.getEquipSound().getLocation());
        this.setClothingLore(toReturn, List.of());

        this.setModelStrata(toReturn, ModelStrata.forSlot(this.getSlot()));
        this.setTextureLocation(toReturn, DEFAULT_TEXTURE_NBT_KEY);
        this.setOverlays(toReturn, new ResourceLocation[]{});
        this.setPartsForVisibility(toReturn, this.defaultPartVisibility());

        this.setModelPartLocations(toReturn, Map.of());

        return toReturn;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return this.getClothingPropertiesTag(stack).getInt(MAX_DAMAGE_KEY);
    }

    public void setMaxDamage(ItemStack stack, int durability) {
        this.getClothingPropertiesTag(stack).putInt(MAX_DAMAGE_KEY, durability);
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
            final ClothingEntryLoader loader = ClothingEntryLoader.getInstance();
            pItems.addAll(loader.getStacks(this));
        } catch (Exception e) {
            LOGGER.error("Unable to generate clothing entries!", e);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(
                new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return ClothingItemRenderer.getInstance();
                    }
                }
        );
    }

    /**
     * Overridden Forge method; see super for more details. This method returns a <code>String</code> representing the
     * path to the texture that should be used for this piece of clothing. It's used internally by both Minecraft
     * and this mod to return the texture for a model. Returns the appropriate texture for the mesh of the clothing
     * item.
     * @param stack  ItemStack for the equipped armor
     * @param entity The entity wearing the clothing
     * @param slot   The slot the clothing is in
     * @param type   The {@link String} representation of an overlay name. If non-null, the return will point
     *               to the location for that overlay.
     * @return The <code>String</code> representing the path to the texture that should be used for this piece of
     *         clothing.
     */
    @Override
    @ParametersAreNullableByDefault
    public String getArmorTexture(@NotNull ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (type != null) {
            ResourceLocation overlayLocation = new ResourceLocation(type);

            return String.format(
                    Locale.ROOT,
                    "%s:textures/models/clothing/overlays/%s.png",
                    overlayLocation.getNamespace(),
                    overlayLocation.getPath()
            );
        }

        ResourceLocation textureLocation = this.getTextureLocation(stack);

        return String.format(
                Locale.ROOT,
                "%s:textures/models/clothing/%s.png",
                textureLocation.getNamespace(),
                textureLocation.getPath()
        );
    }

    public ResourceLocation getClothingName(ItemStack itemStack) {
        return new ResourceLocation(this.getClothingPropertiesTag(itemStack).getString(CLOTHING_NAME_KEY));
    }

    public void setClothingName(ItemStack itemStack, ResourceLocation name) {
        this.getClothingPropertiesTag(itemStack).putString(CLOTHING_NAME_KEY, name.toString());
    }

    /**
     * Used for verification purposes while loading {@link ItemStack}s into the creative menu, etc. This method should
     * always return the same {@link EquipmentSlot} as {@link #getSlot()}.
     * @param itemStack the {@link ItemStack} representing this
     */
    public EquipmentSlot getSlot(ItemStack itemStack) {
        return EquipmentSlot.byName(this.getClothingPropertiesTag(itemStack).getString(CLOTHING_SLOT_NBT_KEY));
    }

    /**
     * This should not be freely used. This method exists to allow
     * {@link io.github.kawaiicakes.clothing.common.resources.NbtStackInitializer}s to easily indicate the slot
     * this piece of clothing is worn on. This ensures that only instances of this whose {@link #getSlot()} returns the
     * slot indicated in the clothing entry gets added to the creative menu.
     * @param itemStack the {@link ItemStack} instance of this; regardless of whether the return of {@link #getSlot()}
     *                  matches what the clothing data entry says.
     * @param slot the {@link EquipmentSlot} which a clothing data entry indicates it is worn on.
     * @see io.github.kawaiicakes.clothing.common.resources.NbtStackInitializer#writeToStack(ClothingItem, ItemStack)
     * @see ClothingEntryLoader#entryContainsSlotDeclaration(JsonObject)
     */
    public void setSlot(ItemStack itemStack, EquipmentSlot slot) {
        this.getClothingPropertiesTag(itemStack).putString(CLOTHING_SLOT_NBT_KEY, slot.getName());
    }

    public List<Component> getClothingLore(ItemStack stack) {
        ListTag loreTag = this.getClothingPropertiesTag(stack).getList(CLOTHING_LORE_NBT_KEY, Tag.TAG_STRING);

        return deserializeLore(loreTag);
    }

    public void setClothingLore(ItemStack stack, List<Component> components) {
        ListTag loreList = new ListTag();

        for (Component component : components) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(component)));
        }

        this.getClothingPropertiesTag(stack).put(CLOTHING_LORE_NBT_KEY, loreList);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the {@link ModelStrata} indicating which layer the passed stack renders to.
     * @see HumanoidClothingLayer#modelForLayer(ModelStrata)
     */
    public ModelStrata getModelStrata(ItemStack itemStack) {
        String strataString = this.getClothingPropertiesTag(itemStack).getString(MODEL_LAYER_NBT_KEY);
        return ModelStrata.byName(strataString);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param modelStrata the {@link ModelStrata} indicating which layer the passed stack renders to.
     * @see HumanoidClothingLayer#modelForLayer(ModelStrata)
     */
    public void setModelStrata(ItemStack itemStack, ModelStrata modelStrata) {
        this.getClothingPropertiesTag(itemStack).putString(MODEL_LAYER_NBT_KEY, modelStrata.getSerializedName());
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the {@link String} pointing to the location of the texture folder.
     */
    public ResourceLocation getTextureLocation(ItemStack itemStack) {
        return new ResourceLocation(this.getClothingPropertiesTag(itemStack).getString(TEXTURE_LOCATION_NBT_KEY));
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param textureLocation the {@link String} pointing to the location of the texture folder.
     */
    public void setTextureLocation(ItemStack itemStack, ResourceLocation textureLocation) {
        this.getClothingPropertiesTag(itemStack).putString(TEXTURE_LOCATION_NBT_KEY, textureLocation.toString());
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the array of {@link String}s whose values point to the overlay textures.
     */
    public ResourceLocation[] getOverlays(ItemStack itemStack) {
        ListTag listTag = this.getClothingPropertiesTag(itemStack).getList(OVERLAY_NBT_KEY, Tag.TAG_STRING);
        ResourceLocation[] toReturn = new ResourceLocation[listTag.size()];
        for (int i = 0; i < listTag.size(); i++) {
            if (!(listTag.get(i) instanceof StringTag stringTag)) throw new RuntimeException();
            toReturn[i] = new ResourceLocation(stringTag.getAsString());
        }
        return toReturn;
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param overlays the array of {@link String}s whose values point to the overlay textures.
     */
    public void setOverlays(ItemStack itemStack, ResourceLocation[] overlays) {
        ListTag overlayTag = new ListTag();

        for (ResourceLocation overlay : overlays) {
            overlayTag.add(StringTag.valueOf(overlay.toString()));
        }

        this.getClothingPropertiesTag(itemStack).put(OVERLAY_NBT_KEY, overlayTag);
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return an array of {@link ModelPartReference} whose elements
     * correspond to what body parts the clothing will visibly render on.
     */
    public ModelPartReference[] getPartsForVisibility(ItemStack itemStack) {
        ListTag partList = this.getClothingPropertiesTag(itemStack).getList(PART_VISIBILITY_KEY, Tag.TAG_STRING);

        ModelPartReference[] toReturn = new ModelPartReference[partList.size()];
        for (int i = 0; i < partList.size(); i++) {
            toReturn[i] = ModelPartReference.byName(partList.getString(i));
        }

        return toReturn;
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param slots an array of {@link ModelPartReference} whose
     *              elements correspond to what body parts the clothing will visibly render on.
     */
    public void setPartsForVisibility(ItemStack itemStack, ModelPartReference[] slots) {
        ListTag partList = new ListTag();

        for (ModelPartReference part : slots) {
            partList.add(StringTag.valueOf(part.getSerializedName()));
        }

        this.getClothingPropertiesTag(itemStack).put(PART_VISIBILITY_KEY, partList);
    }

    /**
     * This method is used exclusively for setting the default {@link ModelPart} visibility on the meshes as
     * returned by {@link #getModelStrata(ItemStack)} and
     * {@link HumanoidClothingLayer#modelForLayer(ModelStrata)}.
     * @return the array of {@link ModelPartReference} this item will
     * appear to be worn on.
     * @see HumanoidArmorLayer#setPartVisibility(HumanoidModel, EquipmentSlot)
     */
    @NotNull
    public ModelPartReference[] defaultPartVisibility() {
        return switch (this.getSlot()) {
            case HEAD:
                yield new ModelPartReference[] {
                        ModelPartReference.HEAD,
                        ModelPartReference.HAT
                };
            case CHEST:
                yield new ModelPartReference[] {
                        ModelPartReference.BODY,
                        ModelPartReference.RIGHT_ARM,
                        ModelPartReference.LEFT_ARM
                };
            case LEGS:
                yield new ModelPartReference[] {
                        ModelPartReference.BODY,
                        ModelPartReference.RIGHT_LEG,
                        ModelPartReference.LEFT_LEG
                };
            case MAINHAND:
                yield new ModelPartReference[] {
                        ModelPartReference.RIGHT_ARM
                };
            case OFFHAND:
                yield new ModelPartReference[] {
                        ModelPartReference.LEFT_ARM
                };
            case FEET:
                yield new ModelPartReference[] {
                        ModelPartReference.RIGHT_LEG,
                        ModelPartReference.LEFT_LEG
                };
        };
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
        CompoundTag root = this.getClothingPropertiesTag(pStack);
        return root.contains(TAG_COLOR, 99) && root.getInt(TAG_COLOR) != 0xFFFFFF;
    }

    @Override
    public void clearColor(@NotNull ItemStack pStack) {
        this.setColor(pStack, 0xFFFFFF);
    }

    // TODO: adv tooltip: colours for each overlay, and the colour(s) for the base piece of clothing (in super)
    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(
            ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced
    ) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

        List<Component> lore = this.getClothingLore(pStack);

        if (!lore.isEmpty()) {
            pTooltipComponents.addAll(lore);
        }

        if (!pIsAdvanced.isAdvanced()) return;

        ResourceLocation[] overlayNames = this.getOverlays(pStack);
        if (overlayNames.length != 0) {
            pTooltipComponents.add(Component.empty());
            pTooltipComponents.add(
                    Component.translatable("item.modifiers.clothing.overlays")
                            .withStyle(ChatFormatting.GRAY)
            );
            for (ResourceLocation overlayName : overlayNames) {
                pTooltipComponents.add(
                        Component.literal(overlayName.toString())
                                .withStyle(ChatFormatting.BLUE)
                );
            }
        }

        pTooltipComponents.add(Component.empty());
        pTooltipComponents.add(
                Component.translatable("item.modifiers.clothing.name")
                        .withStyle(ChatFormatting.GRAY)
        );
        pTooltipComponents.add(
                Component.literal(this.getClothingName(pStack).toString())
                        .withStyle(ChatFormatting.BLUE)
        );

        pTooltipComponents.add(Component.empty());
        pTooltipComponents.add(
                Component.translatable("item.modifiers.clothing.color")
                        .withStyle(ChatFormatting.GRAY)
        );
        String colorAsString = "#" + Integer.toHexString(this.getColor(pStack)).toUpperCase();
        pTooltipComponents.add(
                Component.literal(colorAsString)
                        .withStyle(ChatFormatting.BLUE)
        );
    }

    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack pStack) {
        final String original = super.getDescriptionId(pStack);
        final ResourceLocation clothingLocation = this.getClothingName(pStack);
        final String suffix = clothingLocation.getNamespace() + "-" + clothingLocation.getPath();

        return clothingLocation.getPath().isEmpty() ? original : original + "." + suffix;
    }

    /**
     * This overwrites the attributes completely
     */
    public void setAttributeModifiers(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers) {
        CompoundTag clothingAttributesTag = new CompoundTag();

        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
            ListTag modifierEntries = new ListTag();

            for (AttributeModifier modifier : entry.getValue()) {
                modifierEntries.add(modifier.save());
            }

            ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());

            if (attributeLocation == null) {
                LOGGER.error("Unable to obtain ResourceLocation of Attribute {}!", entry.getKey());
                continue;
            }

            clothingAttributesTag.put(attributeLocation.toString(), modifierEntries);
        }

        this.getClothingPropertiesTag(stack).put(ATTRIBUTES_KEY, clothingAttributesTag);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (!this.getSlot().equals(slot)) return super.getAttributeModifiers(slot, stack);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        CompoundTag clothingAttributesTag = this.getClothingPropertiesTag(stack).getCompound(ATTRIBUTES_KEY);

        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attributeLocation == null) {
                LOGGER.error("Unable to obtain ResourceLocation of Attribute {}!", attribute);
                continue;
            }
            if (!clothingAttributesTag.contains(attributeLocation.toString())) continue;

            ListTag modifierList = clothingAttributesTag.getList(
                    attributeLocation.toString(),
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
        this.getClothingPropertiesTag(stack).putString(EQUIP_SOUND_KEY, location.toString());
    }

    public SoundEvent getEquipSound(ItemStack stack) {
        ResourceLocation equipSoundLocation = new ResourceLocation(
                this.getClothingPropertiesTag(stack).getString(EQUIP_SOUND_KEY)
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
    @Deprecated
    public SoundEvent getEquipSound() {
        return null;
    }


    /**
     * Indicates the {@link ModelPart}s to which the mapped {@link ResourceLocation} will be rendered using a
     * {@link ModelPartReference} as a key.
     * The {@link ItemStack} is included for the implementer's benefit. This method is used to reference model parts 
     * without explicit references to them in common classes.
     * @param itemStack the {@link ItemStack} instance of this.
     * @return          the {@link Map} of key {@link ModelPartReference}s for each {@link ResourceLocation} referencing
     *                  the body part the baked model will render to.
     */
    public @NotNull Map<ModelPartReference, ResourceLocation> getModelPartLocations(ItemStack itemStack) {
        CompoundTag modelPartTag = this.getClothingPropertiesTag(itemStack).getCompound(MODEL_PARENTS_KEY);

        Map<ModelPartReference, ResourceLocation> toReturn = new HashMap<>(modelPartTag.size());

        for (String part : modelPartTag.getAllKeys()) {
            if (!(modelPartTag.get(part) instanceof StringTag modelLocation)) throw new IllegalArgumentException();
            toReturn.put(ModelPartReference.byName(part), new ResourceLocation(modelLocation.toString()));
        }

        return toReturn;
    }

    public void setModelPartLocations(ItemStack itemStack, Map<ModelPartReference, ResourceLocation> modelParts) {
        CompoundTag modelPartMap = new CompoundTag();

        for (Map.Entry<ModelPartReference, ResourceLocation> entry : modelParts.entrySet()) {
            modelPartMap.putString(entry.getKey().getSerializedName(), entry.getValue().toString());
        }

        this.getClothingPropertiesTag(itemStack).put(MODEL_PARENTS_KEY, modelPartMap);
    }

    public ModelPartReference defaultModelPart() {
        return switch (this.getSlot()) {
            case MAINHAND -> ModelPartReference.RIGHT_ARM;
            case OFFHAND -> ModelPartReference.LEFT_ARM;
            case FEET, LEGS -> ModelPartReference.RIGHT_LEG;
            case CHEST -> ModelPartReference.BODY;
            case HEAD -> ModelPartReference.HEAD;
        };
    }

    /**
     * Used to point to the location of the {@link BakedModel}s for render. A baked model is not directly declared
     * as the return type, as this would cause a {@link ClassNotFoundException} serverside.
     * <br><br>
     * @param itemStack the {@link ItemStack} instance of this
     * @param modelPartReference the {@link ModelPartReference} upon
     *                           which a model is parented to.
     * @return the location of the {@link BakedModel} for render.
     */
    @Nullable
    public ResourceLocation getModelPartLocation(ItemStack itemStack, ModelPartReference modelPartReference) {
        String location = this.getClothingPropertiesTag(itemStack)
                .getCompound(MODEL_PARENTS_KEY)
                .getString(modelPartReference.getSerializedName());
        return location.isEmpty() ? null : new ResourceLocation(location);
    }

    public void setModelPartLocation(
            ItemStack itemStack, ModelPartReference modelPartReference, ResourceLocation modelLocation
    ) {
        Map<ModelPartReference, ResourceLocation> existing = this.getModelPartLocations(itemStack);
        existing.put(modelPartReference, modelLocation);
        this.setModelPartLocations(itemStack, existing);
    }

    public static List<Component> deserializeLore(JsonArray loreJson) {
        List<Component> toReturn = new ArrayList<>(loreJson.size());

        try {
            for (JsonElement element : loreJson) {
                toReturn.add(Component.Serializer.fromJson(element.getAsJsonPrimitive().getAsString()));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to parse clothing lore!", e);
            toReturn = List.of();
        }

        return toReturn;
    }

    public static List<Component> deserializeLore(ListTag loreTag) {
        List<Component> toReturn = new ArrayList<>(loreTag.size());

        try {
            for (Tag componentTag : loreTag) {
                toReturn.add(Component.Serializer.fromJson(componentTag.getAsString()));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to parse clothing lore!", e);
            toReturn = List.of();
        }

        return toReturn;
    }

    /**
     * Mirror of {@link DyeableLeatherItem#dyeArmor(ItemStack, List)} but actually mutates the passed {@code stack}
     */
    public static void dyeClothing(ItemStack stack, List<DyeItem> dyeItems) {
        int[] colors = new int[3];
        int i = 0;
        int j = 0;

        Item item = stack.getItem();
        if (!(item instanceof ClothingItem clothingItem)) return;

        if (clothingItem.hasCustomColor(stack)) {
            int k = clothingItem.getColor(stack);
            float f = (float)(k >> 16 & 255) / 255.0F;
            float f1 = (float)(k >> 8 & 255) / 255.0F;
            float f2 = (float)(k & 255) / 255.0F;
            i += (int)(Math.max(f, Math.max(f1, f2)) * 255.0F);
            colors[0] += (int)(f * 255.0F);
            colors[1] += (int)(f1 * 255.0F);
            colors[2] += (int)(f2 * 255.0F);
            ++j;
        }

        for (DyeItem dyeitem : dyeItems) {
            float[] afloat = dyeitem.getDyeColor().getTextureDiffuseColors();
            int i2 = (int)(afloat[0] * 255.0F);
            int l = (int)(afloat[1] * 255.0F);
            int i1 = (int)(afloat[2] * 255.0F);
            i += Math.max(i2, Math.max(l, i1));
            colors[0] += i2;
            colors[1] += l;
            colors[2] += i1;
            ++j;
        }

        int j1 = colors[0] / j;
        int k1 = colors[1] / j;
        int l1 = colors[2] / j;
        float f3 = (float)i / (float)j;
        float f4 = (float)Math.max(j1, Math.max(k1, l1));
        j1 = (int)((float)j1 * f3 / f4);
        k1 = (int)((float)k1 * f3 / f4);
        l1 = (int)((float)l1 * f3 / f4);
        int j2 = (j1 << 8) + k1;
        j2 = (j2 << 8) + l1;

        clothingItem.setColor(stack, j2);
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

    public enum ModelStrata implements StringRepresentable {
        BASE("base"),
        INNER("inner"),
        OUTER("outer"),
        OVER("over"),
        OVER_LEG_ARMOR("over_leg_armor"),
        OVER_ARMOR("over_armor");

        private final String nbtTagID;

        ModelStrata(String nbtTagID) {
            this.nbtTagID = nbtTagID;
        }

        public static ModelStrata byName(String pTargetName) {
            for(ModelStrata modelStrata : values()) {
                if (modelStrata.getSerializedName().equals(pTargetName)) {
                    return modelStrata;
                }
            }

            throw new IllegalArgumentException("Invalid model name '" + pTargetName + "'");
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.nbtTagID;
        }

        public static ModelStrata forSlot(EquipmentSlot equipmentSlot) {
            return switch (equipmentSlot) {
                case FEET -> INNER;
                case LEGS -> BASE;
                case HEAD -> OVER;
                default -> OUTER;
            };
        }
    }
}