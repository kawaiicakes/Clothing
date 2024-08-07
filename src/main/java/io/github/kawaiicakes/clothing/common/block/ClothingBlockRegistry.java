package io.github.kawaiicakes.clothing.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingBlockRegistry {
    public static final DeferredRegister<Block> CLOTHING_BLOCK_REGISTRY
            = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<TextileLoomBlock> TEXTILE_LOOM_BLOCK
            = CLOTHING_BLOCK_REGISTRY.register("textile_loom", TextileLoomBlock::new);
}
