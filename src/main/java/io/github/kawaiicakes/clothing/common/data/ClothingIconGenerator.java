package io.github.kawaiicakes.clothing.common.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ClothingIconGenerator extends ItemModelProvider {
    public static final ResourceLocation customModelDataPredicateLocation
            = new ResourceLocation("custom_model_data");

    protected final ClothingEntryGenerator clothingEntryGenerator;

    public ClothingIconGenerator(
            DataGenerator generator, String modid,
            ExistingFileHelper existingFileHelper,
            ClothingEntryGenerator clothingEntryGenerator
    ) {
        super(generator, modid, existingFileHelper);
        this.clothingEntryGenerator = clothingEntryGenerator;
    }

    // TODO
    @Override
    protected void registerModels() {
    }
}
