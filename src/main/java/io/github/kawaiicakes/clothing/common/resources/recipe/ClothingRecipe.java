package io.github.kawaiicakes.clothing.common.resources.recipe;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.item.ClothingItem;
import io.github.kawaiicakes.clothing.common.resources.BakedClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import io.github.kawaiicakes.clothing.common.resources.GenericClothingEntryLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.github.kawaiicakes.clothing.common.item.ClothingItem.*;
import static io.github.kawaiicakes.clothing.common.item.impl.BakedModelClothingItem.MODEL_PARENTS_KEY;
import static io.github.kawaiicakes.clothing.common.item.impl.GenericClothingItem.*;
import static io.github.kawaiicakes.clothing.ClothingRegistry.CLOTHING_SERIALIZER;
import static net.minecraft.world.item.DyeableLeatherItem.TAG_COLOR;

/*
    TODO: add support for using clothing as ingredients. Unlike when making a result, users will be allowed to use
    any property they see fit. Any declarations made will overwrite the data from the entry they declared. Users will
    also be able to use the "default" entries. The default entries are just concrete entries for the default itemstacks
 */
public class ClothingRecipe extends ShapedRecipe {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final Supplier<ItemStack> resultSupplier;
    protected ItemStack suppliedResult;

    public ClothingRecipe(
            ResourceLocation id, String group,
            int width, int height,
            NonNullList<Ingredient> ingredients, Supplier<ItemStack> result
    ) {
        super(id, group, width, height, ingredients, ItemStack.EMPTY);
        this.resultSupplier = result;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return this.suppliedResult != null
                ? this.suppliedResult
                : (this.suppliedResult = this.resultSupplier.get().copy());
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

            Supplier<ItemStack> result
                    = () -> getItemStack(GsonHelper.getAsJsonObject(pSerializedRecipe, "result"));

            return new ClothingRecipe(pRecipeId, group, width, height, ingredients, result);
        }

        @Override
        @ParametersAreNonnullByDefault
        public void toNetwork(FriendlyByteBuf pBuffer, ShapedRecipe pRecipe) {
            pBuffer.writeVarInt(pRecipe.getWidth());
            pBuffer.writeVarInt(pRecipe.getHeight());
            pBuffer.writeUtf(pRecipe.getGroup());

            for(Ingredient ingredient : pRecipe.getIngredients()) {
                ingredient.toNetwork(pBuffer);
            }

            pBuffer.writeItem(pRecipe.getResultItem());
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

            CompoundTag mergedProperties = new CompoundTag();
            if (defaultStackForEntry.getItem() instanceof ClothingItem<?> clothingItem)
                mergedProperties = clothingItem.getClothingPropertyTag(defaultStackForEntry);

            String[] pissKeys = forbiddenKeys();

            for (String key : miscNbt.getAllKeys()) {
                if (key.equals(CLOTHING_PROPERTY_NBT_KEY)) {
                    for (String propertyKey : miscNbt.getCompound(key).getAllKeys()) {
                        if (Arrays.asList(pissKeys).contains(propertyKey)) continue;

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

            CompoundTag toReturn = new CompoundTag();

            int color;
            CompoundTag attributes;
            String equipSound;
            int maxDamage;

            try {
                color = json.has(TAG_COLOR)
                        ? json.getAsJsonPrimitive(TAG_COLOR).getAsInt()
                        : mergedProperties.getInt(TAG_COLOR);

                attributes = json.has(ATTRIBUTES_KEY)
                        ? asNbt(ClothingEntryLoader.deserializeAttributes(json.getAsJsonObject(ATTRIBUTES_KEY)))
                        : mergedProperties.getCompound(ATTRIBUTES_KEY);

                equipSound = json.has(EQUIP_SOUND_KEY)
                        ? json.getAsJsonPrimitive(EQUIP_SOUND_KEY).getAsString()
                        : mergedProperties.getString(EQUIP_SOUND_KEY);

                maxDamage = json.has(MAX_DAMAGE_KEY)
                        ? json.getAsJsonPrimitive(MAX_DAMAGE_KEY).getAsInt()
                        : mergedProperties.getInt(MAX_DAMAGE_KEY);

                if (maxDamage <= 0)
                    throw new IllegalArgumentException("Recipe declares impossible durability of " + maxDamage +"!");

                if (!ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(equipSound)))
                    throw new IllegalArgumentException("No such equip sound as '" + equipSound + "'!");
            } catch (Exception e) {
                LOGGER.error("Unable to write clothing properties for recipe!");
                return toReturn;
            }

            toReturn.putInt(TAG_COLOR, color);
            toReturn.put(ATTRIBUTES_KEY, attributes);
            toReturn.putString(EQUIP_SOUND_KEY, equipSound);
            toReturn.putInt(MAX_DAMAGE_KEY, maxDamage);

            if (!isGeneric) return toReturn;

            ListTag overlays;

            try {
                overlays = json.has(OVERLAY_NBT_KEY)
                        ? overlaysFromJson(json.getAsJsonArray(OVERLAY_NBT_KEY))
                        : new ListTag();
            } catch (Exception e) {
                LOGGER.error("Unable to write clothing properties for recipe!");
                return toReturn;
            }

            toReturn.put(OVERLAY_NBT_KEY, overlays);

            return toReturn;
        }

        public static ListTag overlaysFromJson(JsonArray array) {
            ListTag toReturn = new ListTag();
            for (JsonElement element : array) {
                toReturn.add(StringTag.valueOf(element.getAsJsonPrimitive().getAsString()));
            }
            return toReturn;
        }

        @NotNull
        public static CompoundTag asNbt(ImmutableMultimap<Attribute, AttributeModifier> attributes) {
            CompoundTag toReturn = new CompoundTag();

            for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : attributes.asMap().entrySet()) {
                Collection<CompoundTag> valueAsCompounds = entry.getValue()
                        .stream()
                        .map(AttributeModifier::save)
                        .toList();

                ResourceLocation attributeKey = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
                if (attributeKey == null) throw new IllegalArgumentException(
                        "Passed map contains unknown attribute '" + entry.getKey() + "'!"
                );

                ListTag valueListTag = new ListTag();
                valueListTag.addAll(valueAsCompounds);

                toReturn.put(attributeKey.toString(), valueListTag);
            }

            return toReturn;
        }

        public static String[] forbiddenKeys() {
            return new String[] {
                    CLOTHING_SLOT_NBT_KEY,
                    CLOTHING_NAME_KEY,
                    MODEL_LAYER_NBT_KEY,
                    TEXTURE_LOCATION_NBT_KEY,
                    PART_VISIBILITY_KEY,
                    MODEL_PARENTS_KEY,
                    CLOTHING_LORE_NBT_KEY
            };
        }
    }
}
