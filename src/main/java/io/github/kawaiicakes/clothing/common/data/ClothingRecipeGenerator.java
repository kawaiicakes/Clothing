package io.github.kawaiicakes.clothing.common.data;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static io.github.kawaiicakes.clothing.common.item.ClothingRegistry.GENERIC_SHIRT;
import static io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem.OVERLAY_NBT_KEY;
import static io.github.kawaiicakes.clothing.common.resources.recipe.ClothingRecipeRegistry.CLOTHING_SERIALIZER;
import static net.minecraft.world.item.DyeableLeatherItem.TAG_COLOR;
import static net.minecraft.world.item.Items.*;

/**
 * Be sure to add this to the data
 */
public class ClothingRecipeGenerator extends RecipeProvider {
    public ClothingRecipeGenerator(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(@NotNull Consumer<FinishedRecipe> recipeConsumer) {
        for (Item wool : woolItems()) {
            String color = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(wool)).toString()
                    .replace("minecraft:", "")
                    .replace("_wool", "");

            Builder.of(new ResourceLocation(MOD_ID, "tank_top"))
                    .define('w', wool)
                    .pattern("w w")
                    .pattern("www")
                    .pattern("www")
                    .color(woolDyeColors(color))
                    .save(recipeConsumer, new ResourceLocation(MOD_ID, "tank_top_" + color));
        }
    }

    protected static class Builder extends ShapedRecipeBuilder {
        protected final CompoundTag clothingProperties = new CompoundTag();
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

        public Builder color(int color) {
            this.clothingProperties.putInt(TAG_COLOR, color);
            return this;
        }

        public Builder setAttributes(Multimap<Attribute, AttributeModifier> attributes) {
            CompoundTag clothingAttributesTag = new CompoundTag();

            for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : attributes.asMap().entrySet()) {
                ListTag modifierEntries = new ListTag();

                for (AttributeModifier modifier : entry.getValue()) {
                    modifierEntries.add(modifier.save());
                }

                ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
                assert attributeLocation != null;

                clothingAttributesTag.put(attributeLocation.toString(), modifierEntries);
            }

            this.clothingProperties.put(ATTRIBUTES_KEY, clothingAttributesTag);

            return this;
        }

        /**
         * Adds to the existing attribute or creates one if it does not exist
         * @param attribute
         * @param modifier
         * @return
         */
        public Builder attribute(Attribute attribute, AttributeModifier modifier) {
            if (!this.clothingProperties.contains(ATTRIBUTES_KEY, Tag.TAG_COMPOUND))
                return this.setAttributes(ImmutableSetMultimap.of(attribute, modifier));

            CompoundTag attributesTag = this.clothingProperties.getCompound(ATTRIBUTES_KEY);

            ResourceLocation attributeLocation = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            assert attributeLocation != null;

            if (!attributesTag.contains(attributeLocation.toString(), Tag.TAG_LIST))
                attributesTag.put(attributeLocation.toString(), new ListTag());

            attributesTag.getList(attributeLocation.toString(), Tag.TAG_COMPOUND).add(modifier.save());

            return this;
        }

        public Builder equipSound(ResourceLocation soundLocation) {
            this.clothingProperties.putString(EQUIP_SOUND_KEY, soundLocation.toString());
            return this;
        }

        public Builder durability(int durability) {
            this.clothingProperties.putInt(MAX_DAMAGE_KEY, durability);
            return this;
        }

        public Builder setOverlays(ResourceLocation[] overlays) {
            ListTag overlayTag = new ListTag();
            for (ResourceLocation location : overlays) {
                overlayTag.add(StringTag.valueOf(location.toString()));
            }
            this.clothingProperties.put(OVERLAY_NBT_KEY, overlayTag);
            return this;
        }

        public Builder addOverlay(String location) {
            if (!this.clothingProperties.contains(OVERLAY_NBT_KEY, Tag.TAG_LIST))
                return this.setOverlays(new ResourceLocation[]{new ResourceLocation(location)});

            ListTag list = this.clothingProperties.getList(OVERLAY_NBT_KEY, Tag.TAG_STRING);
            list.add(StringTag.valueOf(location));

            return this;
        }

        public Builder addOverlay(ResourceLocation location) {
            return this.addOverlay(location.toString());
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
                            pRecipeId,
                            this.resultId,
                            this.clothingProperties,
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
        public void save(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
            this.save(pFinishedRecipeConsumer, this.resultId);
        }

        @Override
        @ParametersAreNonnullByDefault
        public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, String pRecipeId) {
            ResourceLocation recipeIdLocation = new ResourceLocation(pRecipeId);
            if (recipeIdLocation.equals(this.resultId)) {
                throw new IllegalStateException(
                        "Recipe " + pRecipeId + " should remove its 'save' argument as it is equal to default"
                );
            } else {
                this.save(pFinishedRecipeConsumer, recipeIdLocation);
            }
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
        private final ResourceLocation recipeId;
        private final ResourceLocation resultId;
        private final CompoundTag properties;
        private final String group;
        private final List<String> pattern;
        private final Map<Character, Ingredient> key;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(
                ResourceLocation recipeId,
                ResourceLocation resultId,
                CompoundTag properties,
                String group,
                List<String> pattern,
                Map<Character, Ingredient> key,
                Advancement.Builder advancement,
                ResourceLocation advancementId
        ) {

            this.recipeId = recipeId;
            this.resultId = resultId;
            this.properties = properties.copy();
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
            resultJson.addProperty("clothing", this.resultId.toString());
            for (String key : this.properties.getAllKeys()) {
                Tag tag = this.properties.get(key);
                assert tag != null;
                resultJson.add(key, NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag));
            }

            pJson.add("result", resultJson);
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return this.recipeId;
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

    public static Item[] woolItems() {
        return new Item[] {
                WHITE_WOOL,
                ORANGE_WOOL,
                MAGENTA_WOOL,
                LIGHT_BLUE_WOOL,
                YELLOW_WOOL,
                LIME_WOOL,
                PINK_WOOL,
                GRAY_WOOL,
                LIGHT_GRAY_WOOL,
                CYAN_WOOL,
                PURPLE_WOOL,
                BLUE_WOOL,
                BROWN_WOOL,
                GREEN_WOOL,
                RED_WOOL,
                BLACK_WOOL
        };
    }

    public static int woolDyeColors(String color) {
        return switch (color) {
            case "orange" -> 14188339;
            case "magenta" -> 11685080;
            case "light_blue" -> 6724056;
            case "yellow" -> 15066419;
            case "lime" -> 8375321;
            case "pink" -> 15892389;
            case "gray" -> 5000268;
            case "light_gray" -> 10066329;
            case "cyan" -> 5013401;
            case "purple" -> 8339378;
            case "blue" -> 3361970;
            case "brown" -> 6704179;
            case "green" -> 6717235;
            case "red" -> 10040115;
            case "black" -> 1644825;
            default -> 0xFFFFFF;
        };
    }
}
