package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.clothing.ClothingRegistry;
import io.github.kawaiicakes.clothing.common.data.ClothingLayer;
import io.github.kawaiicakes.clothing.common.data.ClothingVisibility;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static net.minecraft.world.item.DyeableLeatherItem.TAG_COLOR;

/**
 * This class is a {@link SimpleJsonResourceReloadListener} that pretty heavily abstracts stuff related to Minecraft
 * datapack loading. Its purpose is to load data entries for clothing items to appropriate
 * {@link ItemStack}s in the creative menu. This data is held on the server solely to be sent to connecting clients,
 * where the data is finally used.
 */
public class ClothingEntryLoader extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    protected static ClothingEntryLoader INSTANCE;

    protected ImmutableMap<ResourceLocation, NbtStackInitializer> stackEntries = ImmutableMap.of();
    protected ImmutableMap<ResourceLocation, ItemStack> stacks = ImmutableMap.of();

    protected ClothingEntryLoader() {
        super(GSON, "clothing");
    }

    public static ClothingEntryLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClothingEntryLoader();
        }

        return INSTANCE;
    }

    /**
     * Returns an instance of the functional interface {@link NbtStackInitializer}, which ultimately
     * ends up writing the appropriate {@link ItemStack}s to the creative menu.
     * @param entryId the {@link ResourceLocation} representing the file name of this entry.
     * @param topElement the {@link JsonObject} holding the serialized JSON data of the entry.
     * @return the {@link NbtStackInitializer} specifically written to load {@link ItemStack}s for the item.
     */
    @NotNull
    public NbtStackInitializer deserializeFromJson(ResourceLocation entryId, JsonObject topElement) {
        return (clothingItem, clothingStack) -> {
            EquipmentSlot slot;
            int color;
            Multimap<Attribute, AttributeModifier> modifiers;
            int durability;
            ResourceLocation equipSoundLocation;
            List<Component> lore;

            Map<MeshStratum, ClothingLayer> meshes;
            Map<ClothingItem.ModelPartReference, ResourceLocation> models;
            Multimap<MeshStratum, ClothingLayer> overlays;

            try {
                slot = EquipmentSlot.byName(topElement.getAsJsonPrimitive(CLOTHING_SLOT_NBT_KEY).getAsString());

                color = topElement.has(TAG_COLOR)
                        ? topElement.getAsJsonPrimitive(TAG_COLOR).getAsInt()
                        : FALLBACK_COLOR;

                modifiers = topElement.has(ATTRIBUTES_KEY)
                        ? deserializeAttributes(topElement.getAsJsonObject(ATTRIBUTES_KEY))
                        : clothingItem.getDefaultAttributeModifiers(slot);

                durability = topElement.has(MAX_DAMAGE_KEY)
                        ? topElement.getAsJsonPrimitive(MAX_DAMAGE_KEY).getAsInt()
                        : clothingItem.getMaxDamage(clothingStack);

                equipSoundLocation = topElement.has(EQUIP_SOUND_KEY)
                        ? new ResourceLocation(topElement.getAsJsonPrimitive(EQUIP_SOUND_KEY).getAsString())
                        : SoundEvents.ARMOR_EQUIP_LEATHER.getLocation();

                lore = topElement.has(CLOTHING_LORE_NBT_KEY)
                        ? deserializeLore(topElement.getAsJsonArray(CLOTHING_LORE_NBT_KEY))
                        : List.of();

                meshes = topElement.has("meshes")
                        ? meshesFromJson(topElement.getAsJsonObject("meshes"))
                        : defaultMeshForEntry(entryId, slot, clothingItem, clothingStack);

                models = topElement.has("models")
                        ? modelsFromJson(topElement.getAsJsonObject("models"))
                        : ImmutableMap.of();

                overlays = topElement.has("overlays")
                        ? overlaysFromJson(topElement.getAsJsonObject("overlays"))
                        : ImmutableMultimap.of();
            } catch (Exception e) {
                LOGGER.error("Error deserializing clothing entry!", e);
                throw e;
            }

            clothingItem.setClothingName(clothingStack, entryId);
            clothingItem.setSlot(clothingStack, slot);
            clothingItem.setColor(clothingStack, color);
            clothingItem.setDefaultColor(clothingStack, color);
            clothingItem.setAttributeModifiers(clothingStack, modifiers);
            clothingItem.setMaxDamage(clothingStack, durability);
            clothingItem.setEquipSound(clothingStack, equipSoundLocation);
            clothingItem.setClothingLore(clothingStack, lore);

            clothingItem.setMeshes(clothingStack, meshes);
            clothingItem.setModels(clothingStack, models);
            clothingItem.setOverlays(clothingStack, overlays);
        };
    }

    public static Map<MeshStratum, ClothingLayer> defaultMeshForEntry(
            ResourceLocation entryId, EquipmentSlot slot, ClothingItem clothingItem, ItemStack clothingStack
    ) {
        try {
            return ImmutableMap.of(
                    MeshStratum.forSlot(slot),
                    new ClothingLayer(
                            entryId,
                            FALLBACK_COLOR,
                            new ClothingVisibility(defaultPartVisibility(slot))
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Error generating mesh for entry '{}'!", entryId, e);
            return clothingItem.getMeshes(clothingStack);
        }
    }

    public static Map<MeshStratum, ClothingLayer> meshesFromJson(JsonObject object) {
        ImmutableMap.Builder<MeshStratum, ClothingLayer> toReturn = ImmutableMap.builder();

        try {
            for (String key : object.keySet()) {
                MeshStratum byName = MeshStratum.byName(key);
                ClothingLayer model = ClothingLayer.fromJson(object.getAsJsonObject(key));

                toReturn.put(byName, model);
            }

            return toReturn.buildOrThrow();
        } catch (Exception e) {
            LOGGER.error("Error deserializing models from JSON!", e);
            return ImmutableMap.copyOf(ClothingItem.defaultMeshes(EquipmentSlot.CHEST));
        }
    }

    public static Map<ModelPartReference, ResourceLocation> modelsFromJson(JsonObject object) {
        ImmutableMap.Builder<ClothingItem.ModelPartReference, ResourceLocation> toReturn = ImmutableMap.builder();

        try {
            JsonObject modelObject = object.getAsJsonObject("models");

            for (String key : modelObject.keySet()) {
                ClothingItem.ModelPartReference byName = ClothingItem.ModelPartReference.byName(key);
                ResourceLocation model = new ResourceLocation(modelObject.getAsJsonPrimitive(key).getAsString());
                toReturn.put(byName, model);
            }

            return toReturn.buildOrThrow();
        } catch (Exception e) {
            LOGGER.error("Error deserializing models from JSON!", e);
            return ImmutableMap.copyOf(ClothingItem.defaultModels());
        }
    }

    public static Multimap<MeshStratum, ClothingLayer> overlaysFromJson(JsonObject object) {
        ImmutableMultimap.Builder<MeshStratum, ClothingLayer> toReturn = ImmutableMultimap.builder();

        try {
            JsonObject modelObject = object.getAsJsonObject("models");

            for (String key : modelObject.keySet()) {
                MeshStratum byName = MeshStratum.byName(key);
                List<ClothingLayer> overlays = new ArrayList<>();

                for (JsonElement element : modelObject.getAsJsonArray(key)) {
                    overlays.add(ClothingLayer.fromJson(element.getAsJsonObject()));
                }

                toReturn.putAll(byName, overlays);
            }

            return toReturn.build();
        } catch (Exception e) {
            LOGGER.error("Error deserializing models from JSON!", e);
            return ImmutableMultimap.copyOf(ClothingItem.defaultOverlays());
        }
    }

    public static ImmutableMultimap<Attribute, AttributeModifier> deserializeAttributes(JsonObject jsonData) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        try {
            for (String key : jsonData.keySet()) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key));
                if (attribute == null) throw new IllegalArgumentException(
                        "Passed JSON contains unknown attribute '" + key + "'!"
                );
                
                if (!(jsonData.get(key) instanceof JsonArray jsonArray))
                    throw new IllegalArgumentException(
                            "Passed JSON does not contain an array for attribute '" + key + "'!"
                    );

                List<AttributeModifier> forKey = new ArrayList<>(jsonArray.size());

                int i = 0;
                for (JsonElement element : jsonArray) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException(
                            "Passed JSON has non-object in attribute array for '" + key + "'!"
                    );
                    
                    CompoundTag attributeModifierTag
                            = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element);

                    String attributeName = key + "." + i;

                    UUID slotUUID = Mth.createInsecureUUID(RandomSource.createNewThreadLocalInstance());

                    attributeModifierTag.putUUID(
                            "UUID",
                            slotUUID
                    );

                    attributeModifierTag.putString("Name", attributeName);

                    AttributeModifier modifier = AttributeModifier.load(attributeModifierTag);

                    if (modifier == null) throw new IllegalStateException(
                            "Unable to load AttributeModifier from tag '" + attributeModifierTag + "'!"
                    );
                    
                    forKey.add(modifier);
                    i++;
                }

                builder.putAll(attribute, forKey.toArray(AttributeModifier[]::new));
            }
        } catch (Exception e) {
            LOGGER.error("Error deserializing clothing attributes!", e);
            throw e;
        }

        return builder.build();
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

        for (ClothingItem clothingItem : ClothingRegistry.getAllClothing()) {
            for (Map.Entry<ResourceLocation, NbtStackInitializer> entry : this.stackEntries.entrySet()) {
                try {
                    assert clothingItem != null;
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
     * @param clothingItemInstance the clothing item instance to return stacks for.
     * @return an {@link ImmutableList} with no null elements containing the {@link ItemStack}s belonging to the passed
     *          clothing item.
     */
    public ImmutableList<ItemStack> getStacks(ClothingItem clothingItemInstance) {
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
    public void setEntries(ImmutableMap<ResourceLocation, NbtStackInitializer> clothingMap) {
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
        ImmutableMap.Builder<ResourceLocation, NbtStackInitializer> builder
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
                        this.deserializeFromJson(entryId, jsonEntry)
                                .and(this.deserializeFromJson(entryId, jsonEntry))
                );

            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading clothing entry {}!", entryId, jsonParseException);
            }
        }

        this.setEntries(builder.build());
        this.getStacks();

        LOGGER.info("Loaded {} clothing entries!", this.stackEntries.size());
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
