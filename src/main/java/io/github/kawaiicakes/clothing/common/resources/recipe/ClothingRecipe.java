package io.github.kawaiicakes.clothing.common.resources.recipe;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingEntryLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem.MODEL_LAYER_NBT_KEY;
import static io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipeRegistry.CLOTHING_SERIALIZER;

public class ClothingRecipe extends ShapedRecipe {
    protected static final Logger LOGGER = LogUtils.getLogger();

    public ClothingRecipe(
            ResourceLocation id, String group,
            int width, int height,
            NonNullList<Ingredient> ingredients, ItemStack result
    ) {
        super(id, group, width, height, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return CLOTHING_SERIALIZER.get();
    }

    public static class Serializer extends ShapedRecipe.Serializer {
        public Serializer() {}

        @Override
        @ParametersAreNonnullByDefault
        public @NotNull ClothingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            String group = GsonHelper.getAsString(pSerializedRecipe, "group", "");
            Map<String, Ingredient> ingredientMap = ShapedRecipe.keyFromJson(
                    GsonHelper.getAsJsonObject(pSerializedRecipe, "key")
            );
            String[] pattern = ShapedRecipe.shrink(
                    ShapedRecipe.patternFromJson(GsonHelper.getAsJsonArray(pSerializedRecipe, "pattern"))
            );
            int width = pattern[0].length();
            int height = pattern.length;
            NonNullList<Ingredient> ingredients = ShapedRecipe.dissolvePattern(pattern, ingredientMap, width, height);

            ItemStack result = getItemStack(GsonHelper.getAsJsonObject(pSerializedRecipe, "result"));

            return new ClothingRecipe(pRecipeId, group, width, height, ingredients, result);
        }

        public static ItemStack getItemStack(JsonObject json) {
            boolean isGenericEntry
                    = GsonHelper.getAsString(json, "type", "generic").equals("generic");

            ResourceLocation entryName = new ResourceLocation(GsonHelper.getAsString(json, "clothing"));

            ItemStack defaultStackForEntry;

            if (isGenericEntry) {
                defaultStackForEntry = GenericClothingEntryLoader.getInstance().getStack(entryName);
            } else {
                defaultStackForEntry = BakedClothingEntryLoader.getInstance().getStack(entryName);
            }

            CompoundTag stackAsNbt = new CompoundTag();

            CompoundTag miscNbt = new CompoundTag();
            if (json.has("nbt")) {
                miscNbt = CraftingHelper.getNBT(json.get("nbt"));

                if (miscNbt.contains("ForgeCaps")) {
                    //noinspection DataFlowIssue
                    stackAsNbt.put("ForgeCaps", miscNbt.get("ForgeCaps"));
                    miscNbt.remove("ForgeCaps");
                }
            }

            CompoundTag stackTag = defaultStackForEntry.getOrCreateTag();

            ClothingItem<?> clothingItem = (ClothingItem<?>) defaultStackForEntry.getItem();
            CompoundTag mergedProperties = clothingItem.getClothingPropertyTag(defaultStackForEntry);

            for (String key : miscNbt.getAllKeys()) {
                if (key.equals(CLOTHING_PROPERTY_NBT_KEY)) {
                    for (String propertyKey : miscNbt.getCompound(key).getAllKeys()) {
                        if (propertyKey.equals(CLOTHING_NAME_KEY)) continue;
                        if (propertyKey.equals(CLOTHING_SLOT_NBT_KEY)) continue;

                        mergedProperties.put(
                                propertyKey, Objects.requireNonNull(miscNbt.getCompound(key).get(propertyKey))
                        );
                    }
                    continue;
                }

                stackTag.put(key, Objects.requireNonNull(miscNbt.get(key)));
            }

            CompoundTag declaredClothingProperties = getProperties(isGenericEntry, mergedProperties, json);

            for (String key : declaredClothingProperties.getAllKeys()) {
                mergedProperties.put(key, Objects.requireNonNull(declaredClothingProperties.get(key)));
            }

            stackTag.put(CLOTHING_PROPERTY_NBT_KEY, mergedProperties);

            stackAsNbt.put("tag", stackTag);
            stackAsNbt.putString(
                    "id",
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(defaultStackForEntry.getItem())).toString()
            );
            stackAsNbt.putInt("Count", GsonHelper.getAsInt(json, "count", 1));

            return ItemStack.of(stackAsNbt);
        }

        @NotNull
        public static CompoundTag getProperties(boolean isGeneric, CompoundTag mergedProperties, JsonObject json) {
            mergedProperties = mergedProperties.copy();

            // FIXME: add validation of values

            CompoundTag toReturn = new CompoundTag();

            int color;
            int baseModelData;
            CompoundTag attributes;
            String equipSound;
            int maxDamage;

            String modelStrata;

            try {
                color = json.has(TAG_COLOR)
                        ? json.getAsJsonPrimitive(TAG_COLOR).getAsInt()
                        : mergedProperties.getInt(TAG_COLOR);

                baseModelData = json.has(BASE_MODEL_DATA_NBT_KEY)
                        ? json.getAsJsonPrimitive(BASE_MODEL_DATA_NBT_KEY).getAsInt()
                        : mergedProperties.getInt(BASE_MODEL_DATA_NBT_KEY);

                // TODO: attributes and remaining generic/baked properties
                attributes = json.has(ATTRIBUTES_KEY)
                        ? new CompoundTag()
                        : mergedProperties.getCompound(ATTRIBUTES_KEY);

                equipSound = json.has(EQUIP_SOUND_KEY)
                        ? json.getAsJsonPrimitive(EQUIP_SOUND_KEY).getAsString()
                        : mergedProperties.getString(EQUIP_SOUND_KEY);

                maxDamage = json.has(MAX_DAMAGE_KEY)
                        ? json.getAsJsonPrimitive(MAX_DAMAGE_KEY).getAsInt()
                        : mergedProperties.getInt(MAX_DAMAGE_KEY);

                modelStrata = json.has(MODEL_LAYER_NBT_KEY)
                        ? json.getAsJsonPrimitive(MODEL_LAYER_NBT_KEY).getAsString()
                        : mergedProperties.getString(MODEL_LAYER_NBT_KEY);
            } catch (Exception e) {
                LOGGER.error("Unable to write clothing properties for recipe!");
                return toReturn;
            }

            toReturn.putInt(TAG_COLOR, color);
            toReturn.putInt(BASE_MODEL_DATA_NBT_KEY, baseModelData);
            toReturn.put(ATTRIBUTES_KEY, attributes);
            toReturn.putString(EQUIP_SOUND_KEY, equipSound);
            toReturn.putInt(MAX_DAMAGE_KEY, maxDamage);

            toReturn.putString(MODEL_LAYER_NBT_KEY, modelStrata);

            return toReturn;
        }
    }
}
