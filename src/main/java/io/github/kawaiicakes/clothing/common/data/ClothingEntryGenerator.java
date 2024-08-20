package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static io.github.kawaiicakes.clothing.ClothingRegistry.*;

public class ClothingEntryGenerator implements DataProvider {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final DataGenerator dataGenerator;
    protected final String modId;
    protected final DataGenerator.PathProvider genericPath;
    protected final DataGenerator.PathProvider bakedPath;

    public ClothingEntryGenerator(DataGenerator dataGenerator, String modId) {
        this.dataGenerator = dataGenerator;
        this.modId = modId;
        this.genericPath = this.dataGenerator.createPathProvider(
                DataGenerator.Target.DATA_PACK, "clothing/generic"
        );
        this.bakedPath = this.dataGenerator.createPathProvider(DataGenerator.Target.DATA_PACK, "clothing/baked");
    }

    public void buildEntries(Consumer<ClothingBuilder<?>> clothingBuilderConsumer) {
        GenericClothingBuilder.shirt(new ResourceLocation(MOD_ID, "tank_top"))
                .addModifier(Attributes.ARMOR, 40.00, AttributeModifier.Operation.ADDITION)
                .setDurability(300)
                .addLoreLine("{\"type\":\"text\", \"text\": \"Woah! You go, big guy!\"}")
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

                        final DataGenerator.PathProvider pathProvider;
                        if (clothingBuilder instanceof GenericClothingBuilder) {
                            pathProvider = this.genericPath;
                        } else if (clothingBuilder instanceof BakedModelClothingBuilder) {
                            pathProvider = this.bakedPath;
                        } else {
                            throw new RuntimeException("Unrecognized builder subclass!");
                        }

                        final Path entryPath = pathProvider.json(clothingId);

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

    public static class GenericClothingBuilder extends ClothingBuilder<GenericClothingItem> {
        protected GenericClothingBuilder(GenericClothingItem clothingItem, ResourceLocation name) {
            super(clothingItem, name);
            this.setTexture(name);
        }

        public GenericClothingBuilder setRenderLayer(GenericClothingItem.ModelStrata renderLayer) {
            this.clothingItem.setGenericLayerForRender(this.clothingStack, renderLayer);
            return this;
        }

        public GenericClothingBuilder setTexture(ResourceLocation path) {
            this.clothingItem.setTextureLocation(this.clothingStack, path);
            return this;
        }

        public GenericClothingBuilder setOverlays(ResourceLocation[] paths) {
            this.clothingItem.setOverlays(this.clothingStack, paths);
            return this;
        }

        public GenericClothingBuilder setPartVisibility(ClothingItem.ModelPartReference[] parts) {
            this.clothingItem.setPartsForVisibility(this.clothingStack, parts);
            return this;
        }

        @Override
        public GenericClothingBuilder setColor(int color) {
            return (GenericClothingBuilder) super.setColor(color);
        }

        public static GenericClothingBuilder hat(ResourceLocation name) {
            return new GenericClothingBuilder(GENERIC_HAT.get(), name);
        }

        public static GenericClothingBuilder shirt(ResourceLocation name) {
            return new GenericClothingBuilder(GENERIC_SHIRT.get(), name);
        }

        public static GenericClothingBuilder pants(ResourceLocation name) {
            return new GenericClothingBuilder(GENERIC_PANTS.get(), name);
        }

        public static GenericClothingBuilder shoes(ResourceLocation name) {
            return new GenericClothingBuilder(GENERIC_SHOES.get(), name);
        }
    }

    public static class BakedModelClothingBuilder extends ClothingBuilder<BakedModelClothingItem> {
        protected BakedModelClothingBuilder(BakedModelClothingItem clothingItem, ResourceLocation name) {
            super(clothingItem, name);
        }

        public BakedModelClothingBuilder setModelLocations(
                Map<ClothingItem.ModelPartReference, ResourceLocation> locations
        ) {
            this.clothingItem.setModelPartLocations(this.clothingStack, locations);
            return this;
        }

        public BakedModelClothingBuilder setModelLocation(
                ClothingItem.ModelPartReference parent, ResourceLocation model
        ) {
            this.clothingItem.setModelPartLocation(this.clothingStack, parent, model);
            return this;
        }

        @Override
        public BakedModelClothingBuilder setColor(int color) {
            return (BakedModelClothingBuilder) super.setColor(color);
        }

        public static BakedModelClothingBuilder hat(ResourceLocation name) {
            return new BakedModelClothingBuilder(BAKED_HAT.get(), name);
        }

        public static BakedModelClothingBuilder shirt(ResourceLocation name) {
            return new BakedModelClothingBuilder(BAKED_SHIRT.get(), name);
        }

        public static BakedModelClothingBuilder pants(ResourceLocation name) {
            return new BakedModelClothingBuilder(BAKED_PANTS.get(), name);
        }

        public static BakedModelClothingBuilder shoes(ResourceLocation name) {
            return new BakedModelClothingBuilder(BAKED_SHOES.get(), name);
        }
    }

    public static abstract class ClothingBuilder<T extends ClothingItem<T>> {
        protected final T clothingItem;
        protected final ItemStack clothingStack;
        protected final EquipmentSlot slotForItem;
        protected final ResourceLocation id;
        protected boolean defaultAttributes = true;

        protected ClothingBuilder(T clothingItem, ResourceLocation id) {
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

        public void save(Consumer<ClothingBuilder<?>> clothingBuilderConsumer) {
            clothingBuilderConsumer.accept(this);
        }

        public ClothingBuilder<T> addLoreLine(String loreAsJsonString) {
            List<Component> appended = this.clothingItem.getClothingLore(this.clothingStack);

            appended.add(Component.Serializer.fromJson(loreAsJsonString));

            this.clothingItem.setClothingLore(this.clothingStack, appended);
            return this;
        }

        public ClothingBuilder<T> setColor(int color) {
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
        public ClothingBuilder<T> addModifier(
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
        public ClothingBuilder<T> setModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
            this.clothingItem.setAttributeModifiers(this.clothingStack, modifiers);
            return this;
        }

        public ClothingBuilder<T> setDurability(int durability) {
            this.clothingItem.setMaxDamage(this.clothingStack, durability);
            return this;
        }

        public ClothingBuilder<T> setEquipSound(ResourceLocation soundLocation) {
            this.clothingItem.setEquipSound(this.clothingStack, soundLocation);
            return this;
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

                return toReturn.getAsJsonObject();
            } catch (RuntimeException e) {
                LOGGER.error("Error serializing NBT tag to JSON in entry generator!", e);
                return null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClothingBuilder<?> builder)) return false;
            return this.clothingItem.equals(builder.clothingItem)
                    && this.clothingStack.equals(builder.clothingStack, false)
                    && this.slotForItem.equals(builder.slotForItem)
                    && this.id.equals(builder.id);
        }
    }
}
