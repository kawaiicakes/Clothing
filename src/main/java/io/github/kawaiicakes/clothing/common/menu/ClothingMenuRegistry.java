package io.github.kawaiicakes.clothing.common.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;

public class ClothingMenuRegistry {
    public static final DeferredRegister<MenuType<?>> CLOTHING_MENU_REGISTRY
            = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<MenuType<TextileLoomMenu>> TEXTILE_LOOM_MENU = CLOTHING_MENU_REGISTRY.register(
            "textile_loom",
            () -> new MenuType<>(TextileLoomMenu::new)
    );
}
