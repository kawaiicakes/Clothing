package io.github.kawaiicakes.clothing.common.resources;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader.GSON;

public class OverlayDefinitionLoader extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected static OverlayDefinitionLoader INSTANCE = null;

    protected ImmutableList<OverlayDefinition> overlays;

    public OverlayDefinitionLoader() {
        super(GSON, "clothing/generic/overlays");

        INSTANCE = this;
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
        Map<ResourceLocation, OverlayDefinition> overlays = new HashMap<>();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation entryId = entry.getKey();

            if (entryId.getPath().startsWith("_")) continue;

            try {
                boolean overlayIsUnique = !overlays.containsKey(entryId);
                OverlayDefinitionBuilder entryBuilder = OverlayDefinitionBuilder.of(entryId);

                JsonObject jsonBuilder = entry.getValue().getAsJsonObject();

                if (
                                        !jsonBuilder.has("slots")
                                                && !jsonBuilder.has("whitelist")
                                                && !jsonBuilder.has("blacklist")
                ) {
                    throw new IllegalArgumentException("Overlay '" + entry.getKey() + "' is empty!");
                }

                if (jsonBuilder.has("slots")) {
                    JsonArray slots = jsonBuilder.getAsJsonArray("slots");
                    for (JsonElement listElement : slots) {
                        entryBuilder.addSlot(
                                EquipmentSlot.byName(listElement.getAsJsonPrimitive().getAsString())
                        );
                    }
                }

                if (jsonBuilder.has("whitelist")) {
                    JsonArray whitelist = jsonBuilder.getAsJsonArray("whitelist");
                    for (JsonElement listElement : whitelist) {
                        entryBuilder.addToWhitelist(
                                new ResourceLocation(listElement.getAsJsonPrimitive().getAsString())
                        );
                    }
                }

                if (jsonBuilder.has("blacklist")) {
                    JsonArray blacklist = jsonBuilder.getAsJsonArray("blacklist");
                    for (JsonElement listElement : blacklist) {
                        entryBuilder.addToBlacklist(
                                new ResourceLocation(listElement.getAsJsonPrimitive().getAsString())
                        );
                    }
                }

                if (
                        jsonBuilder.has("override")
                                && jsonBuilder.getAsJsonPrimitive("override").getAsBoolean()
                ) {
                    overlays.put(entryId, entryBuilder.build());
                } else if (overlayIsUnique) {
                    overlays.put(entryId, entryBuilder.build());
                } else {
                    overlays.put(entryId, overlays.get(entryId).merge(entryBuilder.build()));
                }
            } catch (Exception jsonParseException) {
                LOGGER.error("Parsing error loading overlay entry {}!", entryId, jsonParseException);
            }
        }

        ImmutableList<OverlayDefinition> finalOverlays = ImmutableList.copyOf(overlays.values());

        this.addOverlays(finalOverlays);

        LOGGER.info("Loaded {} clothing overlays!", finalOverlays.size());
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
        protected final ResourceLocation name;
        protected EquipmentSlot[] slotsFor = new EquipmentSlot[0];
        protected ResourceLocation[] whitelist = new ResourceLocation[0];
        protected ResourceLocation[] blacklist = new ResourceLocation[0];

        protected OverlayDefinitionBuilder(ResourceLocation name) {
            this.name = name;
        }

        public static OverlayDefinitionBuilder of(ResourceLocation name) {
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
            ResourceLocation name, EquipmentSlot[] slotsFor, ResourceLocation[] whitelist, ResourceLocation[] blacklist
    ) {
        public OverlayDefinition merge(OverlayDefinition other) {
            if (!this.name.equals(other.name))
                throw new IllegalArgumentException("Cannot merge overlays with different names!");

            Set<EquipmentSlot> slotsFor = new HashSet<>(List.of(this.slotsFor));
            Set<ResourceLocation> whitelist = new HashSet<>(List.of(this.whitelist));
            Set<ResourceLocation> blacklist = new HashSet<>(List.of(this.blacklist));

            slotsFor.addAll(List.of(other.slotsFor));
            whitelist.addAll(List.of(other.whitelist));
            blacklist.addAll(List.of(other.blacklist));

            return new OverlayDefinition(
                    this.name,
                    slotsFor.toArray(EquipmentSlot[]::new),
                    whitelist.toArray(ResourceLocation[]::new),
                    blacklist.toArray(ResourceLocation[]::new)
            );
        }

        public boolean isValidEntry(ItemStack stack) {
            if (!(stack.getItem() instanceof ClothingItem<?> clothingItem)) return false;

            ResourceLocation clothingName = clothingItem.getClothingName(stack);

            return (Arrays.asList(this.slotsFor).contains(clothingItem.getSlot())
                    || Arrays.asList(this.whitelist).contains(clothingName))
                    && !Arrays.asList(this.blacklist).contains(clothingName);
        }

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

        public static void serializeToNetwork(FriendlyByteBuf buf, OverlayDefinition overlay) {
            buf.writeResourceLocation(overlay.name);
            buf.writeCollection(
                    Arrays.stream(overlay.slotsFor)
                            .map(EquipmentSlot::getName)
                            .collect(Collectors.toList()),
                    FriendlyByteBuf::writeUtf)
            ;
            buf.writeCollection(Arrays.asList(overlay.whitelist), FriendlyByteBuf::writeResourceLocation);
            buf.writeCollection(Arrays.asList(overlay.blacklist), FriendlyByteBuf::writeResourceLocation);
        }

        public static OverlayDefinition deserializeFromNetwork(FriendlyByteBuf buf) {
            return new OverlayDefinition(
                    buf.readResourceLocation(),
                    buf.readList(FriendlyByteBuf::readUtf)
                            .stream()
                            .map(EquipmentSlot::byName)
                            .toArray(EquipmentSlot[]::new),
                    buf.readList(FriendlyByteBuf::readResourceLocation).toArray(ResourceLocation[]::new),
                    buf.readList(FriendlyByteBuf::readResourceLocation).toArray(ResourceLocation[]::new)
            );
        }
    }
}
