package io.github.kawaiicakes.clothing.common.item;

import com.google.common.collect.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.ClothingItemRenderer;
import io.github.kawaiicakes.clothing.client.HumanoidClothingLayer;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import io.github.kawaiicakes.clothing.common.data.ClothingVisibility;
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
    public static final String DEFAULT_COLOR_KEY = "default_color";
    public static final String CLOTHING_LORE_NBT_KEY = "lore";
    public static final String ATTRIBUTES_KEY = "attributes";
    public static final String EQUIP_SOUND_KEY = "equip_sound";
    public static final String MAX_DAMAGE_KEY = "durability";
    public static final String MESHES_NBT_KEY = "meshes";
    public static final String OVERLAY_NBT_KEY = "overlays";
    public static final String MODELS_NBT_KEY = "models";
    public static final ResourceLocation DEFAULT_TEXTURE_LOCATION = new ResourceLocation(MOD_ID, "default");
    public static final ResourceLocation ERROR_MODEL_LOCATION = new ResourceLocation(MOD_ID, "error");
    public static final ImmutableMap<ModelPartReference, ResourceLocation> ERROR_MODEL
            = ImmutableMap.of(ModelPartReference.BODY, ERROR_MODEL_LOCATION);
    public static final int FALLBACK_COLOR = 16383998;

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
        if (clothing.getOverlays(pStack).isEmpty()) return InteractionResult.PASS;
        if (pLevel.isClientSide) return InteractionResult.sidedSuccess(true);

        clothing.setOverlays(pStack, defaultOverlays());
        pLevel.playSound(
                null, pBlockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F
        );
        LayeredCauldronBlock.lowerFillLevel(pBlockState, pLevel, pBlockPos);

        return InteractionResult.sidedSuccess(false);
    };

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

        CompoundTag tag = itemStack.getOrCreateTag();

        if (!tag.contains(CLOTHING_PROPERTY_NBT_KEY, Tag.TAG_COMPOUND))
            tag.put(CLOTHING_PROPERTY_NBT_KEY, new CompoundTag());

        return tag.getCompound(CLOTHING_PROPERTY_NBT_KEY);
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
        this.setColor(toReturn, FALLBACK_COLOR);
        this.setDefaultColor(toReturn, FALLBACK_COLOR);
        this.setClothingLore(toReturn, List.of());
        this.setAttributeModifiers(
                toReturn,
                this.getDefaultAttributeModifiers(this.getSlot())
        );
        this.setMaxDamage(toReturn, this.material.getDurabilityForSlot(this.getSlot()));
        this.setEquipSound(toReturn, this.material.getEquipSound().getLocation());

        this.setMeshes(toReturn, defaultMeshes(this.getSlot()));
        this.setModels(toReturn, defaultModels());
        this.setOverlays(toReturn, defaultOverlays());

        return toReturn;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(stack);

            if (!properties.contains(MAX_DAMAGE_KEY, Tag.TAG_INT))
                properties.putInt(MAX_DAMAGE_KEY, this.material.getDurabilityForSlot(this.getSlot()));

            return this.getClothingPropertiesTag(stack).getInt(MAX_DAMAGE_KEY);
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing durability for ItemStack '{}'!", stack, e);
            // lol
            return 420;
        }
    }

    public void setMaxDamage(ItemStack stack, int durability) {
        try {
            this.getClothingPropertiesTag(stack).putInt(MAX_DAMAGE_KEY, durability);
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing durability for ItemStack '{}'!", stack, e);
        }
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
     * Don't use this lol. Overridden in case somehow this method is queried by Forge or Minecraft.
     * I'm not going to make it throw an {@link UnsupportedOperationException} just yet since I don't know
     * where, or even if, it would be called by something else somehow.
     */
    @Override
    @ParametersAreNullableByDefault
    public String getArmorTexture(@NotNull ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return DEFAULT_TEXTURE_LOCATION.toString();
    }

    public ResourceLocation getClothingName(ItemStack itemStack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(itemStack);

            if (!properties.contains(CLOTHING_NAME_KEY, Tag.TAG_STRING))
                properties.putString(CLOTHING_NAME_KEY, DEFAULT_TEXTURE_LOCATION.toString());

            return new ResourceLocation(properties.getString(CLOTHING_NAME_KEY));
        } catch (Exception e) {
            LOGGER.error("Unable to return clothing name for ItemStack '{}'!", itemStack);
            return DEFAULT_TEXTURE_LOCATION;
        }
    }

    public void setClothingName(ItemStack itemStack, ResourceLocation name) {
        try {
            this.getClothingPropertiesTag(itemStack).putString(CLOTHING_NAME_KEY, name.toString());
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing name for ItemStack '{}'!", itemStack, e);
        }
    }

    /**
     * Used for matching specific instances of this to {@link ItemStack}s while loading them into the creative menu,
     * etc. For a given instance, this method should always return the same {@link EquipmentSlot} as {@link #getSlot()}.
     * <br><br>
     * An exception is not thrown if the return does not match this class' {@link #getSlot()} since the caller relies
     * on returning mismatching slots to discriminate between ItemStacks.
     * @param itemStack the {@link ItemStack} representing this.
     */
    public EquipmentSlot getSlot(ItemStack itemStack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(itemStack);

            if (!properties.contains(CLOTHING_SLOT_NBT_KEY, Tag.TAG_STRING))
                properties.putString(CLOTHING_SLOT_NBT_KEY, EquipmentSlot.CHEST.getName());

            return EquipmentSlot.byName(properties.getString(CLOTHING_SLOT_NBT_KEY));
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing slot for ItemStack '{}'!", itemStack, e);
            return EquipmentSlot.CHEST;
        }
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
        try {
            this.getClothingPropertiesTag(itemStack).putString(CLOTHING_SLOT_NBT_KEY, slot.getName());
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing slot for ItemStack '{}'!", itemStack, e);
        }
    }

    public List<Component> getClothingLore(ItemStack stack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(stack);

            if (!properties.contains(CLOTHING_LORE_NBT_KEY, Tag.TAG_LIST))
                properties.put(CLOTHING_LORE_NBT_KEY, new ListTag());

            ListTag loreTag = properties.getList(CLOTHING_LORE_NBT_KEY, Tag.TAG_STRING);

            return deserializeLore(loreTag);
        } catch (Exception e) {
            LOGGER.error("Unable to return clothing lore from ItemStack '{}'!", stack, e);
            return List.of();
        }
    }

    public void setClothingLore(ItemStack stack, List<Component> components) {
        try {
            ListTag loreList = new ListTag();

            for (Component component : components) {
                loreList.add(StringTag.valueOf(Component.Serializer.toJson(component)));
            }

            this.getClothingPropertiesTag(stack).put(CLOTHING_LORE_NBT_KEY, loreList);
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing lore for ItemStack '{}'!", stack, e);
        }
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the {@link MeshStratum} indicating which layer the passed stack renders to.
     * @see HumanoidClothingLayer#modelForLayer(MeshStratum)
     */
    public Map<MeshStratum, ClothingLayer> getMeshes(ItemStack itemStack) {
        try {
            Map<MeshStratum, ClothingLayer> toReturn = new HashMap<>();

            CompoundTag properties = this.getClothingPropertiesTag(itemStack);

            if (!properties.contains(MESHES_NBT_KEY, Tag.TAG_COMPOUND))
                properties.put(MESHES_NBT_KEY, new CompoundTag());

            CompoundTag strataTag = properties.getCompound(MESHES_NBT_KEY);

            for (String meshStratum : strataTag.getAllKeys()) {
                toReturn.put(MeshStratum.byName(meshStratum), ClothingLayer.fromNbt(strataTag.getCompound(meshStratum)));
            }

            return toReturn;
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing meshes for ItemStack '{}'!", itemStack, e);
            return defaultMeshes(this.getSlot());
        }
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @see HumanoidClothingLayer#modelForLayer(MeshStratum)
     */
    public void setMeshes(ItemStack itemStack, Map<MeshStratum, ClothingLayer> meshStrata) {
        try {
            CompoundTag serializedStrata = new CompoundTag();

            for (Map.Entry<MeshStratum, ClothingLayer> entry : meshStrata.entrySet()) {
                serializedStrata.put(entry.getKey().getSerializedName(), entry.getValue().toNbt());
            }

            this.getClothingPropertiesTag(itemStack).put(MESHES_NBT_KEY, serializedStrata);
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing meshes for ItemStack '{}'!", itemStack, e);
        }
    }

    public void setMesh(ItemStack stack, MeshStratum stratum, ClothingLayer mesh) {
        try {
            Map<MeshStratum, ClothingLayer> existing = this.getMeshes(stack);
            existing.put(stratum, mesh);
            this.setMeshes(stack, existing);
        } catch (Exception e) {
            LOGGER.error("Unable to add clothing mesh for ItemStack '{}'!", stack, e);
        }
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @return the array of {@link String}s whose values point to the overlay textures.
     */
    public ImmutableListMultimap<MeshStratum, ClothingLayer> getOverlays(ItemStack itemStack) {
        ImmutableListMultimap.Builder<MeshStratum, ClothingLayer> toReturn = ImmutableListMultimap.builder();

        try {
            CompoundTag properties = this.getClothingPropertiesTag(itemStack);

            if (!properties.contains(OVERLAY_NBT_KEY, Tag.TAG_COMPOUND))
                properties.put(OVERLAY_NBT_KEY, new CompoundTag());

            CompoundTag strataTag = properties.getCompound(OVERLAY_NBT_KEY);

            for (String meshStratum : strataTag.getAllKeys()) {
                ListTag overlaysForStratum = strataTag.getList(meshStratum, Tag.TAG_COMPOUND);
                for (Tag overlayTag : overlaysForStratum) {
                    toReturn.put(MeshStratum.byName(meshStratum), ClothingLayer.fromNbt((CompoundTag) overlayTag));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to obtain clothing overlays from ItemStack '{}'!", itemStack, e);
            toReturn = ImmutableListMultimap.builder();
        }

        return toReturn.build();
    }

    /**
     * @param itemStack the {@code itemStack} representing this.
     * @param overlays the array of {@link String}s whose values point to the overlay textures.
     */
    public void setOverlays(ItemStack itemStack, Multimap<MeshStratum, ClothingLayer> overlays) {
        CompoundTag serializedStrata = new CompoundTag();

        try {
            for (Map.Entry<MeshStratum, Collection<ClothingLayer>> entry : overlays.asMap().entrySet()) {
                ListTag overlaysForStratum = new ListTag();

                for (ClothingLayer layer : entry.getValue()) {
                    overlaysForStratum.add(layer.toNbt());
                }

                serializedStrata.put(entry.getKey().getSerializedName(), overlaysForStratum);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing overlays for ItemStack '{}'!", itemStack, e);
            return;
        }

        this.getClothingPropertiesTag(itemStack).put(OVERLAY_NBT_KEY, serializedStrata);
    }

    /**
     * If the passed overlay already exists in the passed stratum, it's moved to index 0. Exact equality between
     * passed overlay and the existing one is not checked; rather, only the names.
     */
    public void addOverlay(ItemStack stack, MeshStratum stratum, ClothingLayer overlay) {
        try {
            // #put and #add operations do not work on Multimaps or the views contained therein
            ImmutableListMultimap<MeshStratum, ClothingLayer> existing = this.getOverlays(stack);

            ImmutableMultimap.Builder<MeshStratum, ClothingLayer> edited = ImmutableListMultimap.builder();

            if (existing.isEmpty()) {
                this.setOverlays(stack, ImmutableListMultimap.of(stratum, overlay));
                return;
            }

            for (Map.Entry<MeshStratum, Collection<ClothingLayer>> existingEntries : existing.asMap().entrySet()) {
                if (existingEntries.getKey().equals(stratum)) {
                    edited.put(stratum, overlay);

                    for (ClothingLayer existingLayer : existingEntries.getValue()) {
                        if (existingLayer.textureLocation().equals(overlay.textureLocation())) continue;
                        edited.put(stratum, existingLayer);
                    }

                    continue;
                }

                edited.putAll(stratum, existingEntries.getValue());
            }

            this.setOverlays(stack, edited.build());
        } catch (Exception e) {
            LOGGER.error("Unable to add clothing overlay to ItemStack '{}'!", stack, e);
        }
    }

    /**
     * This method is used exclusively for setting the default {@link ModelPart} visibility on the meshes as
     * returned by {@link #getMeshes(ItemStack)} and
     * {@link HumanoidClothingLayer#modelForLayer(MeshStratum)}.
     * @return the array of {@link ModelPartReference} this item will
     * appear to be worn on.
     * @see HumanoidArmorLayer#setPartVisibility(HumanoidModel, EquipmentSlot)
     */
    @NotNull
    public static ModelPartReference[] defaultPartVisibility(EquipmentSlot slot) {
        return switch (slot) {
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

    public static ClothingLayer defaultMeshLayerForSlot(EquipmentSlot slot) {
        return new ClothingLayer(
                DEFAULT_TEXTURE_LOCATION, FALLBACK_COLOR, new ClothingVisibility(defaultPartVisibility(slot))
        );
    }

    @Override
    public int getColor(@NotNull ItemStack pStack) {
        try {
            return this.getColor(pStack, this.getOutermostMesh(pStack));
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing color of outermost mesh from ItemStack '{}'!", pStack, e);
            return FALLBACK_COLOR;
        }
    }

    public int getColor(ItemStack pStack, @Nullable MeshStratum stratum) {
        try {
            if (stratum == null || stratum.equals(this.getOutermostMesh(pStack)))
                return (int) this.getClothingPropertiesTag(pStack).getLong(TAG_COLOR);

            Map<MeshStratum, ClothingLayer> meshes = this.getMeshes(pStack);

            if (meshes == null || meshes.isEmpty())
                return (int) this.getClothingPropertiesTag(pStack).getLong(TAG_COLOR);

            ClothingLayer targeted = meshes.get(stratum);

            if (targeted == null) return (int) this.getClothingPropertiesTag(pStack).getLong(TAG_COLOR);

            return targeted.color();
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing color from ItemStack '{}'!", pStack, e);
            return FALLBACK_COLOR;
        }
    }

    @Nullable
    public MeshStratum getOutermostMesh(ItemStack stack) {
        try {
            Map<MeshStratum, ClothingLayer> meshes = this.getMeshes(stack);
            if (meshes.isEmpty()) return null;
            if (meshes.size() == 1) return meshes.keySet().stream().findFirst().get();

            return meshes.keySet().stream().max(Comparator.comparing(MeshStratum::ordinal)).get();
        } catch (Exception e) {
            LOGGER.error("Unable to get outermost clothing mesh from ItemStack '{}'!", stack, e);
            return MeshStratum.forSlot(this.getSlot());
        }
    }

    @Nullable
    public MeshStratum getInnermostMesh(ItemStack stack) {
        try {
            Map<MeshStratum, ClothingLayer> meshes = this.getMeshes(stack);
            if (meshes.isEmpty()) return null;
            if (meshes.size() == 1) return meshes.keySet().stream().findFirst().get();

            return meshes.keySet().stream().min(Comparator.comparing(MeshStratum::ordinal)).get();
        } catch (Exception e) {
            LOGGER.error("Unable to get outermost clothing mesh from ItemStack '{}'!", stack, e);
            return MeshStratum.forSlot(this.getSlot());
        }
    }

    @Override
    public void setColor(@NotNull ItemStack pStack, int pColor) {
        try {
            this.setColor(pStack, this.getOutermostMesh(pStack), pColor);
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing color for outermost mesh on ItemStack '{}'!", pStack, e);
        }
    }

    public void setColor(@NotNull ItemStack pStack, @Nullable MeshStratum stratum, int pColor) {
        try {
            Map<MeshStratum, ClothingLayer> meshes = this.getMeshes(pStack);

            this.getClothingPropertiesTag(pStack).putLong(TAG_COLOR, pColor);

            if (meshes == null || meshes.isEmpty() || stratum == null) return;

            ClothingLayer targetMesh = meshes.get(stratum);
            ClothingLayer dyedMesh = new ClothingLayer(
                    targetMesh.textureLocation(), pColor, targetMesh.clothingVisibility()
            );

            ImmutableMap.Builder<MeshStratum, ClothingLayer> builder = ImmutableMap.builder();
            builder.putAll(meshes);
            builder.put(stratum, dyedMesh);
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing color on ItemStack '{}'!", pStack, e);
        }
    }

    @Override
    public boolean hasCustomColor(@NotNull ItemStack pStack) {
        try {
            return this.hasCustomColor(pStack, this.getOutermostMesh(pStack));
        } catch (Exception e) {
            LOGGER.error(
                    "Unable to determine if a custom clothing color is present on ItemStack '{}' on outermost stratum!",
                    pStack,
                    e
            );
            return false;
        }
    }

    public boolean hasCustomColor(ItemStack stack, MeshStratum stratum) {
        try {
            return this.getColor(stack, stratum) != this.getDefaultColor(stack);
        } catch (Exception e) {
            LOGGER.error(
                    "Unable to determine if a custom clothing color is present on ItemStack '{}' for strata '{}'!",
                    stack,
                    stratum,
                    e
            );
            return false;
        }
    }

    /**
     * Clearing colour will clear all meshes starting from the outermost one
     */
    @Override
    public void clearColor(@NotNull ItemStack pStack) {
        try {
            Map<MeshStratum, ClothingLayer> meshes = this.getMeshes(pStack);

            for (int i = MeshStratum.values().length - 1; i >= 0; i--) {
                MeshStratum iterated = MeshStratum.values()[i];

                if (!meshes.containsKey(iterated)) continue;

                this.setColor(pStack, iterated, this.getDefaultColor(pStack));

                break;
            }
        } catch (Exception e) {
            LOGGER.error("Unable to clear clothing colors present on ItemStack '{}'!", pStack, e);
        }
    }

    public void setDefaultColor(@NotNull ItemStack pStack, int pColor) {
        try {
            this.getClothingPropertiesTag(pStack).putLong(DEFAULT_COLOR_KEY, pColor);
        } catch (Exception e) {
            LOGGER.error("Unable to set default clothing color for ItemStack '{}'!", pStack, e);
        }
    }

    public int getDefaultColor(ItemStack stack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(stack);

            if (!properties.contains(DEFAULT_COLOR_KEY, Tag.TAG_LONG))
                properties.putLong(DEFAULT_COLOR_KEY, FALLBACK_COLOR);

            return (int) properties.getLong(DEFAULT_COLOR_KEY);
        } catch (Exception e) {
            LOGGER.error("Unable to get default clothing color for ItemStack '{}'!", stack, e);
            return FALLBACK_COLOR;
        }
    }

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

        ImmutableListMultimap<MeshStratum, ClothingLayer> overlayNames = this.getOverlays(pStack);
        if (!overlayNames.isEmpty()) {
            pTooltipComponents.add(Component.empty());
            pTooltipComponents.add(
                    Component.translatable("item.modifiers.clothing.overlays")
                            .withStyle(ChatFormatting.GRAY)
            );

            boolean firstIndex = true;
            for (MeshStratum mesh : MeshStratum.values()) {
                if (!overlayNames.containsKey(mesh)) continue;
                List<ClothingLayer> overlays = overlayNames.get(mesh);

                if (!firstIndex) pTooltipComponents.add(Component.empty());
                else firstIndex = false;

                for (ClothingLayer overlay : overlays) {
                    pTooltipComponents.add(
                            Component.literal(
                                    overlay.textureLocation()
                                            + " - #"
                                            + Integer.toHexString(overlay.color()).toUpperCase()
                            ).withStyle(ChatFormatting.BLUE)
                    );
                }
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

        Map<MeshStratum, ClothingLayer> textures = this.getMeshes(pStack);
        for (MeshStratum stratum : textures.keySet()) {
            String meshName = stratum.getSerializedName();
            String fullString = meshName + " - #" + Integer.toHexString(this.getColor(pStack, stratum)).toUpperCase();
            pTooltipComponents.add(
                    Component.literal(fullString)
                            .withStyle(ChatFormatting.BLUE)
            );
        }
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
        try {
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
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing attributes for ItemStack '{}'!", stack, e);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        try {
            if (!this.getSlot().equals(slot)) return super.getAttributeModifiers(slot, stack);

            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            CompoundTag properties = this.getClothingPropertiesTag(stack);

            if (!properties.contains(ATTRIBUTES_KEY, Tag.TAG_COMPOUND))
                properties.put(ATTRIBUTES_KEY, new CompoundTag());

            CompoundTag clothingAttributesTag = properties.getCompound(ATTRIBUTES_KEY);

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
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing attributes from ItemStack '{}'!", stack, e);
            return ImmutableMultimap.of();
        }
    }

    public void setEquipSound(ItemStack stack, ResourceLocation location) {
        try {
            this.getClothingPropertiesTag(stack).putString(EQUIP_SOUND_KEY, location.toString());
        } catch (Exception e) {
            LOGGER.error("Unable to set clothing equip sound for ItemStack '{}'!", stack, e);
        }
    }

    public SoundEvent getEquipSound(ItemStack stack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(stack);

            if (!properties.contains(EQUIP_SOUND_KEY, Tag.TAG_STRING))
                properties.put(
                        EQUIP_SOUND_KEY, StringTag.valueOf(SoundEvents.ARMOR_EQUIP_LEATHER.getLocation().toString())
                );

            ResourceLocation equipSoundLocation = new ResourceLocation(properties.getString(EQUIP_SOUND_KEY));

            return ForgeRegistries.SOUND_EVENTS.getValue(equipSoundLocation);
        } catch (Exception e) {
            LOGGER.error("Unable to get clothing equip sound for ItemStack '{}'!", stack, e);
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }
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
    public @NotNull Map<ModelPartReference, ResourceLocation> getModels(ItemStack itemStack) {
        try {
            CompoundTag properties = this.getClothingPropertiesTag(itemStack);

            if (!properties.contains(MODELS_NBT_KEY, Tag.TAG_COMPOUND))
                properties.put(MODELS_NBT_KEY, new CompoundTag());

            CompoundTag modelPartTag = properties.getCompound(MODELS_NBT_KEY);
            ImmutableMap.Builder<ModelPartReference, ResourceLocation> toReturn = ImmutableMap.builder();

            for (String part : modelPartTag.getAllKeys()) {
                if (!(modelPartTag.get(part) instanceof StringTag stringTag)) throw new IllegalArgumentException();
                toReturn.put(ModelPartReference.byName(part), new ResourceLocation(stringTag.getAsString()));
            }

            return toReturn.buildOrThrow();
        } catch (Exception e) {
            LOGGER.error("Unable to deserialize Models from NBT in ItemStack '{}'!", itemStack, e);
            return defaultModels();
        }
    }

    public void setModels(ItemStack itemStack, Map<ModelPartReference, ResourceLocation> modelParts) {
        try {
            CompoundTag modelPartMap = new CompoundTag();

            for (Map.Entry<ModelPartReference, ResourceLocation> entry : modelParts.entrySet()) {
                modelPartMap.putString(entry.getKey().getSerializedName(), entry.getValue().toString());
            }

            this.getClothingPropertiesTag(itemStack).put(MODELS_NBT_KEY, modelPartMap);
        } catch (Exception e) {
            LOGGER.error("Unable to set Models for ItemStack '{}'!", itemStack, e);
        }
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
    public ResourceLocation getModel(ItemStack itemStack, ModelPartReference modelPartReference) {
        try {
            StringTag stringTag = (StringTag) this.getClothingPropertiesTag(itemStack)
                    .getCompound(MODELS_NBT_KEY)
                    .get(modelPartReference.getSerializedName());

            assert stringTag != null;
            return new ResourceLocation(stringTag.getAsString());
        } catch (Exception e) {
            LOGGER.error("Unable to get Model from ItemStack '{}'!", itemStack, e);
            return ERROR_MODEL_LOCATION;
        }
    }

    public void setModel(
            ItemStack itemStack, ModelPartReference modelPartReference, ResourceLocation layerInfo
    ) {
        try {
            Map<ModelPartReference, ResourceLocation> existing = this.getModels(itemStack);
            existing.put(modelPartReference, layerInfo);
            this.setModels(itemStack, existing);
        } catch (Exception e) {
            LOGGER.error("Unable to set Model for ItemStack '{}'!", itemStack, e);
        }
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
     * Mirror of {@link DyeableLeatherItem#dyeArmor(ItemStack, List)} but actually mutates the passed {@code stack}.
     * Also accepts the stratum to target
     */
    public static void dyeClothing(ItemStack stack, @Nullable MeshStratum mesh, List<DyeItem> dyeItems) {
        int[] colors = new int[3];
        int i = 0;
        int j = 0;

        Item item = stack.getItem();
        if (!(item instanceof ClothingItem clothingItem)) return;

        if (clothingItem.hasCustomColor(stack, mesh)) {
            int k = clothingItem.getColor(stack, mesh);
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

        clothingItem.setColor(stack, mesh, j2);
    }

    public static Map<ClothingItem.MeshStratum, ClothingLayer> defaultMeshes(EquipmentSlot slot) {
        return ImmutableMap.of(MeshStratum.forSlot(slot), defaultMeshLayerForSlot(slot));
    }

    public static Map<ClothingItem.ModelPartReference, ResourceLocation> defaultModels() {
        return ERROR_MODEL;
    }

    public static Multimap<ClothingItem.MeshStratum, ClothingLayer> defaultOverlays() {
        return ImmutableListMultimap.of();
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

    public enum MeshStratum implements StringRepresentable {
        BASE("base"),
        INNER("inner"),
        OUTER("outer"),
        OVER("over"),
        OVER_LEG_ARMOR("over_leg_armor"),
        OVER_ARMOR("over_armor");

        private final String nbtTagID;

        MeshStratum(String nbtTagID) {
            this.nbtTagID = nbtTagID;
        }

        public static MeshStratum byName(String pTargetName) {
            for(MeshStratum meshStratum : values()) {
                if (meshStratum.getSerializedName().equals(pTargetName)) {
                    return meshStratum;
                }
            }

            throw new IllegalArgumentException("Invalid model name '" + pTargetName + "'");
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.nbtTagID;
        }

        public static MeshStratum forSlot(EquipmentSlot equipmentSlot) {
            return switch (equipmentSlot) {
                case FEET -> INNER;
                case LEGS -> BASE;
                case HEAD -> OVER;
                default -> OUTER;
            };
        }
    }
}