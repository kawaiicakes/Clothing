package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.ClothingRegistry.*;
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;

public class ClothingEntryGenerator implements DataProvider {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final DataGenerator dataGenerator;
    protected final String modId;
    protected final DataGenerator.PathProvider path;

    public ClothingEntryGenerator(DataGenerator dataGenerator, String modId) {
        this.dataGenerator = dataGenerator;
        this.modId = modId;
        this.path = this.dataGenerator.createPathProvider(
                DataGenerator.Target.DATA_PACK, "clothing/"
        );
    }

    public void buildEntries(Consumer<ClothingBuilder> clothingBuilderConsumer) {
        ClothingBuilder.shirt(new ResourceLocation(MOD_ID, "tank_top"))
                .addModifier(Attributes.ARMOR, 40.00, AttributeModifier.Operation.ADDITION)
                .setDurability(300)
                .addLoreLine("{\"type\":\"text\", \"text\": \"Woah! You go, big guy!\"}")
                .save(clothingBuilderConsumer);

        ClothingBuilder.shirt(new ResourceLocation(MOD_ID, "hoodie"))
                .addMesh(MeshStratum.BASE, new ResourceLocation(MOD_ID, "default"))
                .addMesh(MeshStratum.OUTER, new ResourceLocation(MOD_ID, "tank_top"))
                .save(clothingBuilderConsumer);
    }

    @Override
    public void run(@NotNull CachedOutput pOutput) {
        Set<ResourceLocation> locations = new HashSet<>();

        buildEntries(
                (clothingBuilder -> {
                    ResourceLocation clothingId = clothingBuilder.getId();

                    try {
                        if (!locations.add(clothingId))
                            throw new IllegalStateException("Duplicate entry " + clothingId);

                        final Path entryPath = this.path.json(clothingId);

                        DataProvider.saveStable(pOutput, clothingBuilder.serializeToJson(), entryPath);
                    } catch (IOException | RuntimeException ioexception) {
                        LOGGER.error("Couldn't save clothing entry {}!", clothingId, ioexception);
                    }
                })
        );
    }

    @Override
    public @NotNull String getName() {
        return "Clothing Entries:" + " mod id - " + this.modId;
    }

    public static class ClothingBuilder {
        protected final ClothingItem clothingItem;
        protected final ItemStack clothingStack;
        protected final EquipmentSlot slotForItem;
        protected final ResourceLocation id;
        protected boolean defaultAttributes = true;

        protected ClothingBuilder(ClothingItem clothingItem, ResourceLocation id) {
            this.clothingItem = clothingItem;
            this.clothingStack = this.clothingItem.getDefaultInstance();
            this.id = id;
            this.clothingItem.setClothingName(this.clothingStack, id);
            this.slotForItem = this.clothingItem.getSlot();
        }

        public EquipmentSlot getSlotForItem() {
            return this.slotForItem;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        public void save(Consumer<ClothingBuilder> clothingBuilderConsumer) {
            clothingBuilderConsumer.accept(this);
        }

        public ClothingBuilder addLoreLine(String loreAsJsonString) {
            List<Component> appended = this.clothingItem.getClothingLore(this.clothingStack);

            appended.add(Component.Serializer.fromJson(loreAsJsonString));

            this.clothingItem.setClothingLore(this.clothingStack, appended);
            return this;
        }

        public ClothingBuilder setColor(int color) {
            this.clothingItem.setColor(this.clothingStack, color);
            return this;
        }

        /**
         * The first call to this method will wipe the default attributes on the entry.
         * @param attribute
         * @param amount
         * @param operation
         * @return
         */
        public ClothingBuilder addModifier(
                Attribute attribute, double amount, AttributeModifier.Operation operation
        ) {
            if (this.defaultAttributes) {
                this.clothingItem.setAttributeModifiers(this.clothingStack, ImmutableMultimap.of());
                this.defaultAttributes = false;
            }

            Multimap<Attribute, AttributeModifier> modifiers
                    = this.clothingItem.getAttributeModifiers(this.slotForItem, this.clothingStack);

            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(modifiers);

            ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attributeLocation == null) {
                LOGGER.error("Unable to obtain ResourceLocation of Attribute {}!", attribute);
                return this;
            }

            String modifierName = attributeLocation + "." + modifiers.get(attribute).size();

            AttributeModifier attributeModifier = new AttributeModifier(
                    Mth.createInsecureUUID(RandomSource.createNewThreadLocalInstance()),
                    modifierName,
                    amount,
                    operation
            );
            builder.put(attribute, attributeModifier);

            this.clothingItem.setAttributeModifiers(this.clothingStack, builder.build());
            return this;
        }

        /**
         * This overwrites the attributes completely; use
         * {@link #addModifier(Attribute, double, AttributeModifier.Operation)} if this isn't desired
         * @param modifiers
         * @return
         */
        public ClothingBuilder setModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
            this.clothingItem.setAttributeModifiers(this.clothingStack, modifiers);
            return this;
        }

        public ClothingBuilder setDurability(int durability) {
            this.clothingItem.setMaxDamage(this.clothingStack, durability);
            return this;
        }

        public ClothingBuilder setEquipSound(ResourceLocation soundLocation) {
            this.clothingItem.setEquipSound(this.clothingStack, soundLocation);
            return this;
        }

        public ClothingBuilder setMeshes(Map<MeshStratum, ClothingLayer> meshes) {
            this.clothingItem.setMeshes(this.clothingStack, ImmutableMap.copyOf(meshes));
            return this;
        }

        public ClothingBuilder setModels(Map<ModelPartReference, ResourceLocation> locations) {
            this.clothingItem.setModels(this.clothingStack, locations);
            return this;
        }

        public ClothingBuilder setOverlays(Multimap<MeshStratum, ClothingLayer> paths) {
            this.clothingItem.setOverlays(this.clothingStack, ImmutableListMultimap.copyOf(paths));
            return this;
        }

        public ClothingBuilder addMesh(MeshStratum stratum, ClothingLayer mesh) {
            this.clothingItem.setMesh(this.clothingStack, stratum, mesh);
            return this;
        }

        public ClothingBuilder addMesh(MeshStratum stratum, ResourceLocation meshLocation) {
            this.addMesh(
                    stratum,
                    new ClothingLayer(
                            meshLocation,
                            FALLBACK_COLOR,
                            new ClothingVisibility(ClothingItem.defaultPartVisibility(this.clothingItem.getSlot()))
                    )
            );
            return this;
        }

        public ClothingBuilder addModel(ModelPartReference parent, ResourceLocation model) {
            this.clothingItem.setModel(this.clothingStack, parent, model);
            return this;
        }

        public ClothingBuilder addOverlay(MeshStratum stratum, ClothingLayer overlay) {
            this.clothingItem.addOverlay(this.clothingStack, stratum, overlay);
            return this;
        }

        public ClothingBuilder addOverlay(MeshStratum stratum, ResourceLocation meshLocation) {
            this.addMesh(
                    stratum,
                    new ClothingLayer(
                            meshLocation,
                            FALLBACK_COLOR,
                            null
                    )
            );
            return this;
        }

        public static ClothingBuilder hat(ResourceLocation name) {
            return new ClothingBuilder(GENERIC_HAT.get(), name);
        }

        public static ClothingBuilder shirt(ResourceLocation name) {
            return new ClothingBuilder(GENERIC_SHIRT.get(), name);
        }

        public static ClothingBuilder pants(ResourceLocation name) {
            return new ClothingBuilder(GENERIC_PANTS.get(), name);
        }

        public static ClothingBuilder shoes(ResourceLocation name) {
            return new ClothingBuilder(GENERIC_SHOES.get(), name);
        }

        @Nullable
        public JsonObject serializeToJson() {
            final ItemStack defaultStack = this.clothingItem.getDefaultInstance();

            final CompoundTag defaultStackTag = this.clothingItem.getClothingPropertiesTag(defaultStack).copy();
            final CompoundTag clothingStackTag = this.clothingItem.getClothingPropertiesTag(this.clothingStack).copy();

            final CompoundTag tagForSerialization = new CompoundTag();

            for (String key : clothingStackTag.getAllKeys()) {
                if (key.equals(CLOTHING_NAME_KEY)) continue;

                Tag clothingTag = clothingStackTag.get(key);
                Tag defaultTag = defaultStackTag.get(key);

                assert clothingTag != null;

                if (key.equals(CLOTHING_SLOT_NBT_KEY)) {
                    tagForSerialization.put(key, clothingTag);
                    continue;
                }

                if (clothingTag.equals(defaultTag)) continue;

                if (key.equals(ATTRIBUTES_KEY)) {
                    CompoundTag attributeTag = clothingStackTag.getCompound(ATTRIBUTES_KEY).copy();

                    Set<String> emptyModifierListKeys = new HashSet<>();
                    for (String attributeKey : attributeTag.getAllKeys()) {
                        ListTag modifierList = attributeTag.getList(attributeKey, Tag.TAG_COMPOUND);

                        if (modifierList.isEmpty()) {
                            emptyModifierListKeys.add(attributeKey);
                            continue;
                        }

                        for (Tag tagInList : modifierList) {
                            if (!(tagInList instanceof CompoundTag modifierTag)) continue;

                            modifierTag.remove("Name");
                            modifierTag.remove("UUID");
                        }

                        attributeTag.put(attributeKey, modifierList);
                    }
                    // this is to avoid ConcurrentModificationExceptions
                    emptyModifierListKeys.forEach(attributeTag::remove);

                    tagForSerialization.put(key, attributeTag);

                    continue;
                }

                tagForSerialization.put(key, clothingTag);
            }

            JsonElement toReturn = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tagForSerialization);

            try {
                if (this.id == null || this.id.getPath().isEmpty())
                    throw new IllegalStateException("This builder has not had a name set!");

                JsonObject toReturnFr = toReturn.getAsJsonObject();

                if (!toReturnFr.keySet().contains(MESHES_NBT_KEY)) return toReturnFr;

                JsonObject strataObject = toReturnFr.getAsJsonObject(MESHES_NBT_KEY);

                for (String stratum : strataObject.keySet()) {
                    JsonObject stratumObject = strataObject.getAsJsonObject(stratum);

                    if (
                            stratumObject.has(TAG_COLOR)
                                    && stratumObject.getAsJsonPrimitive(TAG_COLOR).getAsInt() == FALLBACK_COLOR
                    ) stratumObject.remove(TAG_COLOR);

                    if (!stratumObject.has("visibility")) continue;

                    JsonArray visibilityJsonArray = stratumObject.getAsJsonArray("visibility");
                    int arrSize = visibilityJsonArray.size();

                    ModelPartReference[] visibility = new ModelPartReference[arrSize];
                    ModelPartReference[] defaultVisibility = defaultPartVisibility(this.clothingItem.getSlot());

                    for (int i = 0; i < arrSize; i++) {
                        JsonPrimitive elementAt = visibilityJsonArray.get(i).getAsJsonPrimitive();
                        visibility[i] = ModelPartReference.byName(elementAt.getAsString());
                    }

                    Set<ModelPartReference> visibilitySet
                            = new HashSet<>(Arrays.stream(visibility).toList());

                    Set<ModelPartReference> defaultVisibilitySet
                            = new HashSet<>(Arrays.stream(defaultVisibility).toList());

                    if (!visibilitySet.equals(defaultVisibilitySet)) continue;

                    stratumObject.remove("visibility");
                }

                return toReturnFr;
            } catch (RuntimeException e) {
                LOGGER.error("Error serializing NBT tag to JSON in entry generator!", e);
                return null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClothingBuilder builder)) return false;
            return this.clothingItem.equals(builder.clothingItem)
                    && this.clothingStack.equals(builder.clothingStack, false)
                    && this.slotForItem.equals(builder.slotForItem)
                    && this.id.equals(builder.id);
        }
    }
}
