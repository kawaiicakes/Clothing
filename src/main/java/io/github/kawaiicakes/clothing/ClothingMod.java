package io.github.kawaiicakes.clothing;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.function.Consumer;

@Mod(ClothingMod.MOD_ID)
public class ClothingMod
{
    public static final String MOD_ID = "clothing";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ARMOR_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<ArmorItem> TEST_ARMOR = ARMOR_REGISTRY.register(
            "test",
            () -> new ArmorItem(
                    ArmorMaterials.NETHERITE,
                    EquipmentSlot.CHEST,
                    new Item.Properties()
            ) {
                @Override
                public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
                    consumer.accept(
                            new IClientItemExtensions() {
                                @Override
                                public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                                    LOGGER.info("TESTING TO SEE IF THIS OVERRIDE WORKS");
                                    return IClientItemExtensions.super.getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
                          }
                      });
                }
            }
    );

    public ClothingMod()
    {
        ARMOR_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static class ClientEvents {

    }
}
