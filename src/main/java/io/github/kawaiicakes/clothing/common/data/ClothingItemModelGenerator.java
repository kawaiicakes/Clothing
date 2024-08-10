package io.github.kawaiicakes.clothing.common.data;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.item.ClothingRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.BASE_MODEL_DATA;

public class ClothingItemModelGenerator extends ItemModelProvider {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final ModelFile GENERATED = new ModelFile.UncheckedModelFile("item/generated");
    public static final String ITEM_CLOTHING_MODEL_PATH = "item/clothing/";

    protected final ClothingEntryGenerator clothingEntryGenerator;
    protected final ClothingOverlayGenerator clothingOverlayGenerator;

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

        Set<ClothingEntryGenerator.ClothingBuilder<?>> clothingEntrySet = new HashSet<>();
        this.clothingEntryGenerator.buildEntries(
                entry -> {
                    clothingEntrySet.add(entry);
                    this.generateEntryModel(entry);
                }
        );

        this.generateItemModel(clothingEntrySet);

        // TODO: pseudo datagen using command from in-game; takes client assets and generates models, lang, entries, overlays...
        // see other mods for ways to write files in a way that won't break
    }

    protected void generateEntryModel(ClothingEntryGenerator.ClothingBuilder<?> entry) {
        String fullPath = ITEM_CLOTHING_MODEL_PATH + entry.getId().getPath();
        this.getBuilder(fullPath)
                .parent(GENERATED)
                .texture("layer0", fullPath);
    }

    protected void generateItemModel(Set<ClothingEntryGenerator.ClothingBuilder<?>> clothingEntries) {
        ClothingItem<?>[] clothingItems = ClothingRegistry.getAll();
        if (clothingItems == null || clothingItems.length == 0) {
            LOGGER.error("No clothing items exist! Is this being called before item registration?");
            return;
        }

        for (ClothingItem<?> clothingItem : clothingItems) {
            try {
                ResourceLocation clothingLocation = ForgeRegistries.ITEMS.getKey(clothingItem);
                assert clothingLocation != null;
                String clothingPath = clothingLocation.getPath();

                ItemModelBuilder modelBuilder = this.getBuilder("item/" + clothingPath)
                        .parent(GENERATED)
                        .texture("layer0", "item/" + clothingPath);

                for (ClothingEntryGenerator.ClothingBuilder<?> clothingEntry : clothingEntries) {
                    if (!clothingItem.getSlot().equals(clothingEntry.clothingItem.getSlot())) continue;
                    ResourceLocation entryLocation = new ResourceLocation(
                            clothingEntry.getId().getNamespace(),
                            ITEM_CLOTHING_MODEL_PATH + clothingEntry.getId().getPath()
                    );

                    ModelFile entryModel = new ModelFile.ExistingModelFile(entryLocation, this.existingFileHelper);

                    String hashValueString = String.valueOf(clothingEntry.hashCodeForBaseModelData());

                    // FIXME: float is serialized in scientific notation
                    modelBuilder.override()
                            .model(entryModel)
                            .predicate(BASE_MODEL_DATA, Float.parseFloat(hashValueString))
                            .end();
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error while generating clothing item model for {}!", clothingItem, e);
            }
        }
    }
}
