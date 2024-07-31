package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Map;

import static io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader.GSON;

// TODO: multiple overlay files that share the same name don't fully overwrite each other by default
public class OverlayDefinitionLoader extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected static OverlayDefinitionLoader INSTANCE = null;

    protected ImmutableList<OverlayDefinition> overlays;

    public OverlayDefinitionLoader() {
        super(GSON, "clothing/generic/overlays");
    }

    public static OverlayDefinitionLoader getInstance() {
        if (INSTANCE == null) {
            return new OverlayDefinitionLoader();
        }

        return INSTANCE;
    }

    public ImmutableList<OverlayDefinition> getOverlays() {
        return ImmutableList.copyOf(this.overlays);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(
            Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler
    ) {
        ImmutableList.Builder<OverlayDefinition> builder
                = ImmutableList.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation entryId = entry.getKey();
            if (entryId.getPath().startsWith("_")) continue;

            try {
                OverlayDefinitionBuilder entryBuilder = OverlayDefinitionBuilder.of(entryId.getPath());

                // TODO

                builder.add(
                        entryBuilder.build()
                );
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading overlay entry {}!", entryId, jsonParseException);
            }
        }

        ImmutableList<OverlayDefinition> overlays = builder.build();

        this.addOverlays(overlays);

        LOGGER.info("Loaded {} clothing overlays!", overlays.size());
    }

    public void addOverlays(ImmutableList<OverlayDefinition> overlays) {
        ImmutableList.Builder<OverlayDefinition> builder
                = ImmutableList.builder();

        if (this.overlays == null || this.overlays.isEmpty()) {
            this.overlays = ImmutableList.copyOf(overlays);
        } else {
            builder.addAll(this.overlays);
            builder.addAll(overlays);

            this.overlays = builder.build();
        }
    }

    public static class OverlayDefinitionBuilder {
        protected final String name;
        protected EquipmentSlot[] slotsFor = new EquipmentSlot[0];
        protected ResourceLocation[] whitelist = new ResourceLocation[0];
        protected ResourceLocation[] blacklist = new ResourceLocation[0];

        protected OverlayDefinitionBuilder(String name) {
            this.name = name;
        }

        public static OverlayDefinitionBuilder of(String name) {
            return new OverlayDefinitionBuilder(name);
        }

        public OverlayDefinitionBuilder addSlot(EquipmentSlot slot) {
            if (Arrays.asList(this.slotsFor).contains(slot))
                throw new IllegalArgumentException("Duplicate slot " + slot.getName() + "!");
            if (this.slotsFor.length > 6)
                throw new IllegalArgumentException("Duplicate slot; all values added!");

            final int lengthPlusOne = this.slotsFor.length + 1;
            EquipmentSlot[] newSlotsFor = new EquipmentSlot[lengthPlusOne];

            System.arraycopy(this.slotsFor, 0, newSlotsFor, 0, this.slotsFor.length);
            newSlotsFor[lengthPlusOne - 1] = slot;

            this.slotsFor = newSlotsFor;
            return this;
        }

        public OverlayDefinitionBuilder addToWhitelist(ResourceLocation resourceLocation) {
            if (Arrays.asList(this.whitelist).contains(resourceLocation))
                throw new IllegalArgumentException("Duplicate whitelist entry " + resourceLocation.toString() + "!");

            final int lengthPlusOne = this.whitelist.length + 1;
            ResourceLocation[] newWhitelist = new ResourceLocation[lengthPlusOne];

            System.arraycopy(this.whitelist, 0, newWhitelist, 0, this.slotsFor.length);
            newWhitelist[lengthPlusOne - 1] = resourceLocation;

            this.whitelist = newWhitelist;
            return this;
        }

        public OverlayDefinitionBuilder addToBlacklist(ResourceLocation resourceLocation) {
            if (Arrays.asList(this.blacklist).contains(resourceLocation))
                throw new IllegalArgumentException("Duplicate whitelist entry " + resourceLocation.toString() + "!");

            final int lengthPlusOne = this.blacklist.length + 1;
            ResourceLocation[] newBlackList = new ResourceLocation[lengthPlusOne];

            System.arraycopy(this.blacklist, 0, newBlackList, 0, this.slotsFor.length);
            newBlackList[lengthPlusOne - 1] = resourceLocation;

            this.blacklist = newBlackList;
            return this;
        }

        public OverlayDefinition build() {
            return new OverlayDefinition(this.name, this.slotsFor, this.whitelist, this.blacklist);
        }
    }

    public record OverlayDefinition(
            String name, EquipmentSlot[] slotsFor, ResourceLocation[] whitelist, ResourceLocation[] blacklist
    ) {
        public JsonObject serializeToJson() {
            final JsonObject toReturn = new JsonObject();

            if (this.slotsFor.length == 0 && this.whitelist.length == 0)
                throw new IllegalStateException("Overlay '" + this.name + "' has nothing declared to be applied to!");

            final JsonArray slotsJson = new JsonArray();
            for (EquipmentSlot slot : this.slotsFor) {
                slotsJson.add(slot.getName());
            }
            toReturn.add("slots", slotsJson);

            final JsonArray whitelistJson = new JsonArray();
            for (ResourceLocation whitelistLocation : this.whitelist) {
                whitelistJson.add(whitelistLocation.toString());
            }
            if (!whitelistJson.isEmpty())
                toReturn.add("whitelist", whitelistJson);

            final JsonArray blacklistJson = new JsonArray();
            for (ResourceLocation blacklistLocation : this.blacklist) {
                blacklistJson.add(blacklistLocation.toString());
            }
            if (!blacklistJson.isEmpty())
                toReturn.add("blacklist", whitelistJson);

            return toReturn;
        }
    }
}
