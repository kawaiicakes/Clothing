package io.github.kawaiicakes.clothing.common.fluid;

import io.github.kawaiicakes.clothing.ClothingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static io.github.kawaiicakes.clothing.ClothingMod.MOD_ID;
import static io.github.kawaiicakes.clothing.ClothingRegistry.*;

public abstract class BleachFluid extends WaterFluid {
    public static final FluidType TYPE = new FluidType(
            FluidType.Properties.create()
                    .descriptionId("block.clothing.bleach")
                    .fallDistanceModifier(0F)
                    .canExtinguish(true)
                    .canConvertToSource(false)
                    .supportsBoating(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                    .canHydrate(false)
    ) {
        @Override
        public @Nullable BlockPathTypes getBlockPathType(
                FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog
        ) {
            return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                private static final ResourceLocation
                        UNDERWATER_LOCATION = new ResourceLocation("textures/misc/underwater.png"),
                        BLEACH_STILL = new ResourceLocation("block/water_still"),
                        BLEACH_FLOW = new ResourceLocation("block/water_flow"),
                        BLEACH_OVERLAY = new ResourceLocation("block/water_overlay");

                @Override
                public ResourceLocation getStillTexture() {
                    return BLEACH_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return BLEACH_FLOW;
                }

                @Override
                public @NotNull ResourceLocation getOverlayTexture() {
                    return BLEACH_OVERLAY;
                }

                @Override
                public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                    return UNDERWATER_LOCATION;
                }

                @Override
                public int getTintColor() {
                    return 0xFFE2FABE;
                }

                @Override
                public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                    return this.getTintColor();
                }
            });
        }
    };

    public static TagKey<Fluid> BLEACH_TAG = TagKey.create(
            ForgeRegistries.Keys.FLUIDS, new ResourceLocation(MOD_ID, "bleach")
    );

    public static TagKey<Fluid> FORGE_BLEACH_TAG = TagKey.create(
            ForgeRegistries.Keys.FLUIDS, new ResourceLocation("forge", "sodium_hypochlorite")
    );

    @Override
    public @NotNull Fluid getSource() {
        return BLEACH_FLUID.get();
    }

    @Override
    public @NotNull Fluid getFlowing() {
        return BLEACH_FLUID_FLOWING.get();
    }

    @Override
    public @NotNull Item getBucket() {
        return BLEACH_BUCKET.get();
    }

    @Override
    public @NotNull FluidType getFluidType() {
        return BLEACH_FLUID_TYPE.get();
    }

    @Nullable
    @Override
    public ParticleOptions getDripParticle() {
        return DRIPPING_BLEACH.get();
    }

    @SuppressWarnings("deprecation")
    @Override
    @ParametersAreNonnullByDefault
    public boolean canBeReplacedWith(
            FluidState pFluidState, BlockGetter pBlockReader, BlockPos pPos, Fluid pFluid, Direction pDirection
    ) {
        return pDirection == Direction.DOWN && !pFluid.is(BLEACH_TAG) && !pFluid.is(FORGE_BLEACH_TAG);
    }

    @Override
    public @NotNull BlockState createLegacyBlock(@NotNull FluidState pState) {
        return ClothingRegistry.LEGACY_BLEACH_BLOCK.get()
                .defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(pState));
    }

    @Override
    protected boolean canConvertToSource() {
        return false;
    }

    @Override
    public boolean isSame(@NotNull Fluid pFluid) {
        return pFluid == ClothingRegistry.BLEACH_FLUID.get() || pFluid == ClothingRegistry.BLEACH_FLUID_FLOWING.get();
    }

    public static class Source extends BleachFluid {
        @Override
        public boolean isSource(@NotNull FluidState pState) {
            return true;
        }

        @Override
        public int getAmount(@NotNull FluidState pState) {
            return 8;
        }
    }

    public static class Flowing extends BleachFluid {
        @Override
        protected void createFluidStateDefinition(@NotNull StateDefinition.Builder<Fluid, FluidState> pBuilder) {
            super.createFluidStateDefinition(pBuilder);
            pBuilder.add(LEVEL);
        }

        @Override
        public boolean isSource(@NotNull FluidState pState) {
            return false;
        }

        @Override
        public int getAmount(@NotNull FluidState pState) {
            return pState.getValue(LEVEL);
        }
    }
}
