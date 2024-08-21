package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.data.ClothingProperties;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class is a {@link SimpleJsonResourceReloadListener} that pretty heavily abstracts stuff related to Minecraft
 * datapack loading. Its purpose is to load data entries for the clothing item {@link T} to appropriate
 * {@link ItemStack}s in the creative menu. This data is held on the server solely to be sent to connecting clients,
 * where the data is finally used.
 * <br><br>
 * Implementations of this class should use a singleton pattern and return the instance in
 * {@link ClothingItem#loaderForType()}.
 * @param <T> The {@link ClothingItem} that data entries are being read for.
 */
public abstract class ClothingEntryLoader<T extends ClothingItem<?>> extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, ClothingEntryLoader<?>> LOADERS = new HashMap<>();

    protected ImmutableMap<ResourceLocation, NbtStackInitializer<T>> stackEntries = ImmutableMap.of();
    protected ImmutableMap<ResourceLocation, ItemStack> stacks = ImmutableMap.of();
    protected final String subDirectory;

    /**
     * @param pSubDirectory the {@code String} for the folder name the data entries will be found in.
     */
    protected ClothingEntryLoader(String pSubDirectory) {
        super(GSON, "clothing/" + pSubDirectory);
        this.subDirectory = pSubDirectory;
        LOADERS.put(this.getName(), this);
    }

    public static @Nullable ClothingEntryLoader<?> getLoader(String pSubDirectory) {
        return LOADERS.get(pSubDirectory);
    }

    /**
     * Implementations return an instance of the functional interface {@link NbtStackInitializer}, which ultimately
     * ends up writing the appropriate {@link ItemStack}s to the creative menu.
     * @param entryId the {@link ResourceLocation} representing the file name of this entry.
     * @param topElement the {@link JsonObject} holding the serialized JSON data of the entry.
     * @return the {@link NbtStackInitializer} specifically written to load {@link ItemStack}s for the item of
     * {@link T}.
     */
    @NotNull
    public abstract NbtStackInitializer<T> deserializeFromJson(ResourceLocation entryId, JsonObject topElement);

    /**
     * Indicates what items are registered for {@link T} of the implementation. Used to verify slots when adding items
     * in {@link ClothingItem#fillItemCategory(CreativeModeTab, NonNullList)}.
     * @return an array of {@link T} containing all the registered instances of {@link T}.
     * @see #getStacks(ClothingItem)
     */
    public abstract T[] clothingItemsForLoader();

    @NotNull
    public NbtStackInitializer<T> defaultDeserialization(ResourceLocation entryId, JsonObject topElement) {
        return (clothingItem, clothingStack) -> {
            EquipmentSlot slot;
            int color;
            Multimap<Attribute, AttributeModifier> modifiers;
            int durability;
            SoundEvent equipSoundLocation;
            List<Component> lore;

            try {
                slot = ClothingProperties.SLOT.readPropertyFromJson(topElement);
                color = ClothingProperties.COLOR.readPropertyFromJson(topElement);
                lore = ClothingProperties.LORE.readPropertyFromJson(topElement);
                modifiers = ClothingProperties.ATTRIBUTES.readPropertyFromJson(topElement);
                durability = ClothingProperties.MAX_DAMAGE.readPropertyFromJson(topElement);
                equipSoundLocation = ClothingProperties.EQUIP_SOUND.readPropertyFromJson(topElement);
            } catch (Exception e) {
                LOGGER.error("Error deserializing clothing entry!", e);
                throw e;
            }

            clothingItem.setClothingName(clothingStack, entryId);
            clothingItem.setSlot(clothingStack, slot);
            clothingItem.setColor(clothingStack, color);
            clothingItem.setClothingLore(clothingStack, lore);
            clothingItem.setAttributeModifiers(clothingStack, modifiers);
            clothingItem.setMaxDamage(clothingStack, durability);
            clothingItem.setEquipSound(clothingStack, equipSoundLocation);
        };
    }

    /**
     * Used primarily for data transfer from server to client. Generates the {@link ItemStack}s specified in the
     * datapack loaded by this by iterating in {@link #stackEntries}. When called, it fills {@link #stacks} with
     * the mapped clothing entries.
     * @return an {@link ImmutableMap} containing the mapped {@link ItemStack}s generated from this loader.
     */
    public ImmutableMap<ResourceLocation, ItemStack> getStacks() {
        if (this.stacks != null && !this.stacks.isEmpty()) {
            ImmutableMap.Builder<ResourceLocation, ItemStack> toReturn = ImmutableMap.builder();

            for (Map.Entry<ResourceLocation, ItemStack> entry : this.stacks.entrySet()) {
                toReturn.put(entry.getKey(), entry.getValue().copy());
            }

            return toReturn.build();
        }

        ImmutableMap.Builder<ResourceLocation, ItemStack> stackMapBuilder = ImmutableMap.builder();

        for (T clothingItem : this.clothingItemsForLoader()) {
            for (Map.Entry<ResourceLocation, NbtStackInitializer<T>> entry : this.stackEntries.entrySet()) {
                try {
                    ItemStack generated = clothingItem.getDefaultInstance();
                    entry.getValue().writeToStack(clothingItem, generated);
                    if (generated.equals(clothingItem.getDefaultInstance())) continue;
                    if (!clothingItem.getSlot().equals(clothingItem.getSlot(generated))) continue;
                    stackMapBuilder.put(entry.getKey(), generated);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception while attempting to load clothing entry {}! Skipped!", entry.getKey(), e);
                }
            }
        }

        this.stacks = stackMapBuilder.build();

        ImmutableMap.Builder<ResourceLocation, ItemStack> toReturn = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, ItemStack> entry : this.stacks.entrySet()) {
            toReturn.put(entry.getKey(), entry.getValue().copy());
        }

        return toReturn.build();
    }

    /**
     * Returns the {@link ItemStack}s from {@link #stacks} filtered down to those stacks whose slot is equal to the
     * slot of the passed instance. Used for {@link ClothingItem#fillItemCategory(CreativeModeTab, NonNullList)}.
     * @param clothingItemInstance the {@link T} instance to obtain stacks for.
     * @return an {@link ImmutableList} with no null elements containing the {@link ItemStack}s belonging to the passed
     *          clothing item.
     */
    public ImmutableList<ItemStack> getStacks(T clothingItemInstance) {
        ImmutableList.Builder<ItemStack> toReturn = ImmutableList.builder();

        toReturn.addAll(
                this.getStacks().values()
                        .stream()
                        .filter(stack -> clothingItemInstance.getSlot().equals(clothingItemInstance.getSlot(stack)))
                        .toList()
        );

        return toReturn.build();
    }

    /**
     * Used mainly for clothing recipes
     * @param entryLocation the {@link ResourceLocation} of the piece of clothing.
     * @return the corresponding {@link ItemStack} for the passed location. Returns as {@link ItemStack#EMPTY} if
     * no such entry location exists.
     * @see io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipe.Serializer
     */
    @NotNull
    public ItemStack getStack(ResourceLocation entryLocation) {
        ImmutableMap<ResourceLocation, ItemStack> stackMap = this.getStacks();
        return Objects.requireNonNull(stackMap.getOrDefault(entryLocation, ItemStack.EMPTY));
    }

    /**
     * Sets {@link #stacks} for the {@link net.minecraft.world.item.CreativeModeTab}. Used on the client; this
     * overwrites existing entries since the server will send ALL entries at least once per reload anyway.
     * @param stacks the mapped {@link ItemStack}s for the clothing entries.
     * @see net.minecraftforge.event.AddReloadListenerEvent
     */
    public void setStacks(Map<ResourceLocation, ItemStack> stacks) {
        this.stacks = ImmutableMap.copyOf(stacks);
    }

    /**
     * Overwrites existing data to avoid duplication of entries when switching worlds or servers.
     * @param clothingMap a {@link ImmutableMap} of types {@link ResourceLocation} and
     * {@link NbtStackInitializer}. Its key corresponds to the entry's file name; including its namespace.
     * @see net.minecraftforge.event.AddReloadListenerEvent
     */
    public void setEntries(ImmutableMap<ResourceLocation, NbtStackInitializer<T>> clothingMap) {
        this.stackEntries = ImmutableMap.copyOf(clothingMap);
        // forces a regeneration of the stacks if previous data exists
        this.stacks = ImmutableMap.of();
    }

    /**
     * Does the file reading
     */
    @Override
    @ParametersAreNonnullByDefault
    protected void apply(
            Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler
    ) {
        ImmutableMap.Builder<ResourceLocation, NbtStackInitializer<T>> builder
                = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation entryId = entry.getKey();
            if (entryId.getPath().startsWith("_")) continue;
            if (entryId.getPath().contains("overlays/")) continue;

            try {
                final JsonObject jsonEntry
                        = GsonHelper.convertToJsonObject(entry.getValue(), "top element");

                if (!entryContainsSlotDeclaration(jsonEntry))
                    throw new IllegalArgumentException("Slot not declared for clothing entry!");

                builder.put(
                        entryId,
                        this.defaultDeserialization(entryId, jsonEntry)
                                .and(this.deserializeFromJson(entryId, jsonEntry))
                );

            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading clothing entry {}!", entryId, jsonParseException);
            }
        }

        this.setEntries(builder.build());
        this.getStacks();

        LOGGER.info("Loaded {} clothing entries in directory {}!", this.stackEntries.size(), this.subDirectory);
    }

    /**
     * Since a slot MUST be declared in the {@link NbtStackInitializer} for purposes of preventing duplicated entries,
     * This method exists to test whether to throw exceptions if the passed JSON data does not declare a slot.
     * @param jsonObject the {@link JsonObject} clothing data entry to verify.
     * @return {@code true} if the passed JSON contains valid slot information. {@code false} otherwise.
     */
    protected static boolean entryContainsSlotDeclaration(JsonObject jsonObject) {
        if (jsonObject.get("slot") == null) return false;
        if (
                !jsonObject.get("slot").isJsonPrimitive() ||
                        !jsonObject.getAsJsonPrimitive("slot").isString()
        ) return false;

        String slotName = jsonObject.getAsJsonPrimitive("slot").getAsString();
        try {
            //noinspection ResultOfMethodCallIgnored
            EquipmentSlot.byName(slotName);
        } catch (IllegalArgumentException e) {
            LOGGER.error("{} is not a valid slot name!", slotName, e);
            return false;
        }

        return true;
    }
}
