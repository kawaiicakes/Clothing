package io.github.kawaiicakes.clothing.common.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import org.apache.commons.lang3.text.WordUtils;

public class ClothingLangGenerator extends LanguageProvider {
    protected final ClothingEntryGenerator clothingEntryGenerator;

    public ClothingLangGenerator(
            DataGenerator gen, String modid, String locale, ClothingEntryGenerator clothingEntryGenerator
    ) {
        super(gen, modid, locale);
        this.clothingEntryGenerator = clothingEntryGenerator;
    }

    @Override
    protected void addTranslations() {
        this.clothingEntryGenerator.buildEntries(
                (clothingBuilder -> {
                    String withoutUnderscores = clothingBuilder.id.replace("_", " ");
                    // Yes, this is deprecated. No, I don't care too much.
                    //noinspection deprecation
                    String capitalized = WordUtils.capitalize(withoutUnderscores);

                    this.add(clothingBuilder.clothingStack, capitalized);
                })
        );
    }
}
