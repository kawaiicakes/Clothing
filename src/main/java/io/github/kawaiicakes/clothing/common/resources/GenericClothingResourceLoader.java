package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO:
 */
public class GenericClothingResourceLoader extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    protected static GenericClothingResourceLoader INSTANCE = null;

    protected Map<ResourceLocation, GenericClothingItem.ItemStackInitializer> clothing = ImmutableMap.of();

    protected GenericClothingResourceLoader() {
        super(GSON, "generic_clothing");
        INSTANCE = this;
    }

    public static GenericClothingResourceLoader getInstance() {
        if (INSTANCE == null) {
            return new GenericClothingResourceLoader();
        }

        return INSTANCE;
    }

    public Set<GenericClothingItem.ItemStackInitializer> genericClothingEntries() {
        Set<GenericClothingItem.ItemStackInitializer> toReturn = new HashSet<>(this.clothing.size());
        for (GenericClothingItem.ItemStackInitializer initializer : this.clothing.values()) {
            toReturn.add(GenericClothingItem.ItemStackInitializer.copyOf(initializer));
        }
        return toReturn;
    }

    /**
     * TODO
     * @param clothingMap
     */
    public void addClothing(ImmutableMap<ResourceLocation, GenericClothingItem.ItemStackInitializer> clothingMap) {
        ImmutableMap.Builder<ResourceLocation, GenericClothingItem.ItemStackInitializer> builder
                = ImmutableMap.builder();

        if (this.clothing == null || this.clothing.isEmpty()) {
            this.clothing = ImmutableMap.copyOf(clothingMap);
        } else {
            builder.putAll(this.clothing);
            builder.putAll(clothingMap);

            this.clothing = builder.build();
        }
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> pObject,
            @NotNull ResourceManager pResourceManager,
            @NotNull ProfilerFiller pProfiler
    ) {
        ImmutableMap.Builder<ResourceLocation, GenericClothingItem.ItemStackInitializer> builder
                = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation entryId = entry.getKey();
            if (entryId.getPath().startsWith("_")) continue;

            try {
                Set<GenericClothingItem.ItemStackInitializer> deserializedEntry
                        = GenericClothingItem.ItemStackInitializer.fromJson(
                                entryId,
                                GsonHelper.convertToJsonObject(entry.getValue(), "top element")
                );
                if (deserializedEntry == null) continue;

                for (GenericClothingItem.ItemStackInitializer initializer : deserializedEntry) {
                    builder.put(entryId, initializer);
                }
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", entryId, jsonParseException);
            }
        }

        this.addClothing(builder.build());

        LOGGER.info("Loaded {} generic clothing entries", this.clothing.size());
    }
}
