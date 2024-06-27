package io.github.kawaiicakes.clothing;

import io.github.kawaiicakes.clothing.item.ClothingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ClothingMod.MOD_ID)
public class ClothingMod
{
    public static final String MOD_ID = "clothing";

    public static final DeferredRegister<Item> ARMOR_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<ClothingItem> TEST_ARMOR = ARMOR_REGISTRY.register(
            "test",
            () -> new ClothingItem(
                    ArmorMaterials.NETHERITE,
                    EquipmentSlot.CHEST,
                    new Item.Properties()
            ) {
                @Override
                public HumanoidModel<?> getClothingModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> originalModel) {
                    return originalModel;
                }
            }
    );

    public ClothingMod()
    {
        ARMOR_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
