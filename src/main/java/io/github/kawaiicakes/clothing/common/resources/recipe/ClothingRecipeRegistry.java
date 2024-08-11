package io.github.kawaiicakes.clothing.common.resources.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingRecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTRY
            = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    public static final RegistryObject<ClothingRecipe.Serializer> CLOTHING_SERIALIZER
            = SERIALIZER_REGISTRY.register("clothing_recipe", ClothingRecipe.Serializer::new);
}
