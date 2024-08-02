package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import io.github.kawaiicakes.clothing.common.resources.OverlayDefinitionLoader;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.CompositeModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.io.Serializable;
import java.util.*;

public class ClothingItemModelGenerator extends ItemModelProvider {
    public static final ModelFile GENERATED = new ModelFile.UncheckedModelFile("item/generated");
    public static ResourceLocation predicateLocation() {
            return new ResourceLocation("custom_model_data");
    }

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

        final Map<ClothingEntryGenerator.ClothingBuilder<?>, Set<OverlayDefinitionLoader.OverlayDefinition>>
                overlaysForEntries = this.generateOverlaysForEntries();

        final Map<OverlayDefinitionLoader.OverlayDefinition, ItemModelBuilder>
                modelsForOverlays = this.generateModelsForOverlays();

        final Map<ClothingEntryGenerator.ClothingBuilder<?>, CompositeModelBuilder<ItemModelBuilder>>
                compositeModelsForEntries = this.generateCompositeModelsForEntries(overlaysForEntries);

        for (
                Map.Entry<ClothingEntryGenerator.ClothingBuilder<?>, CompositeModelBuilder<ItemModelBuilder>> entry
                : compositeModelsForEntries.entrySet()
        ) {
            this.modelsForAllVisibilityPermutationsForEntry(entry);
        }

        // TODO:
        // for each registered generic item, set custom model data overrides to the hashcode for every
        // overlay model whose entry model parent belongs to that slot

        // TODO: pseudo datagen using command from in-game; takes client assets and generates models, lang, entries, overlays...
        // see other mods for ways to write files in a way that won't break
    }

    public Map<ClothingEntryGenerator.ClothingBuilder<?>, Set<OverlayDefinitionLoader.OverlayDefinition>>
    generateOverlaysForEntries() {
        ImmutableMap.Builder<ClothingEntryGenerator.ClothingBuilder<?>, Set<OverlayDefinitionLoader.OverlayDefinition>>
                builder = ImmutableMap.builder();

        ImmutableList.Builder<ClothingEntryGenerator.ClothingBuilder<?>> entryList = ImmutableList.builder();
        this.clothingEntryGenerator.buildEntries(entryList::add);

        for (ClothingEntryGenerator.ClothingBuilder<?> entry : entryList.build()) {
            ImmutableSet.Builder<OverlayDefinitionLoader.OverlayDefinition> overlayDefinitions = ImmutableSet.builder();

            for (OverlayDefinitionLoader.OverlayDefinition overlay : this.clothingOverlayGenerator.getOverlays()) {
                ResourceLocation entryLocation = new ResourceLocation(
                        this.clothingOverlayGenerator.modId,
                        entry.getId()
                );

                if (Arrays.asList(overlay.blacklist()).contains(entryLocation)) continue;
                if (
                        Arrays.asList(overlay.slotsFor()).contains(entry.getSlotForItem())
                                || Arrays.asList(overlay.whitelist()).contains(entryLocation)
                ) overlayDefinitions.add(overlay);
            }

            builder.put(entry, overlayDefinitions.build());
        }

        return builder.build();
    }

    public Map<OverlayDefinitionLoader.OverlayDefinition, ItemModelBuilder> generateModelsForOverlays() {
        final ImmutableMap.Builder<OverlayDefinitionLoader.OverlayDefinition, ItemModelBuilder> overlayModelMapBuilder
                = ImmutableMap.builder();

        for (
                OverlayDefinitionLoader.OverlayDefinition overlayDefinition : this.clothingOverlayGenerator.getOverlays()
        ) {
            final ItemModelBuilder modelForOverlay = this.getBuilder(
                    "item/clothing/overlays/" + overlayDefinition.name()
            )
                    .parent(GENERATED)
                    .texture("layer0", this.modid + ":item/overlays/" + overlayDefinition.name());

            overlayModelMapBuilder.put(overlayDefinition, modelForOverlay);
        }

        return overlayModelMapBuilder.build();
    }

    public Map<ClothingEntryGenerator.ClothingBuilder<?>, CompositeModelBuilder<ItemModelBuilder>>
    generateCompositeModelsForEntries(
            Map<ClothingEntryGenerator.ClothingBuilder<?>, Set<OverlayDefinitionLoader.OverlayDefinition>>
                    overlaysForEntries
    ) {
        ImmutableMap.Builder<ClothingEntryGenerator.ClothingBuilder<?>, CompositeModelBuilder<ItemModelBuilder>>
                builder = ImmutableMap.builder();

        this.clothingEntryGenerator.buildEntries(
                clothingEntry -> {
                    ResourceLocation mainModelLocation
                            = new ResourceLocation(
                                    this.modid, "item/clothing/" + clothingEntry.getId() + "_main"
                    );

                    ItemModelBuilder mainModelBuilder
                            = new ItemModelBuilder(mainModelLocation, this.existingFileHelper)
                            .parent(GENERATED)
                            .texture("layer0", this.modid + ":item/" + clothingEntry.getId());

                    final CompositeModelBuilder<ItemModelBuilder> entryModelBuilder
                            = (CompositeModelBuilder<ItemModelBuilder>)
                            this.getBuilder("item/clothing/" + clothingEntry.getId())
                            .customLoader(CompositeModelBuilder::begin)
                            .child("main_model", mainModelBuilder)
                            .visibility("main_model", true);

                    Set<OverlayDefinitionLoader.OverlayDefinition> overlaysForEntry
                            = overlaysForEntries.get(clothingEntry);
                    assert overlaysForEntry != null;
                    for (OverlayDefinitionLoader.OverlayDefinition overlay : overlaysForEntry) {
                        ResourceLocation overlayLocation
                                = new ResourceLocation(this.modid, "item/clothing/overlays/" + overlay.name());

                        ModelFile.ExistingModelFile overlayModelFile = new ModelFile.ExistingModelFile(
                                overlayLocation,
                                this.existingFileHelper
                        );

                        ItemModelBuilder overlayModelBuilder
                                = new ItemModelBuilder(overlayLocation, this.existingFileHelper);

                        overlayModelBuilder.parent(overlayModelFile);

                        entryModelBuilder
                                .child(
                                        overlay.name(),
                                        overlayModelBuilder
                                )
                                .visibility(overlay.name(), false);
                    }

                    builder.put(clothingEntry, entryModelBuilder);
                }
        );

        return builder.build();
    }

    public Map<ClothingEntryGenerator.ClothingBuilder<?>, List<ResourceLocation>>
    modelsForAllVisibilityPermutationsForEntry(
            Map.Entry<ClothingEntryGenerator.ClothingBuilder<?>, CompositeModelBuilder<ItemModelBuilder>> entry
    ) {
        Map<ClothingEntryGenerator.ClothingBuilder<?>, List<ResourceLocation>> toReturn
                = new HashMap<>();

        JsonObject entryValueJson = new JsonObject();
        entry.getValue().toJson(entryValueJson);

        Set<String> children = entryValueJson.getAsJsonObject("children").keySet();
        children.remove("main_model");
        List<String> childList = children.stream().toList();

        List<Boolean> possibleStates = new ArrayList<>(2);
        possibleStates.add(true);
        possibleStates.add(false);

        List<List<Serializable>> stateValues = Lists.cartesianProduct(
                ImmutableList.of(
                        childList,
                        possibleStates
                )
        );

        int stateValueSize = stateValues.size();
        if ((stateValueSize & 1) != 0) throw new IllegalStateException("Cartesian product has odd number of elements!");


        List<List<List<Serializable>>> permutationList = new ArrayList<>(stateValueSize / 2);
        for (int i = 0; i < (stateValueSize / 2); i++) {
            List<List<Serializable>> listOfLists = new ArrayList<>(2);

            for (int j = 0; j < 2; j++) {
                listOfLists.add(j, new ArrayList<>(2));
            }

            permutationList.add(i, listOfLists);
        }

        for (int i = 0; i < stateValueSize; i++) {
            int permutationListIndex = i / 2;
            List<List<Serializable>> listAt = permutationList.get(permutationListIndex);

            int pairingIndex = (i & 1) == 0 ? 0 : 1;
            listAt.get(pairingIndex).add(0, stateValues.get(i).get(0));
            listAt.get(pairingIndex).add(1, stateValues.get(i).get(1));
        }

        final List<List<List<Serializable>>> finalPermutationList = Lists.cartesianProduct(permutationList);

        List<ResourceLocation> outputList = new ArrayList<>();
        for (List<List<Serializable>> overlayPermutation : finalPermutationList) {
            if (overlayPermutation.stream().noneMatch(streamList -> (Boolean) streamList.get(1)))
                continue;

            ResourceLocation entryLocation = new ResourceLocation(
                    this.modid, "item/clothing/" + entry.getKey().getId()
            );

            String outputString = "item/clothing/" + entry.getKey().getId() + "_" + overlayPermutation.hashCode();
            ResourceLocation outputLocation = new ResourceLocation(this.modid, outputString);

            CompositeModelBuilder<ItemModelBuilder> compositeModelBuilder
                    = this.withExistingParent(outputString, entryLocation)
                    .customLoader(CompositeModelBuilder::begin);

            for (List<Serializable> childVisibility : overlayPermutation) {
                compositeModelBuilder.visibility((String) childVisibility.get(0), (Boolean) childVisibility.get(1));
            }

            outputList.add(outputLocation);
        }

        toReturn.put(entry.getKey(), outputList);

        return toReturn;
    }

    public void generateGenericModels() {
        /*
            GenericClothingItem[] items = new GenericClothingItem[]{
                    GENERIC_HAT.get(),
                    GENERIC_SHIRT.get(),
                    GENERIC_PANTS.get(),
                    GENERIC_SHOES.get()
            };

            for (GenericClothingItem item : items) {
                ItemModelBuilder forItem = this.basicItem(item)
                        .texture("layer0", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString());

                for () {
                    ModelFile modelFile;

                    forItem
                            .override()
                            .predicate(predicateLocation, )
                            .model(modelFile);
                }
            }
         */
    }
}
