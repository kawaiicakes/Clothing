package io.github.kawaiicakes.clothing.common.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.client.model.ClothingItemModel;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingItemModelGenerator extends ItemModelProvider {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final ModelFile GENERATED = new ModelFile.UncheckedModelFile("item/generated");
    public static final String ITEM_CLOTHING_MODEL_PATH = "item/clothing/";
    public static final String ITEM_OVERLAY_MODEL_PATH = ITEM_CLOTHING_MODEL_PATH + "overlays/";

    protected final ClothingEntryGenerator clothingEntryGenerator;
    protected final ClothingOverlayGenerator clothingOverlayGenerator;
    protected ImmutableList<OverlayDefinitionLoader.OverlayDefinition> overlays;

    public ClothingItemModelGenerator(
            DataGenerator generator, String modid,
            ExistingFileHelper existingFileHelper,
            ClothingEntryGenerator clothingEntryGenerator,
            ClothingOverlayGenerator clothingOverlayGenerator
    ) {
        super(generator, modid, existingFileHelper);
        this.clothingEntryGenerator = clothingEntryGenerator;
        this.clothingOverlayGenerator = clothingOverlayGenerator;
    }

    @Override
    protected void registerModels() {
        if (this.clothingOverlayGenerator.overlays == null || this.clothingOverlayGenerator.overlays.isEmpty())
            this.clothingOverlayGenerator.registerOverlays();

        this.overlays = clothingOverlayGenerator.getOverlays();

        // TODO: pseudo datagen using command from in-game; takes client assets and generates models, lang, entries, overlays...
        // see other mods for ways to write files in a way that won't break

        this.registerItemModels();
        this.clothingEntryGenerator.buildEntries(this::registerBaseClothingModels);
        this.registerOverlayModels();
    }

    public ItemModelBuilder getBuilderWithNamespace(String namespace, String path) {
        Preconditions.checkNotNull(path, "Path must not be null");

        ResourceLocation output = path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(namespace, path);

        if (!output.getPath().contains("/"))
            output = new ResourceLocation(output.getNamespace(), this.folder + "/" + output.getPath());

        this.existingFileHelper.trackGenerated(output, MODEL);
        return this.generatedModels.computeIfAbsent(output, this.factory);
    }

    public void registerItemModels() {
        String[] items = {
                "generic_hat",
                "generic_shirt",
                "generic_pants",
                "generic_shoes"
        };

        for (String name : items) {
            String itemName = "item/" + name;

            this.getBuilder(itemName)
                    .parent(GENERATED)
                    .texture("layer0", itemName)
                    .customLoader(ClothingModelBuilder::begin)
                    .end();
        }
    }

    public void registerBaseClothingModels(ClothingEntryGenerator.ClothingBuilder builder) {
        ResourceLocation entryLoc = builder.clothingItem.getClothingName(builder.clothingStack);

        String entryName = ITEM_CLOTHING_MODEL_PATH + entryLoc.getPath();

        this.getBuilderWithNamespace(entryLoc.getNamespace(), entryName)
                .parent(GENERATED)
                .texture("layer0", entryName);
    }

    public void registerOverlayModels() {
        this.overlays.forEach(
                overlay -> {
                    ResourceLocation overlayLocation = overlay.name();
                    String overlayName = ITEM_OVERLAY_MODEL_PATH + overlayLocation.getPath();

                    this.getBuilderWithNamespace(overlayLocation.getNamespace(), overlayName)
                            .parent(GENERATED)
                            .texture("layer0", "item/clothing/overlays/empty")
                            .texture("layer1", overlayName);
                }
        );
    }

    /**
     * This only exists to add the "loader" property to the serialized JSON. That's it.
     */
    public static class ClothingModelBuilder extends CustomLoaderBuilder<ItemModelBuilder> {
        public static ClothingModelBuilder begin(ItemModelBuilder parent, ExistingFileHelper existingFileHelper) {
            return new ClothingModelBuilder(parent, existingFileHelper);
        }

        protected ClothingModelBuilder(ItemModelBuilder parent, ExistingFileHelper existingFileHelper) {
            super(
                    new ResourceLocation(MOD_ID, ClothingItemModel.Loader.ID),
                    parent,
                    existingFileHelper
            );
        }
    }
}
