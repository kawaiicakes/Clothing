package io.github.kawaiicakes.clothing.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.clothing.item.ClothingItem;
import io.github.kawaiicakes.clothing.item.impl.BakedModelClothingItem;
import io.github.kawaiicakes.clothing.item.impl.GenericClothingItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.item.ClothingRegistry.*;

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
        GenericClothingBuilder.shirt("tank_top").save(clothingBuilderConsumer);
    }

    @Override
    public void run(@NotNull CachedOutput pOutput) {
        Set<String> locations = new HashSet<>();

        buildEntries(
                (clothingBuilder -> {
                    ResourceLocation clothingId = new ResourceLocation(this.modId, clothingBuilder.getId());

                    try {
                        if (!locations.add(clothingBuilder.getId()))
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
        protected GenericClothingBuilder(GenericClothingItem clothingItem, String name) {
            super(clothingItem, name);
            this.setTexture(name);
        }

        public GenericClothingBuilder setRenderLayer(GenericClothingItem.ModelStrata renderLayer) {
            this.clothingItem.setGenericLayerForRender(this.clothingStack, renderLayer);
            return this;
        }

        public GenericClothingBuilder setTexture(String path) {
            this.clothingItem.setTextureLocation(this.clothingStack, path);
            return this;
        }

        public GenericClothingBuilder setOverlays(String[] paths) {
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

        public static GenericClothingBuilder hat(String name) {
            return new GenericClothingBuilder(GENERIC_HAT.get(), name);
        }

        public static GenericClothingBuilder shirt(String name) {
            return new GenericClothingBuilder(GENERIC_SHIRT.get(), name);
        }

        public static GenericClothingBuilder pants(String name) {
            return new GenericClothingBuilder(GENERIC_PANTS.get(), name);
        }

        public static GenericClothingBuilder shoes(String name) {
            return new GenericClothingBuilder(GENERIC_SHOES.get(), name);
        }
    }

    public static class BakedModelClothingBuilder extends ClothingBuilder<BakedModelClothingItem> {
        protected BakedModelClothingBuilder(BakedModelClothingItem clothingItem, String name) {
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

        public static BakedModelClothingBuilder hat(String name) {
            return new BakedModelClothingBuilder(BAKED_HAT.get(), name);
        }

        public static BakedModelClothingBuilder shirt(String name) {
            return new BakedModelClothingBuilder(BAKED_SHIRT.get(), name);
        }

        public static BakedModelClothingBuilder pants(String name) {
            return new BakedModelClothingBuilder(BAKED_PANTS.get(), name);
        }

        public static BakedModelClothingBuilder shoes(String name) {
            return new BakedModelClothingBuilder(BAKED_SHOES.get(), name);
        }
    }

    public static abstract class ClothingBuilder<T extends ClothingItem<T>> {
        protected final T clothingItem;
        protected final ItemStack clothingStack;
        protected final EquipmentSlot slotForItem;
        protected final String id;

        protected ClothingBuilder(T clothingItem, String id) {
            this.clothingItem = clothingItem;
            this.clothingStack = this.clothingItem.getDefaultInstance();
            this.id = id;
            this.clothingItem.setClothingName(this.clothingStack, id);
            this.slotForItem = this.clothingItem.getSlot();
        }

        public EquipmentSlot getSlotForItem() {
            return this.slotForItem;
        }

        public String getId() {
            return this.id;
        }

        public void save(Consumer<ClothingBuilder<?>> clothingBuilderConsumer) {
            clothingBuilderConsumer.accept(this);
        }

        public ClothingBuilder<T> setColor(int color) {
            this.clothingItem.setColor(this.clothingStack, color);
            return this;
        }

        @Nullable
        public JsonObject serializeToJson() {
            final ItemStack defaultStack = this.clothingItem.getDefaultInstance();

            final CompoundTag defaultStackTag = this.clothingItem.getClothingPropertyTag(defaultStack).copy();
            final CompoundTag clothingStackTag = this.clothingItem.getClothingPropertyTag(this.clothingStack).copy();

            final CompoundTag tagForSerialization = new CompoundTag();

            for (String key : clothingStackTag.getAllKeys()) {
                if (key.equals("name") || key.equals("BaseModelData") || key.equals("GenericOverlayData")) continue;

                Tag clothingTag = clothingStackTag.get(key);
                Tag defaultTag = defaultStackTag.get(key);

                assert clothingTag != null;

                if (key.equals("slot")) {
                    tagForSerialization.put(key, clothingTag);
                    continue;
                }

                if (clothingTag.equals(defaultTag)) continue;

                tagForSerialization.put(key, clothingTag);
            }

            JsonElement toReturn = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tagForSerialization);

            try {
                if (this.id == null || this.id.isEmpty())
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
