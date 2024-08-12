package io.github.kawaiicakes.clothing.common.resources.recipe;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
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

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.CLOTHING_PROPERTY_NBT_KEY;
import static io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipeRegistry.CLOTHING_SERIALIZER;

// TODO: I change my mind. These should allow changing colour
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

            CompoundTag stackTag = defaultStackForEntry.getTag();
            assert stackTag != null;

            for (String key : miscNbt.getAllKeys()) {
                if (key.equals(CLOTHING_PROPERTY_NBT_KEY)) {
                    LOGGER.error(
                            "ClothingRecipes do not support changing clothing properties in the result! " +
                                    "Make a new clothing entry instead!"
                    );
                    continue;
                }

                stackTag.put(key, Objects.requireNonNull(miscNbt.get(key)));
            }

            stackAsNbt.put("tag", stackTag);
            stackAsNbt.putString(
                    "id",
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(defaultStackForEntry.getItem())).toString()
            );
            stackAsNbt.putInt("Count", GsonHelper.getAsInt(json, "count", 1));

            return ItemStack.of(stackAsNbt);
        }
    }
}
