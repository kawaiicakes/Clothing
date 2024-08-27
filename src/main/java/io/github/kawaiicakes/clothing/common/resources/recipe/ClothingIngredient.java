package io.github.kawaiicakes.clothing.common.resources.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.clothing.common.resources.ClothingEntryLoader;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* TODO:
    add support for using clothing as ingredients. Unlike when making a result, users will be allowed to use
    any property they see fit. Any declarations made will overwrite the data from the entry they declared. Users will
    also be able to use the "default" entries. The default entries are just concrete entries for the default itemstacks
 */
public class ClothingIngredient extends AbstractIngredient {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected ClothingIngredient(ResourceLocation entry, CompoundTag tag, String... partialMatchKeys) {
        super(Stream.of(new ClothingValue(entry, tag, partialMatchKeys)));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean test(@Nullable ItemStack pStack) {
        boolean superTest = super.test(pStack);
        return false;
    }

    @Override
    public @NotNull IIngredientSerializer<ClothingIngredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull JsonObject toJson() {

    }

    public static @NotNull ClothingIngredient fromJson(@Nullable JsonElement element) {
        if (!(element instanceof JsonObject json)) throw new IllegalArgumentException("Expected JsonObject!");
        if (!json.has("clothing")) throw new IllegalArgumentException("No clothing declared!");

        return new ClothingIngredient();
    }

    @MethodsReturnNonnullByDefault
    public static class ClothingValue implements Ingredient.Value {
        protected final Supplier<ItemStack> clothingEntrySupplier;
        protected ItemStack clothingEntryStack = ItemStack.EMPTY;
        protected final Set<String> partialMatchesForKey;

        public ClothingValue(ResourceLocation entry, CompoundTag comparisonNbt, String... partialMatchesForKey) {
            this.partialMatchesForKey = Arrays.stream(partialMatchesForKey).collect(Collectors.toSet());

            // A Supplier is used as we cannot query clothing entries from its loader until after recipes are done loading
            this.clothingEntrySupplier = () -> {
                try {
                    CompoundTag toReturn = ClothingEntryLoader.getInstance().getStack(entry).copy().serializeNBT();

                    CompoundTag returnTag = toReturn.contains("tag", Tag.TAG_COMPOUND)
                            ? toReturn.getCompound("tag")
                            : null;

                    for (String tagKey : comparisonNbt.getAllKeys()) {
                        Tag tagForKey = comparisonNbt.get(tagKey);
                        assert toReturn.getTag() != null;

                        if (this.partialMatchesForKey.contains(tagKey)) {
                            continue;
                        }

                        assert tagForKey != null;
                        returnTag.put(tagKey, tagForKey);
                    }

                    return ItemStack.of(toReturn);
                } catch (Exception e) {
                    LOGGER.error("Unable to query clothing stack from loader!", e);
                    return ItemStack.EMPTY;
                }
            };
        }

        @Override
        public Collection<ItemStack> getItems() {
            if (this.clothingEntryStack.equals(ItemStack.EMPTY))
                this.clothingEntryStack = this.clothingEntrySupplier.get();

            return Collections.singleton(this.clothingEntryStack);
        }

        @Override
        public JsonObject serialize() {
            JsonObject toReturn = new JsonObject();

            toReturn.addProperty();

            return toReturn;
        }
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Serializer implements IIngredientSerializer<ClothingIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ClothingIngredient parse(FriendlyByteBuf buffer) {
            return null;
        }

        @Override
        public ClothingIngredient parse(JsonObject json) {
            return null;
        }

        @Override
        public void write(FriendlyByteBuf buffer, ClothingIngredient ingredient) {

        }
    }
}
