package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.network.ClothingPackets;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

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

    protected ImmutableMap<ResourceLocation, NbtStackInitializer<T>> stackEntries = ImmutableMap.of();
    protected ImmutableList<CompoundTag> stacks = ImmutableList.of();
    protected final String subDirectory;

    /**
     * Whatever {@link String} is passed here is what needs to be added to the {@code switch} block in
     * {@link io.github.kawaiicakes.clothing.common.network.ClothingPackets#handleOnClient(ClothingPackets.S2CClothingPacket, Supplier)}
     * if you decide to use mixins.
     * @param pSubDirectory the {@code String} for the folder name the data entries will be found in.
     */
    protected ClothingEntryLoader(String pSubDirectory) {
        super(GSON, "clothing/" + pSubDirectory);
        this.subDirectory = pSubDirectory;
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
     * Generates the {@link ItemStack}s specified in the datapack loaded by this by iterating in {@link #stackEntries}.
     * @param stackType the clothing item of type {@link T} for which the return is being generated.
     * @return a {@link NonNullList} containing the {@link ItemStack}s generated from this loader.
     */
    public NonNullList<ItemStack> generateStacks(T stackType) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (Map.Entry<ResourceLocation, NbtStackInitializer<T>> entry : this.stackEntries.entrySet()) {
            try {
                ItemStack generated = stackType.getDefaultInstance();
                entry.getValue().writeToStack(stackType, generated);
                if (generated.equals(stackType.getDefaultInstance())) continue;
                if (!stackType.getSlot().equals(stackType.getSlot(generated))) continue;
                stacks.add(generated);
            } catch (RuntimeException e) {
                LOGGER.error("Exception while attempting to load clothing entry {}! Skipped!", entry.getKey(), e);
            }
        }
        return stacks;
    }

    /**
     * Used primarily for data transfer from server to client
     * @return an immutable shallow copy of {@link #stacks}.
     */
    public ImmutableList<CompoundTag> getStacks() {
        ImmutableList.Builder<CompoundTag> toReturn = ImmutableList.builder();
        toReturn.addAll(this.stacks.stream().map(CompoundTag::copy).toList());
        return toReturn.build();
    }

    /**
     * Adds stacks for the {@link net.minecraft.world.item.CreativeModeTab}. Used on the client; this overwrites
     * existing entries since the server will send ALL entries at least once per reload anyway.
     * @param stacks the {@link ImmutableList} of {@link CompoundTag}s which will be written onto {@link ItemStack}s.
     * @see net.minecraftforge.event.AddReloadListenerEvent
     */
    public void addStacks(ImmutableList<CompoundTag> stacks) {
        this.stacks = stacks;
    }

    /**
     * Adds entries for display without overwriting existing entries.
     * @param clothingMap a {@link ImmutableMap} of types {@link ResourceLocation} and
     * {@link NbtStackInitializer}. Its key corresponds to the entry's file name; including its namespace.
     * @see net.minecraftforge.event.AddReloadListenerEvent
     */
    public void addEntries(ImmutableMap<ResourceLocation, NbtStackInitializer<T>> clothingMap) {
        ImmutableMap.Builder<ResourceLocation, NbtStackInitializer<T>> builder
                = ImmutableMap.builder();

        if (this.stackEntries == null || this.stackEntries.isEmpty()) {
            this.stackEntries = ImmutableMap.copyOf(clothingMap);
        } else {
            builder.putAll(this.stackEntries);
            builder.putAll(clothingMap);

            this.stackEntries = builder.build();
        }
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

            try {
                final JsonObject jsonEntry
                        = GsonHelper.convertToJsonObject(entry.getValue(), "top element");

                if (!entryContainsSlotDeclaration(jsonEntry))
                    throw new IllegalArgumentException("Slot not declared for clothing entry!");

                builder.put(
                        entryId,
                        this.deserializeFromJson(
                                entryId,
                                jsonEntry
                        )
                );

            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading clothing entry {}!", entryId, jsonParseException);
            }
        }

        this.addEntries(builder.build());

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

    @Nullable
    public static String[] collapseJsonArrayToStringArray(JsonArray jsonArray) {
        String[] toReturn = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            if (!(jsonArray.get(i) instanceof JsonPrimitive primitive)) return null;
            toReturn[i] = primitive.getAsString();
        }
        return toReturn;
    }
}
