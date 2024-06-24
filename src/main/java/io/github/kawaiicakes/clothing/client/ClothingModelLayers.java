package io.github.kawaiicakes.clothing.client;

import io.github.kawaiicakes.clothing.ClothingMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClothingModelLayers {
    public static final ModelLayerLocation ARMOR_STAND_BASE_GENERIC = registerBaseGeneric("armor_stand");
    public static final ModelLayerLocation ARMOR_STAND_THICK_GENERIC = registerThickGeneric("armor_stand");
    public static final ModelLayerLocation ARMOR_STAND_OVER_GENERIC = registerOverGeneric("armor_stand");
    public static final ModelLayerLocation DROWNED_BASE_GENERIC = registerBaseGeneric("drowned");
    public static final ModelLayerLocation DROWNED_THICK_GENERIC = registerThickGeneric("drowned");
    public static final ModelLayerLocation DROWNED_OVER_GENERIC = registerOverGeneric("drowned");
    public static final ModelLayerLocation GIANT_BASE_GENERIC = registerBaseGeneric("giant");
    public static final ModelLayerLocation GIANT_THICK_GENERIC = registerThickGeneric("giant");
    public static final ModelLayerLocation GIANT_OVER_GENERIC = registerOverGeneric("giant");
    public static final ModelLayerLocation HUSK_BASE_GENERIC = registerBaseGeneric("husk");
    public static final ModelLayerLocation HUSK_THICK_GENERIC = registerThickGeneric("husk");
    public static final ModelLayerLocation HUSK_OVER_GENERIC = registerOverGeneric("husk");

    private static ModelLayerLocation registerBaseGeneric(String pPath) {
        return register(pPath, "base_generic");
    }
    private static ModelLayerLocation registerThickGeneric(String pPath) {
        return register(pPath, "thick_generic");
    }
    private static ModelLayerLocation registerOverGeneric(String pPath) {
        return register(pPath, "over_generic");
    }
    private static ModelLayerLocation register(String pPath, String pModel) {
        return new ModelLayerLocation(new ResourceLocation(ClothingMod.MOD_ID, pPath), pModel);
    }
}
