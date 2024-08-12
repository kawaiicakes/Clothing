package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.item.ClothingRegistry.GENERIC_SHIRT;
import static io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipeRegistry.CLOTHING_SERIALIZER;

/**
 * Be sure to add this to the data
 */
public class ClothingRecipeGenerator extends RecipeProvider {
    public ClothingRecipeGenerator(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(@NotNull Consumer<FinishedRecipe> recipeConsumer) {
        Builder.of(new ResourceLocation(MOD_ID, "tank_top"))
                .define('w', ItemTags.WOOL)
                .pattern("w w")
                .pattern("www")
                .pattern("www")
                .save(recipeConsumer);
    }

    protected static class Builder extends ShapedRecipeBuilder {
        protected final ResourceLocation resultId;

        protected Builder(ResourceLocation entryLocation) {
            super(GENERIC_SHIRT.get(), 1);
            this.resultId = entryLocation;
        }

        public static Builder of(ResourceLocation entryLocation) {
            return new Builder(entryLocation);
        }

        @Override
        @NotNull
        @ParametersAreNonnullByDefault
        public Builder define(Character pSymbol, TagKey<Item> pTag) {
            return (Builder) super.define(pSymbol, pTag);
        }

        @Override
        @NotNull
        @ParametersAreNonnullByDefault
        public Builder define(Character pSymbol, ItemLike pItem) {
            return (Builder) super.define(pSymbol, pItem);
        }

        @Override
        @NotNull
        @ParametersAreNonnullByDefault
        public Builder define(Character pSymbol, Ingredient pIngredient) {
            return (Builder) super.define(pSymbol, pIngredient);
        }

        @Override
        @NotNull
        public Builder pattern(@NotNull String pPattern) {
            return (Builder) super.pattern(pPattern);
        }

        @Override
        @ParametersAreNonnullByDefault
        public @NotNull Builder unlockedBy(String pCriterionName, CriterionTriggerInstance pCriterionTrigger) {
            return (Builder) super.unlockedBy(pCriterionName, pCriterionTrigger);
        }

        @Override
        public @NotNull Builder group(@Nullable String pGroupName) {
            return (Builder) super.group(pGroupName);
        }

        @Override
        @ParametersAreNonnullByDefault
        public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
            this.ensureValid(this.resultId);
            this.advancement.parent(ROOT_RECIPE_ADVANCEMENT)
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(this.resultId))
                    .rewards(AdvancementRewards.Builder.recipe(this.resultId))
                    .requirements(RequirementsStrategy.OR);

            pFinishedRecipeConsumer.accept(
                    new ClothingRecipeGenerator.Result(
                            this.resultId,
                            this.group == null ? "" : this.group,
                            this.rows,
                            this.key,
                            this.advancement,
                            new ResourceLocation(
                                    this.resultId.getNamespace(),
                                    "recipes/clothing/" + this.resultId.getPath()
                            )
                    )
            );
        }

        @Override
        public void ensureValid(@NotNull ResourceLocation pRecipeId) {
            if (this.rows.isEmpty()) {
                throw new IllegalStateException("No pattern is defined for shaped recipe " + pRecipeId + "!");
            } else {
                Set<Character> set = Sets.newHashSet(this.key.keySet());
                set.remove(' ');

                for(String s : this.rows) {
                    for(int i = 0; i < s.length(); ++i) {
                        char c0 = s.charAt(i);
                        if (!this.key.containsKey(c0) && c0 != ' ') {
                            throw new IllegalStateException(
                                    "Pattern in recipe "
                                            + pRecipeId
                                            + " uses undefined symbol '"
                                            + c0 + "'"
                            );
                        }

                        set.remove(c0);
                    }
                }

                if (!set.isEmpty()) {
                    throw new IllegalStateException(
                            "Ingredients are defined but not used in pattern for recipe " + pRecipeId
                    );
                } else if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
                    throw new IllegalStateException(
                            "Shaped recipe "
                                    + pRecipeId
                                    + " only takes in a single item - should it be a shapeless recipe instead?"
                    );
                }
            }
        }
    }

    public static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final String group;
        private final List<String> pattern;
        private final Map<Character, Ingredient> key;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(
                ResourceLocation id,
                String group,
                List<String> pattern,
                Map<Character, Ingredient> key,
                Advancement.Builder advancement,
                ResourceLocation advancementId
        ) {

            this.id = id;
            this.group = group;
            this.pattern = pattern;
            this.key = key;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject pJson) {
            if (!this.group.isEmpty()) {
                pJson.addProperty("group", this.group);
            }

            JsonArray patternJson = new JsonArray();
            for(String s : this.pattern) {
                patternJson.add(s);
            }
            pJson.add("pattern", patternJson);

            JsonObject keyJson = new JsonObject();
            for(Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                keyJson.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            pJson.add("key", keyJson);

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("clothing", this.id.toString());

            pJson.add("result", resultJson);
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return this.id;
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return CLOTHING_SERIALIZER.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
